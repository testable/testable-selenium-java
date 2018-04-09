# Introduction

This library allows you to write Selenium Java tests that integrate with the Testable platform. When 

# Getting Started

When developing locally include the following artifact in your build:

```xml
<dependency>
  <groupId>io.testable</groupId>
  <artifactId>testable-selenium-java</artifactId>
  <version>0.0.2</version>
</dependency>
```


An example test would look as follows:

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

When this example is run outside of Testable it will instantiate a
`RemoteWebDriver` instance with URL `http://localhost:4444/wd/hub`.

The `takeScreenshot(driver, name)` method will simply take the screenshot and
return the path when run locally. On the Testable platform it will also
copy the screenshot into the appropriate output folder to be reported
back as part of the test results.
