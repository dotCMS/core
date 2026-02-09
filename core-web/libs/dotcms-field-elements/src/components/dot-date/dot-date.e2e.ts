import { E2EElement, E2EPage, newE2EPage, EventSpy } from '@stencil/core/testing';

import { dotTestUtil } from '../../utils';

describe('dot-date', () => {
    let page: E2EPage;
    let element: E2EElement;
    let inputCalendar: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-date></dot-date>`
        });
        element = await page.find('dot-date');
        inputCalendar = await page.find('dot-input-calendar ');
    });

    describe('render CSS classes', () => {
        it('should be valid, untouched & pristine on load', () => {
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });

        it('should be valid, touched & dirty when filled', async () => {
            dotTestUtil.triggerStatusChange(false, true, true, inputCalendar);
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        describe('required', () => {
            beforeEach(async () => {
                await element.setProperty('required', 'true');
            });

            it('should be valid, untouched & pristine and required when filled on load', async () => {
                dotTestUtil.triggerStatusChange(true, false, true, inputCalendar);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequiredPristine);
            });

            it('should be valid, touched & dirty and required when filled', async () => {
                dotTestUtil.triggerStatusChange(false, true, true, inputCalendar);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
            });

            it('should be invalid, untouched, pristine and required when empty on load', async () => {
                dotTestUtil.triggerStatusChange(true, false, false, inputCalendar);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
            });

            it('should be invalid, touched, dirty and required when valued is cleared', async () => {
                dotTestUtil.triggerStatusChange(false, true, false, inputCalendar);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
            });

            it('should have touched but pristine on blur', async () => {
                dotTestUtil.triggerStatusChange(true, true, true, inputCalendar);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
            });
        });
    });

    describe('@Props', () => {
        describe('dot-attr', () => {
            it('should set value correctly', async () => {
                page = await newE2EPage({
                    html: `<dot-date dotstep="3"></dot-date>`
                });
                await page.waitForChanges();
                inputCalendar = await page.find('input');
                expect(inputCalendar.getAttribute('step')).toBe('3');
            });
        });

        describe('value', () => {
            it('should render default value', () => {
                expect(inputCalendar.getAttribute('value')).toBe('');
            });

            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('value', '2019-01-01');
                await page.waitForChanges();
                expect(await inputCalendar.getProperty('value')).toBe('2019-01-01');
            });
        });

        describe('name', () => {
            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('name', 'date');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('name')).toBe('date');
            });

            it('should set name prop in dot-label', async () => {
                element.setProperty('name', 'date');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('name')).toBe('date');
            });

            it('should render default value', () => {
                expect(inputCalendar.getAttribute('name')).toBe('');
            });
        });

        describe('label', () => {
            it('should set label prop in dot-label', async () => {
                element.setProperty('label', 'test');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('label')).toBe('test');
            });

            it('should render default value', async () => {
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('label')).toBe('');
            });
        });

        describe('hint', () => {
            it('should set hint and set aria attribute', async () => {
                element.setProperty('hint', 'Test');
                await page.waitForChanges();
                expect((await dotTestUtil.getHint(page)).innerText).toBe('Test');
                expect(inputCalendar.getAttribute('aria-describedby')).toBe('hint-test');
                expect(inputCalendar.getAttribute('tabIndex')).toBe('0');
            });

            it('should not render and set aria attribute', async () => {
                expect(await dotTestUtil.getHint(page)).toBeNull();
                expect(inputCalendar.getAttribute('aria-describedby')).toBeNull();
                expect(inputCalendar.getAttribute('tabIndex')).toBeNull();
            });
        });

        describe('required', () => {
            it('should render required attribute with invalid value', async () => {
                element.setProperty('required', { test: 'test' });
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('required')).toBeDefined();
            });

            it('should not render required attribute', async () => {
                element.setProperty('required', 'false');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('required')).toBeNull();
            });

            it('should render required attribute for the dot-label', async () => {
                element.setProperty('required', 'true');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('required')).toBeDefined();
            });
        });

        describe('requiredMessage', () => {
            beforeEach(() => {
                element.setProperty('required', 'true');
                dotTestUtil.triggerStatusChange(false, true, false, inputCalendar, false);
            });

            it('should render default value', async () => {
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                    'This field is required'
                );
            });

            it('should render custom message', async () => {
                element.setProperty('requiredMessage', 'test');
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe('test');
            });
        });

        describe('validationMessage', () => {
            beforeEach(() => {
                element.setProperty('value', '2010-10-10');
                dotTestUtil.triggerStatusChange(false, true, false, inputCalendar, false);
            });

            it('should render default value', async () => {
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                    "The field doesn't comply with the specified format"
                );
            });

            it('should render custom message', async () => {
                element.setProperty('validationMessage', 'validation');
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe('validation');
            });
        });

        describe('disabled', () => {
            it('should render disabled attribute', async () => {
                element.setProperty('disabled', 'true');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('disabled')).toBeDefined();
            });

            it('should not render disabled attribute', async () => {
                element.setProperty('disabled', 'false');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('disabled')).toBeNull();
            });
        });

        describe('min', () => {
            it('should set correct value when valid', async () => {
                element.setAttribute('min', '2019-01-01');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('min')).toBe('2019-01-01');
            });

            it('should set empty value when invalid', async () => {
                element.setAttribute('min', '2019');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('min')).toBe('');
            });
        });

        describe('max', () => {
            it('should set correct value when valid', async () => {
                element.setAttribute('max', '2018-10-10');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('max')).toBe('2018-10-10');
            });

            it('should set empty value when invalid', async () => {
                element.setAttribute('max', { test: true });
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('max')).toBe('');
            });
        });

        describe('step', () => {
            it('should set default value', () => {
                expect(inputCalendar.getAttribute('step')).toBe('1');
            });

            it('should pass correctly to dot-input-calendar', async () => {
                element.setAttribute('step', '5');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('step')).toBe('5');
            });
        });
    });

    describe('@Events', () => {
        let spyStatusChangeEvent: EventSpy;
        let spyValueChangeEvent: EventSpy;

        beforeEach(async () => {
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
            spyValueChangeEvent = await page.spyOnEvent('valueChange');
        });

        describe('value and status changes', () => {
            it('should display on wrapper not valid css classes when loaded when required and no value set', async () => {
                page = await newE2EPage({
                    html: `
                <dot-form>
                    <dot-date required="true" ></dot-date>
                </dot-form>`
                });
                const form = await page.find('dot-form');
                expect(form).toHaveClasses(dotTestUtil.class.emptyPristineInvalid);
            });

            it('should send value when dot-input-calendar send it', async () => {
                inputCalendar.triggerEvent('_valueChange', {
                    detail: {
                        name: '',
                        value: '2019-01-01'
                    }
                });
                await page.waitForChanges();
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    value: '2019-01-01'
                });
            });

            it('should emit status and value on Reset', async () => {
                await element.callMethod('reset');
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: true,
                        dotTouched: false,
                        dotValid: true
                    }
                });
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    value: ''
                });
            });

            it('should send status and value change and stop dot-input-calendar events', async () => {
                const evt_statusChange = await page.spyOnEvent('_statusChange');
                const evt_valueChange = await page.spyOnEvent('_valueChange');

                inputCalendar.triggerEvent('_valueChange', {
                    detail: {
                        name: '',
                        value: '2019-01-01'
                    }
                });
                dotTestUtil.triggerStatusChange(false, true, true, inputCalendar, true);
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: true
                    }
                });
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    value: '2019-01-01'
                });
                expect(evt_statusChange.events.length).toEqual(0);
                expect(evt_valueChange.events.length).toEqual(0);
            });
        });

        describe('status change', () => {
            it('should send status when dot-input-calendar send it', async () => {
                dotTestUtil.triggerStatusChange(true, false, false, inputCalendar, true);
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: true,
                        dotTouched: false,
                        dotValid: false
                    }
                });
            });
        });
    });
});
