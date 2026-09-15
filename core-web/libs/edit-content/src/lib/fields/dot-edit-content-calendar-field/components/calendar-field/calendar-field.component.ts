import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    forwardRef,
    input,
    viewChild
} from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DatePicker, DatePickerModule } from 'primeng/datepicker';

import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotSystemTimezone
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    CALENDAR_OPTIONS_PER_TYPE,
    convertServerTimeToUtc,
    createUtcDateAtMidnight,
    extractDateComponents,
    getCurrentServerTime,
    processExistingValue,
    processFieldDefaultValue
} from './calendar-field.util';

import { CALENDAR_FIELD_TYPES_WITH_TIME } from '../../../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../../../models/dot-edit-content-field.enum';
import { FieldType } from '../../../../models/dot-edit-content-field.type';
import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';

/**
 * DotEditContentCalendarFieldComponent
 *
 * A reusable form field component that handles date, time, and datetime inputs.
 * Supports different calendar types:
 * - Calendar Field (date)
 * - Time Field (time)
 * - Datetime Field (date and time)
 *
 * Features:
 * - User-centric timezone handling
 * - Configurable field types
 * - Accessibility support
 * - Form integration
 * - Clear timezone information display
 * - Default value support (field.defaultValue):
 *   - "now": Uses current server time
 *   - Fixed date: Parses and uses server timezone
 *   - Empty: No default value
 */
@Component({
    selector: 'dot-calendar-field',
    imports: [ButtonModule, DatePickerModule, ReactiveFormsModule, DotMessagePipe],
    templateUrl: 'calendar-field.component.html',
    styleUrls: ['./calendar-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotCalendarFieldComponent),
            multi: true
        }
    ]
})
export class DotCalendarFieldComponent extends BaseControlValueAccessor<number | null> {
    /**
     * The field configuration (required).
     * Determines the type of calendar field (date, time, datetime).
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Whether the field has an error.
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });

    /**
     * The system timezone (optional).
     * Used to display server timezone information to the user.
     * Alias: utcTimezone
     */
    $systemTimezone = input<DotSystemTimezone | null>(null, { alias: 'utcTimezone' });

    /**
     * The content type (optional).
     * Used to determine if the field is a date or time field.
     * Alias: contentType
     */
    $contentType = input<DotCMSContentType | null>(null, { alias: 'contentType' });

    // Store last value to reprocess when timezone becomes available
    private lastUtcValue: number | null = null;

    /**
     * The internal form control for the calendar field.
     */
    internalFormControl = new FormControl<Date | null>(null);

    constructor() {
        super();
        // Reprocess existing values when timezone becomes available
        effect(() => {
            const systemTimezone = this.$systemTimezone();
            const fieldType = this.$field().fieldType as FieldType;

            // If timezone is now available and we have a stored value, reprocess it
            if (systemTimezone && this.lastUtcValue !== null) {
                const displayValue = processExistingValue(
                    this.lastUtcValue,
                    fieldType as FieldType,
                    systemTimezone
                );
                this.internalFormControl.setValue(displayValue);
            }
        });

        // PrimeNG decides whether to render the clear control by reading the input element's DOM
        // value, and `updateInputfield()` writes that value without marking the component dirty.
        // The DatePicker is OnPush, so on the load path — a value arriving from a saved
        // contentlet — the condition is evaluated before the value lands and nothing ever
        // re-evaluates it: the field shows a value the author has no way to clear until they
        // happen to focus it. The microtask lets PrimeNG's writeValue finish first.
        effect(() => {
            this.$value();
            queueMicrotask(() => this.$picker()?.cd.detectChanges());
        });

        this.handleDisabledChange(this.$isDisabled);
        this.handleChangeValue(this.$value);
    }

    /**
     * The configuration for the field type.
     * Computed based on the field type.
     */
    $fieldTypeConfig = computed(() => {
        const fieldType = this.$field().fieldType;
        return CALENDAR_OPTIONS_PER_TYPE[fieldType];
    });

    /**
     * Whether the picker footer states the timezone the value is interpreted in.
     *
     * Only the two types that carry a time. A date is the same calendar day in every zone, so
     * naming one beside a Date-only picker tells the author nothing — decided explicitly in the
     * issue's refinement table. Absent too when the timezone has not loaded, so the footer never
     * renders an empty slot.
     */
    $showFooterTimezone = computed(() => {
        const fieldType = this.$field().fieldType as FIELD_TYPES;

        return (
            CALENDAR_FIELD_TYPES_WITH_TIME.includes(fieldType) && !!this.$systemTimezone()?.label
        );
    });

