import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-binary-text-field', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-binary-text-field></dot-binary-text-field>`
        });

        element = await page.find('dot-binary-text-field');
        input = await page.find('input');
    });

    describe('@Props', () => {
        describe('value', () => {
            it('should set value correctly', async () => {
                element.setProperty('value', 'hi');
                await page.waitForChanges();
                expect(await input.getProperty('value')).toBe('hi');
            });
            it('should render and not break when is a unexpected value', async () => {
                element.setProperty('value', { test: true });
                await page.waitForChanges();
                expect(await input.getProperty('value')).toBe('[object Object]');
            });
        });

        describe('hint', () => {
            it('should set aria attribute correctly', async () => {
                element.setProperty('hint', 'Test');
                await page.waitForChanges();
                expect(input.getAttribute('aria-describedby')).toBe('hint-test');
            });

            it('should not set aria attribute', () => {
                expect(input.getAttribute('aria-describedby')).toBeNull();
            });
        });

        describe('placeholder', () => {
            it('should set placeholder correctly', async () => {
                element.setProperty('placeholder', 'Test');
                await page.waitForChanges();
                expect(input.getAttribute('placeholder')).toBe('Test');
            });
            it('should render and not break when is a unexpected value', async () => {
                element.setProperty('placeholder', { test: true });
                await page.waitForChanges();
                expect(await input.getProperty('placeholder')).toBe('[object Object]');
            });
        });

        describe('required', () => {
            it('should not render required attribute by default', () => {
                expect(input.getAttribute('required')).toBeNull();
            });

            it('should render required attribute with invalid value', async () => {
                element.setProperty('required', { test: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('required')).toBeDefined();
            });

            it('should not render required attribute', async () => {
                element.setProperty('required', 'false');
                await page.waitForChanges();
                expect(input.getAttribute('required')).toBeNull();
            });
        });

        describe('disabled', () => {
            it('should not render disabled attribute by default', () => {
                expect(input.getAttribute('disabled')).toBeNull();
            });

            it('should render disabled attribute', async () => {
                element.setProperty('disabled', 'true');
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeDefined();
            });

            it('should not render disabled attribute', async () => {
                element.setProperty('disabled', 'false');
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeNull();
            });

            it('should render disabled attribute with invalid value', async () => {
                element.setProperty('disabled', { test: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeDefined();
            });
        });
    });

    describe('@Events', () => {
        let spyFileChangeEvent: EventSpy;
        let spyLostFocusEvent: EventSpy;

        beforeEach(async () => {
            spyFileChangeEvent = await page.spyOnEvent('fileChange');
            spyLostFocusEvent = await page.spyOnEvent('lostFocus');
        });

        describe('blur', () => {
            it('should emit blur event', async () => {
                input.triggerEvent('blur');
                await page.waitForChanges();

                expect(spyLostFocusEvent).toHaveReceivedEvent();
            });
        });

        describe('KeyDown', () => {
            beforeEach(() => {
                element.setAttribute('value', 'name.pdf');
            });

            it('should ignore keypress event of any key', async () => {
                await input.press('a');
                await page.waitForChanges();

                expect(await input.getProperty('value')).toBe('name.pdf');
                expect(spyFileChangeEvent.events.length).toEqual(0);
            });

            it('should clear value and emit fileChange event with null on backSpace key', async () => {
                await page.waitForChanges();
                await input.press('Backspace');
                await page.waitForChanges();

                expect(spyFileChangeEvent).toHaveReceivedEventDetail({
                    file: null,
                    errorType: null
                });

                expect(await input.getProperty('value')).toBe('');
            });
        });

        //TODO: can't mock a ClipboardEvent.
        xdescribe('paste', () => {
            beforeEach(async () => {
                // input.triggerEvent('paste', {  detail: { test: 'TEST' } });
            });

            it('should emit pasted file', async () => {});

            it('should emit pasted URL', async () => {});

            it('should not emit event since file is not supported', async () => {});
        });
    });
});
