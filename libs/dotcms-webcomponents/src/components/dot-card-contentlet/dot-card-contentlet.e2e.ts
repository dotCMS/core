import { newE2EPage, E2EPage } from '@stencil/core/testing';

describe('dot-card-contentlet', () => {
    let page: E2EPage;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-card-contentlet></dot-card-contentlet>`
        });
    });

    it('empty place holder', async () => {
        const dotCard = await page.find('dot-card-contentlet');
        expect(dotCard).not.toBeNull();
    });
});

/*
import { newE2EPage, E2EPage } from '@stencil/core/testing';

const mock = {
    data: {
        title: 'Hola Mundo'
    },
    actions: [
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
    ]
};

describe('dot-card-contentlet', () => {
    let page: E2EPage;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-card-contentlet></dot-card-contentlet>`
        });
    });

    describe('@Elements', () => {
        describe('empty', () => {
            it('should have thumb', async () => {
                const thumbnail = await page.find(
                    'dot-card-contentlet >>> dot-contentlet-thumbnail'
                );
                expect(thumbnail).not.toBeNull();
                expect(thumbnail.getAttribute('width')).toBe('220px');
                expect(thumbnail.getAttribute('height')).toBe('150px');
            });

            it('should hide context menu', async () => {
                const menu = await page.find('dot-card-contentlet >>> dot-context-menu');
                expect(menu).toBeNull();
            });

            it('should have checkbox', async () => {
                const checkbox = await page.find('dot-card-contentlet >>> mwc-checkbox');
                expect(checkbox).not.toBeNull();
            });

            it('should have label', async () => {
                const checkbox = await page.find('dot-card-contentlet >>> label');
                expect(checkbox).not.toBeNull();
                expect(checkbox.innerText).toBe('');
            });
        });

        describe('filled', () => {
            beforeEach(async () => {
                const element = await page.find('dot-card-contentlet');
                element.setProperty('item', mock);
                await page.waitForChanges();
            });

            it('should have thumb', async () => {
                const thumbnail = await page.find(
                    'dot-card-contentlet >>> dot-contentlet-thumbnail'
                );
                expect(thumbnail.getAttribute('alt')).toBe('Hola Mundo');
            });

            it('should hide context menu', async () => {
                const menu = await page.find('dot-card-contentlet >>> dot-context-menu');
                expect(menu).not.toBeNull();
            });

            it('should have label', async () => {
                const checkbox = await page.find('dot-card-contentlet >>> label');
                expect(checkbox).not.toBeNull();
                expect(checkbox.innerText).toBe('Hola Mundo');
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            const element = await page.find('dot-card-contentlet');
            element.setProperty('item', mock);
            await page.waitForChanges();
        });
        it('should select checkbox', async () => {
            const check = await page.spyOnEvent('selected');
            const card = await page.find('dot-card-contentlet');
            await card.click();
            await page.waitForChanges();
            expect(check).toHaveReceivedEventDetail(mock.data);
        });
        it('renders', async () => {
            const page = await newE2EPage();

            await page.setContent('<dot-card-contentlet></dot-card-contentlet>');
            const element = await page.find('dot-card-contentlet');
            expect(element).toHaveClass('hydrated');
        });
    });
});
*/
