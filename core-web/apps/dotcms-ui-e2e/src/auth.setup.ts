import { test as setup } from '@playwright/test';

const AUTH_STATE_PATH = 'test-results/.auth/admin.json';

setup('authenticate as admin', async ({ page }) => {
    await page.goto('/dotAdmin');
    await page.waitForLoadState();

    const emailInput = page.getByTestId('userNameInput');
    await emailInput.waitFor({ state: 'visible', timeout: 10000 });
    await emailInput.click();
    await emailInput.fill('admin@dotcms.com');
    await emailInput.press('Tab');

    const passwordInput = page.getByTestId('password');
    await passwordInput.waitFor({ state: 'visible', timeout: 10000 });
    await passwordInput.fill('admin');

    const responsePromise = page.waitForResponse((r) =>
        r.url().includes('/api/v1/authentication')
    );
    await page.getByTestId('submitButton').click();
    await responsePromise;

    // Wait for app to load after login
    await page.waitForLoadState('networkidle');

    // Save auth state (cookies + localStorage)
    await page.context().storageState({ path: AUTH_STATE_PATH });
});
