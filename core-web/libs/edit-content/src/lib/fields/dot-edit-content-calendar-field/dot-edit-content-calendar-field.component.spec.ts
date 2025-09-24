import { describe } from '@jest/globals';
import { SpectatorHost, byTestId, createHostFactory, mockProvider } from '@ngneat/spectator/jest';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Calendar } from 'primeng/calendar';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotSystemTimezone } from '@dotcms/dotcms-models';

import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field.component';
import * as calendarUtils from './dot-edit-content-calendar-field.util';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FieldType } from '../../models/dot-edit-content-field.type';
import { CONTENT_TYPE_MOCK, DATE_FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentCalendarFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentCalendarFieldComponent>;

    const createHost = createHostFactory({
        component: DotEditContentCalendarFieldComponent,
        imports: [ReactiveFormsModule, TooltipModule],
        detectChanges: false,
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('Never expires')
            })
        ]
    });

    // Mock system timezone
    const MOCK_TIMEZONE: DotSystemTimezone = {
        id: 'America/New_York',
        label: 'Eastern Time (GMT-5)',
        offset: -18000000
    };

    const CONTENT_TYPE_WITH_EXPIRE = {
        ...CONTENT_TYPE_MOCK,
        expireDateVar: DATE_FIELD_MOCK.variable
    };

    const CONTENT_TYPE_WITHOUT_EXPIRE = {
        ...CONTENT_TYPE_MOCK,
        expireDateVar: null
    };

    describe('Calendar field timezone information', () => {
        it('should show timezone info for DATE_AND_TIME fields when timezone is provided', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME },
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const timezoneElement = spectator.query(byTestId('calendar-field-timezone'));
            expect(timezoneElement).toExist();
            expect(timezoneElement).toContainText(MOCK_TIMEZONE.label);
        });

        it('should show timezone info for TIME fields when timezone is provided', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.TIME },
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const timezoneElement = spectator.query(byTestId('calendar-field-timezone'));
            expect(timezoneElement).toExist();
            expect(timezoneElement).toContainText(MOCK_TIMEZONE.label);
        });

        it('should NOT show timezone info for DATE fields', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE },
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    } as unknown
                }
            );
            spectator.detectChanges();

            const timezoneElement = spectator.query(byTestId('calendar-field-timezone'));
            expect(timezoneElement).not.toExist();
        });

        it('should NOT show timezone info when no timezone is provided', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME },
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            // El elemento timezone existe pero no debe mostrar contenido
            const timezoneElement = spectator.query(byTestId('calendar-field-timezone'));
            expect(timezoneElement).toBeNull();
        });
    });

    describe('Calendar field hint', () => {
        it('should show hint when field has hint property', () => {
            const fieldWithHint = {
                ...DATE_FIELD_MOCK,
                fieldType: FIELD_TYPES.DATE_AND_TIME,
                hint: 'Test hint message'
            };

            // Usar DATE_AND_TIME field para que showTimezoneInfo sea true y aparezca el contenedor
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithHint.variable]: new FormControl()
                        }),
                        field: fieldWithHint,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            expect(fieldWithHint.hint).toBe('Test hint message');

            const hintElement = spectator.query(Tooltip);
            expect(hintElement).toExist();
        });

        it('should NOT show hint when field has no hint property', () => {
            const fieldWithoutHint = { ...DATE_FIELD_MOCK, hint: undefined };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutHint.variable]: new FormControl()
                        }),
                        field: fieldWithoutHint,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const hintElement = spectator.query(byTestId('calendar-field-hint'));
            expect(hintElement).not.toExist();
        });
    });

    describe('Expire date field behavior', () => {
        it('should show placeholder and showClear when field is expire date field', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITH_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(Calendar);
            expect(calendar.showClear).toBe(true);

            expect(calendar.placeholder).toBe('Never expires');
        });

        it('should NOT show placeholder and showClear when field is NOT expire date field', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(Calendar);
            expect(calendar.showClear).toBe(false);
            expect(calendar.placeholder).toBe('');
        });
    });

    describe('Disabled state', () => {
        it('should disable calendar when setDisabledState is called with true', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            component.setDisabledState(true);
            spectator.detectChanges();

            const calendar = spectator.query(Calendar);
            expect(calendar.disabled).toBe(true);
        });

        it('should enable calendar when setDisabledState is called with false', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            component.setDisabledState(false);
            spectator.detectChanges();

            const calendar = spectator.query(Calendar);
            expect(calendar.disabled).toBe(false);
        });
    });

    describe('Field type configurations', () => {
        it('should configure DATE_AND_TIME field correctly', () => {
            const dateTimeField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [dateTimeField.variable]: new FormControl()
                        }),
                        field: dateTimeField,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(Calendar);
            expect(calendar.showTime).toBe(true);
            expect(calendar.timeOnly).toBe(false);
            expect(calendar.icon).toBe('pi pi-calendar');
        });

        it('should configure DATE field correctly', () => {
            const dateField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [dateField.variable]: new FormControl()
                        }),
                        field: dateField,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(Calendar);
            expect(calendar.showTime).toBe(false);
            expect(calendar.timeOnly).toBe(false);
            expect(calendar.icon).toBe('pi pi-calendar');
        });

        it('should configure TIME field correctly', () => {
            const timeField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.TIME };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [timeField.variable]: new FormControl()
                        }),
                        field: timeField,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(Calendar);
            expect(calendar.showTime).toBe(true);
            expect(calendar.timeOnly).toBe(true);
            expect(calendar.icon).toBe('pi pi-clock');
        });
    });

    describe('Default value handling', () => {
        beforeEach(() => {
            // Mock utility functions
            jest.spyOn(calendarUtils, 'processFieldDefaultValue').mockReturnValue(null);
            jest.spyOn(calendarUtils, 'processExistingValue').mockReturnValue(null);
            jest.spyOn(calendarUtils, 'getCurrentServerTime').mockReturnValue(
                new Date('2024-01-15T10:30:00Z')
            );
        });

        afterEach(() => {
            jest.restoreAllMocks();
        });

        it('should process field default value when field has defaultValue', () => {
            const mockDefaultResult = {
                displayValue: new Date('2024-01-15T10:30:00Z'),
                formValue: new Date('2024-01-15T15:30:00Z')
            };

            jest.spyOn(calendarUtils, 'processFieldDefaultValue').mockReturnValue(
                mockDefaultResult
            );

            const fieldWithDefault = { ...DATE_FIELD_MOCK, defaultValue: 'now' };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithDefault.variable]: new FormControl()
                        }),
                        field: fieldWithDefault,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            component.writeValue(null);

            expect(calendarUtils.processFieldDefaultValue).toHaveBeenCalledWith(
                fieldWithDefault,
                MOCK_TIMEZONE
            );
        });

        it('should NOT process default value when field has no defaultValue', () => {
            const fieldWithoutDefault = { ...DATE_FIELD_MOCK, defaultValue: undefined };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutDefault.variable]: new FormControl()
                        }),
                        field: fieldWithoutDefault,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            component.writeValue(null);

            expect(calendarUtils.processFieldDefaultValue).toHaveBeenCalledWith(
                fieldWithoutDefault,
                MOCK_TIMEZONE
            );
        });

        it('should process existing value when writeValue is called', () => {
            const existingValue = new Date('2024-01-10T14:20:00Z').getTime(); // Convert to timestamp
            const mockProcessedValue = new Date('2024-01-10T09:20:00Z');

            jest.spyOn(calendarUtils, 'processExistingValue').mockReturnValue(mockProcessedValue);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            component.writeValue(existingValue);

            expect(calendarUtils.processExistingValue).toHaveBeenCalledWith(
                existingValue,
                DATE_FIELD_MOCK.fieldType as FieldType,
                MOCK_TIMEZONE
            );
        });

        it('should handle timezone reprocessing when timezone becomes available', () => {
            const existingValue = new Date('2024-01-10T14:20:00Z').getTime(); // Convert to timestamp
            const mockProcessedValue = new Date('2024-01-10T09:20:00Z');

            jest.spyOn(calendarUtils, 'processExistingValue').mockReturnValue(mockProcessedValue);

            // Start without timezone
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            component.writeValue(existingValue);
            spectator.detectChanges();

            expect(calendarUtils.processExistingValue).toHaveBeenCalledWith(
                existingValue,
                DATE_FIELD_MOCK.fieldType as FieldType,
                MOCK_TIMEZONE
            );
        });
    });

    describe('Calendar value changes', () => {
        beforeEach(() => {
            // Mock utility functions
            jest.spyOn(calendarUtils, 'extractDateComponents').mockReturnValue({
                year: 2024,
                month: 0,
                date: 15,
                hours: 10,
                minutes: 30,
                seconds: 0
            });
            jest.spyOn(calendarUtils, 'createUtcDateAtMidnight').mockReturnValue(
                new Date('2024-01-15T00:00:00Z')
            );
            jest.spyOn(calendarUtils, 'convertServerTimeToUtc').mockReturnValue(
                new Date('2024-01-15T15:30:00Z')
            );
        });

        afterEach(() => {
            jest.restoreAllMocks();
        });

        it('should handle calendar change for DATE field', () => {
            const dateField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [dateField.variable]: new FormControl()
                        }),
                        field: dateField,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            const mockOnChange = jest.fn();
            component.registerOnChange(mockOnChange);

            const selectedDate = new Date('2024-01-15T10:30:00');
            component.onCalendarChange(selectedDate);

            expect(calendarUtils.extractDateComponents).toHaveBeenCalledWith(selectedDate);
            expect(calendarUtils.createUtcDateAtMidnight).toHaveBeenCalledWith(2024, 0, 15);
            expect(mockOnChange).toHaveBeenCalled();
        });

        it('should handle calendar change for TIME field', () => {
            const timeField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.TIME };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [timeField.variable]: new FormControl()
                        }),
                        field: timeField,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            const mockOnChange = jest.fn();
            component.registerOnChange(mockOnChange);

            const selectedDate = new Date('2024-01-15T10:30:00');
            component.onCalendarChange(selectedDate);

            expect(calendarUtils.extractDateComponents).toHaveBeenCalledWith(selectedDate);
            expect(calendarUtils.convertServerTimeToUtc).toHaveBeenCalled();
            expect(mockOnChange).toHaveBeenCalled();
        });

        it('should handle null calendar change', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.component;
            const mockOnChange = jest.fn();
            component.registerOnChange(mockOnChange);

            component.onCalendarChange(null);

            expect(mockOnChange).toHaveBeenCalledWith(null);
        });
    });

    describe('Accessibility', () => {
        it('should set correct aria-label from field name', () => {
            const fieldWithName = { ...DATE_FIELD_MOCK, name: 'Event Date' };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithName.variable]: new FormControl()
                        }),
                        field: fieldWithName,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendarInput = spectator.query(
                byTestId(`calendar-input-${fieldWithName.variable}`)
            );
            expect(calendarInput).toHaveAttribute('aria-label', 'Event Date');
        });

        it('should set aria-describedby when field has hint', () => {
            const fieldWithHint = {
                ...DATE_FIELD_MOCK,
                hint: 'Select a date',
                variable: 'testField'
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithHint.variable]: new FormControl()
                        }),
                        field: fieldWithHint,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendarInput = spectator.query(byTestId('calendar-input-testField'));
            expect(calendarInput).toHaveAttribute('aria-describedby', 'hint-testField');
        });

        it('should NOT set aria-describedby when field has no hint', () => {
            const fieldWithoutHint = { ...DATE_FIELD_MOCK, hint: undefined };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [formControlName]="field.variable" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutHint.variable]: new FormControl()
                        }),
                        field: fieldWithoutHint,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE
                    }
                }
            );
            spectator.detectChanges();

            const calendarInput = spectator.query(
                byTestId(`calendar-input-${fieldWithoutHint.variable}`)
            );
            expect(calendarInput).not.toHaveAttribute('aria-describedby');
        });
    });
});
