package com.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

class FonuClientConfigTest {
    @Test void apiKeyCanBeUsedAsHeader() {
        String apiKey = System.getenv("FONU_API_KEY");
        assertDoesNotThrow(() -> validateHeader(apiKey));
    }
    private void validateHeader(String value) {
        if (value == null) throw new IllegalStateException("Required environment variable FONU_API_KEY is missing");
        if (value.chars().anyMatch(c -> c < 32 || c == 127)) throw new IllegalArgumentException("Validation failed for header 'x-fonu-api-key'");
    }
}
