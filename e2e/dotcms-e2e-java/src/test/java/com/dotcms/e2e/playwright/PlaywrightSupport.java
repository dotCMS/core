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

public class PlaywrightSupport {

    private static final PlaywrightSupport INSTANCE = new PlaywrightSupport();

    public static PlaywrightSupport get() {
        return INSTANCE;
    }

    private PlaywrightSupport() {
    }

    public Playwright playwright() {
        return Playwright.create();
    }

    public boolean isCi() {
        return Boolean.parseBoolean(EnvironmentService.get().getProperty(E2eKeys.CI_KEY));
    }

    public String getBrowser() {
        return EnvironmentService.get().getProperty(E2eKeys.E2E_BROWSER_KEY);
    }

    public Browser launchBrowser(final Playwright playwright) {
        return resolveBrowser(playwright, getBrowser().toUpperCase())
                .launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(isCi()));
    }

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

    public Tuple2<BrowserContext, Page> createContextAndPage(final com.microsoft.playwright.Browser browser) {
        final BrowserContext context = browser.newContext();
        final Page page = context.newPage();
        return new Tuple2<>(context, page);
    }

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

    public LocatorAssertions.IsVisibleOptions visibleTimeout(final double timeout) {
        return new LocatorAssertions.IsVisibleOptions().setTimeout(timeout);
    }

}
