import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import { DotWorkflowActionsFireService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentStore } from './store/edit-content.store';

import { DotEditContentAsideComponent } from '../../components/dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotWorkflowActionParams } from '../../models/dot-edit-content.model';
import { DotEditContentService } from '../../services/dot-edit-content.service';

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
        DotEditContentAsideComponent,
        ConfirmDialogModule
    ],
    providers: [
        DotWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotEditContentService,
        MessageService,
        DotEditContentStore
    ],

    host: {
        '[class.edit-content--with-sidebar]': '$store.showSidebar()'
    },
    templateUrl: './edit-content.layout.component.html',
    styleUrls: ['./edit-content.layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditContentLayoutComponent {
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);

    formValue = signal<Record<string, string>>({});

    /**
     * Set the form value to be saved.
     *
     * @param {Record<string, string>} formValue - An object containing the key-value pairs of the contentlet to be saved.
     * @memberof EditContentLayoutComponent
     */
    setFormValue(formValue: Record<string, string>) {
        this.formValue.set(formValue);
    }

    /**
     * Fire the workflow action.
     *
     * @param {DotCMSWorkflowAction} action
     * @memberof EditContentLayoutComponent
     */
    fireWorkflowAction({ actionId, inode, contentType }: DotWorkflowActionParams): void {
        this.$store.fireWorkflowAction({
            actionId,
            inode,
            data: {
                contentlet: {
                    ...this.formValue(),
                    contentType
                }
            }
        });
    }
}
