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

public abstract class BaseE2eTest {

    protected static Playwright playwright;
    protected static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    public static void launchBrowser() {
        playwright = PlaywrightSupport.get().playwright();
        browser = PlaywrightSupport.get().launchBrowser(playwright);
    }

    @AfterAll
    public static void closeBrowser() {
        PlaywrightSupport.get().close(browser, playwright);
    }

    @BeforeEach
    void createContextAndPage() {
        final Tuple2<BrowserContext, Page> contextAndPage = PlaywrightSupport.get().createContextAndPage(browser);
        context = contextAndPage._1;
        page = contextAndPage._2;
    }

    @AfterEach
    void closeContextAndPage() {
        PlaywrightSupport.get().close(page, context);
    }

    public String getProperty(final String key) {
        return EnvironmentService.get().getProperty(key);
    }

    public String getProperty(final String key, final String defaultValue) {
        return EnvironmentService.get().getProperty(key, defaultValue);
    }

    public String getBaseUrl() {
        return getProperty(E2eKeys.E2E_BASE_URL_KEY, E2eKeys.DEFAULT_BASE_URL);
    }

    public void navigateToBaseUrl() {
        page.navigate(getBaseUrl());
    }

}
