import { newE2EPage } from '@stencil/core/testing';

describe('dot-badge', () => {
    it('renders', async () => {
        const page = await newE2EPage();

        await page.setContent('<dot-badge></dot-badge>');
        const element = await page.find('dot-badge');
        expect(element).toHaveClass('hydrated');
    });
});
