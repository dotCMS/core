import { newE2EPage } from '@stencil/core/testing';

describe('dot-contentlet-lock-icon', () => {
    it('renders', async () => {
        const page = await newE2EPage();

        await page.setContent('<dot-contentlet-lock-icon></dot-contentlet-lock-icon>');
        const element = await page.find('dot-contentlet-lock-icon');
        expect(element).toHaveClass('hydrated');
    });
});
