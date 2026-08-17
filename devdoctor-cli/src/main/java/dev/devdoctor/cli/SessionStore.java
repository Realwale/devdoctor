package dev.devdoctor.cli;

import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.DiagnosticSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

final class SessionStore {
    private final DiagnosticJson json = new DiagnosticJson();
    Path save(Path projectRoot, DiagnosticSession session) throws IOException {
        Path directory = projectRoot.resolve(".devdoctor/sessions").normalize(); Files.createDirectories(directory);
        Path target = directory.resolve(session.diagnosticId() + ".json"); Path temporary = Files.createTempFile(directory, session.diagnosticId(), ".tmp");
        Files.writeString(temporary, json.write(session), StandardCharsets.UTF_8);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        Files.writeString(directory.resolve("latest"), target.getFileName().toString(), StandardCharsets.UTF_8);
        return target;
    }
    DiagnosticSession load(Path projectRoot, String id) throws IOException {
        Path directory = projectRoot.resolve(".devdoctor/sessions").normalize(); Path target;
        if (id == null || id.isBlank()) {
            Path latest = directory.resolve("latest");
            if (Files.isRegularFile(latest)) target = directory.resolve(Files.readString(latest, StandardCharsets.UTF_8).trim());
            else try (var files = Files.list(directory)) { target = files.filter(p -> p.toString().endsWith(".json")).max(Comparator.comparingLong(this::modified)).orElseThrow(); }
        } else target = directory.resolve(id.endsWith(".json") ? id : id + ".json");
        if (!target.normalize().startsWith(directory) || !Files.isRegularFile(target)) throw new IOException("Diagnostic session not found");
        return json.read(Files.readString(target, StandardCharsets.UTF_8));
    }
    private long modified(Path path) { try { return Files.getLastModifiedTime(path).toMillis(); } catch (IOException e) { return 0; } }
}
