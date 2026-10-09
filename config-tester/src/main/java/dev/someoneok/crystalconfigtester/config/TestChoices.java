package dev.someoneok.crystalconfigtester.config;

/** Small enums shared by every selection/list control. */
public final class TestChoices {
    private TestChoices() { }

    public enum Mode {
        OFF("Off"), SIMPLE("Simple"), FANCY("Fancy"), DEBUG("Debug"), BENCHMARK("Benchmark");
        private final String title;
        Mode(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }

    public enum Category {
        GENERAL("General"), ADVANCED("Advanced");
        private final String title;
        Category(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }
}
