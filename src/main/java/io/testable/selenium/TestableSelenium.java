package io.testable.selenium;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point for creating a selenium driver for use on the Testable platform as well as a set of useful
 * utilities like taking screenshots.
 */
public class TestableSelenium {

    public static final int SELENIUM_PORT = Integer.getInteger("SELENIUM_PORT", -1);
    public static final String OUTPUT_DIR = System.getProperty("OUTPUT_DIR");
    public static final String REGION_NAME = System.getProperty("TESTABLE_REGION_NAME");
    public static final String REGIONAL_CLIENT_INDEX = System.getProperty("TESTABLE_REGIONAL_CLIENT_INDEX");
    public static final String ITERATION = System.getProperty("TESTABLE_ITERATION");
    public static final String PROXY_AUTOCONFIG_URL = System.getProperty("PROXY_AUTOCONFIG_URL");

    public static WebDriver newWebDriver(Capabilities capabilities) {
        try {
            if (PROXY_AUTOCONFIG_URL != null && capabilities instanceof MutableCapabilities) {
                Proxy proxy = new Proxy();
                proxy.setProxyType(Proxy.ProxyType.PAC);
                proxy.setProxyAutoconfigUrl(PROXY_AUTOCONFIG_URL);
                ((MutableCapabilities)capabilities).setCapability(CapabilityType.PROXY, proxy);
            }
            int port = SELENIUM_PORT > 0 ? SELENIUM_PORT : 4444;
            return new RemoteWebDriver(new URL("http://localhost:" + port + "/wd/hub"), capabilities);
        } catch (MalformedURLException e) {
            throw new WebDriverException(e);
        }
    }

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

    private static String toName(String name) {
        if (REGION_NAME != null) {
            return REGION_NAME + "-" + REGIONAL_CLIENT_INDEX + "-" + ITERATION + "-" + name;
        } else {
            return name;
        }
    }

}
