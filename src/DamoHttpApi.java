import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * GET {@code /damo/enc/<평문>} — 암호문만, GET {@code /damo/dec/<암호문>} — 평문만 ({@code text/plain}).
 * URL 경로는 {@link URLDecoder}(UTF-8).
 * <p>
 * 환경변수: {@code PORT}(기본 8081), {@code DAMO_CLASS}, {@code DAMO_CONF_PATH}, {@code DAMO_GROUP},
 * {@code DAMO_ENCRYPT_METHOD}(기본 scpEncrypt), {@code DAMO_DECRYPT_METHOD}(기본 scpDecrypt), 선택 {@code DAMO_KEY}
 */
public class DamoHttpApi {

  public static void main(String[] args) throws Exception {
    int port = port();
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.setExecutor(null);
    server.createContext("/damo/enc/", new EncHandler());
    server.createContext("/damo/enc", badPath("use /damo/enc/<plain>"));
    server.createContext("/damo/dec/", new DecHandler());
    server.createContext("/damo/dec", badPath("use /damo/dec/<cipher>"));
    System.out.println(
        "Listening " + port + "  GET /damo/enc/<plain>  GET /damo/dec/<cipher> (URL-encoded UTF-8)");
    server.start();
  }

  private static HttpHandler badPath(String msg) {
    return exchange -> {
      byte[] b = msg.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
      exchange.sendResponseHeaders(400, b.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(b);
      }
    };
  }

  static final class EncHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
        respond(ex, 405, "GET only\n");
        return;
      }
      URI uri = ex.getRequestURI();
      String path = uri.getPath();
      final String prefix = "/damo/enc/";
      if (!path.startsWith(prefix)) {
        respond(ex, 404, "not found\n");
        return;
      }
      String tail = path.substring(prefix.length());
      if (tail.isEmpty()) {
        respond(ex, 400, "missing text after /damo/enc/\n");
        return;
      }
      String plain;
      try {
        plain = URLDecoder.decode(tail, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException e) {
        respond(ex, 400, "bad encoding\n");
        return;
      }
      try {
        String cipher = DamoEnc.encrypt(plain);
        byte[] body = cipher.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
          os.write(body);
        }
      } catch (Exception e) {
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        respond(ex, 500, m + "\n");
      }
    }

    static void respond(HttpExchange ex, int code, String text) throws IOException {
      byte[] body = text.getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
      ex.sendResponseHeaders(code, body.length);
      try (OutputStream os = ex.getResponseBody()) {
        os.write(body);
      }
    }
  }

  static final class DecHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
        EncHandler.respond(ex, 405, "GET only\n");
        return;
      }
      URI uri = ex.getRequestURI();
      String path = uri.getPath();
      final String prefix = "/damo/dec/";
      if (!path.startsWith(prefix)) {
        EncHandler.respond(ex, 404, "not found\n");
        return;
      }
      String tail = path.substring(prefix.length());
      if (tail.isEmpty()) {
        EncHandler.respond(ex, 400, "missing text after /damo/dec/\n");
        return;
      }
      String cipher;
      try {
        cipher = URLDecoder.decode(tail, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException e) {
        EncHandler.respond(ex, 400, "bad encoding\n");
        return;
      }
      try {
        String plain = DamoEnc.decrypt(cipher);
        byte[] body = plain.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
          os.write(body);
        }
      } catch (Exception e) {
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        EncHandler.respond(ex, 500, m + "\n");
      }
    }
  }

  static final class DamoEnc {
    private static final String CLS = getenv("DAMO_CLASS", "");
    private static final String CONF = getenv("DAMO_CONF_PATH", "/data/app/airflow/damo/scp.ini");
    private static final String GROUP = getenv("DAMO_GROUP", "");
    private static final String METHOD_ENC = getenv("DAMO_ENCRYPT_METHOD", "scpEncrypt");
    private static final String METHOD_DEC = getenv("DAMO_DECRYPT_METHOD", "scpDecrypt");
    private static final String KEY = getenv("DAMO_KEY", "");
    private static volatile Object agent;

    static String encrypt(String plain) throws Exception {
      return invoke(METHOD_ENC, plain);
    }

    static String decrypt(String cipher) throws Exception {
      return invoke(METHOD_DEC, cipher);
    }

    private static String invoke(String methodName, String data) throws Exception {
      if (CLS.isEmpty()) {
        throw new IllegalStateException("Set DAMO_CLASS (e.g. com.penta.scpdb.ScpDbAgent)");
      }
      Class<?> z = Class.forName(CLS);
      String m = methodName;

      if (!CONF.isEmpty() && !GROUP.isEmpty()) {
        Method m0 = find(z, m, String.class, String.class, String.class);
        if (m0 != null) {
          Object t = Modifier.isStatic(m0.getModifiers()) ? null : agent(z);
          Object r = m0.invoke(t, CONF, GROUP, data);
          return r == null ? "" : r.toString();
        }
      }
      Method m1 = find(z, m, String.class);
      if (m1 != null) {
        Object t = Modifier.isStatic(m1.getModifiers()) ? null : agent(z);
        Object r = m1.invoke(t, data);
        return r == null ? "" : r.toString();
      }
      if (!KEY.isEmpty()) {
        Method m2 = find(z, m, String.class, String.class);
        if (m2 != null) {
          Object t = Modifier.isStatic(m2.getModifiers()) ? null : agent(z);
          Object r = m2.invoke(t, data, KEY);
          return r == null ? "" : r.toString();
        }
      }
      throw new NoSuchMethodException("no " + m + " for " + CLS);
    }

    private static Method find(Class<?> z, String n, Class<?>... p) throws NoSuchMethodException {
      try {
        Method m = z.getMethod(n, p);
        m.setAccessible(true);
        return m;
      } catch (NoSuchMethodException e) {
        try {
          Method m = z.getDeclaredMethod(n, p);
          m.setAccessible(true);
          return m;
        } catch (NoSuchMethodException e2) {
          throw e2;
        }
      }
    }

    private static Object agent(Class<?> z) throws Exception {
      if (agent != null) return agent;
      synchronized (DamoEnc.class) {
        if (agent != null) return agent;
        Constructor<?> c = z.getDeclaredConstructor();
        c.setAccessible(true);
        agent = c.newInstance();
        return agent;
      }
    }

    private static String getenv(String k, String d) {
      String v = System.getenv(k);
      if (v != null && !v.isBlank()) return v.trim();
      v = System.getProperty(k);
      if (v != null && !v.isBlank()) return v.trim();
      return d;
    }
  }

  private static int port() {
    String p = System.getenv("PORT");
    if (p != null && !p.isBlank()) {
      try {
        return Integer.parseInt(p.trim());
      } catch (NumberFormatException ignored) {
        //
      }
    }
    try {
      return Integer.parseInt(DamoEnc.getenv("PORT", "8081"));
    } catch (NumberFormatException e) {
      return 8081;
    }
  }
}
