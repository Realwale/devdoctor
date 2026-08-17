package dev.devdoctor.engine.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Security boundary used before untrusted text enters evidence, logs, JSON or reports. */
public final class SecretRedactor {
    public static final String REDACTED = "[REDACTED]";
    private static final String KEY = "(?i)(password|passwd|pwd|secret|token|api[-_.]?key|access[-_.]?key|client[-_.]?secret|authorization|cookie)";
    private static final Pattern KEY_VALUE = Pattern.compile("(" + KEY + "\\s*[:=]\\s*)([^\\s,;]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._~+/-]{8,}={0,2}");
    private static final Pattern BASIC = Pattern.compile("(?i)(Basic\\s+)[A-Za-z0-9+/]{8,}={0,2}");
    private static final Pattern JWT = Pattern.compile("(?<![A-Za-z0-9_-])eyJ[A-Za-z0-9_-]{8,}\\.eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}(?![A-Za-z0-9_-])");
    private static final Pattern AWS_ACCESS = Pattern.compile("(?<![A-Z0-9])(AKIA|ASIA)[A-Z0-9]{16}(?![A-Z0-9])");
    private static final Pattern URI_CREDS = Pattern.compile("(?i)([a-z][a-z0-9+.-]*://[^:/\\s]+:)([^@/\\s]+)(@)");
    private static final Pattern PRIVATE_KEY = Pattern.compile("(?s)-----BEGIN [A-Z ]*PRIVATE KEY-----.*?-----END [A-Z ]*PRIVATE KEY-----");
    private static final Pattern LONG_TOKEN = Pattern.compile("(?<![A-Za-z0-9+/=_-])[A-Za-z0-9+/=_-]{48,}(?![A-Za-z0-9+/=_-])");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}&&[^\\r\\n\\t]]");

    public String redact(String input) {
        if (input == null || input.isEmpty()) return input == null ? "" : input;
        String safe = PRIVATE_KEY.matcher(input).replaceAll(REDACTED);
        safe = replacement(BEARER, safe, 1);
        safe = replacement(BASIC, safe, 1);
        safe = JWT.matcher(safe).replaceAll(REDACTED);
        safe = AWS_ACCESS.matcher(safe).replaceAll(REDACTED);
        safe = URI_CREDS.matcher(safe).replaceAll("$1" + REDACTED + "$3");
        safe = replacement(KEY_VALUE, safe, 1);
        safe = redactLongTokens(safe);
        return CONTROL.matcher(safe).replaceAll("");
    }

    private String redactLongTokens(String input) {
        Matcher matcher = LONG_TOKEN.matcher(input); StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group();
            boolean mixed = token.chars().anyMatch(Character::isDigit) && token.chars().anyMatch(Character::isUpperCase)
                    && token.chars().anyMatch(Character::isLowerCase);
            matcher.appendReplacement(out, Matcher.quoteReplacement(mixed ? REDACTED : token));
        }
        return matcher.appendTail(out).toString();
    }

    private String replacement(Pattern pattern, String input, int retainedGroup) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(retainedGroup) + REDACTED));
        return matcher.appendTail(out).toString();
    }

    public Map<String, Object> characteristics(String name, String rawValue) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", redact(name));
        result.put("present", rawValue != null);
        result.put("length", rawValue == null ? 0 : rawValue.length());
        result.put("blank", rawValue != null && rawValue.isBlank());
        result.put("leadingWhitespace", rawValue != null && !rawValue.isEmpty() && Character.isWhitespace(rawValue.charAt(0)));
        result.put("trailingWhitespace", rawValue != null && !rawValue.isEmpty() && Character.isWhitespace(rawValue.charAt(rawValue.length() - 1)));
        result.put("trailingNewline", rawValue != null && (rawValue.endsWith("\n") || rawValue.endsWith("\r")));
        result.put("value", REDACTED);
        return Map.copyOf(result);
    }

    public boolean looksSensitiveName(String name) { return name != null && Pattern.compile(KEY).matcher(name).find(); }
}
