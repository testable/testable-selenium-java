* [Introduction](#introduction)
* [Getting Started](#getting-started)
* [API](#api)
  * [Screenshots](#screenshots)
  * [Assertions/Test Steps](#assertions)
  * [Custom Metrics](#custom-metrics)
  * [Logging](#logging)
  * [Read from CSV](#read-from-csv)
  * [Browser Performance Metrics](#browser-performance-metrics)

# Introduction

This library allows you to write Selenium Java tests that integrate with the Testable platform. When 

# Getting Started

When developing locally include the following artifact in your build:

```xml
<dependency>
  <groupId>io.testable</groupId>
  <artifactId>testable-selenium-java</artifactId>
  <version>0.0.14</version>
</dependency>
```


A simple example test would look as follows:

```java
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.testable.selenium.TestableSelenium;

public class TestableExample {

    public static void main(String[] args) throws Exception {
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = TestableSelenium.newWebDriver(options);
        driver.get("https://www.google.com");
        driver.close();
    }

}
```

When this example is run outside of Testable it will instantiate a
`RemoteWebDriver` instance with URL `http://localhost:4444/wd/hub`.

# API

## Screenshots

The `takeScreenshot(driver, name)` method will simply take the screenshot and
return the path when run locally. On the Testable platform it will also
copy the screenshot into the appropriate output folder to be reported
back as part of the test results.

```java
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.testable.selenium.TestableSelenium;

public class TestableExample {

    public static void main(String[] args) throws Exception {
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = TestableSelenium.newWebDriver(options);
        driver.get("https://www.google.com");
        TestableSelenium.takeScreenshot(driver, "HomePage.png");
        driver.close();
    }

}
```

### Assertions/Test Steps

Capture assertions or test steps as part of the test results including test step description, 
duration, and any errors that occurred.

In the results you can see these within the Assertions widget. For example:

```java
TestableTest test = TestableSelenium.startTest("Google Related");
test.startStep("Open google home page");
driver.get("https://www.google.com");
test.finishSuccessfulStep(); // or test.finishFailedStep("My error message");
test.runStep("Open google news using Runnable", new Runnable() {
    public void run() {
        driver.get("https://news.google.com"); // any exception here gets logged as a test step failure
    }
});
test.finish();
```

## Custom Metrics

Capture a custom counter, timing, or histogram metric. When run on Testable
it is reported and aggregated into the test results. When run locally
it will simply output the metric details to the console.

See https://testable.io/documentation/scripts/custom-metrics.html for
more details.

Counter Example:

```java
TestableSelenium.reportMetric(TestableMetric.newCounterBuilder()
        .withName("My Request Counter")
        .withVal(1)
        .withUnits("requests")
        .build());
```

Add 1 to the "My Request Counter" metric.

Timing Example:

```java
long start = System.currentTimeMillis();
driver.get("https://www.google.com");
long loadTime = System.currentTimeMillis() - start;
TestableSelenium.reportMetric(TestableMetric.newTimingBuilder()
        .withName("Page Load Time")
        .withVal(loadTime)
        .withUnits("ms")
        .build());
```

Capture how long it takes to open https://www.google.com and capture that
as the "Page Load Time" metric.

Histogram Example:

```java
String status = "MyStatus";
TestableSelenium.reportMetric(TestableMetric.newHistogramBuilder()
        .withName("Status Histogram")
        .withKey(status)
        .withVal(1)
        .build());
```

Add 1 to the "Status Histogram" metric.



## Logging

Log a message or exception into the test results at the specified level.
When run locally it simply outputs to the console.

Trace logging will only be output during a smoke test.
Fatal logging will cause the test run to stop immediately.

Example:

```java
TestableSelenium.log(TestableLog.Level.Trace, "detailed stuff for smoke test only");
TestableSelenium.log(TestableLog.Level.Debug, "my debug message");
TestableSelenium.log(TestableLog.Level.Info, "some info");
TestableSelenium.log(TestableLog.Level.Error, new RuntimeException("An error occurred"));
TestableSelenium.log(TestableLog.Level.Fatal, new RuntimeException("Something bad happened stop everything!"));
```

## Read from CSV

Read from a CSV file that has been uploaded to your scenario. When run locally
this will load the CSV from the classpath or current working directory.
It is assumed your CSV has a header row with column names.

**Get row by index**: Return a row by index. The first row after the header row
is considered row 0.

```java
TestableCSVReader reader = TestableSelenium.readCsv("credentials.csv");
CSVRecord record = reader.get(2);
System.out.println(record.get("username"));
```

**Get random row**: Returns a random row from the CSV.

```java
TestableCSVReader reader = TestableSelenium.readCsv("credentials.csv");
CSVRecord record = reader.random();
System.out.println(record.get("username"));
```

**Get the next row**: Return the next row in the CSV **using a global iterator**.
This means that the rows in the CSV will be evenly distributed across all
virtual users that are part of your test execution.

```java
TestableCSVReader reader = TestableSelenium.readCsv("credentials.csv");
CSVRecord record = reader.next();
System.out.println(record.get("username"));
```