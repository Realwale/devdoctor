# Benchmark methodology

Each fixture has a machine-readable manifest containing failure definition, ground-truth root cause, required evidence, hypotheses expected to be eliminated, forbidden diagnoses, allowed probes, and secret canaries. The harness runs fixtures in isolation and records top-1 accuracy, top-3 accuracy, false-positive count, duration, probe count, evidence completeness, and secret leakage.

Healthy fixtures are first-class and penalize manufactured diagnoses. A correct category without the required evidence chain is incomplete. Results include tool version, fixture commit, Java/OS details, repetitions, timeouts, and raw sanitized session artifacts.

Human/tool comparisons measure time to correct root cause, first-diagnosis accuracy, interactions, manually supplied context, and false-root-cause rate under the same fixture and time budget. Developer + search, ChatGPT, Claude, coding agents, and DevDoctor can be evaluated, but no competitor result is published without a recorded reproducible run.
