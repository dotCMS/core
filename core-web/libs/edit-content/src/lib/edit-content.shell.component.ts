import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

@Component({
    selector: 'dot-edit-content',
    imports: [RouterModule, ToastModule],
    providers: [MessageService],
    template: '<p-toast /> <router-outlet />',
    styleUrls: ['./edit-content.shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditContentShellComponent {}
