# Explicit V1 limitations

The following capabilities are deliberately not represented as complete:

* Database probes establish bounded TCP reachability only. They do not authenticate or execute SQL, because doing so safely requires explicit credential/permission handling and database-specific protocol support.
* Maven/Gradle dependency inspection reports declarations and explicit overrides. A fully resolved runtime-vs-compile tree and symbol-to-library version database are not yet implemented; impact: complex transitive `NoSuchMethodError` cases can remain high-confidence rather than confirmed.
* The Docker-hostname fixture has a reproducible recorded mode. Automatic dual-namespace probing from inside the application container and against the Compose service is not implemented; impact: the engine does not label this diagnosis `CONFIRMED`.
* Kafka has deterministic failure signatures and generic DNS/TCP probes, but no Kafka protocol handshake probe.
* The optional external AI adapter has only its sanitized SPI. No provider is bundled, preserving offline-first behavior and explicit-consent requirements.
* Testcontainers is reserved for future database/Docker protocol integration tests; current tests require no local daemon and remain deterministic.

These gaps do not use placeholder return values. Reports retain categorical uncertainty and expose probe scope/limitations in evidence metadata.
