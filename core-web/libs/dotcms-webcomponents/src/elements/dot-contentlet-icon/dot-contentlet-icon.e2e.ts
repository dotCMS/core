import { E2EPage, newE2EPage } from '@stencil/core/testing';

describe('dot-contentlet-icon', () => {
    let page: E2EPage;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-contentlet-icon></dot-contentlet-icon>`
        });
    });

    describe('@Elements', () => {
        describe('empty', () => {
            it('should have material design web component', async () => {
                const materialIcon = await page.find('dot-contentlet-icon >>> mwc-icon');
                expect(materialIcon.innerText).toBe('insert_drive_file');
            });
        });

        describe('filled', () => {
            beforeEach(async () => {
                const element = await page.find('dot-contentlet-icon');
                element.setProperty('icon', 'asfIcon');
                element.setProperty('size', '33px');
                await page.waitForChanges();
            });

            it('should set icon type and size', async () => {
                const materialIcon = await page.find('dot-contentlet-icon >>> mwc-icon');
                expect((await materialIcon.getComputedStyle())['fontSize']).toBe('33px');
                expect(materialIcon.innerText).toBe('videocam');
            });
        });
    });
});
