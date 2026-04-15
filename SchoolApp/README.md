# SchoolApp — демонстрация политики защиты

Java Swing приложение к лабораторной работе
«Применение политики защиты» по дисциплине
«Администрирование баз данных».

## Стек

- Java 21 (Swing)
- SQLite (через `sqlite-jdbc`)

## Структура

```
SchoolApp/
├── db/
│   ├── init.sql          # схема + тестовые данные
│   └── school.db         # создаётся из init.sql
├── lib/
│   └── sqlite-jdbc.jar   # JDBC-драйвер (скачивается при сборке)
├── src/
│   └── SchoolApp.java    # исходный код
├── screenshots/          # PNG с экранами для каждой роли
└── run_shots.sh          # Xvfb + запуск приложения + снимки
```

## Сборка и запуск

```bash
cd SchoolApp
# 1. (однократно) скачать JDBC-драйвер
mkdir -p lib
curl -L -o lib/sqlite-jdbc.jar \
    https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar

# 2. Создать БД
sqlite3 db/school.db < db/init.sql

# 3. Скомпилировать
javac -encoding UTF-8 -d build src/SchoolApp.java

# 4. Запустить
java -cp "build:lib/sqlite-jdbc.jar" SchoolApp
```

## Тестовые пользователи

| Логин        | Пароль   | Роль     |
|--------------|----------|----------|
| `ivanov_dir` | `pwd_dir`  | Директор |
| `petrov_t`   | `pwd_tch`  | Учитель (кл.рук. 9А) |
| `smirnova_t` | `pwd_tch2` | Учитель (кл.рук. 9Б) |
| `sidorov_s`  | `pwd_stud` | Учащийся |
| `kuzmina_med`| `pwd_med`  | Фельдшер |

## Политика защиты (реализована на уровне приложения)

| Таблица / роль    | Директор | Учитель          | Учащийся              | Фельдшер |
|-------------------|:--------:|:----------------:|:---------------------:|:--------:|
| students          |  R/W     | R (свои классы)  | R (только сам)        |   R      |
| parents           |  R/W     | R (свои классы)  |         —             |   —      |
| teachers          |  R/W     | R                | R                     |   —      |
| subjects, classes |  R/W     | R                | R                     |   —      |
| grades            |  R/W     | R/W (свои классы)| R (свои)              |   —      |
| medical\_records  |  R/W     |        —         |         —             |  R/W     |
| users             |  R/W     |        —         |         —             |   —      |

Горизонтальная фильтрация (row-level security) для учителя и учащегося
реализована в SQL-запросах методов `panel*` класса `MainFrame`.

## Автоматизированный снимок экрана

```bash
./run_shots.sh
ls screenshots/
```
