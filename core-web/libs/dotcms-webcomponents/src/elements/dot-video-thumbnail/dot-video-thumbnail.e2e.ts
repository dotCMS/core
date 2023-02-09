import { newE2EPage } from '@stencil/core/testing';

describe('dot-video-thumbnail', () => {
    it('renders', async () => {
        const page = await newE2EPage();

        await page.setContent('<dot-video-thumbnail></dot-video-thumbnail>');
        const element = await page.find('dot-video-thumbnail');
        expect(element).toHaveClass('hydrated');
    });

    it('renders changes to the name data', async () => {
        const page = await newE2EPage();

        await page.setContent('<dot-video-thumbnail></dot-video-thumbnail>');
        const component = await page.find('dot-video-thumbnail');
        const element = await page.find('dot-video-thumbnail >>> div');
        expect(element.textContent).toEqual(`Hello, World! I'm `);

        component.setProperty('first', 'James');
        await page.waitForChanges();
        expect(element.textContent).toEqual(`Hello, World! I'm James`);

        component.setProperty('last', 'Quincy');
        await page.waitForChanges();
        expect(element.textContent).toEqual(`Hello, World! I'm James Quincy`);

        component.setProperty('middle', 'Earl');
        await page.waitForChanges();
        expect(element.textContent).toEqual(`Hello, World! I'm James Earl Quincy`);
    });
});
