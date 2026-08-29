package dev.devdoctor.engine.observe;

public final class RuntimeFailureFixture {
    private RuntimeFailureFixture() { }

    public static void main(String[] args) throws Exception {
        long deadline = System.nanoTime() + 8_000_000_000L;
        while (System.nanoTime() < deadline) {
            try { fail(); } catch (IllegalStateException ignored) { }
            Thread.sleep(10);
        }
    }

    private static void fail() {
        throw new IllegalStateException("Required environment variable DEVDOCTOR_RUNTIME_TOKEN is missing");
    }
}
