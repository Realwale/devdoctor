# Security

DevDoctor is read-only by design. It does not modify source, Git, databases, environment, containers, processes, or services. All probes declare safety, network probes have tight timeouts, and only explicitly supplied commands run.

Redaction is an architectural boundary, not report cleanup. Collectors use `SecretRedactor` before constructing evidence. Tests scan terminal, JSON, saved sessions, logs, and reasoner payloads for seeded secrets. Security issues should be reported privately to maintainers rather than opened with live credentials.
