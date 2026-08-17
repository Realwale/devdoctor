package dev.devdoctor.engine.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class SecretRedactorTest {
    private final SecretRedactor redactor = new SecretRedactor();

    @Test void redactsAdversarialSecretFormats() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.ABCDabcd0123456789_-";
        String privateKey = "-----BEGIN PRIVATE KEY-----\nabcDEF0123456789\n-----END PRIVATE KEY-----";
        String longToken = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDabcdEFGH";
        String input = "password=hunter2\nAuthorization: Bearer liveToken123456\n" + jwt
                + "\njdbc:postgresql://alice:swordfish@db/orders\nAKIAIOSFODNN7EXAMPLE\n" + privateKey + "\n" + longToken;
        String safe = redactor.redact(input);
        assertThat(safe).doesNotContain("hunter2", "liveToken123456", jwt, "swordfish", "AKIAIOSFODNN7EXAMPLE", "abcDEF0123456789", longToken)
                .contains(SecretRedactor.REDACTED);
    }

    @Test void exposesOnlySecretCharacteristics() {
        var shape = redactor.characteristics("FONU_API_KEY", "never-print-this\n");
        assertThat(shape).containsEntry("present", true).containsEntry("trailingNewline", true).containsEntry("value", "[REDACTED]");
        assertThat(shape.toString()).doesNotContain("never-print-this");
    }
}
