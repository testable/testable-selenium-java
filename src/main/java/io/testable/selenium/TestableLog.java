package io.testable.selenium;

public class TestableLog {

    public enum Level { Trace, Debug, Info, Error, Fatal }

    private Level level;
    private String message;
    private long timestamp;

    public TestableLog(Level level, String message, long timestamp) {
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
