# Explicit limitations

* A laptop installation cannot observe a remote live server without authorized remote execution or transferred JFR/log evidence.
* An already-running JVM that was not started with outcome instrumentation provides generic JFR exception candidates. DevDoctor intentionally does not hot-inject full OpenTelemetry instrumentation into a serving JVM; exact transaction correlation then requires logs, an imported trace/recording, or a restart through `devdoctor run`.
* Database probes establish bounded reachability but do not execute SQL. Kafka has signatures and DNS/TCP probes but no protocol handshake.
* Complex transitive dependency/symbol mismatches may remain high-confidence rather than confirmed.
* No external AI provider, production collector, SaaS, Postman importer, reverse proxy, traffic replay, autonomous repair, or non-Java runtime is bundled.

These are evidence-access limits, not placeholder implementations. Reports preserve uncertainty when the required observation is unavailable.
