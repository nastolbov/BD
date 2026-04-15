import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Лабораторная работа «Политика защиты».
 *
 * Десктопное приложение (Swing) для БД общеобразовательной школы.
 * SQLite используется как хранилище; ролевой доступ обеспечивается
 * на уровне приложения в соответствии со спецификацией:
 *
 *   director : полные права;
 *   teacher  : оценки учеников его классов + справочники + родители;
 *   student  : только своя запись + общая доска успеваемости + свои оценки;
 *   medic    : медицинские карты всех учеников.
 */
public class SchoolApp {

    static final String DB_URL =
        "jdbc:sqlite:" + System.getProperty("user.dir") + "/db/school.db";

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        // Явная регистрация JDBC-драйвера — даёт понятную диагностику,
        // если sqlite-jdbc.jar отсутствует в classpath.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            String msg =
                "Не найден JDBC-драйвер SQLite (org.sqlite.JDBC).\n\n" +
                "Скачайте его в папку lib/ и добавьте в classpath:\n\n" +
                "  mkdir -p lib && curl -L -o lib/sqlite-jdbc.jar \\\n" +
                "    https://repo1.maven.org/maven2/org/xerial/" +
                "sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar\n\n" +
                "Запуск:\n" +
                "  java -cp \"build:lib/sqlite-jdbc.jar\" SchoolApp";
            System.err.println(msg);
            JOptionPane.showMessageDialog(null, msg,
                "Ошибка конфигурации", JOptionPane.ERROR_MESSAGE);
            System.exit(2);
        }

        // Проверяем, что файл БД существует.
        java.io.File dbFile = new java.io.File(
            System.getProperty("user.dir"), "db/school.db");
        if (!dbFile.exists()) {
            String msg =
                "Файл БД не найден:\n" + dbFile.getAbsolutePath() +
                "\n\nСоздайте его командой:\n" +
                "  sqlite3 db/school.db < db/init.sql";
            JOptionPane.showMessageDialog(null, msg,
                "Ошибка конфигурации", JOptionPane.ERROR_MESSAGE);
            System.exit(3);
        }

        // Поддержка «screenshot» режима — автозапуск с готовым логином.
        String autoLogin = System.getProperty("app.autologin", "");
        SwingUtilities.invokeLater(() -> {
            if (!autoLogin.isEmpty()) {
                try (Connection c = DriverManager.getConnection(DB_URL)) {
                    User u = User.load(c, autoLogin);
                    if (u != null) {
                        new MainFrame(u).setVisible(true);
                        return;
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            new LoginDialog().setVisible(true);
        });
    }

    // -----------------------------------------------------------------
    //                         USER MODEL
    // -----------------------------------------------------------------
    static class User {
        String login;
        String role;
        String fullName;
        Integer studentId;      // для роли student
        Integer teacherId;      // для роли teacher

        static User load(Connection c, String login) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT login, role, full_name FROM users WHERE login=?")) {
                ps.setString(1, login);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    User u = new User();
                    u.login    = rs.getString(1);
                    u.role     = rs.getString(2);
                    u.fullName = rs.getString(3);
                    return resolveRefs(c, u);
                }
            }
        }

        static User authenticate(Connection c, String login, String password)
                throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT login, role, full_name FROM users " +
                    "WHERE login=? AND password=?")) {
                ps.setString(1, login);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    User u = new User();
                    u.login    = rs.getString(1);
                    u.role     = rs.getString(2);
                    u.fullName = rs.getString(3);
                    return resolveRefs(c, u);
                }
            }
        }

        private static User resolveRefs(Connection c, User u) throws SQLException {
            if ("student".equals(u.role)) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM students WHERE login=?")) {
                    ps.setString(1, u.login);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) u.studentId = rs.getInt(1);
                    }
                }
            } else if ("teacher".equals(u.role)) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM teachers WHERE login=?")) {
                    ps.setString(1, u.login);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) u.teacherId = rs.getInt(1);
                    }
                }
            }
            return u;
        }

        String roleRus() {
            switch (role) {
                case "director": return "Директор";
                case "teacher":  return "Учитель";
                case "student":  return "Учащийся";
                case "medic":    return "Фельдшер";
                default:         return role;
            }
        }
    }

    // -----------------------------------------------------------------
    //                         LOGIN DIALOG
    // -----------------------------------------------------------------
    static class LoginDialog extends JFrame {
        LoginDialog() {
            super("Школьная ИС — Вход в систему");
            setDefaultCloseOperation(EXIT_ON_CLOSE);

            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new EmptyBorder(24, 28, 24, 28));

            JLabel title = new JLabel(
                "Информационная система школы", SwingConstants.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            title.setBorder(new EmptyBorder(0, 0, 12, 0));
            root.add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(8, 8, 8, 8);
            g.anchor = GridBagConstraints.WEST;
            g.fill   = GridBagConstraints.HORIZONTAL;

            JLabel l1 = new JLabel("Логин:");
            JLabel l2 = new JLabel("Пароль:");
            l1.setFont(l1.getFont().deriveFont(14f));
            l2.setFont(l2.getFont().deriveFont(14f));

            JTextField tfLogin = new JTextField(20);
            JPasswordField tfPass = new JPasswordField(20);
            tfLogin.setFont(tfLogin.getFont().deriveFont(14f));
            tfPass.setFont(tfPass.getFont().deriveFont(14f));
            // Гарантируем, что поле не «схлопнется» под pack():
            Dimension fieldSize = new Dimension(260, 30);
            tfLogin.setPreferredSize(fieldSize);
            tfPass.setPreferredSize(fieldSize);

            g.gridx=0; g.gridy=0; g.weightx=0;               form.add(l1,      g);
            g.gridx=1; g.gridy=0; g.weightx=1;               form.add(tfLogin, g);
            g.gridx=0; g.gridy=1; g.weightx=0;               form.add(l2,      g);
            g.gridx=1; g.gridy=1; g.weightx=1;               form.add(tfPass,  g);

            JLabel info = new JLabel(
                "<html><div style='font-size:11px;color:#555;'>" +
                "<b>Тестовые учётные записи:</b><br>" +
                "ivanov_dir / pwd_dir&nbsp;&nbsp;(директор)<br>" +
                "petrov_t / pwd_tch&nbsp;&nbsp;(учитель)<br>" +
                "sidorov_s / pwd_stud&nbsp;&nbsp;(учащийся)<br>" +
                "kuzmina_med / pwd_med&nbsp;&nbsp;(фельдшер)" +
                "</div></html>");
            g.gridx=0; g.gridy=2; g.gridwidth=2; g.weightx=1;
            g.insets = new Insets(16, 8, 8, 8);
            form.add(info, g);

            root.add(form, BorderLayout.CENTER);

            JButton btn = new JButton("Войти");
            btn.setFont(btn.getFont().deriveFont(Font.BOLD, 14f));
            btn.setPreferredSize(new Dimension(140, 34));
            btn.addActionListener(e -> {
                try (Connection c = DriverManager.getConnection(DB_URL)) {
                    User u = User.authenticate(c,
                        tfLogin.getText().trim(),
                        new String(tfPass.getPassword()));
                    if (u == null) {
                        JOptionPane.showMessageDialog(this,
                            "Неверный логин или пароль",
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    dispose();
                    new MainFrame(u).setVisible(true);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this,
                        "Ошибка БД: " + ex.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            getRootPane().setDefaultButton(btn);
            JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
            south.setBorder(new EmptyBorder(8, 0, 0, 0));
            south.add(btn);
            root.add(south, BorderLayout.SOUTH);
            setContentPane(root);

            pack();
            setMinimumSize(new Dimension(460, getHeight()));
            setLocationRelativeTo(null);
        }
    }

    // -----------------------------------------------------------------
    //                         MAIN FRAME
    // -----------------------------------------------------------------
    static class MainFrame extends JFrame {
        final User user;
        final Connection conn;

        MainFrame(User user) {
            super("Школьная ИС — " + user.fullName + " [" + user.roleRus() + "]");
            this.user = user;
            try {
                this.conn = DriverManager.getConnection(DB_URL);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            buildUI();
            pack();
            setMinimumSize(new Dimension(900, 560));
            setSize(new Dimension(1100, 640));
            setLocationRelativeTo(null);
        }

        private void buildUI() {
            JPanel root = new JPanel(new BorderLayout());

            // Верхняя панель
            JPanel top = new JPanel(new BorderLayout());
            top.setBorder(new EmptyBorder(10,15,10,15));
            top.setOpaque(true);
            top.setBackground(new Color(0x1E, 0x88, 0xE5));
            JLabel lbl = new JLabel("Пользователь: " + user.fullName +
                "   •   Роль: " + user.roleRus());
            lbl.setForeground(Color.WHITE);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            top.add(lbl, BorderLayout.WEST);

            JButton logout = new JButton("Выход");
            logout.addActionListener(e -> { dispose(); new LoginDialog().setVisible(true); });
            JPanel logoutWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            logoutWrap.setOpaque(false);
            logoutWrap.add(logout);
            top.add(logoutWrap, BorderLayout.EAST);

            root.add(top, BorderLayout.NORTH);

            // Tabs — состав зависит от роли
            JTabbedPane tabs = new JTabbedPane();
            switch (user.role) {
                case "director":
                    tabs.addTab("Ученики",      panelStudents(false));
                    tabs.addTab("Родители",     panelParents());
                    tabs.addTab("Учителя",      panelTeachers());
                    tabs.addTab("Оценки",       panelGrades());
                    tabs.addTab("Медкарты",     panelMedical());
                    tabs.addTab("Пользователи", panelUsers());
                    break;
                case "teacher":
                    tabs.addTab("Мои ученики", panelStudents(true));
                    tabs.addTab("Родители",    panelParents());
                    tabs.addTab("Оценки",      panelGrades());
                    break;
                case "student":
                    tabs.addTab("Моё личное дело", panelStudentSelf());
                    tabs.addTab("Мои оценки",      panelMyGrades());
                    tabs.addTab("Доска успеваемости", panelGradesBoard());
                    break;
                case "medic":
                    tabs.addTab("Медкарты", panelMedical());
                    tabs.addTab("Ученики",  panelStudents(false));
                    break;
            }
            root.add(tabs, BorderLayout.CENTER);

            JLabel status = new JLabel(
                "  Политика защиты применена на уровне приложения " +
                "(роль «" + user.roleRus() + "»)");
            status.setBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.LIGHT_GRAY));
            root.add(status, BorderLayout.SOUTH);

            setContentPane(root);
        }

        // ------------------- helpers ----------------------
        private JScrollPane makeTable(String[] headers, List<Object[]> rows) {
            DefaultTableModel m = new DefaultTableModel(headers, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
            for (Object[] r : rows) m.addRow(r);
            JTable t = new JTable(m);
            t.setRowHeight(22);
            t.getTableHeader().setFont(t.getFont().deriveFont(Font.BOLD));
            return new JScrollPane(t);
        }

        private List<Object[]> fetch(String sql, Object... params) {
            List<Object[]> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i=0;i<params.length;i++) ps.setObject(i+1, params[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    int n = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        Object[] r = new Object[n];
                        for (int i=0;i<n;i++) r[i] = rs.getObject(i+1);
                        out.add(r);
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка БД: " + ex.getMessage());
            }
            return out;
        }

        // ------------------- panels ----------------------

        private JScrollPane panelStudents(boolean onlyMyClasses) {
            String sql =
                "SELECT s.id, s.last_name, s.first_name, c.name AS class, s.birth_date " +
                "FROM students s LEFT JOIN classes c ON c.id = s.class_id ";
            Object[] params = new Object[0];
            if (onlyMyClasses && user.teacherId != null) {
                sql += "WHERE c.teacher_id = ? ";
                params = new Object[]{ user.teacherId };
            }
            sql += "ORDER BY s.last_name";
            return makeTable(
                new String[]{"ID","Фамилия","Имя","Класс","Дата рождения"},
                fetch(sql, params));
        }

        private JScrollPane panelStudentSelf() {
            return makeTable(
                new String[]{"ID","Фамилия","Имя","Класс","Дата рождения"},
                fetch("SELECT s.id,s.last_name,s.first_name,c.name,s.birth_date " +
                      "FROM students s LEFT JOIN classes c ON c.id=s.class_id " +
                      "WHERE s.id = ?", user.studentId));
        }

        private JScrollPane panelParents() {
            String sql =
                "SELECT p.id, s.last_name || ' ' || s.first_name AS ученик, " +
                "       p.last_name, p.first_name, p.phone " +
                "FROM parents p JOIN students s ON s.id = p.student_id ";
            Object[] params = new Object[0];
            if ("teacher".equals(user.role) && user.teacherId != null) {
                sql += "JOIN classes c ON c.id = s.class_id WHERE c.teacher_id = ? ";
                params = new Object[]{ user.teacherId };
            }
            sql += "ORDER BY ученик";
            return makeTable(
                new String[]{"ID","Ученик","Фамилия","Имя","Телефон"},
                fetch(sql, params));
        }

        private JScrollPane panelTeachers() {
            return makeTable(
                new String[]{"ID","Фамилия","Имя","Специальность"},
                fetch("SELECT id,last_name,first_name,subject_speciality " +
                      "FROM teachers ORDER BY last_name"));
        }

        private JScrollPane panelGrades() {
            String sql =
                "SELECT g.id, s.last_name || ' ' || s.first_name AS ученик, " +
                "       sub.name AS предмет, g.grade, g.date " +
                "FROM grades g " +
                "JOIN students s ON s.id = g.student_id " +
                "JOIN subjects sub ON sub.id = g.subject_id ";
            Object[] params = new Object[0];
            if ("teacher".equals(user.role) && user.teacherId != null) {
                sql += "JOIN classes c ON c.id = s.class_id " +
                       "WHERE c.teacher_id = ? ";
                params = new Object[]{ user.teacherId };
            }
            sql += "ORDER BY g.date DESC";
            return makeTable(
                new String[]{"ID","Ученик","Предмет","Оценка","Дата"},
                fetch(sql, params));
        }

        private JScrollPane panelMyGrades() {
            return makeTable(
                new String[]{"Предмет","Оценка","Дата"},
                fetch("SELECT sub.name, g.grade, g.date " +
                      "FROM grades g JOIN subjects sub ON sub.id=g.subject_id " +
                      "WHERE g.student_id = ? ORDER BY g.date DESC",
                      user.studentId));
        }

        private JScrollPane panelGradesBoard() {
            // Учащийся видит обезличенную доску средних оценок по классам
            return makeTable(
                new String[]{"Класс","Предмет","Средний балл","Оценок"},
                fetch("SELECT c.name, sub.name, " +
                      "       printf('%.2f', AVG(g.grade)), COUNT(*) " +
                      "FROM grades g " +
                      "JOIN students s ON s.id = g.student_id " +
                      "JOIN classes  c ON c.id = s.class_id " +
                      "JOIN subjects sub ON sub.id = g.subject_id " +
                      "GROUP BY c.name, sub.name " +
                      "ORDER BY c.name, sub.name"));
        }

        private JScrollPane panelMedical() {
            return makeTable(
                new String[]{"ID","Ученик","Диагноз","Дата"},
                fetch("SELECT m.id, s.last_name || ' ' || s.first_name, " +
                      "       m.diagnosis, m.date " +
                      "FROM medical_records m " +
                      "JOIN students s ON s.id = m.student_id " +
                      "ORDER BY m.date DESC"));
        }

        private JScrollPane panelUsers() {
            return makeTable(
                new String[]{"Логин","Роль","ФИО"},
                fetch("SELECT login, role, full_name FROM users ORDER BY role, login"));
        }
    }
}
