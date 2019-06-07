import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-input-calendar', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-input-calendar></dot-input-calendar>`
        });
        element = await page.find('dot-input-calendar');
        input = await page.find('input');
    });

    describe('@Props', () => {
        describe('value', () => {
            it('should set value correctly', async () => {
                element.setProperty('value', 'text');
                await page.waitForChanges();
                expect(await input.getProperty('value')).toBe('text');
            });
        });

        describe('name', () => {
            it('should render with valid id name', async () => {
                element.setProperty('name', 'text01');
                await page.waitForChanges();
                expect(input.getAttribute('id')).toBe('dot-text01');
            });

            it('should render when is a unexpected value', async () => {
                element.setProperty('name', { input: 'text01' });
                await page.waitForChanges();
                expect(input.getAttribute('id')).toBe('dot-object-object');
            });
        });

        describe('required', () => {
            it('should not render required attribute by default', () => {
                expect(input.getAttribute('required')).toBeNull();
            });

            it('should render required attribute with  value', async () => {
                element.setProperty('required', { test: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('required')).toBeDefined();
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
        });

        describe('min', () => {
            it('should not render  attribute by default', () => {
                expect(input.getAttribute('min')).toBe('');
            });

            it('should set value correctly', async () => {
                element.setProperty('min', '111');
                await page.waitForChanges();
                expect(input.getAttribute('min')).toBe('111');
            });
        });

        describe('max', () => {
            it('should not render  attribute by default', () => {
                expect(input.getAttribute('max')).toBe('');
            });

            it('should set value correctly', async () => {
                element.setProperty('max', '9');
                await page.waitForChanges();
                expect(input.getAttribute('max')).toBe('9');
            });
        });

        describe('step', () => {
            it('should set value default value correctly', () => {
                expect(input.getAttribute('step')).toBe('1');
            });
            it('should set value correctly', async () => {
                element.setProperty('step', '2');
                await page.waitForChanges();
                expect(input.getAttribute('step')).toBe('2');
            });
        });

        describe('type', () => {
            it('should not render empty by default', () => {
                expect(input.getAttribute('type')).toBe('');
            });

            it('should set value correctly', async () => {
                element.setProperty('type', 'time');
                await page.waitForChanges();
                expect(input.getAttribute('type')).toBe('time');
            });
        });
    });

    describe('@Events', () => {
        let spyStatusChangeEvent: EventSpy;
        let spyValueChange: EventSpy;

        beforeEach(async () => {
            spyValueChange = await page.spyOnEvent('_valueChange');
            spyStatusChangeEvent = await page.spyOnEvent('_statusChange');
        });

        describe('status and value change', () => {
            it('should emit value correctly ', async () => {
                await input.press('4');
                await page.waitForChanges();

                expect(spyValueChange).toHaveReceivedEventDetail({
                    name: '',
                    value: '4'
                });
            });

            it('should emit status and value events on Reset', async () => {
                await element.callMethod('reset');
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: true,
                        dotTouched: false,
                        dotValid: true
                    },
                    isValidRange: true
                });
                expect(spyValueChange).toHaveReceivedEventDetail({ name: '', value: '' });
            });
        });

        describe('status change', () => {
            it('should mark as touched when onblur', async () => {
                await input.triggerEvent('blur');
                await page.waitForChanges();

                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: true,
                        dotTouched: true,
                        dotValid: true
                    },
                    isValidRange: true
                });
            });

            it('should send valid and isValidRange when value is empty', async () => {
                await input.press('1');
                await input.press('Backspace');
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: true
                    },
                    isValidRange: true
                });
            });

            it('should send invalid when value is empty but required', async () => {
                element.setProperty('required', 'true');
                await input.press('1');
                await input.press('Backspace');
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: false
                    },
                    isValidRange: true
                });
            });

            it('should send isValidRange and valid false when value is out of range', async () => {
                element.setProperty('min', '06:00:00');
                element.setProperty('max', '22:00:00');
                await input.press('2');
                await input.press('Backspace');
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: false
                    },
                    isValidRange: false
                });
            });
        });
    });
});
