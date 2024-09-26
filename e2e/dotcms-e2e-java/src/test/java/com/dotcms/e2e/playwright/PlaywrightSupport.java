package com.dotcms.e2e.playwright;

import com.dotcms.e2e.E2eKeys;
import com.dotcms.e2e.browser.BrowserType;
import com.dotcms.e2e.logging.Logger;
import com.dotcms.e2e.service.EnvironmentService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Support class for Playwright operations.
 *
 * This singleton class provides methods to interact with Playwright, including launching browsers,
 * creating contexts and pages, and closing resources.
 *
 * Example usage:
 * <pre>
 *  PlaywrightSupport support = PlaywrightSupport.get();
 *  Playwright playwright = support.playwright();
 *  Browser browser = support.launchBrowser(playwright);
 *  Tuple2<BrowserContext, Page> contextAndPage = support.createContextAndPage(browser);
 * </pre>
 *
 * Methods:
 * <ul>
 *   <li>{@link #get()} - Returns the singleton instance of the support class.</li>
 *   <li>{@link #playwright()} - Creates a new Playwright instance.</li>
 *   <li>{@link #isCi()} - Checks if the current environment is a CI environment.</li>
 *   <li>{@link #getBrowser()} - Retrieves the browser type from the environment properties.</li>
 *   <li>{@link #launchBrowser(Playwright)} - Launches a browser based on the environment properties.</li>
 *   <li>{@link #close(AutoCloseable...)} - Closes the provided resources.</li>
 *   <li>{@link #createContextAndPage(Browser)} - Creates a new browser context and page.</li>
 *   <li>{@link #resolveBrowser(Playwright, String)} - Resolves the browser type from a string.</li>
 *   <li>{@link #assertVisibleTimeout(double)} - Creates visibility timeout options for assertions.</li>
 * </ul>
 *
 * @author vico
 */
public class PlaywrightSupport {

    private static final double DEFAULT_TIMEOUT = 10000;

    private static final PlaywrightSupport INSTANCE = new PlaywrightSupport();

    public static PlaywrightSupport get() {
        return INSTANCE;
    }

    private final boolean ci;

    private PlaywrightSupport() {
        ci = Boolean.parseBoolean(EnvironmentService.get().getProperty(E2eKeys.CI_KEY));
    }

    /**
     * Creates a new Playwright instance.
     *
     * @return a new Playwright instance
     */
    public Playwright playwright() {
        return Playwright.create();
    }

    /**
     * Checks if the current environment is a CI environment.
     *
     * @return true if the current environment is a CI environment, false otherwise
     */
    public boolean isCi() {
        return ci;
    }

    /**
     * Retrieves the browser type from the environment properties.
     *
     * @return the browser type as a String
     */
    public String getBrowser() {
        return EnvironmentService.get().getProperty(E2eKeys.E2E_BROWSER_KEY);
    }

    /**
     * Launches a browser based on the environment properties.
     *
     * @param playwright the Playwright instance
     * @return the launched Browser instance
     */
    public Browser launchBrowser(final Playwright playwright) {
        return resolveBrowser(playwright, getBrowser().toUpperCase())
                .launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(isCi()));
    }

    /**
     * Closes the provided resources.
     *
     * @param stuffToClose the resources to be closed
     * @param <C> the type of AutoCloseable resources
     */
    @SafeVarargs
    public final <C extends AutoCloseable> void close(C... stuffToClose) {
        Stream.of(stuffToClose)
                .filter(Objects::nonNull)
                .forEach(closeable -> Try
                        .run(closeable::close)
                        .onFailure(ex -> Logger.warn(
                                PlaywrightSupport.class,
                                "Could not close Playwright component",
                                ex)));
    }

    /**
     * Creates a new browser context and page.
     *
     * @param browser the Browser instance
     * @return a tuple containing the BrowserContext and Page
     */
    public Tuple2<BrowserContext, Page> createContextAndPage(final com.microsoft.playwright.Browser browser) {
        final BrowserContext context = browser.newContext();
        final Page page = context.newPage();
        return new Tuple2<>(context, page);
    }

    /**
     * Resolves the browser type from a string.
     *
     * @param playwright the Playwright instance
     * @param browser the browser type as a string
     * @return the resolved BrowserType
     */
    public com.microsoft.playwright.BrowserType resolveBrowser(final Playwright playwright, final String browser) {
        final com.microsoft.playwright.BrowserType resolved;
        switch (BrowserType.fromString(browser)) {
            case FIREFOX:
                resolved = playwright.firefox();
                break;
            case WEBKIT:
                resolved = playwright.webkit();
                break;
            case CHROMIUM:
            default:
                resolved = playwright.chromium();
        }

        return resolved;
    }

    /**
     * Creates visibility timeout options for assertions.
     *
     * @param timeout the timeout value in milliseconds
     * @return the visibility timeout options
     */
    public LocatorAssertions.IsVisibleOptions assertVisibleTimeout(final double timeout) {
        return new LocatorAssertions.IsVisibleOptions().setTimeout(timeout);
    }

    /**
     * Creates visibility timeout options for assertions.
     *
     * @return the visibility timeout options
     */
    public LocatorAssertions.IsVisibleOptions assertVisibleTimeout() {
        return assertVisibleTimeout(DEFAULT_TIMEOUT);
    }

}
