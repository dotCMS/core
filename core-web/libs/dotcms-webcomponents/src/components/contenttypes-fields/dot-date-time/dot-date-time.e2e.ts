import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';
import { dotTestUtil } from '../../../test';

describe('dot-date-time', () => {
    let page: E2EPage;
    let element: E2EElement;
    let dateInput: E2EElement;
    let timeInput: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-date-time></dot-date-time>`
        });
        element = await page.find('dot-date-time');
        dateInput = await page.find('dot-input-calendar[type=date]');
        timeInput = await page.find('dot-input-calendar[type=time]');
    });

    describe('render CSS classes', () => {
        it('should be valid, untouched & pristine on load', () => {
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });

        it('should be valid, touched & dirty when filled', async () => {
            dotTestUtil.triggerStatusChange(false, true, true, timeInput);
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        describe('required', () => {
            beforeEach(async () => {
                await element.setProperty('required', 'true');
            });

            it('should be valid, untouched & pristine and required when filled on load', async () => {
                dotTestUtil.triggerStatusChange(true, false, true, timeInput);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequiredPristine);
            });

            it('should be valid, touched & dirty and required when filled', async () => {
                dotTestUtil.triggerStatusChange(false, true, true, timeInput);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
            });

            it('should be invalid, untouched, pristine and required when empty on load', async () => {
                dotTestUtil.triggerStatusChange(true, false, false, timeInput);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
            });

            it('should be invalid, touched, dirty and required when valued is cleared', async () => {
                dotTestUtil.triggerStatusChange(false, true, false, timeInput);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
            });

            it('should have touched but pristine on blur', async () => {
                dotTestUtil.triggerStatusChange(true, true, true, timeInput);
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
            });
        });
    });

    describe('@Props', () => {
        describe('dot-attr', () => {
            it('should set value correctly', async () => {
                page = await newE2EPage({
                    html: `<dot-date-time step="3,6"></dot-date-time>`
                });
                await page.waitForChanges();
                dateInput = await page.find('input[type=date]');
                timeInput = await page.find('input[type=time]');
                expect(dateInput.getAttribute('step')).toBe('3');
                expect(timeInput.getAttribute('step')).toBe('6');
            });
        });

        describe('value', () => {
            it('should render default value', () => {
                expect(dateInput.getAttribute('value')).toBeNull();
                expect(timeInput.getAttribute('value')).toBeNull();
            });

            it('should pass correctly date and time value', async () => {
                element.setProperty('value', '2019-01-01 10:10:01');
                await page.waitForChanges();
                expect(await dateInput.getProperty('value')).toBe('2019-01-01');
                expect(await timeInput.getProperty('value')).toBe('10:10:01');
            });

            it('should pass correctly date value and empty time', async () => {
                element.setProperty('value', '2019-01-01');
                await page.waitForChanges();
                expect(await dateInput.getProperty('value')).toBe('2019-01-01');
                expect(await timeInput.getProperty('value')).toBeNull();
            });

            it('should pass correctly time value and empty date', async () => {
                element.setProperty('value', '10:10:01');
                await page.waitForChanges();
                expect(await dateInput.getProperty('value')).toBeNull();
                expect(await timeInput.getProperty('value')).toBe('10:10:01');
            });
        });

        describe('name', () => {
            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('name', 'test');
                await page.waitForChanges();
                expect(dateInput.getAttribute('name')).toBe('test-date');
                expect(timeInput.getAttribute('name')).toBe('test-time');
            });

            it('should set name prop in dot-label', async () => {
                element.setProperty('name', 'test');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('name')).toBe('test');
            });

            it('should render default value', () => {
                expect(dateInput.getAttribute('name')).toBe('-date');
                expect(timeInput.getAttribute('name')).toBe('-time');
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
            it('should render hint correctly and set aria attribute', async () => {
                element.setProperty('hint', 'Test');
                await page.waitForChanges();
                const dateTimeBody = await page.find('.dot-date-time__body');
                expect((await dotTestUtil.getHint(page)).innerText).toBe('Test');
                expect(dateTimeBody.getAttribute('aria-describedby')).toBe('hint-test');
                expect(dateTimeBody.getAttribute('tabIndex')).toBe('0');
            });

            it('should not render hint and not set aria attribute', async () => {
                const dateTimeBody = await page.find('.dot-date-time__body');
                expect(await dotTestUtil.getHint(page)).toBeNull();
                expect(dateTimeBody.getAttribute('aria-describedby')).toBeNull();
                expect(dateTimeBody.getAttribute('tabIndex')).toBeNull();
            });
        });

        describe('required', () => {
            it('should not render required attribute', async () => {
                element.setProperty('required', 'false');
                await page.waitForChanges();
                expect(dateInput.getAttribute('required')).toBeNull();
                expect(timeInput.getAttribute('required')).toBeNull();
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
                dotTestUtil.triggerStatusChange(false, true, false, timeInput, false);
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
                element.setProperty('value', '2010-10-10 10:10:10');
                dotTestUtil.triggerStatusChange(false, true, false, timeInput);
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
                expect(dateInput.getAttribute('disabled')).toBeDefined();
                expect(timeInput.getAttribute('disabled')).toBeDefined();
            });

            it('should not render disabled attribute', async () => {
                element.setProperty('disabled', 'false');
                await page.waitForChanges();
                expect(dateInput.getAttribute('disabled')).toBeNull();
                expect(timeInput.getAttribute('disabled')).toBeNull();
            });
        });

        describe('min', () => {
            it('should set date correct value when valid', async () => {
                element.setProperty('min', '2019-01-01');
                await page.waitForChanges();
                expect(dateInput.getAttribute('min')).toBe('2019-01-01');
                expect(timeInput.getAttribute('min')).toBeNull();
            });

            it('should set time correct value when valid', async () => {
                element.setProperty('min', '10:10:10');
                await page.waitForChanges();
                expect(dateInput.getAttribute('min')).toBeNull();
                expect(timeInput.getAttribute('min')).toBe('10:10:10');
            });

            it('should set date and time correct value when valid', async () => {
                element.setProperty('min', '2019-01-01 10:10:10');
                await page.waitForChanges();
                expect(dateInput.getAttribute('min')).toBe('2019-01-01');
                expect(timeInput.getAttribute('min')).toBe('10:10:10');
            });

            it('should set empty value when invalid', async () => {
                element.setAttribute('min', '2019');
                await page.waitForChanges();
                expect(dateInput.getAttribute('min')).toBeNull();
                expect(timeInput.getAttribute('min')).toBeNull();
            });
        });

        describe('max', () => {
            // it('should set date correct value when valid', async () => {
            //     element.setProperty('max', '2019-01-01');
            //     await page.waitForChanges();
            //     expect(dateInput.getAttribute('max')).toBe('2019-01-01');
            //     expect(timeInput.getAttribute('max')).toBeNull();
            // });
            // it('should set time correct value when valid', async () => {
            //     element.setProperty('max', '10:10:10');
            //     await page.waitForChanges();
            //     expect(dateInput.getAttribute('max')).toBeNull();
            //     expect(timeInput.getAttribute('max')).toBe('10:10:10');
            // });
            // it('should set date and time correct value when valid', async () => {
            //     element.setProperty('max', '2019-01-01 10:10:10');
            //     await page.waitForChanges();
            //     expect(dateInput.getAttribute('max')).toBe('2019-01-01');
            //     expect(timeInput.getAttribute('max')).toBe('10:10:10');
            // });
            // it('should set empty value when invalid', async () => {
            //     element.setAttribute('max', '2019');
            //     await page.waitForChanges();
            //     expect(dateInput.getAttribute('max')).toBeNull();
            //     expect(timeInput.getAttribute('max')).toBeNull();
            // });
        });

        describe('step', () => {
            it('should set default value', async () => {
                expect(dateInput.getAttribute('step')).toBe('1');
                expect(timeInput.getAttribute('step')).toBe('1');
            });

            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('step', '2,3');
                await page.waitForChanges();
                expect(dateInput.getAttribute('step')).toBe('2');
                expect(timeInput.getAttribute('step')).toBe('3');
            });
        });

        describe('dateLabel', () => {
            let dateLabel: E2EElement;
            beforeEach(async () => {
                dateLabel = (await page.findAll('.dot-date-time__body label'))[0];
            });

            it('should set label prop in dot-label', async () => {
                element.setProperty('dateLabel', 'test');
                await page.waitForChanges();
                expect(dateLabel.innerText).toBe('test');
            });

            it('should render default value', () => {
                expect(dateLabel.innerText).toContain('Date');
            });
        });

        describe('timeLabel', () => {
            let timeLabel: E2EElement;
            beforeEach(async () => {
                timeLabel = (await page.findAll('.dot-date-time__body label'))[1];
            });

            it('should set label prop in dot-label', async () => {
                element.setProperty('timeLabel', 'test');
                await page.waitForChanges();
                expect(timeLabel.innerText).toBe('test');
            });

            it('should render default value', () => {
                expect(timeLabel.innerText).toContain('Time');
            });
        });
    });

    describe('@Events', () => {
        let spyStatusChangeEvent: EventSpy;
        let spyValueChangeEvent: EventSpy;

        beforeEach(async () => {
            spyStatusChangeEvent = await page.spyOnEvent('dotStatusChange');
            spyValueChangeEvent = await page.spyOnEvent('dotValueChange');
        });

        describe('value and status changes', () => {
            it('should display on wrapper not valid css classes when loaded when required and no value set', async () => {
                page = await newE2EPage({
                    html: `
                <dot-form>
                    <dot-date-time required="true" ></dot-date-time>
                </dot-form>`
                });
                const form = await page.find('dot-form');
                expect(form).toHaveClasses(dotTestUtil.class.emptyPristineInvalid);
            });

            it('should send value when both date and time are set', async () => {
                dateInput.triggerEvent('_dotValueChange', {
                    detail: {
                        name: '-date',
                        value: '2019-01-01'
                    }
                });
                timeInput.triggerEvent('_dotValueChange', {
                    detail: {
                        name: '-time',
                        value: '10:10:10'
                    }
                });

                await page.waitForChanges();
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    value: '2019-01-01 10:10:10'
                });
                expect(spyValueChangeEvent.events.length).toEqual(1);
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
                const evt_dotStatusChange = await page.spyOnEvent('_dotStatusChange');
                const evt_dotValueChange = await page.spyOnEvent('_dotValueChange');

                dateInput.triggerEvent('_dotValueChange', {
                    detail: {
                        name: '-date',
                        value: '2019-01-01'
                    }
                });
                timeInput.triggerEvent('_dotValueChange', {
                    detail: {
                        name: '-time',
                        value: '10:10:10'
                    }
                });
                dotTestUtil.triggerStatusChange(false, true, true, timeInput);

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
                    value: '2019-01-01 10:10:10'
                });

                expect(evt_dotStatusChange.events.length).toEqual(0);
                expect(evt_dotValueChange.events.length).toEqual(0);
            });
        });

        describe('status change', () => {
            it('should send status when dot-input-calendar send it', async () => {
                dotTestUtil.triggerStatusChange(false, true, false, timeInput);
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: false
                    }
                });
            });
        });
    });
});
