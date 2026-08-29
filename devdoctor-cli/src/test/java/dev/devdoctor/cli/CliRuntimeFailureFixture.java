package dev.devdoctor.cli;

public final class CliRuntimeFailureFixture {
    private CliRuntimeFailureFixture() { }

    public static void main(String[] args) throws Exception {
        long deadline = System.nanoTime() + 8_000_000_000L;
        while (System.nanoTime() < deadline) {
            try { fail(); } catch (IllegalArgumentException ignored) { }
            Thread.sleep(10);
        }
    }

    private static void fail() {
        throw new IllegalArgumentException("Runtime validation failed in application code");
    }
}
