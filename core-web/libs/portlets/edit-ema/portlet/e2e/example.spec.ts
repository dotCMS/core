import { test, expect } from '@playwright/test';

// test('iframe in editor', async ({ page, context }) => {
//     await context.addCookies([
//         {
//             name: 'access_token',
//             value: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3MTgxNjdjOC1jN2ZmLTQyYzAtOWY1MC1mMjdlMjI4NWU2ZTMiLCJ4bW9kIjoxNzE0ODU3NTUyMTA3LCJzdWIiOiJkb3RjbXMub3JnLjEiLCJpYXQiOjE3MTQ4NTc1NTIsImlzcyI6ImI1YTNlYTI3ZGQiLCJleHAiOjE3MTQ5NDM5NTJ9.M4oym7m_29IgI5Lp606EdkrwFD5HUUxPdXMyzueBSI4',
//             domain: 'localhost',
//             path: '/'
//         },
//     ]);

//     await page.goto(
//         '/dotAdmin/#/edit-page/content?url=index&language_id=1&device_inode=&com.dotmarketing.persona.id=modes.persona.no.persona'
//     );

//     await page.waitForSelector('iframe');

//     // Check if iframe exists
//     const iframeElement = await page.$('iframe');
//     expect(iframeElement).toBeTruthy();

//     // Check if iframe has HTML content
//     const iframeContent = await iframeElement?.contentFrame();
//     const htmlContent = await iframeContent?.evaluate(() => document.body.innerHTML);
//     expect(htmlContent).toBeTruthy();
//     expect(htmlContent).toContain('<div');
// });

// test('starter page', async ({ page, context }) => {
//     await context.addCookies([
//         {
//             name: 'access_token',
//             value: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3MTgxNjdjOC1jN2ZmLTQyYzAtOWY1MC1mMjdlMjI4NWU2ZTMiLCJ4bW9kIjoxNzE0ODU3NTUyMTA3LCJzdWIiOiJkb3RjbXMub3JnLjEiLCJpYXQiOjE3MTQ4NTc1NTIsImlzcyI6ImI1YTNlYTI3ZGQiLCJleHAiOjE3MTQ5NDM5NTJ9.M4oym7m_29IgI5Lp606EdkrwFD5HUUxPdXMyzueBSI4',
//             domain: 'localhost',
//             path: '/'
//         },
//     ]);
//     await page.goto('/dotAdmin/#/starter');

//     // Check if exist h2 with "Welcome" text inside
//     const welcomeText = await page.innerText('h2');
//     expect(welcomeText).toBe('Welcome!');
// });

test('drag content from palette to iframe and dropzone must to be rendered', async ({ page, context }) => {
    await context.addCookies([
        {
            name: 'access_token',
            value: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3MTgxNjdjOC1jN2ZmLTQyYzAtOWY1MC1mMjdlMjI4NWU2ZTMiLCJ4bW9kIjoxNzE0ODU3NTUyMTA3LCJzdWIiOiJkb3RjbXMub3JnLjEiLCJpYXQiOjE3MTQ4NTc1NTIsImlzcyI6ImI1YTNlYTI3ZGQiLCJleHAiOjE3MTQ5NDM5NTJ9.M4oym7m_29IgI5Lp606EdkrwFD5HUUxPdXMyzueBSI4',
            domain: 'localhost',
            path: '/'
        },
    ]);

    await page.goto(
        '/dotAdmin/#/edit-page/content?url=index&language_id=1&device_inode=&com.dotmarketing.persona.id=modes.persona.no.persona'
    );

    const frameElement = await page.waitForSelector('iframe');
    await frameElement.contentFrame();

    await page.waitForSelector('dot-edit-ema-palette');

    // // await page.dragAndDrop('.content-type-card', 'iframe', {
    // //     timeout: 5000
    // // });


    // // const paletteItem = await page.$('.content-type-card');
    // // await paletteItem. ('iframe');
    // // const iframe = page.locator('iframe');
    // // await page.locator('.content-type-card').first().dragTo(iframe);
    // await page.locator('.content-type-card').first().hover();
    // await page.mouse.down();
    // await page.locator('iframe').hover();
    // // await page.locator('iframe').hover();
    // await page.mouse.up();

    // const dropzone = await page.waitForSelector('dot-ema-page-dropzone');
    // expect(dropzone).not.toBeNull();

    const paletteItem = await page.locator('.content-type-card').first();
    const iframe = page.locator('iframe');
  
    const [dragPromise, dropzonePromise] = await Promise.all([
      paletteItem.dragTo(iframe),
      page.waitForSelector('dot-ema-page-dropzone', { timeout: 2000 }), // Set a shorter timeout for this element
    ]);
  
    // Optional: Assert successful drag completion (if needed)
    await expect(dragPromise).not.toBeNull();
  
    // Check if dropzone element appeared during drag
    const dropzone = await dropzonePromise;
    expect(dropzone).not.toBeNull();  
})