package io.testable.selenium;

import java.util.UUID;

public class TestableStartSuite {

    private final String uuid;
    private final String name;
    private final long started;

    public TestableStartSuite(String name, long started) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.started = started;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public long getStarted() {
        return started;
    }

}