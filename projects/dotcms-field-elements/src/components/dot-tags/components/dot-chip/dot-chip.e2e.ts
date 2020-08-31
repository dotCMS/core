import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';

describe('dot-chip', () => {
    let page: E2EPage;
    let element: E2EElement;

    const getLabel = () => page.find('span');
    const getButton = () => page.find('button');

    describe('@Props', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: '<dot-chip></dot-chip>'
            });

            element = await page.find('dot-chip');
        });

        describe('label', () => {
            it('should render', async () => {
                element.setProperty('label', 'hello chip');
                await page.waitForChanges();

                const label = await getLabel();
                const button = await getButton();
                expect(label.innerText).toBe('hello chip');
                expect(await button.getAttribute('aria-label')).toBe('Delete hello chip');
            });

            it('should render default', async () => {
                await page.waitForChanges();

                const label = await getLabel();
                const button = await getButton();
                expect(label.innerText).toBe('');
                expect(await button.getAttribute('aria-label')).toBeNull();
            });
        });

        describe('deleteLabel', () => {
            it('should render', async () => {
                element.setProperty('deleteLabel', 'Remove');
                await page.waitForChanges();

                const button = await getButton();
                expect(button.innerText).toBe('Remove');
            });

            it('should render default', async () => {
                await page.waitForChanges();

                const button = await getButton();
                expect(button.innerText).toBe('Delete');
            });
        });

        describe('disabled', () => {
            it('should render', async () => {
                element.setProperty('disabled', true);
                await page.waitForChanges();

                const button = await getButton();
                expect(button.getAttribute('disabled')).toBeDefined();
            });

            it('should render default', async () => {
                await page.waitForChanges();

                const button = await getButton();
                expect(button.getAttribute('disabled')).toBeNull();
            });
        });
    });

    describe('@Events', () => {
        let spyRemoveEvent;

        beforeEach(async () => {
            page = await newE2EPage({
                html: '<dot-chip label="test-tag"></dot-chip>'
            });

            element = await page.find('dot-chip');
            spyRemoveEvent = await element.spyOnEvent('remove');
            await page.waitForChanges();
        });

        describe('remove', () => {
            it('should trigger', async () => {
                const button = await getButton();
                await button.click();
                await page.waitForChanges();

                expect(spyRemoveEvent).toHaveReceivedEventDetail('test-tag');
            });

            it('should not trigger', async () => {
                element.setProperty('disabled', true);
                await page.waitForChanges();

                const button = await getButton();
                await button.click();

                expect(spyRemoveEvent).not.toHaveReceivedEvent();
            });
        });
    });
});
