import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotExperimentsService } from '@dotcms/data-access';

import { DotExperimentsStore } from './store/dot-experiments.store';

@Component({
    selector: 'dot-experiments-shell',
    imports: [RouterModule, ToastModule],
    providers: [MessageService, DotExperimentsStore, DotExperimentsService],
    templateUrl: 'dot-experiments-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'w-full min-h-full flex bg-[var(--color-white)]'
    }
})
export class DotExperimentsShellComponent {}
