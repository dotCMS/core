import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';

const mock = [
    {
        label: 'Publish',
        action: jest.fn((e) => {
            console.log(e);
        })
    },
    {
        label: 'Archived',
        action: jest.fn((e) => {
            console.log(e);
        })
    }
];

describe('dot-context-menu', () => {
    let page: E2EPage;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-context-menu></dot-context-menu>`
        });
    });

    describe('@Elements', () => {
        it('should have icon button', async () => {
            const button = await page.find('dot-context-menu >>> button');
            expect(button.innerHTML).toBe('<mwc-icon>more_vert</mwc-icon>');
        });

        it('should have mwc-menu', async () => {
            const menu = await page.find('dot-context-menu >>> mwc-menu');
            expect(menu).not.toBeNull();
        });

        it('should have mwc-list-item', async () => {
            const element = await page.find('dot-context-menu');
            element.setProperty('options', mock);
            await page.waitForChanges();

            const menu = await page.findAll('dot-context-menu >>> mwc-list-item');
            expect(menu.length).toBe(2);
            expect(menu.map((i: E2EElement) => i.innerHTML)).toEqual(['Publish', 'Archived']);
        });
    });

    describe('@Events', () => {
        it('should show menu', async () => {
            const button = await page.find('dot-context-menu >>> button');
            const menu = await page.find('dot-context-menu >>> mwc-menu');
            expect(menu.getAttribute('open')).toBeNull();
            await button.click();
            await page.waitForChanges();

            expect(menu.getAttribute('open')).not.toBeNull();
        });

        xit('should call passed action', async () => {
            // For some reason the `action` is not passing as a function, you can pass string tho
            const element = await page.find('dot-context-menu');
            element.setProperty('options', mock);
            await page.waitForChanges();

            const button = await page.find('dot-context-menu >>> button');
            await button.click();

            await page.waitForChanges();

            const item = await page.find('dot-context-menu >>> mwc-list-item');
            await item.click();
            await page.waitForChanges();

            expect(mock[0].action).toBeCalledTimes(2);
        });
    });
});
