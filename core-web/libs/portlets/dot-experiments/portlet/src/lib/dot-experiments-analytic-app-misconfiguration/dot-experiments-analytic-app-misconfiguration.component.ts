import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotEmptyContainerComponent, DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    selector: 'dot-experiments-analytic-app-misconfiguration',
    standalone: true,
    imports: [
        CommonModule,
        DotExperimentsUiHeaderComponent,
        DotMessagePipe,
        ButtonModule,
        DotEmptyContainerComponent
    ],
    templateUrl: './dot-experiments-analytic-app-misconfiguration.component.html',
    styleUrls: ['./dot-experiments-analytic-app-misconfiguration.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsAnalyticAppMisconfigurationComponent {
    private router = inject(Router);

    goToBrowserBack() {
        this.router.navigate(['edit-page/content'], { queryParamsHandling: 'merge' });
    }

    goToAnalyticsApp() {
        this.router.navigate(['/apps/dotAnalytics-config']);
    }
}
