# Security

DevDoctor does not modify source, Git, local configuration, containers, or processes. All diagnostic probes are read-only and safety classified. Only explicitly supplied commands and HTTP requests run; HTTP methods such as `POST`, `PUT`, `PATCH`, and `DELETE` can mutate the target application and therefore produce a warning before replay.

Redaction is an architectural boundary, not report cleanup. Collectors use `SecretRedactor` before constructing evidence. Tests scan terminal, JSON, saved sessions, logs, and reasoner payloads for seeded secrets. Security issues should be reported privately to maintainers rather than opened with live credentials.
