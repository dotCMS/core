import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    standalone: true,
    selector: 'dot-experiments-shell',
    imports: [
        RouterModule,
        DotLoadingIndicatorModule,
        DotExperimentsUiHeaderComponent,
        // PrimeNg
        ToastModule
    ],
    providers: [MessageService],
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent implements OnInit {
    constructor(private readonly router: Router) {}

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
