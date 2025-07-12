import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, FormControl, ReactiveFormsModule } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';

import { SystemTimezone } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { CALENDAR_FIELD_TYPES_WITH_TIME } from '@dotcms/edit-content/models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '@dotcms/edit-content/models/dot-edit-content-field.enum';

import {
    CALENDAR_OPTIONS_PER_TYPE
} from './dot-edit-content-calendar-field.util';

/**
 * Calendar field component that handles date, time, and datetime inputs with timezone support.
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
    #controlContainer = inject(ControlContainer);

    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $systemTimezone = input<SystemTimezone | null>(null, { alias: 'utcTimezone' });

    $fieldTypeConfig = computed(() => {
        const fieldType = this.$field().fieldType as FIELD_TYPES;
        return CALENDAR_OPTIONS_PER_TYPE[fieldType] || CALENDAR_OPTIONS_PER_TYPE[FIELD_TYPES.DATE];
    });

    $showSystemTimezoneLabel = computed(() => {
        const fieldType = this.$field().fieldType as FIELD_TYPES;
        return CALENDAR_FIELD_TYPES_WITH_TIME.includes(fieldType);
    });

    $control = computed(() => {
        const controlContainer = this.#controlContainer;
        if (!controlContainer || !controlContainer.control) {
            return null;
        }
        return controlContainer.control.get(this.$field().variable) as FormControl;
    });

    $defaultDate = computed(() => {
        const timezone = this.$systemTimezone();

        if (!timezone) {
            return new Date();
        }

        const now = new Date();
        const serverOffsetMs = Number(timezone.offset);
        const utcTimestamp = now.getTime();
        const serverTimestamp = utcTimestamp + serverOffsetMs;
        const serverDateTime = new Date(serverTimestamp);

        const serverYear = serverDateTime.getUTCFullYear();
        const serverMonth = serverDateTime.getUTCMonth();
        const serverDay = serverDateTime.getUTCDate();
        const serverHours = serverDateTime.getUTCHours();
        const serverMinutes = serverDateTime.getUTCMinutes();
        const serverSeconds = serverDateTime.getUTCSeconds();

        return new Date(serverYear, serverMonth, serverDay, serverHours, serverMinutes, serverSeconds);
    });
}
