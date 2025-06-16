import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';

import { ContentTypeCalendarField } from '@dotcms/dotcms-models';

import { CALENDAR_OPTIONS_PER_TYPE } from './utils';

/**
 * Calendar field component for the content edit form.
 * Renders a PrimeNG calendar component with appropriate options based on the field type.
 * This component maintains its parent form context through ControlContainer injection.
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
    /**
     * Required input that contains the calendar field configuration.
     * Determines the behavior and display options of the calendar.
     */
    $field = input.required<ContentTypeCalendarField>({ alias: 'field' });

    /**
     * Computed property that determines the calendar configuration options
     * based on the field type (date, time, or date-time).
     * @returns The appropriate calendar options for the field type
     */
    $calendarOptions = computed(() => {
        const field = this.$field();

        return CALENDAR_OPTIONS_PER_TYPE[field.fieldType];
    });
}
