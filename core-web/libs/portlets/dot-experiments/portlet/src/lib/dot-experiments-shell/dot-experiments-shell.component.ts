import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotExperimentsService } from '@dotcms/data-access';

import { DotExperimentsStore } from './store/dot-experiments.store';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    standalone: true,
    selector: 'dot-experiments-shell',
    imports: [RouterModule, DotExperimentsUiHeaderComponent, ToastModule],
    providers: [MessageService, DotExperimentsStore, DotExperimentsService],
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent implements OnInit {
    private store = inject(DotExperimentsStore);
    private router = inject(Router);

    ngOnInit() {
        this.removeVariantQueryParams();
    }

    private removeVariantQueryParams() {
        this.router.navigate([], {
            queryParams: {
                mode: null,
                variantName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge',
            replaceUrl: true
        });
    }
}
