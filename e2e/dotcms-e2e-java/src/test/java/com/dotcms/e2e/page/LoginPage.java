package com.dotcms.e2e.page;

import com.dotcms.e2e.E2eKeys;
import com.dotcms.e2e.service.EnvironmentService;
import com.microsoft.playwright.Page;

public class LoginPage {

    private static final String USERNAME_LOCATOR = "input[id=inputtext]";
    private static final String PASSWORD_LOCATOR = "input[id=password]";
    private static final String LOGIN_BUTTON = "submitButton";
    private static final String LOGIN_LANGUAGE = "language";

    private final Page loginPage;

    /**
     * Constructor
     *
     * @param loginPage the page object
     */
    public LoginPage(final Page loginPage) {
        this.loginPage = loginPage;
    }

    public void login(final String user, final String password) {
        loginPage.locator(USERNAME_LOCATOR).fill(user);
        loginPage.locator(PASSWORD_LOCATOR).click();
        loginPage.locator(PASSWORD_LOCATOR).fill(password);
        loginPage.getByTestId(LOGIN_BUTTON).click();
    }

    public void successfulLogin() {
        final EnvironmentService environmentService = EnvironmentService.get();
        login(
                environmentService.getProperty(E2eKeys.DEFAULT_USER_KEY),
                environmentService.getProperty(E2eKeys.DEFAULT_PASSWORD_KEY));
    }

    /**
     * Switch the language of the login page
     *
     * @param language the language to switch to
     */
    public void switchLanguage(final String language) {
        loginPage.getByTestId(LOGIN_LANGUAGE).click();
        loginPage.getByLabel(language).click();
    }

}