    /**
     * The picker instance: the footer action dismisses the overlay through it, and the
     * constructor's effect drives its change detection when a value arrives (see there).
     * Optional rather than required — the effect can run before the view exists.
     */
    $picker = viewChild(DatePicker);

    /**
     * Message key for the footer action: a time-only field jumps to "now", the two that carry
     * a date jump to "today".
     */
    $footerActionLabel = computed(() =>
        this.$fieldTypeConfig().timeOnly
            ? 'edit.content.form.field.calendar.now'
            : 'edit.content.form.field.calendar.today'
    );

    $isExpireDateField = computed(() => {
        const contentType = this.$contentType();
        const field = this.$field();
        return contentType?.expireDateVar === field.variable;
    });

    /**
     * Computed property for the default date when calendar opens (navigation only)
     * Shows current server time without affecting the form value
     */
    $defaultDate = computed(() => {
        return getCurrentServerTime(this.$systemTimezone());
    });

    /**
     * Handles calendar value changes from user selection
     * Converts the selected date appropriately based on field type
     */
    onCalendarChange(selectedDate: Date | null): void {
        if (!selectedDate) {
            this.internalFormControl.setValue(null);
            this.onChange(null);
            this.onTouched();
            return;
        }

        const systemTimezone = this.$systemTimezone();
        const fieldType = this.$field().fieldType;

        // Extract date/time components from user selection
        const { year, month, date, hours, minutes, seconds } = extractDateComponents(selectedDate);

        // Create display value (what user sees in the input)
        const displayValue = new Date(year, month, date, hours, minutes, seconds);

        // Create form value based on field type
        let formValue: Date;

        if (fieldType === FIELD_TYPES.DATE) {
            // For date-only fields: UTC midnight represents "the date" globally
            formValue = createUtcDateAtMidnight(year, month, date);
        } else if (fieldType === FIELD_TYPES.TIME) {
            // For time-only fields: preserve time components but use consistent date base (today)
            // This ensures time is stored consistently regardless of date
            const today = new Date();
            const timeInServerTz = new Date(
                today.getFullYear(),
                today.getMonth(),
                today.getDate(),
                hours,
                minutes,
                seconds
            );
            formValue = convertServerTimeToUtc(timeInServerTz, systemTimezone);
        } else {
            // For datetime fields: convert server timezone selection to UTC for storage
            formValue = convertServerTimeToUtc(displayValue, systemTimezone);
        }

        // Update internal display value (what user sees)
        this.internalFormControl.setValue(displayValue);

        // Send numeric timestamp to the parent form control for consistent storage
        this.onChange(formValue.getTime());
        this.onTouched();
    }

    /**
     * Fills the field with the current moment, as the SERVER sees it.
     *
     * Deliberately does not use the `todayCallback` PrimeNG hands to the button-bar template:
     * that callback reads `new Date()`, the browser's clock, which on a server in another
     * timezone resolves to the wrong calendar day. Routing through `onCalendarChange` also
     * means the per-type UTC conversion stays in one place rather than being duplicated here.
     */
    setCurrentServerDateTime(): void {
        this.onCalendarChange(getCurrentServerTime(this.$systemTimezone()));

        // A date-only pick is complete, so the picker closes as it does for a day click. The
        // types carrying a time stay open so the hour can still be adjusted.
        if (!this.$fieldTypeConfig().showTime) {
            this.$picker()?.hideOverlay();
        }
    }

    readonly handleDisabledChange = signalMethod<boolean>((isDisabled) => {
        if (isDisabled) {
            this.internalFormControl.disable();
        } else {
            this.internalFormControl.enable();
        }
    });

    readonly handleChangeValue = signalMethod<number | null>((utcValue) => {
        // Store the value for reprocessing when timezone is available
        this.lastUtcValue = utcValue;

        if (utcValue !== null && utcValue !== undefined) {
            // Process existing value from form/backend (numeric timestamp)
            const displayValue = processExistingValue(
                utcValue,
                this.$field().fieldType as FieldType,
                this.$systemTimezone()
            );

            this.internalFormControl.setValue(displayValue);
        } else {
            // Process default value for new/empty field (this is NOT a UTC value, it's literal)
            const defaultResult = processFieldDefaultValue(this.$field(), this.$systemTimezone());

            if (defaultResult) {
                // Use displayValue directly - no conversion needed for default values
                this.internalFormControl.setValue(defaultResult.displayValue);

                // Store pending default value to apply when onChange is registered
                this.onChange(defaultResult.formValue.getTime());
            } else {
                this.internalFormControl.setValue(null);
            }
        }
    });
}
