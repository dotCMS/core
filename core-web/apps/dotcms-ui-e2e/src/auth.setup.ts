import { expect, test as setup } from '@playwright/test';
import { admin1 } from '@utils/credentials';

import path from 'path';

const AUTH_STATE_PATH = path.join(__dirname, '..', '.auth', 'admin.json');

setup('authenticate as admin', async ({ page }) => {
    await page.goto('/dotAdmin');
    await page.waitForLoadState();

    const emailInput = page.getByTestId('userNameInput');
    await emailInput.waitFor({ state: 'visible', timeout: 10000 });
    await emailInput.click();
    await emailInput.fill(admin1.username);
    await emailInput.press('Tab');

    const passwordInput = page.getByTestId('password');
    await passwordInput.waitFor({ state: 'visible', timeout: 10000 });
    await passwordInput.fill(admin1.password);

    const responsePromise = page.waitForResponse((r) => r.url().includes('/api/v1/authentication'));
    await page.getByTestId('submitButton').click();
    const response = await responsePromise;
    expect(response.status()).toBe(200);
    // Wait until the shell has navigated so cookies / storage match a logged-in session
    await page.waitForURL(/dotAdmin/i, { timeout: 30000 });
    await page.context().storageState({ path: AUTH_STATE_PATH });
});
