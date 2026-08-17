# Fixture strategy

Fixtures are minimal Spring/Maven projects or recorded deterministic failure bundles. Each directory contains `fixture.json`, a sanitized failure log or reproducible test, and only the configuration/source needed to establish provenance. Container-dependent fixtures have a recorded mode for CI without Docker and an executable mode for local integration.

Initial broken fixtures: `spring-port-conflict`, `spring-missing-env`, `spring-invalid-header`, `spring-db-down`, `spring-db-auth`, `spring-redis-down`, `spring-flyway-checksum`, `spring-nosuchmethod`, `spring-java-version`, `spring-dns-failure`, `spring-invalid-url`, `spring-docker-hostname`, `spring-config-binding`, and `spring-bean-creation`. Healthy fixtures include plain Spring, optional-unused Redis, valid secret shape, and benign warning logs.

Acceptance compares hypothesis IDs/statuses, required evidence types, graph edges, root-cause rank, prohibited root causes, and leakage canaries—not brittle prose.
