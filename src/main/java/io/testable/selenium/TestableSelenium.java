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
    public static final String OUTPUT_DIR = System.getProperty("OUTPUT_DIR");
    public static final String REGION_NAME = System.getProperty("TESTABLE_REGION_NAME");
    public static final String GLOBAL_CLIENT_INDEX = System.getProperty("TESTABLE_GLOBAL_CLIENT_INDEX");
    public static final String ITERATION = System.getProperty("TESTABLE_ITERATION");
    public static final String PROXY_AUTOCONFIG_URL = System.getProperty("PROXY_AUTOCONFIG_URL");
    public static final String CHROME_BINARY_PATH = System.getProperty("CHROME_BINARY_PATH");
    public static final String FIREFOX_BINARY_PATH = System.getProperty("FIREFOX_BINARY_PATH");
    public static final String EDGE_BINARY_PATH = System.getProperty("EDGE_BINARY_PATH");
    public static final String PROFILE_DIR = System.getProperty("TESTABLE_PROFILE_DIR");
    public static final String RESULT_FILE = System.getProperty("TESTABLE_RESULT_FILE");

    public static final String DISPLAY_SIZE = System.getProperty("DISPLAY_SIZE");
    public static final String USER_AGENT = System.getProperty("USER_AGENT");
    public static final String SCALE_FACTOR = System.getProperty("SCALE_FACTOR");

    private static String RUM_SPEEDINDEXJS;
    static {
        try {
            URL url = Resources.getResource("rum-speedindex.js");
            RUM_SPEEDINDEXJS = Resources.toString(url, Charsets.UTF_8);
        } catch(Exception e) {
            System.out.println("Issue reading rum-speedindex.js from file");
            e.printStackTrace();
            RUM_SPEEDINDEXJS = null;
        }
    }


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

    /**
     * Create a {@link RemoteWebDriver} instance that is compatible with Testable. All this means is that the URL
     * will be http://localhost:[port]/wd/hub where [port] is passed from Testable to the Selenium process as
     * the SELENIUM_PORT system property. When run locally outside Testable, the default Selenium port (4444) is used.
     *
     * @param capabilities Capabilities to utilize
     * @return A WebDriver instance that is compatible with the local Testable Selenium instance.
     */
    public static WebDriver newWebDriver(Capabilities capabilities) {
        try {
            Capabilities caps;
            if (capabilities instanceof DesiredCapabilities) {
                DesiredCapabilities desiredCapabilities = (DesiredCapabilities) capabilities;
                String browserName = desiredCapabilities.getBrowserName();
                if (browserName == null)
                    throw new RuntimeException("DesiredCapabilities with no browser passed");
                if (browserName.equals(BrowserType.CHROME))
                    caps = new ChromeOptions();
                else if (browserName.equals(BrowserType.FIREFOX))
                    caps = new FirefoxOptions();
                else if (browserName.equals(BrowserType.EDGE))
                    caps = new EdgeOptions();
                else
                    throw new RuntimeException("Currently only Chrome and Firefox are supported on Testable");
                caps.merge(desiredCapabilities);
            } else {
                caps = capabilities;
            }
            if (caps instanceof MutableCapabilities) {
                if (PROXY_AUTOCONFIG_URL != null && caps instanceof ChromeOptions) {
                    ChromeOptions opts = (ChromeOptions)caps;
                    opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
                    opts.addArguments(
                            "--proxy-pac-url=" + PROXY_AUTOCONFIG_URL,
                            "--always-authorize-plugins",
                            "--disable-gpu",
                            "--no-sandbox",
                            "--whitelisted-ips",
                            "--enable-precise-memory-info",
                            "--ignore-certificate-errors");
                    if (PROFILE_DIR != null)
                        opts.addArguments("--user-data-dir=" + PROFILE_DIR);
                    opts.addArguments("--profile-directory=Profile" + GLOBAL_CLIENT_INDEX);

                    opts.addArguments("--window-position=0,0");

                    if(SCALE_FACTOR != null) {
                        opts.addArguments("--force-device-scale-factor=" + SCALE_FACTOR);
                        opts.addArguments("--high-dpi-support=" + SCALE_FACTOR);
                    }

                    if (DISPLAY_SIZE != null) {
                        String[] screenSize = DISPLAY_SIZE.split("x", -1);
                        Float width = Float.parseFloat(screenSize[0]);
                        Float height = Float.parseFloat(screenSize[1]);

                        if (SCALE_FACTOR != null) {
                            width = width / Float.parseFloat(SCALE_FACTOR);
                            height = height / Float.parseFloat(SCALE_FACTOR);
                        }

                        opts.addArguments("--window-size=" + Math.round(width) + "," + Math.round(height));
                    }

                    if(USER_AGENT != null){
                        String[] screenSize = DISPLAY_SIZE.split("x", -1);
                        Float width = Float.parseFloat(screenSize[0]);
                        Float height = Float.parseFloat(screenSize[1]);

                        Map<String, Object> mobileEmulation = new HashMap<>();
                        Map<String, Object> deviceMetrics = new HashMap<>();

                        if (SCALE_FACTOR != null) {
                            width = width / Float.parseFloat(SCALE_FACTOR);
                            height = height / Float.parseFloat(SCALE_FACTOR);

                            deviceMetrics.put("width", Math.round(width));
                            deviceMetrics.put("height", Math.round(height));
                            deviceMetrics.put("pixelRatio", Float.parseFloat(SCALE_FACTOR));
                        }
                        mobileEmulation.put("deviceMetrics", deviceMetrics);
                        mobileEmulation.put("userAgent", USER_AGENT);

                        opts.setExperimentalOption("mobileEmulation", mobileEmulation);
                    }

                    if (CHROME_BINARY_PATH != null)
                        opts.setBinary(CHROME_BINARY_PATH);
                } else if (caps instanceof FirefoxOptions) {
                    FirefoxOptions opts = (FirefoxOptions)caps;
                    opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
                    opts.addPreference("browser.tabs.remote.autostart", false);
                    opts.addPreference("browser.tabs.remote.autostart.2", false);
                    opts.addPreference("dom.webnotifications.enabled", false);
                    opts.addPreference("dom.push.connection.enabled", false);
                    opts.addPreference("dom.push.enabled", false);
                    opts.addPreference("dom.push.alwaysConnect", false);
                    if (PROXY_AUTOCONFIG_URL != null) {
                        opts.addPreference("network.proxy.type", 2);
                        opts.addPreference("network.proxy.autoconfig_url", PROXY_AUTOCONFIG_URL);
                    }
                    opts.addPreference("browser.startup.page", 0);
                    opts.addPreference("network.captive-portal-service.enabled", false);
                    opts.addPreference("browser.newtabpage.activity-stream.disableSnippets", true);
                    opts.addPreference("browser.newtabpage.activity-stream.feeds.snippets", false);
                    opts.addPreference("services.sync.prefs.sync.browser.newtabpage.activity-stream.feeds.snippets", false);
                    if (FIREFOX_BINARY_PATH != null)
                        opts.setBinary(FIREFOX_BINARY_PATH);

                    if(USER_AGENT != null){
                        opts.addPreference("general.useragent.override", USER_AGENT);
                        opts.addPreference("layout.css.devPixelsPerPx", SCALE_FACTOR);
                    }

                    if (DISPLAY_SIZE != null) {
                        String[] screenSize = DISPLAY_SIZE.split("x", -1);

                        Float width = Float.parseFloat(screenSize[0]);
                        Float height = Float.parseFloat(screenSize[1]);

                        if (SCALE_FACTOR != null) {
                            width = width / Float.parseFloat(SCALE_FACTOR);
                            height = height / Float.parseFloat(SCALE_FACTOR);
                        }

                        opts.addArguments("--width", "" + Math.round(width));
                        opts.addArguments("--height", "" + Math.round(height));
                    }
                } else if (PROXY_AUTOCONFIG_URL != null && caps instanceof EdgeOptions) {
                    EdgeOptions edgeOptions = (EdgeOptions)caps;
                    edgeOptions.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

                    List<String> args = new ArrayList<>();
                    Map<String, Object> experimentalOptions = new HashMap<>();
                    Map<String, Object> options = new TreeMap<>();

                    args.add("--proxy-pac-url=" + PROXY_AUTOCONFIG_URL);
                    args.add("--always-authorize-plugins");
                    args.add("--disable-gpu");
                    args.add("--no-sandbox");
                    args.add("--whitelisted-ips");
                    args.add("--enable-precise-memory-info");
                    args.add("--ignore-certificate-errors");

                    if (PROFILE_DIR != null)
                        args.add("--user-data-dir=" + PROFILE_DIR);
                    args.add("--profile-directory=Profile" + GLOBAL_CLIENT_INDEX);

                    args.add("--window-position=0,0");

                    if(SCALE_FACTOR != null) {
                        args.add("--force-device-scale-factor=" + SCALE_FACTOR);
                    }

                    if (DISPLAY_SIZE != null) {
                        String[] screenSize = DISPLAY_SIZE.split("x", -1);
                        Float width = Float.parseFloat(screenSize[0]);
                        Float height = Float.parseFloat(screenSize[1]);

                        if (SCALE_FACTOR != null) {
                            width = width / Float.parseFloat(SCALE_FACTOR);
                            height = height / Float.parseFloat(SCALE_FACTOR);
                        }

                        args.add("--window-size=" + Math.round(width) + "," + Math.round(height));
                    }

                    if(USER_AGENT != null){
                        String[] screenSize = DISPLAY_SIZE.split("x", -1);
                        Float width = Float.parseFloat(screenSize[0]);
                        Float height = Float.parseFloat(screenSize[1]);

                        Map<String, Object> deviceMetrics = new HashMap<>();

                        if (SCALE_FACTOR != null) {
                            width = width / Float.parseFloat(SCALE_FACTOR);
                            height = height / Float.parseFloat(SCALE_FACTOR);
                            deviceMetrics.put("width", Math.round(width));
                            deviceMetrics.put("height", Math.round(height));
                            deviceMetrics.put("pixelRatio", Float.parseFloat(SCALE_FACTOR));
                        }

                        Map<String, Object> mobileEmulation = new HashMap<>();
                        mobileEmulation.put("deviceMetrics", deviceMetrics);
                        mobileEmulation.put("userAgent", USER_AGENT);

                        options.put("mobileEmulation", mobileEmulation);
                    }

                    if (EDGE_BINARY_PATH != null)
                        options.put("binary", EDGE_BINARY_PATH);

                    options.put("args", ImmutableList.copyOf(args));

                    edgeOptions.setCapability("ms:edgeOptions", options);
                }
            }
            int port = SELENIUM_PORT > 0 ? SELENIUM_PORT : 4444;
            return new RemoteWebDriver(new URL("http://localhost:" + port + "/wd/hub"), caps);
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
                return Files.copy(screenshot.toPath(), Paths.get(OUTPUT_DIR, toName(name)));
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
     * Collect some useful browser performance metrics by executing some Javascript in the browser. Metrics
     * include: page load time, speed index, page requests, page weight, time to first byte, time to first paint,
     * time to first contentful paint, and time to interactive.
     *
     * @param driver The WebDriver instance
     */
    public static Map<String,Object> collectPerformanceMetrics(WebDriver driver) {
        if (RUM_SPEEDINDEXJS != null) {
            Map<String, Object> results = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript(RUM_SPEEDINDEXJS);
            writeToStream(new Result("BrowserMetrics", results));
            return results;
        }
        return Collections.emptyMap();
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
