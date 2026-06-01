package shoptools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code ./gradlew dbReset} 이 부르는 DB 초기화 도구.
 *
 * <ul>
 *   <li>개발 DB {@code .local/db/dev}: schema.sql·data.sql. 계정은 .env 의 DB_DEV_USER (읽기·쓰기)</li>
 *   <li>운영 흉내 DB {@code .local/db/prod}: 같은 데이터. 관리자 비밀번호는 실행할 때마다 무작위로 만들고
 *       어디에도 저장하지 않는다. 수강생·AI에게는 .env 의 DB_PROD_USER (SELECT만) 계정만 준다</li>
 *   <li>{@code --perf}: 개발 DB에 tools/sql/perf-data.sql 을 추가로 실행한다 (대량 데이터)</li>
 * </ul>
 */
public final class DbReset {

    private static final Path DB_DIR = Path.of(".local", "db");

    private DbReset() {
    }

    public static void main(String[] args) throws Exception {
        boolean perf = List.of(args).contains("--perf");
        Map<String, String> env = readEnv();
        Files.createDirectories(DB_DIR);
        for (String name : List.of("dev", "prod")) {
            Files.deleteIfExists(DB_DIR.resolve(name + ".mv.db"));
            Files.deleteIfExists(DB_DIR.resolve(name + ".trace.db"));
        }

        String devUser = require(env, "DB_DEV_USER");
        try (Connection con = DriverManager.getConnection(fileUrl("dev"), devUser, require(env, "DB_DEV_PASSWORD"))) {
            loadSchemaAndData(con);
            if (perf) {
                Path perfSql = Path.of("tools", "sql", "perf-data.sql");
                if (Files.exists(perfSql)) {
                    long start = System.nanoTime();
                    runScript(con, perfSql);
                    System.out.printf("개발 DB 대량 데이터 추가 (%d초)%n", (System.nanoTime() - start) / 1_000_000_000L);
                } else {
                    System.out.println("이 저장소에는 대량 데이터 스크립트(tools/sql/perf-data.sql)가 없습니다.");
                }
            }
        }
        System.out.println("개발 DB 준비: .local/db/dev (계정 " + devUser + ", 읽기·쓰기)");

        String roUser = require(env, "DB_PROD_USER");
        String adminPassword = randomPassword();
        try (Connection con = DriverManager.getConnection(fileUrl("prod"), "shop_admin", adminPassword)) {
            loadSchemaAndData(con);
            try (Statement st = con.createStatement()) {
                st.execute("CREATE USER " + roUser + " PASSWORD '" + require(env, "DB_PROD_PASSWORD").replace("'", "''")
                        + "'");
                st.execute("GRANT SELECT ON SCHEMA PUBLIC TO " + roUser);
            }
        }
        System.out.println("운영 흉내 DB 준비: .local/db/prod (계정 " + roUser + ", SELECT만)");
    }

    private static void loadSchemaAndData(Connection con) throws SQLException {
        runScript(con, Path.of("src", "main", "resources", "schema.sql"));
        runScript(con, Path.of("src", "main", "resources", "data.sql"));
    }

    private static void runScript(Connection con, Path script) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("RUNSCRIPT FROM '" + script.toAbsolutePath().toString().replace("'", "''")
                    + "' CHARSET 'UTF-8'");
        }
    }

    private static String fileUrl(String name) {
        return "jdbc:h2:file:./.local/db/" + name + ";MODE=MSSQLServer";
    }

    /** .env 를 읽는다. 없으면 .env.example 을 읽는다. */
    private static Map<String, String> readEnv() throws IOException {
        Path file = Files.exists(Path.of(".env")) ? Path.of(".env") : Path.of(".env.example");
        if (!Files.exists(file)) {
            throw new IllegalStateException(".env 가 없습니다. cp .env.example .env 를 먼저 실행하세요.");
        }
        if (file.toString().equals(".env.example")) {
            System.out.println("(.env 가 없어 .env.example 의 값을 씁니다. cp .env.example .env 를 권장합니다.)");
        }
        Map<String, String> env = new HashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.strip();
            int eq = trimmed.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 0) {
                continue;
            }
            env.put(trimmed.substring(0, eq).strip(), unquote(trimmed.substring(eq + 1).strip()));
        }
        return env;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String require(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(".env 에 " + key + " 가 없습니다.");
        }
        return value;
    }

    private static String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < 24; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
