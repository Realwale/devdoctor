# Security

DevDoctor never generates or proxies application traffic and never performs automatic repair. Explicit commands remain user-authorized and bounded. Runtime observation uses JFR and optional startup instrumentation; the full instrumentation agent is not hot-attached to an already-serving JVM.

The runtime agent exports no traces, metrics, or logs, disables network context propagation, and samples spans only during a bounded diagnostic window. It records only normalized route/method, status, trace linkage, duration, dependency shape, and exception metadata—never bodies, header values, cookies, query values, or credentials. Raw JFR exists only inside an owner-readable temporary directory and is deleted after redacted aggregation. Imported recordings remain user-owned.

Redaction precedes evidence construction, JSON, saved sessions, reports, and any future external reasoning. Security issues should be reported privately without live credentials.
