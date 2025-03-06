import { test, expect } from "@playwright/test";
import { admin1, wrong1, wrong2 } from "./credentialsData";
import { LoginPage } from "@pages";

const validCredentials = [
  { username: admin1.username, password: admin1.password }, // admin user
];

/**
 * Test to validate the login functionality with valid credentials
 */
validCredentials.forEach(({ username, password }) => {
  test(`Login with Valid Credentials: ${username}`, async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.login(username, password);

    const gettingStartedLocator = page.getByRole("link", {
      name: "Getting Started",
    });
    await expect(gettingStartedLocator).toBeVisible();
  });
});

const invalidCredentials = [
  { username: wrong1.username, password: wrong1.password }, // Valid username, invalid password
  { username: wrong2.username, password: wrong2.password }, // Invalid username, valid password
];

/**
 * Test to validate the login functionality with invalid credentials
 */
invalidCredentials.forEach((credentials) => {
  test(`Login with invalid Credentials: ${credentials.username}`, async ({
    page,
  }) => {
    const { username, password } = credentials;

    const loginPage = new LoginPage(page);
    await loginPage.login(username, password);

    const errorMessageLocator = page.getByTestId("message");
    await expect(errorMessageLocator).toBeVisible({ timeout: 30000 });
  });
});
