package io.testable.selenium;

import java.util.UUID;

public class TestableStartSuiteTest {

    private final String suiteUuid;
    private final String suiteName;
    private final String uuid;
    private final String name;
    private final long started;

    public TestableStartSuiteTest(TestableStartSuite startSuite, String name, long started) {
        this.suiteUuid = startSuite.getUuid();
        this.suiteName = startSuite.getName();
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.started = started;
    }

    public String getSuiteUuid() { return suiteUuid; }

    public String getSuiteName() { return suiteName; }

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