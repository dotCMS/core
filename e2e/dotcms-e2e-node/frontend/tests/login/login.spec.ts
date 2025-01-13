import { test, expect } from "@playwright/test";
import { admin1, wrong1, wrong2 } from "./credentialsData";

const validCredentials = [
  { username: admin1.username, password: admin1.password }, // admin user
];

/**
 * Test to validate the login functionality with valid credentials
 */
validCredentials.forEach(({ username, password }) => {
  test(`Login with Valid Credentials: ${username}`, async ({ page }) => {
    await page.goto("/dotAdmin");

    await page.fill('input[id="inputtext"]', username);
    await page.fill('input[id="password"]', password);
    await page.getByTestId("submitButton").click();

    // Assertion and further test steps
    await expect(
      page.getByRole("link", { name: "Getting Started" }),
    ).toBeVisible();
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

    await page.goto("/dotAdmin");

    await page.fill('input[id="inputtext"]', username);
    await page.fill('input[id="password"]', password);
    await page.getByTestId("submitButton").click();

    // Assertion and further test steps
    await expect(page.getByTestId("message")).toBeVisible({ timeout: 30000 });
  });
});
