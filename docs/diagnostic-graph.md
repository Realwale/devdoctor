# Diagnostic graph

The graph is the canonical explanation, not a rendering detail. It is a directed typed multigraph held in memory as immutable node and edge lists. Node payloads remain in their domain collections; graph nodes carry only ID, type, and a safe label to prevent duplicated sensitive content.

Graph validation rejects blank IDs, duplicate node IDs, dangling edge endpoints, self-inconsistent relationships, and unredacted labels. JSON consumers can reconstruct evidence chains by joining IDs. Terminal reports traverse root cause -> hypothesis -> evidence/probe paths and separately display contradictions and ruled-out hypotheses.

Persistence uses the V1 JSON session document. A graph database would add operational cost without improving MVP diagnosis and is deferred until measured query or scale requirements justify it.
