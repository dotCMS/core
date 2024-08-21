import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { CALENDAR_OPTIONS_PER_TYPE } from './utils';

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
    @Input() field!: DotCMSContentTypeField;

    readonly calendarOptions = CALENDAR_OPTIONS_PER_TYPE;
}
