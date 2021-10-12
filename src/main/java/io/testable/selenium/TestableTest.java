package io.testable.selenium;

/**
 * Keep track of a suite of test steps that you want to report back into the Testable results. Make sure to call
 * the finished() method when the test is completed.
 */
public class TestableTest {

    private final TestableStartSuite startSuite;
    private TestableStartSuiteTest currentTest = null;
    private boolean hasError = false;

    TestableTest(String name) {
        this.startSuite = new TestableStartSuite(name, System.currentTimeMillis());
        write("StartSuite", this.startSuite);
    }

    /**
     * Runs your code and reports back as a test step into the test result. Any exception that occurs in your
     * code is captured and considered to be a test step failure.
     * @param name Test step name
     * @param step Code to run
     */
    public void runStep(String name, Runnable step) {
        try {
            startStep(name);
            step.run();
            finishSuccessfulStep();
        } catch(Exception e) {
            finishFailedStep(e);
        }
    }

    /**
     * Indicate to Testable that you are starting a test step. It is expected that you will call one of the
     * finishXXXStep() methods with the results.
     *
     * @param name Name of the test step
     */
    public void startStep(String name) {
        currentTest = new TestableStartSuiteTest(startSuite, name, System.currentTimeMillis());
        write("StartSuiteTest", currentTest);
    }

    /**
     * Indicates that the test step previously started with startStep(name) finished successfully.
     */
    public void finishSuccessfulStep() {
        finishStep(TestableFinishSuiteTest.passed(currentTest));
    }

    /**
     * Indicates that the test step previously started with startStep(name) finished with an exception.
     *
     * @param t The error that occurred while running the test step.
     */
    public void finishFailedStep(Throwable t) {
        finishStep(TestableFinishSuiteTest.failed(currentTest, t));
    }

    /**
     * Indicates that the test step previously started with startStep(name) finished with a failure.
     *
     * @param errorMsg The error message
     */
    public void finishFailedStep(String errorMsg) {
        finishStep(TestableFinishSuiteTest.failed(currentTest, errorMsg));
    }

    private void finishStep(TestableFinishSuiteTest finishMsg) {
        if (currentTest != null) {
            write("FinishSuiteTest", finishMsg);
            if (finishMsg.getError() != null)
                hasError = true;
            currentTest = null;
        }
    }

    /**
     * Indicates that an assertion passed and the duration that it took.
     *
     * @param assertion The assertion description
     * @param duration The duration it took to run assertion related code, can be 0.
     */
    public void assertionPassed(String assertion, long duration) {
        currentTest = new TestableStartSuiteTest(startSuite, assertion, System.currentTimeMillis() - duration);
        write("StartSuiteTest", currentTest);
        finishStep(TestableFinishSuiteTest.passed(currentTest));
    }

    /**
     * Indicates that an assertion failed.
     *
     * @param assertion The assertion description
     * @param duration The duration it took to run assertion related code, can be 0.
     * @param t The error that occurred
     */
    public void assertionFailed(String assertion, long duration, Throwable t) {
        currentTest = new TestableStartSuiteTest(startSuite, assertion, System.currentTimeMillis() - duration);
        write("StartSuiteTest", currentTest);
        finishStep(TestableFinishSuiteTest.failed(currentTest, t));
    }

    /**
     * Indicates that an assertion failed.
     *
     * @param assertion The assertion description
     * @param duration The duration it took to run assertion related code, can be 0.
     * @param errorMessage The error that occurred
     */
    public void assertionFailed(String assertion, long duration, String errorMessage) {
        currentTest = new TestableStartSuiteTest(startSuite, assertion, System.currentTimeMillis() - duration);
        write("StartSuiteTest", currentTest);
        finishStep(TestableFinishSuiteTest.failed(currentTest, errorMessage));
    }

    /**
     * Indicates that this test is done running. This will be reported back to the Testable results.
     */
    public void finish() {
        if (currentTest != null) {
            write("FinishSuiteTest", TestableFinishSuiteTest.passed(currentTest));
        }
        write("FinishSuite", new TestableFinishSuite(this.startSuite, hasError));
    }

    private static void write(String type, Object event) {
        TestableSelenium.writeToStream(new TestableSelenium.Result(type, event));
    }

}
