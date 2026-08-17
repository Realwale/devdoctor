# ADR 007: Small plugin SPI

**Status:** Accepted

Plugins contribute collectors, rules, and probes based on a project profile. Core correlation and transports remain ecosystem-independent. The initial build uses three Maven modules rather than one module per integration.
