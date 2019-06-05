import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';
import { dotTestUtil } from '../../utils';

describe('dot-time', () => {
    let page: E2EPage;
    let element: E2EElement;
    let inputCalendar: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-time></dot-time>`
        });
        element = await page.find('dot-time');
        inputCalendar = await page.find('dot-input-calendar ');
    });

    describe('render CSS classes', () => {
        it('should be valid, untouched & pristine on load', () => {
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });

        it('should be valid, touched & dirty when filled', async () => {
            inputCalendar.triggerEvent('_statusChange', {
                detail: {
                    name: '',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: true
                    }
                }
            });
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        describe('required', () => {
            beforeEach(async () => {
                await element.setProperty('required', 'true');
            });

            it('should be valid, untouched & pristine and required when filled on load', async () => {
                inputCalendar.triggerEvent('_statusChange', {
                    detail: {
                        name: '',
                        status: {
                            dotPristine: true,
                            dotTouched: false,
                            dotValid: true
                        }
                    }
                });
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequiredPristine);
            });

            it('should be valid, touched & dirty and required when filled', async () => {
                inputCalendar.triggerEvent('_statusChange', {
                    detail: {
                        name: '',
                        status: {
                            dotPristine: false,
                            dotTouched: true,
                            dotValid: true
                        }
                    }
                });
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
            });

            it('should be invalid, untouched, pristine and required when empty on load', async () => {
                inputCalendar.triggerEvent('_statusChange', {
                    detail: {
                        name: '',
                        status: {
                            dotPristine: true,
                            dotTouched: false,
                            dotValid: false
                        }
                    }
                });
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
            });

            it('should be invalid, touched, dirty and required when valued is cleared', async () => {
                inputCalendar.triggerEvent('_statusChange', {
                    detail: {
                        name: '',
                        status: {
                            dotPristine: false,
                            dotTouched: true,
                            dotValid: false
                        }
                    }
                });
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
            });

            it('should have touched but pristine', async () => {
                inputCalendar.triggerEvent('_statusChange', {
                    detail: {
                        name: '',
                        status: {
                            dotPristine: true,
                            dotTouched: true,
                            dotValid: true
                        }
                    }
                });

                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
            });
        });
    });

    describe('@Props', () => {
        describe('value', () => {
            it('should render default value', () => {
                expect(inputCalendar.getAttribute('value')).toBe('');
            });

            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('value', '10:10:00');
                await page.waitForChanges();
                expect(await inputCalendar.getProperty('value')).toBe('10:10:00');
            });
        });

        describe('name', () => {
            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('name', 'time');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('name')).toBe('time');
            });

            it('should set name prop in dot-label', async () => {
                element.setProperty('name', 'time');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('name')).toBe('time');
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
            it('should set hint correctly', async () => {
                element.setProperty('hint', 'Test');
                await page.waitForChanges();
                expect((await dotTestUtil.getHint(page)).innerText).toBe('Test');
            });

            it('should not render hint', async () => {
                expect(await dotTestUtil.getHint(page)).toBeNull();
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
            it('should render default value', () => {
                expect(inputCalendar.getAttribute('required-message')).toBe(
                    'This field is required'
                );
            });

            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('requiredMessage', 'test');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('required-message')).toBe('test');
            });
        });

        describe('validationMessage', () => {
            it('should render default value', () => {
                expect(inputCalendar.getAttribute('validation-message')).toBe(
                    "The field doesn't comply with the specified format"
                );
            });

            it('should pass correctly to dot-input-calendar', async () => {
                element.setProperty('validationMessage', 'test');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('validation-message')).toBe('test');
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
                element.setAttribute('min', '10:10:01');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('min')).toBe('10:10:01');
            });

            it('should set empty value when invalid', async () => {
                element.setAttribute('min', '10');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('min')).toBe('');
            });
        });

        describe('max', () => {
            it('should set correct value when valid', async () => {
                element.setAttribute('max', '10:10:01');
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('max')).toBe('10:10:01');
            });

            it('should set empty value when invalid', async () => {
                element.setAttribute('max', { test: true });
                await page.waitForChanges();
                expect(inputCalendar.getAttribute('max')).toBe('');
            });
        });

        describe('step', () => {
            it('should set default value', async () => {
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
            it('should send value when dot-input-calendar send it', async () => {
                inputCalendar.triggerEvent('_valueChange', {
                    detail: {
                        name: '',
                        value: '21:30:30'
                    }
                });
                await page.waitForChanges();
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    value: '21:30:30'
                });
            });

            it('should emit status and value on Reset', async () => {
                await inputCalendar.callMethod('reset');
                await page.waitForChanges();
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
                        value: '21:30:30'
                    }
                });
                inputCalendar.triggerEvent('_statusChange', {
                    detail: {
                        name: '',
                        status: {
                            dotPristine: false,
                            dotTouched: true,
                            dotValid: true
                        }
                    }
                });
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
                    value: '21:30:30'
                });
                expect(evt_statusChange.events).toEqual([]);
                expect(evt_valueChange.events).toEqual([]);
            });
        });

        describe('status change', () => {
            it('should send status when dot-input-calendar send it', async () => {
                inputCalendar.triggerEvent('_statusChange', {
                    detail: {
                        name: '',
                        status: {
                            dotPristine: true,
                            dotTouched: false,
                            dotValid: false
                        }
                    }
                });
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
