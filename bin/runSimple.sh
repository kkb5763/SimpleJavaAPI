#!/usr/bin/env sh
set -eu

ROOT_DIR="/data/app/airflow/damo"
OUT_DIR="/data/app/airflow/damo/out"
SRC="/data/app/airflow/damo/src/SimpleUtilHttpApi.java"

# compile 먼저 (out/ 갱신)
mkdir -p "$OUT_DIR"
javac -encoding UTF-8 -d "$OUT_DIR" "$SRC"

# 기본 PORT=8082 (원하면 export PORT=... 로 변경)
exec java -cp "$OUT_DIR" SimpleUtilHttpApi

