import { SpectatorHost, byTestId, createHostFactory, mockProvider } from '@openng/spectator/vitest';
import { describe, vi } from 'vitest';

import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DatePicker } from 'primeng/datepicker';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotSystemTimezone
} from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotCalendarFieldComponent } from './components/calendar-field/calendar-field.component';
import * as calendarUtils from './components/calendar-field/calendar-field.util';
import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field.component';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { DotEditContentStore } from '../../store/edit-content.store';
import { CONTENT_TYPE_MOCK, DATE_FIELD_MOCK } from '../../utils/mocks';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props — assigned by Spectator through hostProps, never in a constructor.
    formGroup!: FormGroup;
    field!: DotCMSContentTypeField;
    contentlet!: DotCMSContentlet;
    utcTimezone: DotSystemTimezone | null = null;
    contentType!: DotCMSContentType;
}

const submitAttempted = signal(false);

describe('DotEditContentCalendarFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentCalendarFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentCalendarFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule, TooltipModule],
        detectChanges: false,
        providers: [
            { provide: DotEditContentStore, useValue: { hasAttemptedSubmit: submitAttempted } },
            mockProvider(DotMessageService, {
                get: vi.fn().mockReturnValue('Never expires')
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
        // Absent, not null: `expireDateVar` is optional on DotCMSContentType, and the
        // component compares it with `===` against a string either way.
        expireDateVar: undefined
    };

    // The 'Calendar field timezone information' suite that lived here tested the timezone line
    // under the input. FR-009 removes that line entirely — the timezone now renders inside the
    // picker footer, so its coverage moved to calendar-field.component.spec.ts ('Picker footer —
    // timezone'). What remains here is the guarantee that nothing renders under the input.
    describe('Calendar field timezone placement', () => {
        it.each([FIELD_TYPES.DATE_AND_TIME, FIELD_TYPES.TIME, FIELD_TYPES.DATE])(
            'should NOT render a timezone line under the input for a %s field',
            (fieldType) => {
                const field = { ...DATE_FIELD_MOCK, fieldType };

                spectator = createHost(
                    `<form [formGroup]="formGroup">
                        <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                    </form>`,
                    {
                        hostProps: {
                            formGroup: new FormGroup({
                                [field.variable]: new FormControl()
                            }),
                            field,
                            utcTimezone: MOCK_TIMEZONE,
                            contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                            contentlet: createFakeContentlet({ [field.variable]: null })
                        }
                    }
                );
                spectator.detectChanges();

                expect(spectator.query(byTestId('calendar-field-timezone'))).not.toExist();
            }
        );
    });

    describe('Calendar field hint', () => {
        it('should NOT show hint when field has no hint property', () => {
            const fieldWithoutHint = { ...DATE_FIELD_MOCK, hint: undefined };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutHint.variable]: new FormControl()
                        }),
                        field: fieldWithoutHint,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [fieldWithoutHint.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const hintElement = spectator.query(byTestId(`hint-${fieldWithoutHint.variable}`));
            expect(hintElement).not.toExist();
        });

        // T022 — FR-009. The timezone moves into the picker, so the field footer goes back to
        // carrying the hint like every other field type does.
        it('should render the hint under the input, and no timezone line, when a timezone is present', () => {
            const fieldWithHint = {
                ...DATE_FIELD_MOCK,
                fieldType: FIELD_TYPES.DATE_AND_TIME,
                hint: 'Pick the go-live date'
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithHint.variable]: new FormControl()
                        }),
                        field: fieldWithHint,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [fieldWithHint.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const hintElement = spectator.query(byTestId(`hint-${fieldWithHint.variable}`));
            expect(hintElement).toExist();
            expect(hintElement).toContainText(fieldWithHint.hint);

            expect(spectator.query(byTestId('calendar-field-timezone'))).not.toExist();
        });

        // The required error must not evict the hint: the hint explains what to enter, which is
        // exactly what the author needs while the field is in error. Error first, hint below it.
        it('should show the required error AND keep the hint, error first', () => {
            const fieldWithHint = {
                ...DATE_FIELD_MOCK,
                fieldType: FIELD_TYPES.DATE_AND_TIME,
                required: true,
                hint: 'Pick the go-live date'
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            // A real validator, not a forced flag: $hasError now reads the control's
                            // actual validity, which is the point of the #37464 gate.
                            [fieldWithHint.variable]: new FormControl(null, Validators.required)
                        }),
                        field: fieldWithHint,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [fieldWithHint.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            // $hasError is computed from the store's submit flag and the control's validity;
            // it is no longer settable, which is the point of the #37464 gate.
            submitAttempted.set(true);
            spectator.detectChanges();

            const error = spectator.query('.p-field-error');
            const hint = spectator.query(byTestId(`hint-${fieldWithHint.variable}`));

            expect(error).toExist();
            expect(hint).toExist();
            expect(hint).toContainText(fieldWithHint.hint);

            // Order matters: the error is the new information, the hint is the standing guidance.
            expect(
                (error as Node).compareDocumentPosition(hint as Node) &
                    Node.DOCUMENT_POSITION_FOLLOWING
            ).toBeTruthy();
        });

        // T023 — FR-009. The label tooltip existed only to make room for the timezone line in
        // the footer. With the timezone gone, so is the reason.
        it('should NOT route the hint into the label tooltip when a timezone is present', () => {
            const fieldWithHint = {
                ...DATE_FIELD_MOCK,
                fieldType: FIELD_TYPES.DATE_AND_TIME,
                hint: 'Pick the go-live date'
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithHint.variable]: new FormControl()
                        }),
                        field: fieldWithHint,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [fieldWithHint.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.query(Tooltip)).not.toExist();
        });
    });

    describe('Expire date field behavior', () => {
        it('should show placeholder and showClear when field is expire date field', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITH_EXPIRE,
                        contentlet: createFakeContentlet({
                            [DATE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(DatePicker);
            expect(calendar?.showClear).toBe(true);

            expect(calendar?.placeholder).toBe('Never expires');
        });

        it('should NOT show the placeholder, but still allow clearing, when field is NOT expire date field', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [DATE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(DatePicker);
            // showClear is now unconditional (FR-005): every field type can be emptied, not
            // just the expire-date one. The placeholder stays exclusive to the expire date.
            expect(calendar?.showClear).toBe(true);
            expect(calendar?.placeholder).toBe('');
        });

        // T011 — FR-007b. The expire-date field is the one field that could already be cleared,
        // so making the clear control unconditional must not leave it with two of them.
        it('should render exactly ONE clear control on the expire date field holding a value', async () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl(
                                new Date(2026, 3, 9, 9, 33).getTime()
                            )
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: null,
                        contentType: CONTENT_TYPE_WITH_EXPIRE,
                        contentlet: createFakeContentlet({
                            [DATE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();
            await Promise.resolve();
            spectator.detectChanges();

            expect(spectator.queryAll('[data-testid="calendar-clear-button"]')).toHaveLength(1);
            expect(spectator.query(DatePicker)?.placeholder).toBe('Never expires');
        });
    });

    describe('Disabled state', () => {
        it('should disable calendar when setDisabledState is called with true', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [DATE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const formGroup = spectator.hostComponent.formGroup;
            const control = formGroup.get(DATE_FIELD_MOCK.variable) as FormControl;
            control.disable();
            spectator.detectChanges();

            // Calendar-field uses internalFormControl; PrimeNG DatePicker binds to it.
            // Assert on the calendar-field's internalFormControl since DatePicker.disabled may be a signal.
            const calendarField = spectator.query(DotCalendarFieldComponent);
            expect(calendarField?.internalFormControl.disabled).toBe(true);
        });

        it('should enable calendar when setDisabledState is called with false', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [DATE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const formGroup = spectator.hostComponent.formGroup;
            const control = formGroup.get(DATE_FIELD_MOCK.variable) as FormControl;
            control.enable();
            spectator.detectChanges();

            const calendarField = spectator.query(DotCalendarFieldComponent);
            expect(calendarField?.internalFormControl.disabled).toBe(false);
        });
    });

    describe('Field type configurations', () => {
        it('should configure DATE_AND_TIME field correctly', () => {
            const dateTimeField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE_AND_TIME };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [dateTimeField.variable]: new FormControl()
                        }),
                        field: dateTimeField,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [dateTimeField.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(DatePicker);
            expect(calendar?.showTime).toBe(true);
            expect(calendar?.timeOnly).toBe(false);
            expect(calendar?.icon).toBe('pi pi-calendar');
        });

        it('should configure DATE field correctly', () => {
            const dateField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [dateField.variable]: new FormControl()
                        }),
                        field: dateField,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [dateField.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(DatePicker);
            expect(calendar?.showTime).toBe(false);
            expect(calendar?.timeOnly).toBe(false);
            expect(calendar?.icon).toBe('pi pi-calendar');
        });

        it('should configure TIME field correctly', () => {
            const timeField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.TIME };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [timeField.variable]: new FormControl()
                        }),
                        field: timeField,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [timeField.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const calendar = spectator.query(DatePicker);
            expect(calendar?.showTime).toBe(true);
            expect(calendar?.timeOnly).toBe(true);
            expect(calendar?.icon).toBe('pi pi-clock');
        });
    });

    describe('Picker presentation (issue #36156)', () => {
        const buildHost = (fieldType: FIELD_TYPES) => {
            const field = { ...DATE_FIELD_MOCK, fieldType };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.variable]: new FormControl()
                        }),
                        field,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [field.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();
        };

        const FIELD_TYPES_UNDER_TEST = [
            ['Date', FIELD_TYPES.DATE],
            ['Date/Time', FIELD_TYPES.DATE_AND_TIME],
            ['Time', FIELD_TYPES.TIME]
        ] as const;

        it.each(FIELD_TYPES_UNDER_TEST)(
            'should keep the %s picker open on selection (closes only on click-outside)',
            (_label, fieldType) => {
                buildHost(fieldType);

                const calendar = spectator.query(DatePicker);
                expect(calendar?.hideOnDateTimeSelect).toBe(false);
            }
        );

        it.each(FIELD_TYPES_UNDER_TEST)(
            'should size the %s control from its stylesheet, not from utility classes',
            (_label, fieldType) => {
                buildHost(fieldType);

                // The control fills its column (#37465 FR-001) through the component's own
                // stylesheet, so no width utility class or inputStyleClass should appear here.
                const calendar = spectator.query(DatePicker);
                expect(calendar?.inputStyleClass).toBeFalsy();

                const datepickerEl = spectator.query('p-datepicker');
                expect(datepickerEl?.classList.contains('w-full')).toBe(false);
            }
        );
    });

    describe('Default value handling', () => {
        beforeEach(() => {
            // Mock utility functions
            vi.spyOn(calendarUtils, 'processFieldDefaultValue').mockReturnValue(null);
            vi.spyOn(calendarUtils, 'processExistingValue').mockReturnValue(null);
            vi.spyOn(calendarUtils, 'getCurrentServerTime').mockReturnValue(
                new Date('2024-01-15T10:30:00Z')
            );
        });

        afterEach(() => {
            vi.restoreAllMocks();
        });

        it('should process field default value when field has defaultValue', () => {
            const mockDefaultResult = {
                displayValue: new Date('2024-01-15T10:30:00Z'),
                formValue: new Date('2024-01-15T15:30:00Z')
            };

            vi.spyOn(calendarUtils, 'processFieldDefaultValue').mockReturnValue(mockDefaultResult);

            const fieldWithDefault = { ...DATE_FIELD_MOCK, defaultValue: 'now' };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithDefault.variable]: new FormControl()
                        }),
                        field: fieldWithDefault,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        // No inode: content being created, which is the only state where a
                        // default applies (FR-017). On a saved contentlet an empty field is a
                        // value the author chose, and the default must not overwrite it.
                        contentlet: { [fieldWithDefault.variable]: null } as DotCMSContentlet
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.hostComponent.formGroup;
            const control = component.get(fieldWithDefault.variable) as FormControl;
            control.setValue(null);

            expect(calendarUtils.processFieldDefaultValue).toHaveBeenCalledWith(
                fieldWithDefault,
                MOCK_TIMEZONE
            );
        });

        it('should NOT process default value when field has no defaultValue', () => {
            const fieldWithoutDefault = { ...DATE_FIELD_MOCK, defaultValue: undefined };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutDefault.variable]: new FormControl()
                        }),
                        field: fieldWithoutDefault,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: { [fieldWithoutDefault.variable]: null } as DotCMSContentlet
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.hostComponent.formGroup;
            const control = component.get(fieldWithoutDefault.variable) as FormControl;
            control.setValue(null);

            expect(calendarUtils.processFieldDefaultValue).toHaveBeenCalledWith(
                fieldWithoutDefault,
                MOCK_TIMEZONE
            );
        });

        it('should process existing value when writeValue is called', () => {
            const existingValue = new Date('2024-01-10T14:20:00Z').getTime(); // Convert to timestamp
            const mockProcessedValue = new Date('2024-01-10T09:20:00Z');

            vi.spyOn(calendarUtils, 'processExistingValue').mockReturnValue(mockProcessedValue);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [DATE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.hostComponent.formGroup;
            const control = component.get(DATE_FIELD_MOCK.variable) as FormControl;
            control.setValue(existingValue);

            spectator.detectChanges();

            expect(calendarUtils.processExistingValue).toHaveBeenCalledWith(
                existingValue,
                DATE_FIELD_MOCK.fieldType as FIELD_TYPES,
                MOCK_TIMEZONE
            );
        });

        it('should handle timezone reprocessing when timezone becomes available', () => {
            const existingValue = new Date('2024-01-10T14:20:00Z').getTime(); // Convert to timestamp
            const mockProcessedValue = new Date('2024-01-10T09:20:00Z');

            vi.spyOn(calendarUtils, 'processExistingValue').mockReturnValue(mockProcessedValue);

            // Start without timezone
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [DATE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: DATE_FIELD_MOCK,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [DATE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const component = spectator.hostComponent.formGroup;
            const control = component.get(DATE_FIELD_MOCK.variable) as FormControl;
            control.setValue(existingValue);
            spectator.detectChanges();

            expect(calendarUtils.processExistingValue).toHaveBeenCalledWith(
                existingValue,
                DATE_FIELD_MOCK.fieldType as FIELD_TYPES,
                MOCK_TIMEZONE
            );
        });
    });

    describe('Calendar value changes', () => {
        beforeEach(() => {
            // Mock utility functions
            vi.spyOn(calendarUtils, 'extractDateComponents').mockReturnValue({
                year: 2024,
                month: 0,
                date: 15,
                hours: 10,
                minutes: 30,
                seconds: 0
            });
            vi.spyOn(calendarUtils, 'createUtcDateAtMidnight').mockReturnValue(
                new Date('2024-01-15T00:00:00Z')
            );
            vi.spyOn(calendarUtils, 'convertServerTimeToUtc').mockReturnValue(
                new Date('2024-01-15T15:30:00Z')
            );
        });

        afterEach(() => {
            vi.restoreAllMocks();
        });

        it('should handle calendar change for DATE field', () => {
            const dateField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.DATE };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [dateField.variable]: new FormControl()
                        }),
                        field: dateField,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [dateField.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const selectedDate = new Date('2024-01-15T10:30:00');
            spectator.triggerEventHandler(DatePicker, 'onSelect', selectedDate);

            expect(calendarUtils.extractDateComponents).toHaveBeenCalledWith(selectedDate);
            expect(calendarUtils.createUtcDateAtMidnight).toHaveBeenCalledWith(2024, 0, 15);
        });

        it('should handle calendar change for TIME field', () => {
            const timeField = { ...DATE_FIELD_MOCK, fieldType: FIELD_TYPES.TIME };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [timeField.variable]: new FormControl()
                        }),
                        field: timeField,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [timeField.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const selectedDate = new Date('2024-01-15T10:30:00');
            spectator.triggerEventHandler(DatePicker, 'onSelect', selectedDate);

            expect(calendarUtils.extractDateComponents).toHaveBeenCalledWith(selectedDate);
            expect(calendarUtils.convertServerTimeToUtc).toHaveBeenCalled();
        });
    });

    describe('Clearing the field', () => {
        // Seeded timestamp representing an existing, persisted value.
        const EXISTING_TIMESTAMP = 1701380400000;

        // Base field mock WITHOUT a defaultValue so clearing does not re-push a value
        // through handleChangeValue when the control transitions to null.
        const fieldWithoutDefault = { ...DATE_FIELD_MOCK, defaultValue: undefined };

        // The `(onClearClick)` tests that lived here drove the event synthetically with
        // triggerEventHandler. PrimeNG emits onClearClick from exactly one place —
        // onClearButtonClick (primeng-datepicker.mjs:3129) — reachable only from the stock footer
        // Clear button this feature removes, or from the buttonbar template's clearCallback, which
        // we deliberately do not use. The on-field X goes through clear(), which emits onClear
        // only (:1746). So they asserted a path production can no longer take; the onClear test
        // below covers the real one.

        it('should clear the parent form value via onClear (X icon path) for an expire date field', () => {
            const field = { ...fieldWithoutDefault, fieldType: FIELD_TYPES.DATE_AND_TIME };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.variable]: new FormControl(EXISTING_TIMESTAMP)
                        }),
                        field,
                        utcTimezone: MOCK_TIMEZONE,
                        contentType: CONTENT_TYPE_WITH_EXPIRE,
                        contentlet: createFakeContentlet({
                            [field.variable]: EXISTING_TIMESTAMP
                        })
                    }
                }
            );
            spectator.detectChanges();

            const formGroup = spectator.hostComponent.formGroup;
            expect(formGroup.get(field.variable)?.value).toBe(EXISTING_TIMESTAMP);

            spectator.triggerEventHandler(DatePicker, 'onClear', {});
            spectator.detectChanges();

            expect(formGroup.get(field.variable)?.value).toBeNull();
        });
    });

    describe('Accessibility', () => {
        it('should set correct aria-label from field name', () => {
            const fieldWithName = { ...DATE_FIELD_MOCK, name: 'Event Date' };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithName.variable]: new FormControl()
                        }),
                        field: fieldWithName,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [fieldWithName.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            // The host's aria-label is not what a screen reader reads — the <input> is the
            // focusable element, and PrimeNG does not forward aria-label to it. The name now
            // reaches the input through the ariaLabelledBy id list.
            const input = spectator.query(`#${fieldWithName.variable}`);
            const ids = (input?.getAttribute('aria-labelledby') ?? '').split(' ').filter(Boolean);
            const name = ids
                .map((id) => document.getElementById(id)?.textContent?.trim() ?? '')
                .join(' ');

            expect(name).toContain('Event Date');
        });

        it('should NOT set aria-describedby when field has no hint', () => {
            const fieldWithoutHint = { ...DATE_FIELD_MOCK, hint: undefined };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-calendar-field [field]="field" [contentlet]="contentlet" [utcTimezone]="utcTimezone" [contentType]="contentType" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutHint.variable]: new FormControl()
                        }),
                        field: fieldWithoutHint,
                        utcTimezone: undefined,
                        contentType: CONTENT_TYPE_WITHOUT_EXPIRE,
                        contentlet: createFakeContentlet({
                            [fieldWithoutHint.variable]: null
                        })
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
