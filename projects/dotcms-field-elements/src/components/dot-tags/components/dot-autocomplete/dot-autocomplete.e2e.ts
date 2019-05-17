import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';

describe('dot-autocomplete', () => {
    let page: E2EPage;
    let element: E2EElement;

    describe('render all attributes', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-autocomplete
                            placeholder='placeholder'
                            threshold='3'
                            maxResults='5'
                            debounce='100'
                            disabled
                        >
                        </dot-autocomplete>`
            });

            element = await page.find('dot-autocomplete');
        });

        it('should render', async () => {
            const input = await element.find('input');

            expect(input.getAttribute('id').startsWith('autoComplete')).toBe(true);
            expect(input.getAttribute('disabled')).not.toBeNull();
            expect(input.getAttribute('placeholder')).toBe('placeholder');
        });
    });

    describe('render each attributes', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-autocomplete></dot-autocomplete>`
            });

            element = await page.find('dot-autocomplete');
        });

        it('should render', async () => {
            const input = await element.find('input');
            expect(input.getAttribute('id').startsWith('autoComplete')).toBe(true);
        });

        it('should disabled', async () => {
            element.setAttribute('disabled', true);
            await page.waitForChanges();

            const input = await element.find('input');
            expect(input.getAttribute('disabled')).not.toBeNull();
        });

        it('should put a placeholder', async () => {
            element.setAttribute('placeholder', 'placeholder');
            await page.waitForChanges();

            const input = await element.find('input');
            expect(input.getAttribute('placeholder')).toBe('placeholder');
        });

        describe('invalid inputs', () => {
            it('should not broke when disabled is not a boolean', async () => {
                element.setAttribute('disabled', {});
                await page.waitForChanges();

                const input = await element.find('input');
                expect(input.getAttribute('disabled')).toBeTruthy();
            });

            it('should not broke when placeholder is not a string', async () => {
                element.setAttribute('placeholder', {});
                await page.waitForChanges();

                const input = await element.find('input');
                expect(input.getAttribute('placeholder')).toBe('[object Object]');
            });

            it('should not broke when threshold is not a number', async () => {
                element.setAttribute('threshold', {});
                await page.waitForChanges();

                expect(await element.find('input')).toBeDefined();
            });

            it('should not broke when debounce is not a number', async () => {
                element.setAttribute('debounce', {});
                await page.waitForChanges();

                expect(await element.find('input')).toBeDefined();
            });

            it('should not broke when maxResults is not a number', async () => {
                element.setAttribute('maxResults', {});
                await page.waitForChanges();

                expect(await element.find('input')).toBeDefined();
            });

            it('should not broke when data does is not a function', async () => {
                element.setAttribute('data', {});
                await page.waitForChanges();

                expect(await element.find('input')).toBeDefined();
            });
        });
    });

    describe('show options', () => {
        let input;

        beforeEach(async () => {
            await page.$eval('dot-autocomplete', (elm: any) => {
                elm.data = () => ['tag-1', 'label'];
            });

            await page.waitForChanges();

            input = await page.find('input');
            await input.press('t');
            await page.waitForChanges();
        });

        it('should put get data', async () => {
            const ul = await element.find('ul');
            expect(ul.innerHTML).toEqualHtml(`
                <li data-result="tag-1" class="autoComplete_result" tabindex="1">
                    <span class="autoComplete_highlighted">t</span>ag-1
                </li>
            `);
        });

        it('should clean value and hide options when press esc', async () => {
            await input.press('Escape');
            await page.waitForChanges();

            const ul = await element.find('ul');
            expect(ul.innerHTML).toEqualHtml('');
        });

        it('should clean options when lost focus', async (done) => {
            element.spyOnEvent('lostFocus').then(async () => {
                await page.waitForChanges();
                const options = await page.findAll('li');
                expect(options.length).toBe(0);
                done();
            });

            await input.triggerEvent('blur');
        });

    });

    describe('events', () => {
        let input;
        let spySelectionEvent;

        beforeEach(async () => {
            await page.$eval('dot-autocomplete', (elm: any) => {
                elm.data = () => ['tag-1', 'label'];
            });

            await page.waitForChanges();
            input = await page.find('input');

            spySelectionEvent = await element.spyOnEvent('selection');
        });

        it('should trigger selection event when press enter', async () => {
            await input.press('t');
            await input.press('e');
            await input.press('s');
            await input.press('t');
            await input.press('Enter');
            await page.waitForChanges();

            expect(spySelectionEvent).toHaveReceivedEventDetail('test');
        });

        it('should trigger selection event when click a option', async () => {
            await input.press('t');
            await page.waitForChanges();

            const option = await page.find('li');
            await option.press('Enter');
            await page.waitForChanges();

            expect(await page.find('li')).toBeNull();
            expect(spySelectionEvent).toHaveReceivedEventDetail('tag-1');
        });

        it('should trigger lost focus event', async (done) => {
            element.spyOnEvent('lostFocus').then(() => {
                done();
            });

            await input.triggerEvent('blur');
        });
    });
});
