import { describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { Calendar } from 'primeng/calendar';

import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field.component';
import {
    CALENDAR_OPTIONS_PER_TYPE,
    convertToServerTimezoneForDisplay,
    convertToUTCForSaving
} from './dot-edit-content-calendar-field.util';

import { SystemTimezone } from '@dotcms/dotcms-js';
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
        calendar = spectator.query(byTestId(DATE_FIELD_MOCK.variable));
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
                By.css(`[data-testId="${DATE_FIELD_MOCK.variable}"]`)
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

    describe('Form integration with timezone', () => {
        let component: DotEditContentCalendarFieldComponent;
        let formGroup: FormGroup;
        let formGroupDirective: FormGroupDirective;

        const estTimezone: SystemTimezone = {
            id: 'America/New_York',
            label: 'Eastern Standard Time',
            offset: '-18000' // -5 hours in seconds (as string)
        };

        const jstTimezone: SystemTimezone = {
            id: 'Asia/Tokyo',
            label: 'Japan Standard Time',
            offset: '32400' // +9 hours in seconds (as string)
        };

        beforeEach(() => {
            formGroup = new FormGroup({
                [DATE_FIELD_MOCK.variable]: new FormControl(null)
            });

            formGroupDirective = createFormGroupDirectiveMock(formGroup);

            spectator = createComponent({
                props: {
                    $field: DATE_FIELD_MOCK,
                    $systemTimezone: estTimezone
                }
            });

            // Manually inject the form group directive for testing
            spectator.inject(ControlContainer).control = formGroup;
            component = spectator.component;
        });

        it('should convert string values to Date in effect', () => {
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            control?.setValue('2024-01-15T12:00:00.000Z');

            spectator.detectChanges();

            expect(control?.value).toBeInstanceOf(Date);
            expect(control?.value.toISOString()).toBe('2024-01-15T12:00:00.000Z');
        });

        it('should not modify Date values in effect', () => {
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            const originalDate = new Date('2024-01-15T12:00:00.000Z');
            control?.setValue(originalDate);

            spectator.detectChanges();

            expect(control?.value).toBe(originalDate);
        });

        it('should handle null values gracefully', () => {
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            control?.setValue(null);

            spectator.detectChanges();

            expect(control?.value).toBeNull();
        });

        it('should update form control when onDateSelect is called', () => {
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            const selectedDate = new Date('2024-01-15T07:00:00.000Z'); // 7:00 AM EST display time

            component.onDateSelect(selectedDate);

            // Should save as UTC (12:00 UTC) when timezone is EST
            expect(control?.value).toBeInstanceOf(Date);
            expect(control?.value.getUTCHours()).toBe(12);
        });

        it('should save original date when no timezone', () => {
            spectator.setInput('$systemTimezone', null);
            const control = formGroup.get(DATE_FIELD_MOCK.variable);
            const selectedDate = new Date('2024-01-15T12:00:00.000Z');

            component.onDateSelect(selectedDate);

            expect(control?.value).toBe(selectedDate);
        });
    });

    describe('Utility functions integration', () => {
        const estTimezone: SystemTimezone = {
            id: 'America/New_York',
            label: 'Eastern Standard Time',
            offset: '-18000' // -5 hours in seconds
        };

        const jstTimezone: SystemTimezone = {
            id: 'Asia/Tokyo',
            label: 'Japan Standard Time',
            offset: '32400' // +9 hours in seconds
        };

        it('should convert UTC date to server timezone for display (EST)', () => {
            const utcDate = new Date('2024-01-15T12:00:00.000Z'); // 12:00 UTC

            const result = convertToServerTimezoneForDisplay(utcDate, estTimezone);

            // Should be 7:00 AM EST (12:00 UTC - 5 hours)
            expect(result.getUTCHours()).toBe(7);
            expect(result.getUTCMinutes()).toBe(0);
        });

        it('should convert UTC date to server timezone for display (JST)', () => {
            const utcDate = new Date('2024-01-15T12:00:00.000Z'); // 12:00 UTC

            const result = convertToServerTimezoneForDisplay(utcDate, jstTimezone);

            // Should be 21:00 JST (12:00 UTC + 9 hours)
            expect(result.getUTCHours()).toBe(21);
            expect(result.getUTCMinutes()).toBe(0);
        });

        it('should convert server timezone date back to UTC for saving (EST)', () => {
            const serverDate = new Date('2024-01-15T07:00:00.000Z'); // 7:00 AM EST display

            const result = convertToUTCForSaving(serverDate, estTimezone);

            // Should be 12:00 UTC (7:00 + 5 hours)
            expect(result.getUTCHours()).toBe(12);
            expect(result.getUTCMinutes()).toBe(0);
        });

        it('should convert server timezone date back to UTC for saving (JST)', () => {
            const serverDate = new Date('2024-01-15T21:00:00.000Z'); // 21:00 JST display

            const result = convertToUTCForSaving(serverDate, jstTimezone);

            // Should be 12:00 UTC (21:00 - 9 hours)
            expect(result.getUTCHours()).toBe(12);
            expect(result.getUTCMinutes()).toBe(0);
        });

        it('should maintain consistency in round-trip conversion', () => {
            const originalUTC = new Date('2024-01-15T15:30:00.000Z');

            // Convert for display
            const displayDate = convertToServerTimezoneForDisplay(originalUTC, estTimezone);

            // Convert back for saving
            const savedDate = convertToUTCForSaving(displayDate, estTimezone);

            // Should be the same as original
            expect(savedDate.getTime()).toBe(originalUTC.getTime());
        });

        it('should handle null timezone gracefully', () => {
            const date = new Date('2024-01-15T12:00:00.000Z');

            const displayResult = convertToServerTimezoneForDisplay(date, null);
            const saveResult = convertToUTCForSaving(date, null);

            expect(displayResult).toBe(date);
            expect(saveResult).toBe(date);
        });

        it('should handle UTC timezone (offset 0)', () => {
            const utcTimezone: SystemTimezone = {
                id: 'UTC',
                label: 'Coordinated Universal Time',
                offset: '0'
            };
            const date = new Date('2024-01-15T12:00:00.000Z');

            const displayResult = convertToServerTimezoneForDisplay(date, utcTimezone);
            const saveResult = convertToUTCForSaving(date, utcTimezone);

            expect(displayResult.getTime()).toBe(date.getTime());
            expect(saveResult.getTime()).toBe(date.getTime());
        });
    });

    describe('Edge cases', () => {
        it('should handle year boundary dates', () => {
            const newYearUTC = new Date('2023-12-31T23:30:00.000Z');
            const aucklandTimezone: SystemTimezone = {
                id: 'Pacific/Auckland',
                label: 'New Zealand Standard Time',
                offset: '43200' // +12 hours in seconds
            };

            const result = convertToServerTimezoneForDisplay(newYearUTC, aucklandTimezone);

            // Should be 11:30 AM on January 1st, 2024
            expect(result.getUTCHours()).toBe(11);
            expect(result.getUTCMinutes()).toBe(30);
            expect(result.getUTCDate()).toBe(1); // Next day
        });

        it('should handle leap year dates', () => {
            const leapDate = new Date('2024-02-29T12:00:00.000Z');
            const timezone: SystemTimezone = {
                id: 'UTC',
                label: 'UTC',
                offset: '0'
            };

            const result = convertToServerTimezoneForDisplay(leapDate, timezone);

            expect(result.getTime()).toBe(leapDate.getTime());
        });

        it('should handle large timezone offsets', () => {
            const date = new Date('2024-01-15T12:00:00.000Z');
            const extremeTimezone: SystemTimezone = {
                id: 'Pacific/Kiritimati',
                label: 'Line Islands Time',
                offset: '50400' // +14 hours in seconds
            };

            const result = convertToServerTimezoneForDisplay(date, extremeTimezone);

            // Should be 2:00 AM next day
            expect(result.getUTCHours()).toBe(2);
            expect(result.getUTCDate()).toBe(16); // Next day
        });
    });
});
