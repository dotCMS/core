import { expect, test } from '@playwright/test';

test('has title', async ({ page }) => {
    await page.goto('/dotAdmin/#/public/login');

    // Test of the E2E login page
    expect(await page.locator('.login__title').innerText()).toContain('Welcome');
});
