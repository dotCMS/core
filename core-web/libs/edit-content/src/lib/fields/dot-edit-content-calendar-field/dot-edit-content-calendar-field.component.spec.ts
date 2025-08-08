import { describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { Calendar } from 'primeng/calendar';

import { DotSystemTimezone } from '@dotcms/dotcms-models';

import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field.component';
import { CALENDAR_OPTIONS_PER_TYPE } from './dot-edit-content-calendar-field.util';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { DATE_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentCalendarFieldComponent', () => {
    let spectator: Spectator<DotEditContentCalendarFieldComponent>;
    let calendar: Element;

    const createComponent = createComponentFactory({
        component: DotEditContentCalendarFieldComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [FormGroupDirective]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                $field: DATE_FIELD_MOCK
            }
        });
        calendar = spectator.query(byTestId(`calendar-input-${DATE_FIELD_MOCK.variable}`));
    });

    // ASK: This is necessary?
    test.each([
        {
            variable: `calendar-id-${DATE_FIELD_MOCK.variable}`,
            attribute: 'id'
        },
        {
            variable: DATE_FIELD_MOCK.variable,
            attribute: 'ng-reflect-name'
        }
    ])('should have the $variable as $attribute', ({ variable, attribute }) => {
        expect(calendar.getAttribute(attribute)).toBe(variable);
    });

    describe.each([
        {
            fieldType: FIELD_TYPES.DATE_AND_TIME
        },
        {
            fieldType: FIELD_TYPES.DATE
        },
        {
            fieldType: FIELD_TYPES.TIME
        }
    ])('with fieldType as $fieldType', ({ fieldType }) => {
        let calendar: Calendar;

        const options = CALENDAR_OPTIONS_PER_TYPE[fieldType];

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    $field: { ...DATE_FIELD_MOCK, fieldType }
                }
            });

            calendar = spectator.debugElement.query(
                By.css(`[data-testid="calendar-input-${DATE_FIELD_MOCK.variable}"]`)
            ).componentInstance;
        });

        it('should have showTime as defined in the options', () => {
            expect(calendar.showTime).toBe(options.showTime);
        });

        it('should have the timeOnly as defined in the options', () => {
            expect(calendar.timeOnly).toBe(options.timeOnly);
        });

        it('should have the icon as defined in the options', () => {
            expect(calendar.icon).toBe(options.icon);
        });
    });

    describe('Timezone functionality', () => {
        it('should show timezone info for datetime fields', () => {
            spectator = createComponent({
                props: {
                    $field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME }
                }
            });

            expect(spectator.component.$showTimezoneInfo()).toBe(true);
        });

        it('should not show timezone info for date-only fields', () => {
            spectator = createComponent({
                props: {
                    $field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE }
                }
            });

            expect(spectator.component.$showTimezoneInfo()).toBe(false);
        });
    });

    describe('Form integration', () => {
        let component: DotEditContentCalendarFieldComponent;
        let formGroup: FormGroup;

        const systemTimezone: DotSystemTimezone = {
            id: 'Asia/Dubai',
            label: 'Gulf Standard Time',
            offset: 14400000 // +4 hours in milliseconds
        };

        beforeEach(() => {
            formGroup = new FormGroup({
                [DATE_FIELD_MOCK.variable]: new FormControl(null)
            });

            spectator = createComponent({
                props: {
                    $field: DATE_FIELD_MOCK,
                    $systemTimezone: systemTimezone
                }
            });

            // Setup form for the component
            Object.defineProperty(spectator.inject(ControlContainer), 'control', {
                value: formGroup,
                writable: true
            });
            component = spectator.component;
        });

        it('should update form control when onCalendarChange is called', () => {
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            const selectedDate = new Date('2024-01-15T14:30:00');

            component.onCalendarChange(selectedDate);

            // The control should receive a UTC date converted from server time
            expect(control?.value).toBeDefined();
            expect(control?.value).toBeInstanceOf(Date);
        });

        it('should handle null values gracefully', () => {
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            control?.setValue(null);

            spectator.detectChanges();

            expect(control?.value).toBeNull();
        });

        it('should convert server time to UTC properly', () => {
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            const serverDate = new Date('2024-01-15T14:30:00');

            component.onCalendarChange(serverDate);

            // Verify the control received a date (exact conversion depends on timezone)
            expect(control?.value).toBeInstanceOf(Date);
            expect(control?.value).not.toBe(serverDate); // Should be a new Date object (converted)
        });

        it('should display internal value from writeValue', () => {
            const utcDate = new Date('2024-01-15T14:30:00.000Z');

            component.writeValue(utcDate);
            spectator.detectChanges();

            const internalValue = component.$internalValue();
            expect(internalValue).toBeInstanceOf(Date);
        });

        it('should handle null values in writeValue', () => {
            component.writeValue(null);
            spectator.detectChanges();

            const internalValue = component.$internalValue();
            expect(internalValue).toBeNull();
        });
    });

    describe('UI display', () => {
        it('should display timezone information for datetime fields', () => {
            spectator = createComponent({
                props: {
                    $field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME }
                }
            });

            spectator.detectChanges();

            const timezoneInfo = spectator.query('[data-testid="calendar-field-timezone"]');
            expect(timezoneInfo).toBeTruthy();
        });

        it('should display server timezone when provided', () => {
            const systemTimezone: DotSystemTimezone = {
                id: 'Asia/Dubai',
                label: 'Gulf Standard Time',
                offset: 14400000
            };

            spectator = createComponent({
                props: {
                    $field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME },
                    $systemTimezone: systemTimezone
                }
            });

            spectator.detectChanges();

            const timezoneInfo = spectator.query('[data-testid="calendar-field-timezone"]');
            expect(timezoneInfo).toBeTruthy();
            expect(timezoneInfo.textContent).toContain('Gulf Standard Time');
        });
    });

    describe('Timezone conversion consistency - Save/Load cycle', () => {
        let component: DotEditContentCalendarFieldComponent;
        let formGroup: FormGroup;

        const dubaTimezone: DotSystemTimezone = {
            id: 'Asia/Dubai',
            label: 'Gulf Standard Time (Asia/Dubai)',
            offset: 14400000 // +4 hours
        };

        beforeEach(() => {
            formGroup = new FormGroup({
                [DATE_FIELD_MOCK.variable]: new FormControl(null)
            });

            spectator = createComponent({
                props: {
                    $field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME },
                    $systemTimezone: dubaTimezone
                }
            });

            // Setup form for the component
            Object.defineProperty(spectator.inject(ControlContainer), 'control', {
                value: formGroup,
                writable: true
            });
            component = spectator.component;
        });

        it('should maintain timezone consistency for datetime fields in Dubai timezone', () => {
            // Given: User selects 20:44 in Dubai timezone
            const userSelection = new Date(2025, 7, 6, 20, 44, 0); // Aug 6, 2025 20:44

            // When: User changes the calendar value
            component.onCalendarChange(userSelection);

            // Then: The form control should receive UTC time
            const formControlValue = formGroup.get(DATE_FIELD_MOCK.variable)?.value;
            expect(formControlValue).toBeInstanceOf(Date);
            expect(formControlValue.getUTCHours()).toBe(16); // 20:44 Dubai = 16:44 UTC
            expect(formControlValue.getUTCMinutes()).toBe(44);

            // When: Form value is written back (like after save/reload)
            component.writeValue(formControlValue);

            // Then: Internal value should display the original time (20:44)
            const displayValue = component.$internalValue();
            expect(displayValue).toBeInstanceOf(Date);
            expect(displayValue!.getHours()).toBe(20);
            expect(displayValue!.getMinutes()).toBe(44);
        });

        it('should handle edge cases where time crosses day boundaries', () => {
            // Given: User selects 23:30 in Dubai (which becomes 19:30 UTC same day)
            const userSelection = new Date(2025, 7, 6, 23, 30, 0);

            // When: User changes the calendar value
            component.onCalendarChange(userSelection);

            // Then: Form control should have correct UTC time
            const formControlValue = formGroup.get(DATE_FIELD_MOCK.variable)?.value;
            expect(formControlValue.getUTCDate()).toBe(6); // Same day
            expect(formControlValue.getUTCHours()).toBe(19);
            expect(formControlValue.getUTCMinutes()).toBe(30);

            // When: Form value is written back
            component.writeValue(formControlValue);

            // Then: Should display original server time
            const displayValue = component.$internalValue();
            expect(displayValue!.getDate()).toBe(6);
            expect(displayValue!.getHours()).toBe(23);
            expect(displayValue!.getMinutes()).toBe(30);
        });

        it('should handle date-only fields correctly (no timezone conversion)', () => {
            // Given: Date-only field
            spectator = createComponent({
                props: {
                    $field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE },
                    $systemTimezone: dubaTimezone
                }
            });

            Object.defineProperty(spectator.inject(ControlContainer), 'control', {
                value: formGroup,
                writable: true
            });
            component = spectator.component;

            // When: User selects a date
            const userSelection = new Date(2025, 7, 6, 0, 0, 0);
            component.onCalendarChange(userSelection);

            // Then: Form control should receive UTC midnight
            const formControlValue = formGroup.get(DATE_FIELD_MOCK.variable)?.value;
            expect(formControlValue.getUTCFullYear()).toBe(2025);
            expect(formControlValue.getUTCMonth()).toBe(7);
            expect(formControlValue.getUTCDate()).toBe(6);
            expect(formControlValue.getUTCHours()).toBe(0);
            expect(formControlValue.getUTCMinutes()).toBe(0);
        });
    });
});
