import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import { DotWorkflowActionsFireService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentStore } from './store/edit-content.store';

import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../../components/dot-edit-content-sidebar/dot-edit-content-sidebar.component';
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
        DotEditContentSidebarComponent,
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
}
