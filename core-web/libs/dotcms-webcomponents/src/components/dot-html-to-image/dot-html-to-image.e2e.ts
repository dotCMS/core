import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';

describe('dot-html-to-image', () => {
    let page: E2EPage;
    let dotHtmlToImage: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-html-to-image></dot-html-to-image>`
        });
        dotHtmlToImage = await page.find('dot-html-to-image');
        dotHtmlToImage.setProperty('value', '<p>test</p>');
        dotHtmlToImage.setProperty('height', '768');
        dotHtmlToImage.setProperty('width', '1024');
        await page.waitForChanges();
    });

    describe('@Elements', () => {
        let iframe: E2EElement;
        let progressIcon: E2EElement;

        it('should have iframe with id', async () => {
            iframe = await page.find('iframe');
            expect(iframe.getAttribute('id')).toBeDefined();
        });

        it('should have progress icon on init', async () => {
            progressIcon = await page.find('mwc-circular-progress');
            expect(progressIcon).toBeDefined();
        });
    });

    describe('@Events', () => {
        let progressIcon: E2EElement;

        xit('should hide progress icon on preview', async () => {
            // TODO: find a way how to emit custom event
            await page.waitForChanges();
            progressIcon = await page.find('mwc-circular-progress');
            expect(progressIcon).toBeDefined();
        });
    });
});
