#!/usr/bin/env sh
set -eu

ROOT_DIR="/data/app/airflow/damo"
OUT_DIR="/data/app/airflow/damo/out"
SRC="/data/app/airflow/damo/src/DamoHttpApi.java"

mkdir -p "$OUT_DIR"
echo "javac -> $OUT_DIR"
javac -encoding UTF-8 -d "$OUT_DIR" "$SRC"
echo "Done."
