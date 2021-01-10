package io.testable.selenium;

import java.util.UUID;

public class TestableFinishSuite {

    private final String uuid;
    private final String name;
    private final long finished = System.currentTimeMillis();
    private final long duration;
    private final boolean hasError;

    public TestableFinishSuite(TestableStartSuite start, boolean hasError) {
        this.uuid = start.getUuid();
        this.name = start.getName();
        this.duration = System.currentTimeMillis() - start.getStarted();
        this.hasError = hasError;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public long getFinished() {
        return finished;
    }

    public long getDuration() { return duration; }

    public boolean isHasError() { return hasError; }
}