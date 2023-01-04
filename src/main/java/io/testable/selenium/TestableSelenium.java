package io.testable.selenium;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Main entry point for creating a selenium driver for use on the Testable platform as well as a set of useful
 * utilities like taking screenshots and reporting custom metrics.
 */
public class TestableSelenium {

    public static final int SELENIUM_PORT = Integer.getInteger("SELENIUM_PORT", -1);
    public static final int CHROMEDRIVER_PORT = Integer.getInteger("CHROMEDRIVER_PORT", -1);
    public static final String SELENIUM_REMOTE_URL = System.getProperty("SELENIUM_REMOTE_URL");
    public static final String OUTPUT_DIR = System.getProperty("OUTPUT_DIR");
    public static final String REGION_NAME = System.getProperty("TESTABLE_REGION_NAME");
    public static final String GLOBAL_CLIENT_INDEX = System.getProperty("TESTABLE_GLOBAL_CLIENT_INDEX");
    public static final String ITERATION = System.getProperty("TESTABLE_ITERATION");
    public static final String RESULT_FILE = System.getProperty("TESTABLE_RESULT_FILE");

    private static PrintWriter resultStream;
    static {
        try {
            resultStream = RESULT_FILE != null ? new PrintWriter(new FileWriter(RESULT_FILE, true)) : null;
        } catch (IOException ioe) {
            System.out.println("Issue writing to Testable result file");
            ioe.printStackTrace();
            resultStream = null;
        }
    }

    private static String WEBDRIVER_URL;
    static {
        if (SELENIUM_REMOTE_URL != null)
            WEBDRIVER_URL = SELENIUM_REMOTE_URL;
        else if (CHROMEDRIVER_PORT > 0)
            WEBDRIVER_URL = "http://localhost:" + CHROMEDRIVER_PORT;
        else
            WEBDRIVER_URL = "http://localhost:" + (SELENIUM_PORT > 0 ? SELENIUM_PORT : 4444) + "/wd/hub";
    }

    /**
     * Create a {@link RemoteWebDriver} instance that is compatible with Testable. All this means is that the URL
     * will be http://localhost:[port]/wd/hub where [port] is passed from Testable to the Selenium process as
     * the SELENIUM_PORT system property or http://localhost:[port] in cases where Testable is expecting to use
     * chromedriver directly (e.g. OpenFin). When run locally outside Testable, the default Selenium port (4444) is used.
     *
     * @param capabilities Capabilities to utilize
     * @return A WebDriver instance that is compatible with the local Testable Selenium instance.
     */
    public static WebDriver newWebDriver(Capabilities capabilities) {
        try {
            return new RemoteWebDriver(new URL(WEBDRIVER_URL), capabilities);
        } catch (MalformedURLException e) {
            throw new WebDriverException(e);
        }
    }

    /**
     * Takes a screenshot of the current browser and copies it to the appropriate output directory to be picked up
     * by the test runner (found at the OUTPUT_DIR system property). When run locally the file is not copied but the
     * screenshot is still taken.
     *
     * @param driver The WebDriver instance
     * @param name Name of the file to use. Actual file name when run on Testable will be
     *             [region_name]-[virtual_user]-[test_iteration]-[name] so you have context on which region,
     *             virtual user, and iteration of the test this image is associated with.
     * @return The path where the screenshot can be found.
     */
    public static Path takeScreenshot(WebDriver driver, String name) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            if (OUTPUT_DIR != null) {
                return Files.copy(screenshot.toPath(), Paths.get(OUTPUT_DIR, toName(name)), StandardCopyOption.REPLACE_EXISTING);
            } else {
                return screenshot.toPath();
            }
        } catch (IOException e) {
            throw new WebDriverException(e);
        }
    }

    /**
     * Report a custom metric into the test results. This can be a counter, timing, or histogram. When run locally the
     * metric will be output to the console.
     *
     * Example:
     *
     * <pre>
     * {@code TestableSelenium.reportMetric(TestableMetric.newCounterBuilder()
     *     .withName("My Request Counter")
     *     .withVal(1)
     *     .withUnits("requests")
     *     .build()); }
     * </pre>
     *
     * @param metric
     */
    public static void reportMetric(TestableMetric metric) {
        writeToStream(new Result(metric.getType().name(), metric));
    }

    /**
     * Log a message into the test results at the chosen level. When run outside Testable logging is simply written to
     * the console. Trace level logging is only available while smoke testing a scenario. Fatal logging will cause
     * your entire test run to stop.
     *
     * @param level The logging level
     * @param msg The message to log.
     */
    public static void log(TestableLog.Level level, String msg) {
        writeToStream(new Result("Log", new TestableLog(level, msg, System.currentTimeMillis())));
    }

    /**
     * Log an exception into the test results at the chosen level. The entire stack trace will be logged.
     * When run outside Testable logging is simply written to the console. Trace level logging is only available while
     * smoke testing a scenario. Fatal logging will cause your entire test run to stop.
     *
     * @param level The logging level
     * @param cause The exception to log
     */
    public static void log(TestableLog.Level level, Throwable cause) {
        String msg = Throwables.getStackTraceAsString(cause);
        writeToStream(new Result("Log", new TestableLog(level, msg, System.currentTimeMillis())));
    }

    /**
     * Read data from a CSV uploaded to your scenario. {@link TestableCSVReader} for more details of the available API.
     * When run locally, the CSV will be loaded from the local classpath.
     *
     * @param path Path to your CSV file. Relative to the classpath or working directory.
     * @return A {@link TestableCSVReader} instance to access the contents of the CSV in various ways.
     * @throws IOException
     */
    public static TestableCSVReader readCsv(String path) throws IOException {
        return new TestableCSVReader(path);
    }

    /**
     * Start a new set of test steps that you want to record and view in the Assertions widget within the Testable
     * test results. Allows you to track a series of test steps, whether they pass, any errors that occurred, and the
     * duration of each step.
     *
     * @param name The name of the test.
     * @return An object that lets you record test steps and their outcome
     */
    public static TestableTest startTest(String name) {
        return new TestableTest(name);
    }

    private static String toName(String name) {
        if (REGION_NAME != null) {
            return REGION_NAME + "-" + GLOBAL_CLIENT_INDEX + "-" + ITERATION + "-" + name;
        } else {
            return name;
        }
    }

    static void writeToStream(Result result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String text = mapper.writeValueAsString(result);
            if (resultStream != null) {
                resultStream.println(text);
                resultStream.flush();
            } else
                System.out.println("[" + result.getType() + "] " + text);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }

    static class Result {
        private String type;
        private Object data;

        public Result(String type, Object data) {
            this.type = type;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

}
