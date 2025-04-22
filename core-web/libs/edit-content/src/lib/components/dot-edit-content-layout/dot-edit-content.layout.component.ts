import { ChangeDetectionStrategy, Component, inject, model, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import {
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import { DotEditContentFormComponent } from '../dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../dot-edit-content-sidebar/dot-edit-content-sidebar.component';

/**
 * Component that displays the edit content layout.
 *
 * @export
 * @class EditContentLayoutComponent
 */
@Component({
    selector: 'dot-edit-content-form-layout',
    standalone: true,
    imports: [
        DotMessagePipe,
        ButtonModule,
        ToastModule,
        MessagesModule,
        RouterLink,
        DotEditContentFormComponent,
        DotEditContentSidebarComponent,
        ConfirmDialogModule
    ],
    providers: [
        DotWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotEditContentService,
        DotWorkflowService,
        DotEditContentStore,
        DialogService
    ],

    host: {
        '[class.edit-content--with-sidebar]': '$store.isSidebarOpen()'
    },
    templateUrl: './dot-edit-content.layout.component.html',
    styleUrls: ['./dot-edit-content.layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentLayoutComponent {
    /**
     * The store instance.
     *
     * @type {InstanceType<typeof DotEditContentStore>}
     * @memberof EditContentLayoutComponent
     */
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);

    /**
     * Whether the beta message should be shown.
     *
     * @type {boolean}
     * @memberof EditContentLayoutComponent
     */
    readonly $showBetaMessage = signal(true);

    /**
     * Whether the select workflow dialog should be shown.
     *
     * @type {boolean}
     * @memberof EditContentLayoutComponent
     */
    readonly $showDialog = model<boolean>(false);

    /**
     * Emits an event to show the select workflow dialog.
     *
     * @memberof EditContentLayoutComponent
     */
    selectWorkflow() {
        this.$showDialog.set(true);
    }

    /**
     * Handles the form change event.
     *
     * @param {Record<string, string>} value
     * @memberof EditContentLayoutComponent
     */
    onFormChange(value: FormValues) {
        this.$store.onFormChange(value);
    }

    /**
     * Closes the beta message.
     *
     * @memberof EditContentLayoutComponent
     */
    closeBetaMessage() {
        this.$showBetaMessage.set(false);
    }
}
