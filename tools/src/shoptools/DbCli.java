package shoptools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * scripts/db.mjs 가 부르는 SQL 실행기. 접속 정보는 환경변수(DB_URL, DB_USER, DB_PASSWORD)로 받는다.
 *
 * <pre>
 *   java -cp ... shoptools.DbCli "SELECT ..."
 *   java -cp ... shoptools.DbCli --file query.sql
 * </pre>
 *
 * 결과 표와 DB 실행 시간(JVM 기동 시간 제외)을 출력한다. 실패하면 종료 코드 1.
 */
public final class DbCli {

    private static final int MAX_ROWS_SHOWN = 50;

    private DbCli() {
    }

    public static void main(String[] args) throws Exception {
        String sql;
        if (args.length == 2 && "--file".equals(args[0])) {
            sql = Files.readString(Path.of(args[1]), StandardCharsets.UTF_8);
        } else if (args.length == 1) {
            sql = args[0];
        } else {
            System.err.println("사용법: DbCli \"<SQL>\" | DbCli --file <파일>");
            System.exit(2);
            return;
        }
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        if (url == null || user == null) {
            System.err.println("DB_URL, DB_USER 환경변수가 없습니다. node scripts/db.mjs 로 실행하세요.");
            System.exit(2);
            return;
        }
        int failed = 0;
        try (Connection con = DriverManager.getConnection(url, user, password == null ? "" : password)) {
            con.setAutoCommit(true);
            for (String statement : split(sql)) {
                failed += execute(con, statement);
            }
        } catch (SQLException e) {
            System.err.println("오류: " + firstLine(e.getMessage()));
            System.exit(1);
        }
        System.exit(failed > 0 ? 1 : 0);
    }

    private static int execute(Connection con, String sql) {
        long start = System.nanoTime();
        try (Statement st = con.createStatement()) {
            boolean hasResult = st.execute(sql);
            if (hasResult) {
                try (ResultSet rs = st.getResultSet()) {
                    int rows = print(rs);
                    System.out.printf("(%d행, DB 실행 %d ms)%n", rows, elapsedMs(start));
                }
            } else {
                System.out.printf("(%d행 변경, DB 실행 %d ms)%n", st.getUpdateCount(), elapsedMs(start));
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("오류: " + firstLine(e.getMessage()));
            return 1;
        }
    }

    private static int print(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        List<String> header = new ArrayList<>();
        for (int i = 1; i <= cols; i++) {
            header.add(md.getColumnLabel(i));
        }
        System.out.println(String.join(" | ", header));
        int rows = 0;
        while (rs.next()) {
            rows++;
            if (rows <= MAX_ROWS_SHOWN) {
                List<String> values = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    values.add(v == null ? "NULL" : String.valueOf(v));
                }
                System.out.println(String.join(" | ", values));
            }
        }
        if (rows > MAX_ROWS_SHOWN) {
            System.out.printf("... (%d행 중 %d행만 표시)%n", rows, MAX_ROWS_SHOWN);
        }
        return rows;
    }

    /** 세미콜론으로 문장을 나눈다. 따옴표 안의 세미콜론은 나누지 않고, {@code --} 주석은 버린다. */
    static List<String> split(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (!inQuote && c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
                current.append('\n');
                continue;
            }
            if (c == '\'') {
                inQuote = !inQuote;
            }
            if (c == ';' && !inQuote) {
                addIfNotBlank(result, current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfNotBlank(result, current.toString());
        return result;
    }

    private static void addIfNotBlank(List<String> list, String sql) {
        if (!sql.isBlank()) {
            list.add(sql.strip());
        }
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int idx = message.indexOf('\n');
        return idx < 0 ? message : message.substring(0, idx);
    }
}
