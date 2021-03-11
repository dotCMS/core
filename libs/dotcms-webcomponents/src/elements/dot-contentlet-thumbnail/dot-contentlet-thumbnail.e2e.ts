import { E2EPage, newE2EPage } from '@stencil/core/testing';
import { contentletMock } from '../../test';

describe('dot-contentlet-thumbnail', () => {
    let page: E2EPage;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-contentlet-thumbnail \>`
        });
    });

    describe('@Elements', () => {
        describe('empty', () => {
            it('should show dot-contentlet-icon', async () => {
                const contentletIcon = await page.find(
                    'dot-contentlet-thumbnail  dot-contentlet-icon'
                );
                expect(contentletIcon).not.toBeNull();
            });

            it('should hide image', async () => {
                const image = await page.find('dot-contentlet-thumbnail img');
                expect(image).toBeNull();
            });
        });

        describe('filled', () => {
            let element;

            beforeEach(async () => {
                element = await page.find('dot-contentlet-thumbnail');
                element.setProperty('contentlet', contentletMock);
                element.setProperty('height', '100px');
                element.setProperty('width', '100');
                element.setProperty('alt', 'Alt test');
                element.setProperty('iconSize', '30px');
                await page.waitForChanges();
            });

            // TODO: find a way to avoid the onError with an invalid image.
            xit('should show image', async () => {});

            it('should show dot-contentlet-icon', async () => {
                element.setProperty('contentlet', { ...contentletMock, hasTitleImage: 'false' });
                await page.waitForChanges();
                const icon = await page.find('dot-contentlet-icon');
                expect(await icon.getAttribute('icon')).toEqual(contentletMock.__icon__);
                expect(await icon.getAttribute('size')).toEqual('30px');
                expect(await icon.getAttribute('aria-label')).toEqual('Alt test');
            });
        });
    });
});
