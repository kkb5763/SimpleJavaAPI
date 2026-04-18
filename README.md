# DamoHttpApi

단일 소스 `src/DamoHttpApi.java` — **GET** `/damo/enc/<평문>` → 암호문만, **GET** `/damo/dec/<암호문>` → 평문만 (`text/plain`).

## SimpleUtilHttpApi (JDK 기본만)

`scpdb.jar` 없이도 동작하는 테스트용 서버입니다.

- 소스: `src/SimpleUtilHttpApi.java`
- 기본 포트: `8082` (`PORT`로 변경)
- `GET /mock/enc/<평문>` → `ENC_<평문>`
- `GET /base64/enc/<평문>` → Base64
- `GET /base64/dec/<base64>` → 평문

리눅스에서 실행:

```bash
cd /data/app/airflow/damo
javac -encoding UTF-8 -d /data/app/airflow/damo/out src/SimpleUtilHttpApi.java
PORT=8082 java -cp /data/app/airflow/damo/out SimpleUtilHttpApi
```

## 배치

프로젝트 전체를 **`/data/app/airflow/damo/`** 아래에 두면 `bin/build.sh`, `bin/run.sh`가 그 절대 경로를 그대로 씁니다.

## 빌드

```bash
chmod +x /data/app/airflow/damo/bin/build.sh /data/app/airflow/damo/bin/run.sh
/data/app/airflow/damo/bin/build.sh
```

## 실행 전 환경변수

| 변수 | 설명 |
|------|------|
| `PORT` | listen 포트 (기본 `8081`) |
| `DAMO_CLASS` | 예: `com.penta.scpdb.ScpDbAgent` |
| `DAMO_CONF_PATH` | `scp.ini` 등 절대 경로 (기본 `/data/app/airflow/damo/scp.ini`) |
| `DAMO_GROUP` | 암호화 그룹명 |
| `DAMO_ENCRYPT_METHOD` | 기본 `scpEncrypt` |
| `DAMO_DECRYPT_METHOD` | 기본 `scpDecrypt` |
| `DAMO_KEY` | (선택) 2인자 시그니처일 때 |

## 실행

```bash
export DAMO_CLASS=com.penta.scpdb.ScpDbAgent
export DAMO_GROUP=KEY1
export DAMO_CONF_PATH=/data/app/airflow/damo/scp.ini
/data/app/airflow/damo/bin/run.sh
```

classpath: `/data/app/airflow/damo/out`, `/data/app/airflow/damo/libs/*`, `/data/app/airflow/damo/scpdb.jar`. 추가 JAR만 `EXTRA_JARS`로 뒤에 붙입니다.

## 백그라운드 실행 (nohup)

터미널을 닫아도 프로세스가 유지되게 하려면 `nohup`으로 띄우고, 로그와 PID를 파일로 남기면 편합니다.

```bash
mkdir -p /data/app/airflow/damo/logs

export DAMO_CLASS=com.penta.scpdb.ScpDbAgent
export DAMO_GROUP=KEY1
export DAMO_CONF_PATH=/data/app/airflow/damo/scp.ini

nohup /data/app/airflow/damo/bin/run.sh \
  >> /data/app/airflow/damo/logs/damo-http-api.log 2>&1 &

echo $! > /data/app/airflow/damo/logs/damo-http-api.pid
```

- **로그 보기**: `tail -f /data/app/airflow/damo/logs/damo-http-api.log`
- **종료**: `kill $(cat /data/app/airflow/damo/logs/damo-http-api.pid)` — 안 내려가면 `kill -9` (최후 수단)

같은 포트로 다시 띄우기 전에 기존 프로세스가 꺼졌는지 확인하세요.

## API

- **GET** `/damo/enc/<평문>` — 평문 **URL 인코딩**(UTF-8). 응답: 암호문만.
- **GET** `/damo/dec/<암호문>` — 암호문 **URL 인코딩**. 응답: 평문만.

```bash
curl -s "http://127.0.0.1:8081/damo/enc/hello"

CIPHER=$(curl -s "http://127.0.0.1:8081/damo/enc/hello")
E=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$CIPHER")
curl -s "http://127.0.0.1:8081/damo/dec/$E"
```

암호문 전체를 URL 인코딩해 `/damo/dec/` 뒤에 붙입니다.

- `/damo/enc`, `/damo/dec` 만 호출(경로 뒤 없음): `400`
