# Security policy

## Supported versions

Security fixes are applied to the latest released version.

## Reporting a vulnerability

Do not open a public issue containing a vulnerability, credential, token, private key, customer log, or unredacted configuration. Use GitHub's private vulnerability reporting feature on the repository Security tab. Include the affected version, impact, a minimal sanitized reproduction, and any suggested mitigation.

DevDoctor treats secret leakage as a release-blocking defect. Reports involving suspected leakage should identify the output surface—terminal, logs, JSON, saved session, or external-reasoner payload—without including the secret itself.
