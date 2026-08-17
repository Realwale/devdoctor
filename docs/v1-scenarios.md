# V1 diagnostic scenarios

| ID | Scenario | Discriminating evidence | Key alternatives to test |
|---|---|---|---|
| JVM-01 | Unsupported class version | class/runtime major versions | corrupt class, wrong artifact |
| JVM-02 | Out of memory | OOME subtype + memory indicators | native crash, timeout |
| DEP-01 | Class not found | missing runtime class + dependency tree | typo, optional scope |
| DEP-02 | No class definition | cause chain + runtime classpath | static-init failure |
| DEP-03 | No such method | symbol + expected/actual versions | application linkage |
| SPR-01 | Bean creation failure | deepest cause + bean/source | database/network cause |
| SPR-02 | Missing bean | required type + component scan | conditional disabled |
| SPR-03 | Config binding failure | property path + rejected shape | missing converter |
| SPR-04 | Circular dependency | bean cycle | unrelated startup failure |
| SPR-05 | Context startup failure | causal chain + app boundary | wrapper exception only |
| DB-01 | Connection refused | host/port + TCP refusal | DNS, authentication |
| DB-02 | Authentication failed | DB handshake/auth code | reachability, unknown DB |
| DB-03 | Unknown database | server error/database name | authentication |
| DB-04 | Pool exhaustion | pool timeout + active/limit | DB down, slow query |
| DB-05 | Flyway checksum mismatch | migration/version/checksum | DB auth, missing file |
| DB-06 | Migration validation failure | validation details | connectivity |
| NET-01 | DNS failure | resolution probe | port/firewall |
| NET-02 | Connection refused | successful DNS + refused TCP | timeout, wrong host |
| NET-03 | Connect timeout | DNS + timed TCP | application timeout |
| NET-04 | TLS validation | certificate/PKIX cause | DNS, auth |
| HTTP-01 | Invalid header value | header error + whitespace shape | invalid header name, network |
| HTTP-02 | Malformed URL | parser failure + configured shape | DNS |
| HTTP-03 | Client timeout | request phase + reachability | pool exhaustion |
| INF-01 | Port conflict | bind error + port owner | permission, bad address |
| INF-02 | Redis unavailable | Redis config + TCP/handshake | unused Redis |
| INF-03 | Kafka unavailable | bootstrap config + broker evidence | serialization |
| INF-04 | Docker unavailable | daemon probe | stopped service only |
| INF-05 | Docker service stopped | compose service + state | daemon down |
| INF-06 | Container localhost mistake | container context + service reachability | DB down |
| CFG-01 | Missing environment variable | required reference + absence | wrong profile |
| CFG-02 | Blank property | resolved blank + binding/use site | missing property |
| CFG-03 | Malformed property | parser/binder error + source | type converter bug |
| CFG-04 | Trailing newline secret | value shape + consumer boundary | missing/invalid name |
| CFG-05 | Incorrect profile | active/available profiles | missing config |

Rules must require scenario-specific causal or discriminating evidence; suspicious configuration alone never diagnoses a failure.
