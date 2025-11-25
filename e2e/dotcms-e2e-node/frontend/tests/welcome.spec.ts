import { test, expect } from '@playwright/test';

test('has title', async ({ page }) => {
    await page.goto('/dotAdmin');

    // Expect h3 to contain a substring.
    expect(await page.locator('h3').textContent()).toContain('Welcome!');
});
