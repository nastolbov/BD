-- Инициализация БД "School" для лабораторной работы «Политика защиты»

PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS medical_records;
DROP TABLE IF EXISTS parents;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    login    TEXT PRIMARY KEY,
    password TEXT NOT NULL,
    role     TEXT NOT NULL CHECK (role IN ('director','teacher','student','medic')),
    full_name TEXT NOT NULL
);

CREATE TABLE teachers (
    id        INTEGER PRIMARY KEY,
    login     TEXT UNIQUE,
    last_name TEXT NOT NULL,
    first_name TEXT NOT NULL,
    subject_speciality TEXT
);

CREATE TABLE classes (
    id         INTEGER PRIMARY KEY,
    name       TEXT NOT NULL,
    teacher_id INTEGER REFERENCES teachers(id)
);

CREATE TABLE subjects (
    id   INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE students (
    id         INTEGER PRIMARY KEY,
    login      TEXT UNIQUE,
    last_name  TEXT NOT NULL,
    first_name TEXT NOT NULL,
    class_id   INTEGER REFERENCES classes(id),
    birth_date TEXT
);

CREATE TABLE parents (
    id         INTEGER PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES students(id),
    last_name  TEXT NOT NULL,
    first_name TEXT NOT NULL,
    phone      TEXT
);

CREATE TABLE grades (
    id         INTEGER PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES students(id),
    subject_id INTEGER NOT NULL REFERENCES subjects(id),
    grade      INTEGER NOT NULL CHECK (grade BETWEEN 2 AND 5),
    date       TEXT NOT NULL
);

CREATE TABLE medical_records (
    id         INTEGER PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES students(id),
    diagnosis  TEXT NOT NULL,
    date       TEXT NOT NULL
);

-- ------------------------------------------------------------------
-- Тестовые данные
-- ------------------------------------------------------------------

INSERT INTO users(login,password,role,full_name) VALUES
    ('ivanov_dir',  'pwd_dir',  'director', 'Иванов И. И.'),
    ('petrov_t',    'pwd_tch',  'teacher',  'Петров П. П.'),
    ('smirnova_t',  'pwd_tch2', 'teacher',  'Смирнова А. В.'),
    ('sidorov_s',   'pwd_stud', 'student',  'Сидоров С. С.'),
    ('kozlov_s',    'pwd_stud', 'student',  'Козлов К. К.'),
    ('orlova_s',    'pwd_stud', 'student',  'Орлова О. О.'),
    ('kuzmina_med', 'pwd_med',  'medic',    'Кузьмина Е. Н.');

INSERT INTO teachers(id,login,last_name,first_name,subject_speciality) VALUES
    (1,'petrov_t',   'Петров',   'Пётр',   'Математика'),
    (2,'smirnova_t', 'Смирнова', 'Анна',   'Русский язык');

INSERT INTO subjects(id,name) VALUES
    (1,'Математика'),
    (2,'Русский язык'),
    (3,'Физика'),
    (4,'История');

INSERT INTO classes(id,name,teacher_id) VALUES
    (1,'9А',1),
    (2,'9Б',2);

INSERT INTO students(id,login,last_name,first_name,class_id,birth_date) VALUES
    (1,'sidorov_s','Сидоров','Сергей',1,'2009-05-12'),
    (2,'kozlov_s', 'Козлов', 'Кирилл',1,'2009-09-03'),
    (3,'orlova_s', 'Орлова', 'Ольга', 2,'2009-02-28'),
    (4, NULL,      'Новиков','Николай',2,'2009-11-17');

INSERT INTO parents(id,student_id,last_name,first_name,phone) VALUES
    (1,1,'Сидоров', 'Семён',   '+7-900-111-11-11'),
    (2,1,'Сидорова','Светлана','+7-900-111-11-22'),
    (3,2,'Козлов',  'Константин','+7-900-222-22-11'),
    (4,3,'Орлова',  'Оксана',  '+7-900-333-33-11'),
    (5,4,'Новиков', 'Николай', '+7-900-444-44-11');

INSERT INTO grades(id,student_id,subject_id,grade,date) VALUES
    (1,1,1,5,'2026-04-01'),
    (2,1,2,4,'2026-04-02'),
    (3,1,3,5,'2026-04-05'),
    (4,2,1,3,'2026-04-01'),
    (5,2,2,4,'2026-04-02'),
    (6,3,1,5,'2026-04-01'),
    (7,3,4,5,'2026-04-03'),
    (8,4,1,4,'2026-04-01'),
    (9,4,2,3,'2026-04-02');

INSERT INTO medical_records(id,student_id,diagnosis,date) VALUES
    (1,1,'Здоров',            '2026-01-12'),
    (2,2,'ОРВИ, выздоровел',  '2026-02-05'),
    (3,3,'Аллергия на пыльцу','2026-03-10'),
    (4,4,'Здоров',            '2026-01-18');
