import { test, expect } from '@playwright/test';
import * as dotenv from 'dotenv';


dotenv.config({ path: 'properties/config.properties' });  

const serverURL = process.env.BASE_URL; 
async function login(page) {
    test.setTimeout(40000);

    await page.goto(`${serverURL}/c`);
    
    let title = await page.title();
    expect(title).toBe('dotCMS Content Management Platform');
    await page.getByTestId('header').isVisible();
    await page.fill('input[id="inputtext"]', 'admin@dotcms.com');
    await page.fill('input[id="password"]', 'admin');
    await page.getByTestId('submitButton').click();
    await page.waitForURL(`${serverURL}/dotAdmin/#/starter`);

    title = await page.title();
    console.log('Page title after login:', title);
    expect(title).toBe('Welcome - dotCMS Content Management Platform');
}

test('Verify dotCMS UI is running and ready', async ({ page }) => {
    await login(page);  
});

// Second test that makes a reindex after logging in
test('Make a reindex', async ({ page }) => {
    test.setTimeout(60000);
    await login(page);  

    // Open the settings menu
    await page.getByText('settingsSettingsarrow_drop_up').click(); 
    await page.getByRole('link', { name: 'Maintenance' }).click();
    
    // Switch to the relevant frame and click the 'Index' tab
    await page.frameLocator('iframe[name="detailFrame"]').getByRole('tab', { name: 'Index' }).click();
    await page.locator('iframe[name="detailFrame"]').contentFrame().locator('#live_20241017194349Row').getByRole('cell', { name: 'Active' }).isVisible();
    
    // Wait for the dialog to appear and accept it
    page.once('dialog', async dialog => {
        //console.log(`Dialog message: ${dialog.message()}`);
        await dialog.accept('2');  // Accept the dialog
    });

    // Click the Reindex button in the iframe
    await page.frameLocator('iframe[name="detailFrame"]').getByLabel('Reindex', { exact: true }).click();
    
    await page.waitForTimeout(3000); // Wait for 3 seconds

    // Verify that the reindexing message is visible
    await expect(page.locator('iframe[name="detailFrame"]').contentFrame().getByText('A full reindex is in progress.')).toBeVisible();
    await expect(page.locator('iframe[name="detailFrame"]').contentFrame().getByText('A full reindex is in progress.')).toBeHidden({ timeout: 60000 }); 
});








