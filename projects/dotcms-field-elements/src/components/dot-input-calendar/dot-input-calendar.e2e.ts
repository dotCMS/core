import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';
import { dotTestUtil } from '../../utils';

xdescribe('dot-input-calendar', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;

    describe('@Props', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-input-calendar></dot-input-calendar>`
            });
            element = await page.find('dot-input-calendar');
            input = await page.find('input');
        });

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
            it('should not render disabled attribute by default', async () => {
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
            it('should not render  attribute by default', async () => {
                expect(input.getAttribute('min')).toBe('');
            });

            it('should set value correctly', async () => {
                element.setProperty('min', '111');
                await page.waitForChanges();
                expect(await input.getAttribute('min')).toBe('111');
            });
        });

        describe('max', () => {
            it('should not render  attribute by default', async () => {
                expect(input.getAttribute('max')).toBe('');
            });

            it('should set value correctly', async () => {
                element.setProperty('max', '9');
                await page.waitForChanges();
                expect(await input.getAttribute('max')).toBe('9');
            });
        });

        describe('step', () => {
            it('should set value default value correctly', async () => {
                expect(await input.getAttribute('step')).toBe('1');
            });
            it('should set value correctly', async () => {
                element.setProperty('step', '2');
                await page.waitForChanges();
                expect(await input.getAttribute('step')).toBe('2');
            });
        });

        describe('type', () => {
            it('should not render empty by default', async () => {
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
        let spyErrorMessageEvt: EventSpy;

        beforeEach(async () => {
            page = await newE2EPage({
                html: `
              <dot-input-calendar name="time01"></dot-input-calendar>`
            });

            spyValueChange = await page.spyOnEvent('_valueChange');
            spyStatusChangeEvent = await page.spyOnEvent('_statusChange');
            spyErrorMessageEvt = await page.spyOnEvent('_errorMessage');
            element = await page.find('dot-input-calendar');
            input = await page.find('input');
        });

        describe('status value and error change', () => {
            it('should emit value correctly ', async () => {
                await input.press('4');
                await page.waitForChanges();

                expect(spyValueChange).toHaveReceivedEventDetail({
                    name: 'time01',
                    value: '4'
                });
            });

            it('should emit status and value and errorElement events on Reset when value is out of range', async () => {
                element.setProperty('min', '06:00:00');
                element.setProperty('max', '22:00:00');
                await element.callMethod('reset');
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'time01',
                    status: {
                        dotPristine: true,
                        dotTouched: false,
                        dotValid: false
                    }
                });
                expect(spyValueChange).toHaveReceivedEventDetail({ name: 'time01', value: '' });
                expect(spyErrorMessageEvt).toHaveReceivedEventDetail({
                    message: 'The field doesn\'t comply with the specified format',
                    show: false
                });
            });

            it('should emit status, value and errorElement events on changes  when value is out of range', async () => {
                await input.press('2');
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'time01',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: true
                    }
                });
                expect(spyValueChange).toHaveReceivedEventDetail({
                    name: 'time01',
                    value: '2'
                });
                expect(spyErrorMessageEvt).toHaveReceivedEventDetail({
                    show: false,
                    message: ''
                });
            });
        });

        describe('error event change', () => {
            describe('validationMessage', () => {
                beforeEach(async () => {
                    element.setProperty('min', '06:00:00');
                    element.setProperty('max', '22:00:00');
                    await input.press('0');
                });

                it('should emit default validationMessage message when value is not in valid range', async () => {
                    await page.waitForChanges();
                    expect(spyErrorMessageEvt).toHaveReceivedEventDetail({
                        show: true,
                        message: 'The field doesn\'t comply with the specified format'
                    });
                });

                it('should emit validationMessage message when value is not in valid range', async () => {
                    element.setProperty('validationMessage', 'Test');
                    await page.waitForChanges();
                    expect(spyErrorMessageEvt).toHaveReceivedEventDetail({
                        show: true,
                        message: 'Test'
                    });
                });
            });

            describe('requiredMessage', () => {
                beforeEach(async () => {
                    element.setProperty('required', true);
                    await input.press('1');
                    await input.press('Backspace');
                });

                it('should emit default required message when value is not in valid range', async () => {
                    await page.waitForChanges();
                    expect(spyErrorMessageEvt).toHaveReceivedEventDetail({
                        show: true,
                        message: 'This field is required'
                    });
                });

                it('should emit required message when value is not in valid range', async () => {
                    element.setProperty('requiredMessage', 'Test');
                    await page.waitForChanges();
                    expect(spyErrorMessageEvt).toHaveReceivedEventDetail({
                        show: true,
                        message: 'Test'
                    });
                });
            });
        });

        describe('status change', () => {
            it('should mark as touched when onblur', async () => {
                await input.triggerEvent('blur');
                await page.waitForChanges();

                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'time01',
                    status: {
                        dotPristine: true,
                        dotTouched: true,
                        dotValid: true
                    }
                });
            });
        });
    });
});
