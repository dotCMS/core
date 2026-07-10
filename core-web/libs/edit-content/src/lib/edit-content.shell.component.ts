import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from './fields/shared/image-editor-launcher';
import { EDIT_CONTENT_HOST } from './services/host/edit-content-host.model';
import { RouterEditContentHost } from './services/host/router-edit-content-host';

@Component({
    selector: 'dot-edit-content',
    imports: [RouterModule, ToastModule],
    providers: [
        MessageService,
        // Scope the new Angular image editor to the edit-content shell so it only
        // activates here and never leaks into the legacy/web-component path. DialogService
        // is required by AngularImageEditorLauncher to open the modal.
        DialogService,
        { provide: IMAGE_EDITOR_LAUNCHER, useClass: AngularImageEditorLauncher },
        // Full-screen presentation: the editor navigates via the router and owns
        // the document title / shell breadcrumb. Inherited by the layout and its
        // store. The dialog path provides OverlayEditContentHost instead.
        { provide: EDIT_CONTENT_HOST, useClass: RouterEditContentHost }
    ],
    template: '<p-toast /> <router-outlet />',
    styleUrls: ['./edit-content.shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditContentShellComponent {}
