import { Component, Input, OnInit } from '@angular/core';
import { DotPortletToolbarActions } from '@shared/models/dot-portlet-toolbar.model/dot-portlet-toolbar-actions.model';

@Component({
    selector: 'dot-portlet-toolbar',
    templateUrl: './dot-portlet-toolbar.component.html',
    styleUrls: ['./dot-portlet-toolbar.component.scss']
})
export class DotPortletToolbarComponent implements OnInit {
    @Input() title: string;

    @Input() cancelButtonLabel: string;

    @Input() actionsButtonLabel: string;

    @Input() actions: DotPortletToolbarActions;

    constructor() {}

    ngOnInit(): void {}

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
}
