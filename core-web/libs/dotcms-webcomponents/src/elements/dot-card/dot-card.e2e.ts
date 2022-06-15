import { newE2EPage } from '@stencil/core/testing';

describe('dot-card', () => {
    it('renders slot', async () => {
        const page = await newE2EPage();

        await page.setContent('<dot-card><h3>Hello World</h3></dot-card>');
        const element = await page.find('dot-card');
        const slot = await element.find('h3');
        expect(slot.textContent).toBe('Hello World');
    });
});
