import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPortletToolbarActions } from '../../../../../shared/models/dot-portlet-toolbar.model/dot-portlet-toolbar-actions.model';

@Component({
    selector: 'dot-portlet-toolbar',
    templateUrl: './dot-portlet-toolbar.component.html',
    styleUrls: ['./dot-portlet-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ToolbarModule, ButtonModule, MenuModule, DotMessagePipe]
})
export class DotPortletToolbarComponent {
    @Input() title: string;

    @Input() cancelButtonLabel: string;

    @Input() actionsButtonLabel: string;

    @Input() actions: DotPortletToolbarActions;

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
    onPrimaryClick($event: Event): void {
        try {
            this.actions.primary[0].command({ originalEvent: $event });
        } catch (error) {
            console.error(error);
        }
    }
}
