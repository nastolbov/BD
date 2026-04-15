#!/usr/bin/env bash
# Одношаговая подготовка проекта: скачивает JDBC-драйвер,
# создаёт БД из init.sql и компилирует Java-код.
set -e
cd "$(dirname "$0")"

JDBC_VERSION=3.46.1.3
JDBC_URL="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/${JDBC_VERSION}/sqlite-jdbc-${JDBC_VERSION}.jar"

mkdir -p lib build db

if [ ! -s lib/sqlite-jdbc.jar ]; then
    echo "==> Скачиваем sqlite-jdbc ${JDBC_VERSION}..."
    curl -fL -o lib/sqlite-jdbc.jar "$JDBC_URL"
else
    echo "==> sqlite-jdbc.jar уже скачан"
fi

if [ ! -s db/school.db ]; then
    if command -v sqlite3 >/dev/null 2>&1; then
        echo "==> Создаём db/school.db через sqlite3 ..."
        sqlite3 db/school.db < db/init.sql
    else
        echo "==> sqlite3 CLI не найден, создаём БД средствами Java ..."
        # создаст пустой файл — драйвер сам проинициализирует
        : > db/school.db
    fi
else
    echo "==> db/school.db уже существует"
fi

echo "==> Компилируем исходники ..."
javac -encoding UTF-8 -d build src/SchoolApp.java

echo
echo "Готово.  Запуск:"
echo "   java -cp \"build:lib/sqlite-jdbc.jar\" SchoolApp"
