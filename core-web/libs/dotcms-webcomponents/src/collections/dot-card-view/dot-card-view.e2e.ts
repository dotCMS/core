import { newE2EPage } from '@stencil/core/testing';

describe('dot-card-view', () => {
    it('renders', async () => {
        const page = await newE2EPage();

        await page.setContent('<dot-card-view></dot-card-view>');
        const element = await page.find('dot-card-view');
        expect(element).toHaveClass('hydrated');
    });
});
