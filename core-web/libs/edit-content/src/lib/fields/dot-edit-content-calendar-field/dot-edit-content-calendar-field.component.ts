import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';

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
} from './dot-edit-content-calendar-field.util';

import { CALENDAR_FIELD_TYPES_WITH_TIME } from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FieldType } from '../../models/dot-edit-content-field.type';
import { BaseFieldComponent } from '../shared/base-field.component';

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
    selector: 'dot-edit-content-calendar-field',
    imports: [CalendarModule, FormsModule, DotMessagePipe],
    templateUrl: 'dot-edit-content-calendar-field.component.html',
    styleUrls: ['./dot-edit-content-calendar-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: DotEditContentCalendarFieldComponent,
            multi: true
        }
    ]
})
export class DotEditContentCalendarFieldComponent
    extends BaseFieldComponent
    implements ControlValueAccessor
{
    /**
     * The field configuration (required).
     * Determines the type of calendar field (date, time, datetime).
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

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

    // Internal state for calendar display (always in server timezone)
    $internalValue = signal<Date | null>(null);

    // Internal state for disabled status
    $isDisabled = signal<boolean>(false);

    // Store last value to reprocess when timezone becomes available
    private lastUtcValue: number | null = null;

    // Pending default value to apply when onChange is ready
    private pendingDefaultValue: Date | null = null;

    // ControlValueAccessor callbacks
    private onChange = (_value: Date | null) => {
        // Callback will be set by Angular Forms via registerOnChange
    };
    private onTouched = () => {
        // Callback will be set by Angular Forms via registerOnTouched
    };

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
                this.$internalValue.set(displayValue);
            }
        });
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
     * Whether to show timezone information.
     * Only shown for fields that include time.
     */
    $showTimezoneInfo = computed(() => {
        const fieldType = this.$field().fieldType as FIELD_TYPES; // TODO: Fix fieldType on DotCMSContentTypeField to FieldType instead of string
        return CALENDAR_FIELD_TYPES_WITH_TIME.includes(fieldType);
    });

    $isExpireDateField = computed(() => {
        const contentType = this.$contentType();
        const field = this.$field();
        return contentType?.expireDateVar === field.variable;
    });

    // ControlValueAccessor implementation
    writeValue(utcValue: number | null): void {
        // Store the value for reprocessing when timezone is available
        this.lastUtcValue = utcValue;

        if (utcValue !== null && utcValue !== undefined) {
            // Debug logging for unexpected value types
            if (typeof utcValue !== 'number') {
                console.warn('Calendar field received non-number value:', {
                    value: utcValue,
                    type: typeof utcValue,
                    fieldVariable: this.$field().variable
                });
            }

            // Process existing value from form/backend (numeric timestamp)
            const displayValue = processExistingValue(
                utcValue,
                this.$field().fieldType as FieldType,
                this.$systemTimezone()
            );

            this.$internalValue.set(displayValue);
        } else {
            // Process default value for new/empty field (this is NOT a UTC value, it's literal)
            const defaultResult = processFieldDefaultValue(this.$field(), this.$systemTimezone());

            if (defaultResult) {
                // Use displayValue directly - no conversion needed for default values
                this.$internalValue.set(defaultResult.displayValue);

                // Store pending default value to apply when onChange is registered
                this.pendingDefaultValue = defaultResult.formValue;
            } else {
                this.$internalValue.set(null);
            }
        }
    }

    registerOnChange(fn: (value: Date | null) => void): void {
        this.onChange = fn;

        // Apply pending default value if it exists
        if (this.pendingDefaultValue !== null) {
            const pendingValue = this.pendingDefaultValue;
            setTimeout(() => {
                // Ensure we send a simple Date object, not a TZDate with complex internal structure
                const simpleDateForForm = new Date(pendingValue.getTime());
                this.onChange(simpleDateForForm);
                this.pendingDefaultValue = null;
            }, 0);
        }
    }

    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

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
            this.$internalValue.set(null);
            this.onChange(null);
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
        this.$internalValue.set(displayValue);

        // Send the correct moment to form control
        this.onChange(formValue);
        this.onTouched();
    }
}
