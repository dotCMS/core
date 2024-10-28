import { test, expect } from '@playwright/test';
import * as dotenv from 'dotenv';


dotenv.config({ path: 'properties/config.properties' });  


const serverURL = process.env.BASE_URL; 
test('Verify dotCMS UI is running and ready', async ({ page }) => {
    test.setTimeout(60000);
    await page.goto(`${serverURL}/c`);

    let title = await page.title();
    console.log('Page title before login:', title);
    expect(title).toBe('dotCMS Content Management Platform');
    await page.getByTestId('header').isVisible();

    await page.fill('input[id="inputtext"]', 'admin@dotcms.com');
    await page.fill('input[id="password"]', 'admin');
    await page.getByTestId('submitButton').click();
    await page.waitForURL("http://localhost:8080/dotAdmin/#/starter");

    title = await page.title();
    console.log('Page title after login:', title);
    expect(title).toBe('Welcome - dotCMS Content Management Platform');
});



