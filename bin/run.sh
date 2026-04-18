#!/usr/bin/env sh
set -eu

ROOT_DIR="/data/app/airflow/damo"
OUT_DIR="/data/app/airflow/damo/out"
JAR="/data/app/airflow/damo/scpdb.jar"

# classpath: 클래스 출력 + (선택) libs + scpdb.jar
CP="$OUT_DIR:/data/app/airflow/damo/libs/*:$JAR"
if [ -n "${EXTRA_JARS:-}" ]; then
  CP="$CP:$EXTRA_JARS"
fi

# export PORT=8081
# export DAMO_CLASS=com.penta.scpdb.ScpDbAgent
# export DAMO_CONF_PATH=/data/app/airflow/damo/scp.ini
# export DAMO_GROUP=KEY1

exec java -cp "$CP" DamoHttpApi
