package io.testable.selenium;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestableFinishSuiteTest {

    private final String suiteUuid;
    private final String suiteName;
    private final String uuid;
    private final String name;
    private final long finished;
    private final long duration;
    private final String state;
    private final String errorType;
    private final String error;
    private final String errorTrace;

    private TestableFinishSuiteTest(TestableStartSuiteTest startSuiteTest,
                                    boolean passed,
                                    Throwable t,
                                    boolean captureTrace,
                                    boolean skipped) {
        this.suiteUuid = startSuiteTest.getSuiteUuid();
        this.suiteName = startSuiteTest.getSuiteName();
        this.uuid = startSuiteTest.getUuid();
        this.name = startSuiteTest.getName();
        this.finished = startSuiteTest.getStarted() > 0 ? System.currentTimeMillis() : 0;
        this.duration = startSuiteTest.getStarted() > 0 ? this.finished - startSuiteTest.getStarted() : 0;
        this.state = t!= null ? "failed": (skipped ? "skipped" : (passed ? "passed" : "na"));
        if (t != null) {
            this.errorType = t.getClass().getSimpleName();
            this.error = t.getMessage();
            if (captureTrace) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                this.errorTrace = sw.toString();
            } else {
                this.errorTrace = null;
            }
        } else {
            this.errorType = null;
            this.error = null;
            this.errorTrace = null;
        }
    }

    public static TestableFinishSuiteTest passed(TestableStartSuiteTest startSuiteTest) {
        return new TestableFinishSuiteTest(startSuiteTest, true, null, false, false);
    }

    public static TestableFinishSuiteTest skipped(TestableStartSuiteTest startSuiteTest) {
        return new TestableFinishSuiteTest(startSuiteTest, false, null, false, true);
    }

    public static TestableFinishSuiteTest failed(TestableStartSuiteTest startSuiteTest, Throwable t) {
        return new TestableFinishSuiteTest(startSuiteTest, false, t, true, false);
    }

    public static TestableFinishSuiteTest failed(TestableStartSuiteTest startSuiteTest, String msg) {
        return new TestableFinishSuiteTest(startSuiteTest, false, new Exception(msg), false, false);
    }

    public String getSuiteUuid() { return suiteUuid; }

    public String getSuiteName() { return suiteName; }

    public String getUuid() { return uuid; }

    public String getName() { return name; }

    public long getFinished() { return finished; }

    public long getDuration() { return duration; }

    public String getState() { return state; }

    public String getErrorType() { return errorType; }

    public String getError() { return error; }

    public String getErrorTrace() { return errorTrace; }
}
