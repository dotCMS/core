import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';

describe('dot-chip', () => {
    let page: E2EPage;
    let element: E2EElement;

    describe('render all attributes', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-chip
                            label='test-tag'
                            disabled>
                        </dot-chip>`
            });

            element = await page.find('dot-chip');
        });

        it('should render a span', async () => {
            const span = await element.find('span');
            expect(span.innerHTML).toBe('test-tag');
        });

        it('should render a delete button', async () => {
            const button = await element.find('button');
            expect(button.innerHTML).toBe('delete');
            expect(button.getAttribute('type')).toBe('button');
        });
    });

    describe('render each attributes', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-chip></dot-chip>`
            });

            element = await page.find('dot-chip');
        });

        it('should render a span', async () => {
            const span = await element.find('span');
            expect(span.innerHTML).toBe('');
        });

        it('should render a delete button', async () => {
            const button = await element.find('button');
            expect(button.innerHTML).toBe('delete');
            expect(button.getAttribute('type')).toBe('button');
        });

        it('should render with deleteLabel', async () => {
            element.setAttribute('delete-label', 'x');
            await page.waitForChanges();

            const button = await element.find('button');
            expect(button.innerHTML).toBe('x');
        });

        it('should be disabled', async () => {
            element.setAttribute('disabled', true);
            await page.waitForChanges();

            const button = await element.find('button');
            expect(button.getAttribute('disabled')).not.toBeNull();
        });

        describe('render with invalid attributes', () => {
            it('should render with no string in deleteLabel', async () => {
                element.setAttribute('delete-label', {});
                await page.waitForChanges();

                const button = await element.find('button');
                expect(button.innerHTML).toBe('[object Object]');
            });

            it('should be disabled', async () => {
                element.setAttribute('disabled', {});
                await page.waitForChanges();

                const button = await element.find('button');
                expect(button.getAttribute('disabled')).not.toBeNull();
            });
        });
    });

    describe('events', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-chip
                            label='test-tag'
                            disabled>
                        </dot-chip>`
            });

            element = await page.find('dot-chip');
        });

        it('should trigger remove event', async () => {
            const spyRemoveEvent = await element.spyOnEvent('remove');

            const removeButton = await page.find('button');
            await removeButton.triggerEvent('click');
            await page.waitForChanges();

            expect(spyRemoveEvent).toHaveReceivedEventDetail('test-tag');
        });
    });
});
