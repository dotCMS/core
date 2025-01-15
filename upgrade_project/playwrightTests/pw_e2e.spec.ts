import { test, expect } from '@playwright/test';
import * as dotenv from 'dotenv';
import {
    f
} from "../venv/lib/python3.12/site-packages/playwright/driver/package/lib/vite/traceViewer/assets/workbench-D5oSwIMK.js";

dotenv.config({ path: 'properties/config.properties' });

const serverURL = process.env.BASE_URL;
async function login(page, username, password) {
    test.setTimeout(40000);

    await page.goto(`${serverURL}/c`);

    let title = await page.title();
    expect(title).toBe('dotCMS Content Management Platform');
    await page.getByTestId('header').isVisible();
    await page.fill('input[id="inputtext"]', username);
    await page.getByTestId('password').fill('admin');
    await page.getByTestId('submitButton').click();
    await page.waitForURL(`${serverURL}/dotAdmin/#/starter`);

    title = await page.title();
    console.log('Page title after login:', title);
    expect(title).toBe('Welcome - dotCMS Content Management Platform');
}

test('Login: Verify dotCMS UI is running and ready', {tag: '@afterUpgrade @beforeUpgrade'}, async ({ page }) => {
    test.setTimeout(60000);
    const username = process.env.username;
    const password = process.env.password; 
    await login(page, username, password );  
});


// Second test that makes a reindex after logging in
test('Make a reindex', { tag: '@afterUpgrade' }, async ({ page }) => {
    test.setTimeout(60000);
    const username = process.env.username;
    const password = process.env.password;

    // Login
    await login(page,username, password);
    await page.waitForLoadState();

    // Open the settings menu
    await page.getByText('settingsSettingsarrow_drop_up').hover();
    await page.getByRole('link', { name: 'Maintenance' }).click();
    
    // Switch to the relevant frame and click the 'Index' tab
    const detailFrame = page.frameLocator('iframe[name="detailFrame"]');
    await detailFrame.getByRole('tab', { name: 'Index' }).click();
    await detailFrame.locator('#live_20241017194349Row').getByRole('cell', { name: 'Active' }).isVisible();
    
    // Wait for the dialog to appear and accept it
    page.once('dialog', async dialog => {
        await dialog.accept('2');  // Accept the dialog
    });

    // Click the Reindex button in the iframe
    await detailFrame.getByLabel('Reindex', { exact: true }).click();
    await page.waitForTimeout(3000); // Wait for 3 seconds

    // Verify that the reindexing message is visible
    const reindexMessage = detailFrame.getByText('A full reindex is in progress.');
    await expect(reindexMessage).toBeVisible();
    await expect(reindexMessage).toBeHidden({ timeout: 60000 });
});








