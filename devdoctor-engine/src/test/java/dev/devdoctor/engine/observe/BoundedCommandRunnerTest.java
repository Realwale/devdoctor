package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;
import dev.devdoctor.engine.security.SecretRedactor;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoundedCommandRunnerTest {
    @Test void timesOutCapsAndRedacts(@TempDir Path root) {
        var runner = new BoundedCommandRunner(new SecretRedactor(), Duration.ofMillis(200), 64);
        var result = runner.run(root, "printf 'password=visible-secret\\n'; while :; do printf x; done");
        assertThat(result.timedOut()).isTrue(); assertThat(result.truncated()).isTrue(); assertThat(result.stdout()).doesNotContain("visible-secret").contains("[REDACTED]");
    }
}
