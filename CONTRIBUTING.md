# Contributing to DevDoctor

DevDoctor values diagnostic accuracy, evidence provenance, security, reproducibility, and low false-positive rates over feature count.

## Development setup

Use Java 21 and run:

```bash
./mvnw clean verify
./devdoctor benchmark --fixtures test-fixtures
```

Changes to diagnostic behavior should include a fixture manifest with ground truth, required evidence, expected hypothesis elimination, and secret canaries where applicable. Changes must preserve the read-only product boundary: no automatic repair, process killing, source mutation, Git mutation, or database mutation.

Never commit real secrets, customer logs, proprietary source, or unredacted configuration. Security-sensitive reports should follow `SECURITY.md` rather than a public issue.
