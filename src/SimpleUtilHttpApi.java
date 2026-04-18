import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JDK 기본 기능만 사용하는 간단 HTTP 유틸 API.
 *
 * - GET /base64/enc/<plain>  -> Base64
 * - GET /base64/dec/<b64>    -> plain
 * - GET /mock/enc/<plain>    -> ENC_<plain>
 *
 * 평문/값은 URL 인코딩(UTF-8)해서 경로에 넣으세요.
 * 포트는 환경변수 PORT, 기본값 8082.
 */
public class SimpleUtilHttpApi {
  public static void main(String[] args) throws Exception {
    int port = port();
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.setExecutor(null);

    server.createContext("/base64/enc/", new Base64EncHandler());
    server.createContext("/base64/dec/", new Base64DecHandler());
    server.createContext("/mock/enc/", new MockEncHandler());

    System.out.println("Listening " + port
        + "  GET /base64/enc/<plain>  GET /base64/dec/<b64>  GET /mock/enc/<plain>");
    server.start();
  }

  static final class Base64EncHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
        respond(ex, 405, "GET only\n");
        return;
      }
      String plain = tail(ex.getRequestURI(), "/base64/enc/");
      if (plain == null) {
        respond(ex, 400, "use /base64/enc/<plain>\n");
        return;
      }
      String b64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
      respond(ex, 200, b64);
    }
  }

  static final class Base64DecHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
        respond(ex, 405, "GET only\n");
        return;
      }
      String b64 = tail(ex.getRequestURI(), "/base64/dec/");
      if (b64 == null) {
        respond(ex, 400, "use /base64/dec/<b64>\n");
        return;
      }
      try {
        byte[] decoded = Base64.getDecoder().decode(b64);
        respond(ex, 200, new String(decoded, StandardCharsets.UTF_8));
      } catch (IllegalArgumentException e) {
        respond(ex, 400, "invalid base64\n");
      }
    }
  }

  static final class MockEncHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
        respond(ex, 405, "GET only\n");
        return;
      }
      String plain = tail(ex.getRequestURI(), "/mock/enc/");
      if (plain == null) {
        respond(ex, 400, "use /mock/enc/<plain>\n");
        return;
      }
      respond(ex, 200, "ENC_" + plain);
    }
  }

  private static String tail(URI uri, String prefix) {
    String path = uri.getPath();
    if (!path.startsWith(prefix)) return null;
    String t = path.substring(prefix.length());
    if (t.isEmpty()) return null;
    try {
      return URLDecoder.decode(t, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static void respond(HttpExchange ex, int code, String text) throws IOException {
    byte[] body = text.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
    ex.sendResponseHeaders(code, body.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(body);
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
    return 8082;
  }
}

