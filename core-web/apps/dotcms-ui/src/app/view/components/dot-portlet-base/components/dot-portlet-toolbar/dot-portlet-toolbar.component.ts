import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { DotPortletToolbarActions } from '@shared/models/dot-portlet-toolbar.model/dot-portlet-toolbar-actions.model';
import { UntypedFormGroup } from '@angular/forms';

@Component({
    selector: 'dot-portlet-toolbar',
    templateUrl: './dot-portlet-toolbar.component.html',
    styleUrls: ['./dot-portlet-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPortletToolbarComponent {
    @Input() title: string;

    @Input() cancelButtonLabel: string;

    @Input() actionsButtonLabel: string;

    @Input() actions: DotPortletToolbarActions;

    @Input()
    form: UntypedFormGroup;

    @Output() saveAndPublish: EventEmitter<Event> = new EventEmitter();

    /**
     * Handle cancel button click
     *
     * @param {MouseEvent} $event
     * @memberof DotPortletToolbarComponent
     */
    onCancelClick($event: MouseEvent): void {
        try {
            this.actions.cancel($event);
        } catch (error) {
            console.error(error);
        }
    }

    /**
     * Handle primary button click
     *
     * @param {MouseEvent} $event
     * @memberof DotPortletToolbarComponent
     */
    onPrimaryClick($event: MouseEvent): void {
        try {
            this.actions.primary[0].command($event);
        } catch (error) {
            console.error(error);
        }
    }

    onSaveAndPublish(): void {
        this.saveAndPublish.emit(this.form.value);
    }
}
