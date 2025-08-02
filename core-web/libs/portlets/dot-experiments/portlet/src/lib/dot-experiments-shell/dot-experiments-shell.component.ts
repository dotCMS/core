import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotExperimentsService } from '@dotcms/data-access';

import { DotExperimentsStore } from './store/dot-experiments.store';

@Component({
    selector: 'dot-experiments-shell',
    imports: [RouterModule, ToastModule],
    providers: [MessageService, DotExperimentsStore, DotExperimentsService],
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent implements OnInit {
    private router = inject(Router);
    private activatedRoute = inject(ActivatedRoute);

    ngOnInit() {
        this.removeVariantQueryParams();
    }

    private removeVariantQueryParams() {
        const { mode, variantName, experimentId } = this.activatedRoute.snapshot.queryParams;

        if (mode || variantName || experimentId) {
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
}
