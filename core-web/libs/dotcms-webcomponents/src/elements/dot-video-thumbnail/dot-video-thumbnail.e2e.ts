import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';
import { contentletMock } from '../../test/mocks';

describe('dot-video-thumbnail', () => {
    let page: E2EPage;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-video-thumbnail></dot-video-thumbnail>`
        });
    });

    it('renders', async () => {
        const element = await page.find('dot-video-thumbnail');
        expect(element).toHaveClass('hydrated');
    });

    describe('@Elements', () => {
        let element: E2EElement;

        beforeEach(async () => {
            element = await page.find('dot-video-thumbnail');
            element.setProperty('contentlet', contentletMock);
            element.setProperty('src', '/dA/image');
            await page.waitForChanges();
        });
    });
});
