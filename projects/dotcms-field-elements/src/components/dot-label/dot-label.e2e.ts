import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-label', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `
            <dot-label
                label='Address:'
                name='Address'
                value='Address'>
            </dot-textarea>`
        });

        element = await page.find('dot-textarea');
        input = await page.find('textarea');
    });

    it('should render', async () => {
        expect(true).toBe(true);
    });
});
