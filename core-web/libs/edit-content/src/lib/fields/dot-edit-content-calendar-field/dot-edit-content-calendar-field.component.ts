import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { ControlContainer, FormControl, ReactiveFormsModule } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';

import { SystemTimezone } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { CALENDAR_FIELD_TYPES_WITH_TIME } from '@dotcms/edit-content/models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '@dotcms/edit-content/models/dot-edit-content-field.enum';

import { CALENDAR_OPTIONS_PER_TYPE } from './utils';

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
 * - Timezone support
 * - Configurable field types
 * - Accessibility support
 * - Form integration
 * - Hint display
 *
 * @example
 * ```html
 * <dot-edit-content-calendar-field
 *   [field]="field"
 *   [utcTimezone]="timezone"
 * />
 * ```
 */
@Component({
    selector: 'dot-edit-content-calendar-field',
    standalone: true,
    imports: [CalendarModule, ReactiveFormsModule],
    templateUrl: 'dot-edit-content-calendar-field.component.html',
    styleUrls: ['./dot-edit-content-calendar-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentCalendarFieldComponent {
    private readonly controlContainer = inject(ControlContainer);

    /**
     * The field configuration (required).
     * Determines the type of calendar field (date, time, datetime).
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * The system timezone (optional).
     * Used to adjust the display and storage of date/time values.
     * Alias: utcTimezone
     */
    $systemTimezone = input<SystemTimezone | null>(null, { alias: 'utcTimezone' });

    /**
     * The configuration for the field type.
     * Computed based on the field type.
     */
    $fieldTypeConfig = computed(() => {
        const fieldType = this.$field().fieldType;

        return CALENDAR_OPTIONS_PER_TYPE[fieldType];
    });

    /**
     * Whether to show the system timezone label.
     * Only shown for fields that include time.
     */
    $showSystemTimezoneLabel = computed(() => {
        const fieldType = this.$field().fieldType as FIELD_TYPES;

        return CALENDAR_FIELD_TYPES_WITH_TIME.includes(fieldType);
    });

    /**
     * The current form control for this field
     */
    readonly $control = computed(() => {
        const field = this.$field();

        return this.controlContainer.control?.get(field.variable) as FormControl;
    });

    /**
     * The current value of the calendar field
     */
    readonly $currentValue = computed(() => {
        const control = this.$control();

        return control?.value;
    });

    /**
     * The default date to display in the calendar.
     * Adjusted based on the system timezone.
     */
    readonly $defaultDate = computed(() => {
        const timezone = this.$systemTimezone();
        if (!timezone) return null;

        return this.getServerAdjustedDate();
    });

    constructor() {
        // Adjust timezone for existing values
        effect(() => {
            const control = this.$control();
            const timezone = this.$systemTimezone();
            const value = this.$currentValue();

            if (control && timezone && value) {
                const date = new Date(value);
                const adjustedDate = this.adjustDateToServerTimezone(date);
                control.setValue(adjustedDate, { emitEvent: false });
            }
        });
    }

    /**
     * Adjusts a date from local timezone to server timezone
     * @param localDate The date in local timezone
     * @returns Date adjusted to server timezone
     */
    private adjustDateToServerTimezone(localDate: Date): Date {
        const timezone = this.$systemTimezone();

        if (!timezone) return localDate;

        // Get the local timezone offset in milliseconds
        const localOffset = localDate.getTimezoneOffset() * 60 * 1000;

        // Get server offset in milliseconds
        const serverOffset = Number(timezone.offset);

        // Calculate the difference between server and local
        const offsetDiff = serverOffset + localOffset;

        // Apply the offset difference to get the server time
        const serverTimestamp = localDate.getTime() + offsetDiff;

        return new Date(serverTimestamp);
    }

    /**
     * Gets the current date adjusted to server timezone
     * @returns Date object adjusted to server timezone
     * @private
     */
    private getServerAdjustedDate(): Date {
        return this.adjustDateToServerTimezone(new Date());
    }
}
