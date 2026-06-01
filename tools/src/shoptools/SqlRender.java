package shoptools;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * {@code node scripts/render-sql.mjs} 가 Gradle 태스크 renderSql 로 부르는 SQL 렌더러.
 * 매퍼 XML과 파라미터 JSON으로 MyBatis가 실제로 만드는 SQL을 출력한다 (DB 연결 없음).
 *
 * <p>요청 파일(한 줄에 인자 하나): {@code <Mapper.queryId> [<파라미터.json>...] [--cases <폴더>] [--xml <경로>] [--inline]}
 */
public final class SqlRender {

    private static final Path MAPPER_DIR = Path.of("src", "main", "resources", "mapper");

    private SqlRender() {
    }

    public static void main(String[] args) throws Exception {
        List<String> request = args.length == 2 && "--request".equals(args[0])
                ? Files.readAllLines(Path.of(args[1]), StandardCharsets.UTF_8).stream().filter(s -> !s.isEmpty())
                        .toList()
                : List.of(args);
        String statementName = null;
        List<Path> paramFiles = new ArrayList<>();
        Path xml = null;
        boolean inline = false;
        for (int i = 0; i < request.size(); i++) {
            String arg = request.get(i);
            switch (arg) {
                case "--inline" -> inline = true;
                case "--xml" -> xml = Path.of(request.get(++i));
                case "--cases" -> paramFiles.addAll(listJson(Path.of(request.get(++i))));
                default -> {
                    if (statementName == null) {
                        statementName = arg;
                    } else {
                        paramFiles.add(Path.of(arg));
                    }
                }
            }
        }
        if (statementName == null) {
            System.err.println("사용법: node scripts/render-sql.mjs <Mapper.queryId> [파라미터.json | --cases <폴더>]"
                    + " [--xml <매퍼 XML>] [--inline]");
            System.exit(2);
        }

        Configuration configuration = new Configuration();
        loadMappers(configuration, xml);
        MappedStatement statement = findStatement(configuration, statementName);

        int failed = 0;
        if (paramFiles.isEmpty()) {
            failed += render(statement, null, Map.of(), inline);
        }
        for (Path file : paramFiles) {
            Object params;
            try {
                params = Json.parse(Files.readString(file, StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                System.out.println("-- " + file + "\n-- 파라미터 JSON 오류: " + e.getMessage() + "\n");
                failed++;
                continue;
            }
            failed += render(statement, file, params, inline);
        }
        System.exit(failed > 0 ? 1 : 0);
    }

    private static int render(MappedStatement statement, Path file, Object params, boolean inline) {
        String title = "-- " + statement.getId().substring(statement.getId().lastIndexOf('.',
                statement.getId().lastIndexOf('.') - 1) + 1) + (file == null ? "" : "  (" + display(file) + ")");
        System.out.println(title);
        try {
            BoundSql boundSql = statement.getBoundSql(params);
            List<Object> values = bindValues(statement.getConfiguration(), boundSql, params);
            String sql = tidy(boundSql.getSql());
            if (inline) {
                System.out.println(inline(sql, values) + ";");
            } else {
                System.out.println(sql);
                if (!values.isEmpty()) {
                    List<String> shown = new ArrayList<>();
                    for (int i = 0; i < values.size(); i++) {
                        shown.add("[" + (i + 1) + "] " + literal(values.get(i)));
                    }
                    System.out.println("-- 바인딩: " + String.join(", ", shown));
                }
            }
            System.out.println();
            return 0;
        } catch (RuntimeException e) {
            System.out.println("-- 렌더링 오류: " + rootMessage(e) + "\n");
            return 1;
        }
    }

    private static String display(Path file) {
        Path root = Path.of("").toAbsolutePath();
        Path abs = file.toAbsolutePath();
        return (abs.startsWith(root) ? root.relativize(abs) : abs).toString().replace('\\', '/');
    }

    private static List<Object> bindValues(Configuration configuration, BoundSql boundSql, Object params) {
        List<Object> values = new ArrayList<>();
        MetaObject meta = params == null ? null : configuration.newMetaObject(params);
        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            if (mapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            String property = mapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(property)) {
                value = boundSql.getAdditionalParameter(property);
            } else if (params == null) {
                value = null;
            } else if (configuration.getTypeHandlerRegistry().hasTypeHandler(params.getClass())) {
                value = params;
            } else {
                value = meta.hasGetter(property) || params instanceof Map ? meta.getValue(property) : null;
            }
            values.add(value);
        }
        return values;
    }

    /** 빈 줄을 지우고 공통 들여쓰기를 걷어 낸다. MyBatis는 첫 줄 앞 공백을 지우므로 둘째 줄부터 맞춘다. */
    private static String tidy(String sql) {
        List<String> lines = sql.lines().filter(l -> !l.isBlank()).map(String::stripTrailing).toList();
        if (lines.isEmpty()) {
            return "";
        }
        int first = indentOf(lines.get(0));
        int rest = lines.stream().skip(1).mapToInt(SqlRender::indentOf).min().orElse(0);
        int cut = first > 0 ? first : Math.max(0, rest - 1);
        List<String> out = new ArrayList<>();
        out.add(lines.get(0).strip());
        lines.stream().skip(1).forEach(l -> out.add(l.substring(Math.min(cut, indentOf(l)))));
        return String.join("\n", out);
    }

    private static int indentOf(String line) {
        return line.length() - line.stripLeading().length();
    }

    /** ? 자리에 값을 채운다. 따옴표 안의 ? 는 건드리지 않는다. */
    private static String inline(String sql, List<Object> values) {
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        int index = 0;
        for (char c : sql.toCharArray()) {
            if (c == '\'') {
                inQuote = !inQuote;
            }
            if (c == '?' && !inQuote && index < values.size()) {
                sb.append(literal(values.get(index++)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String literal(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

    private static void loadMappers(Configuration configuration, Path override) throws IOException {
        String overrideNamespace = override == null ? null : namespaceOf(override);
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(MAPPER_DIR)) {
            walk.filter(p -> p.toString().endsWith(".xml")).sorted().forEach(files::add);
        }
        if (override != null) {
            files.removeIf(p -> overrideNamespace.equals(namespaceOfQuiet(p)));
            files.add(override);
        }
        for (Path file : files) {
            try (InputStream in = Files.newInputStream(file)) {
                new XMLMapperBuilder(in, configuration, file.toString(), configuration.getSqlFragments()).parse();
            }
        }
    }

    private static String namespaceOfQuiet(Path file) {
        try {
            return namespaceOf(file);
        } catch (IOException e) {
            return "";
        }
    }

    private static String namespaceOf(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        int idx = text.indexOf("namespace=\"");
        if (idx < 0) {
            return "";
        }
        int start = idx + "namespace=\"".length();
        return text.substring(start, text.indexOf('"', start));
    }

    private static MappedStatement findStatement(Configuration configuration, String name) {
        List<String> matches = new ArrayList<>();
        for (Object o : configuration.getMappedStatements()) {
            if (o instanceof MappedStatement ms && (ms.getId().equals(name) || ms.getId().endsWith("." + name))
                    && !matches.contains(ms.getId())) {
                matches.add(ms.getId());
            }
        }
        if (matches.size() != 1) {
            System.err.println(matches.isEmpty() ? "쿼리를 찾지 못했습니다: " + name + " (형식: 매퍼이름.쿼리id)"
                    : "같은 이름의 쿼리가 여러 개입니다: " + matches);
            System.exit(2);
        }
        return configuration.getMappedStatement(matches.get(0));
    }

    private static List<Path> listJson(Path dir) throws IOException {
        try (Stream<Path> list = Files.list(dir)) {
            return list.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String message = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        return message.lines().findFirst().orElse(message);
    }

    /** 의존성 없는 작은 JSON 파서. 객체는 Map, 배열은 List, 숫자는 Long/BigDecimal. */
    static final class Json {
        private final String s;
        private int pos;

        private Json(String s) {
            this.s = s;
        }

        static Object parse(String text) {
            Json json = new Json(text);
            Object value = json.value();
            json.skip();
            if (json.pos != text.length()) {
                throw new IllegalArgumentException("JSON 뒤에 남은 글자가 있습니다 (위치 " + json.pos + ")");
            }
            return value;
        }

        private Object value() {
            skip();
            if (pos >= s.length()) {
                throw new IllegalArgumentException("JSON이 비어 있습니다");
            }
            char c = s.charAt(pos);
            if (c == '{') {
                return object();
            }
            if (c == '[') {
                return array();
            }
            if (c == '"') {
                return string();
            }
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skip();
            if (s.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skip();
                String key = string();
                skip();
                expect(':');
                map.put(key, value());
                skip();
                if (s.charAt(pos) == ',') {
                    pos++;
                } else {
                    expect('}');
                    return map;
                }
            }
        }

        private Collection<Object> array() {
            List<Object> list = new ArrayList<>();
            pos++;
            skip();
            if (s.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(value());
                skip();
                if (s.charAt(pos) == ',') {
                    pos++;
                } else {
                    expect(']');
                    return list;
                }
            }
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (s.charAt(pos) != '"') {
                char c = s.charAt(pos++);
                if (c == '\\') {
                    char e = s.charAt(pos++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            pos++;
            return sb.toString();
        }

        private Object number() {
            int start = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            String n = s.substring(start, pos);
            if (n.isEmpty()) {
                throw new IllegalArgumentException("알 수 없는 값 (위치 " + start + ")");
            }
            if (n.contains(".") || n.contains("e") || n.contains("E")) {
                return new BigDecimal(n);
            }
            return Long.parseLong(n);
        }

        private void expect(char c) {
            if (pos >= s.length() || s.charAt(pos) != c) {
                throw new IllegalArgumentException("'" + c + "' 가 와야 합니다 (위치 " + pos + ")");
            }
            pos++;
        }

        private void skip() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }
    }
}
