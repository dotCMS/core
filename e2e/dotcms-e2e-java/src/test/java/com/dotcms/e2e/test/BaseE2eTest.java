package com.dotcms.e2e.test;

import com.dotcms.e2e.E2eKeys;
import com.dotcms.e2e.playwright.PlaywrightSupport;
import com.dotcms.e2e.service.EnvironmentService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.vavr.Tuple2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for end-to-end tests.
 *
 * This class provides setup and teardown methods for Playwright browser contexts and pages,
 * as well as utility methods for retrieving environment properties and navigating to the base URL.
 *
 * Example usage:
 * <pre>
 *  public class MyE2eTest extends BaseE2eTest {
 *      // Test methods here
 *  }
 * </pre>
 *
 * Methods:
 * <ul>
 *   <li>{@link #launchBrowser()} - Launches the Playwright browser.</li>
 *   <li>{@link #closeBrowser()} - Closes the Playwright browser.</li>
 *   <li>{@link #createContextAndPage()} - Creates a new browser context and page.</li>
 *   <li>{@link #closeContextAndPage()} - Closes the current browser context and page.</li>
 *   <li>{@link #getProperty(String)} - Retrieves a property value by key.</li>
 *   <li>{@link #getProperty(String, String)} - Retrieves a property value by key with a default value.</li>
 *   <li>{@link #getBaseUrl()} - Retrieves the base URL for the tests.</li>
 *   <li>{@link #navigateToBaseUrl()} - Navigates to the base URL.</li>
 * </ul>
 *
 * @author vico
 */
public abstract class BaseE2eTest {

    protected static Playwright playwright;
    protected static Browser browser;

    protected BrowserContext context;
    protected Page page;

    /**
     * Launches the Playwright browser.
     */
    @BeforeAll
    public static void launchBrowser() {
        playwright = PlaywrightSupport.get().playwright();
        browser = PlaywrightSupport.get().launchBrowser(playwright);
    }

    /**
     * Closes the Playwright browser.
     */
    @AfterAll
    public static void closeBrowser() {
        PlaywrightSupport.get().close(browser, playwright);
    }

    /**
     * Creates a new browser context and page.
     */
    @BeforeEach
    void createContextAndPage() {
        final Tuple2<BrowserContext, Page> contextAndPage = PlaywrightSupport.get().createContextAndPage(browser);
        context = contextAndPage._1;
        page = contextAndPage._2;
    }

    /**
     * Closes the current browser context and page.
     */
    @AfterEach
    void closeContextAndPage() {
        PlaywrightSupport.get().close(page, context);
    }

    /**
     * Retrieves a property value by key.
     *
     * @param key the property key
     * @return the property value
     */
    public String getProperty(final String key) {
        return EnvironmentService.get().getProperty(key);
    }

    /**
     * Retrieves a property value by key with a default value.
     *
     * @param key the property key
     * @param defaultValue the default value if the property is not found
     * @return the property value
     */
    public String getProperty(final String key, final String defaultValue) {
        return EnvironmentService.get().getProperty(key, defaultValue);
    }

    /**
     * Retrieves the base URL for the tests.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return getProperty(E2eKeys.E2E_BASE_URL_KEY, E2eKeys.DEFAULT_BASE_URL);
    }

    /**
     * Retrieves the base URL for the tests.
     *
     * @return the base URL
     */
    public void navigateToBaseUrl() {
        page.navigate(getBaseUrl());
    }

}
