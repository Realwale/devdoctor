package dev.devdoctor.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.devdoctor.core.model.DiagnosticSession;

public final class DiagnosticJson {
    private final ObjectMapper mapper;

    public DiagnosticJson() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public String write(DiagnosticSession session) {
        try { return mapper.writeValueAsString(session); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Unable to serialize diagnostic session", e); }
    }

    public DiagnosticSession read(String json) {
        try { return mapper.readValue(json, DiagnosticSession.class); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid diagnostic session JSON", e); }
    }
}
