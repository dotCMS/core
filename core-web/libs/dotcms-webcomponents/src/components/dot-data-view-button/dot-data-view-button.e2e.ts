import { newE2EPage } from '@stencil/core/testing';

describe('dot-data-view-button', () => {
    it('renders', async () => {
        const page = await newE2EPage();

        await page.setContent('<dot-data-view-button></dot-data-view-button>');
        const element = await page.find('dot-data-view-button');
        expect(element).toHaveClass('hydrated');
    });
});
