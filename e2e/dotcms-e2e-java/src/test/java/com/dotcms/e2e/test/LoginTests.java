package com.dotcms.e2e.test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.dotcms.e2e.page.LoginPage;
import com.dotcms.e2e.playwright.PlaywrightSupport;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Login portlet tests")
public class LoginTests extends BaseE2eTest {

    private LoginPage loginPage;

    @BeforeEach
    public void setup() {
        loginPage = new LoginPage(page);
        page.navigate(getBaseUrl());
    }

    /**
     * Test the login functionality with the default user
     */
    @Test
    public void loginSuccess() {
        loginPage.successfulLogin();
        assertThat(page.getByRole(AriaRole.IMG)).isVisible();
    }

    /**
     * Test the login functionality with a wrong user
     */
    @Test
    public void loginWrongUser() {
        loginPage.login("jon.snow@got.com", "admin");
        assertThat(page.getByTestId("message")).isVisible();
    }

    /**
     * Test the login functionality with a wrong password
     */
    @Test
    public void loginWrongPass() {
        loginPage.login("admin@dotcms.com", "winteriscoming");
        assertThat(page.getByTestId("message")).isVisible(PlaywrightSupport.get().visibleTimeout(10000));
    }

    /**
     * Test the login functionality with Spanish language
     */
    @Test
    public void loginSpanishLanguage() {
        loginPage.switchLanguage("español (España)");
        assertThat(page.getByText("¡Bienvenido!")).isVisible();
    }

    /**
     * Test the login functionality with Italian language
     */
    @Test
    public void loginItalianLanguage() {
        loginPage.switchLanguage("italiano (Italia)");
        assertThat(page.getByText("Benvenuto! ")).isVisible();
    }

    /**
     * Test the login functionality with French language
     */
    @Test
    public void loginFrenchLanguage() {
        loginPage.switchLanguage("français (France)");
        assertThat(page.getByText("Bienvenue !")).isVisible();
    }

    /**
     * Test the login functionality with Dutch language
     */
    @Test
    public void loginDutchLanguage() {
        loginPage.switchLanguage("Deutsch (Deutschland)");
        assertThat(page.getByText("Willkommen! ")).isVisible();
    }

}
