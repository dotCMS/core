package com.dotcms.e2e.test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.dotcms.e2e.page.LoginPage;
import com.dotcms.e2e.playwright.PlaywrightSupport;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for login functionality.
 *
 * This class provides test methods to validate the login functionality with different scenarios.
 * Ensures the environment is set up before each test case execution.
 *
 * @author vico
 */
@DisplayName("Login portlet tests")
public class LoginTests extends BaseE2eTest {

    private LoginPage loginPage;

    @BeforeEach
    public void setup() {
        loginPage = new LoginPage(page);
        page.navigate(getBaseUrl());
    }

    /**
     * Scenario: Successful login with the default user
     * Given the user is on the login page
     * When the user logs in with valid credentials
     * Then the user should be logged in successfully
     * And the user should see the dashboard
     */
    @Test
    public void test_loginSuccess() {
        loginPage.successfulLogin();
        assertThat(page.getByRole(AriaRole.IMG)).isVisible();
    }

    /**
     * Scenario: Unsuccessful login with a wrong user
     * Given the user is on the login page
     * When the user logs in with an invalid username
     * Then the user should see an error message
     */
    @Test
    public void test_loginWrongUser() {
        loginPage.login("jon.snow@got.com", "admin");
        assertThat(page.getByTestId("message")).isVisible();
    }

    /**
     * Scenario: Unsuccessful login with a wrong password
     * Given the user is on the login page
     * When the user logs in with an invalid password
     * Then the user should see an error message
     */
    @Test
    public void test_loginWrongPass() {
        loginPage.login("admin@dotcms.com", "winteriscoming");
        assertThat(page.getByTestId("message")).isVisible(PlaywrightSupport.get().assertVisibleTimeout());
    }

    /**
     * Scenario: Successful login with Spanish language
     * Given the user is on the login page
     * When the user switches the language to Spanish
     * Then the user should see the login page in Spanish
     */
    @Test
    public void test_loginSpanishLanguage() {
        loginPage.switchLanguage("español (España)");
        assertThat(page.getByText("¡Bienvenido!")).isVisible();
    }

    /**
     * Scenario: Successful login with Italian language
     * Given the user is on the login page
     * When the user switches the language to Italian
     * Then the user should see the login page in Italian
     */
    @Test
    public void test_loginItalianLanguage() {
        loginPage.switchLanguage("italiano (Italia)");
        assertThat(page.getByText("Benvenuto! ")).isVisible();
    }

    /**
     * Scenario: Successful login with French language
     * Given the user is on the login page
     * When the user switches the language to French
     * Then the user should see the login page in French
     */
    @Test
    public void test_loginFrenchLanguage() {
        loginPage.switchLanguage("français (France)");
        assertThat(page.getByText("Bienvenue !")).isVisible();
    }

    /**
     * Scenario: Successful login with Dutch language
     * Given the user is on the login page
     * When the user switches the language to Dutch
     * Then the user should see the login page in Dutch
     */
    @Test
    public void test_loginDutchLanguage() {
        loginPage.switchLanguage("Deutsch (Deutschland)");
        assertThat(page.getByText("Willkommen! ")).isVisible();
    }

}
