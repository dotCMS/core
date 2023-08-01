import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    selector: 'dot-experiments-analytic-app-misconfiguration',
    standalone: true,
    imports: [
        DotExperimentsUiHeaderComponent,
        DotMessagePipe,
        DotEmptyContainerComponent,
        ButtonModule
    ],
    templateUrl: './dot-experiments-analytic-app-misconfiguration.component.html',
    styleUrls: ['./dot-experiments-analytic-app-misconfiguration.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsAnalyticAppMisconfigurationComponent {
    private router = inject(Router);
    private dotMessageService: DotMessageService = inject(DotMessageService);

    emptyConfiguration: PrincipalConfiguration = {
        title: this.dotMessageService.get('experiments.analytics-app-misconfiguration.title'),
        subtitle: this.dotMessageService.get('experiments.analytics-app-misconfiguration.subtitle'),
        icon: 'pi-chart-line'
    };

    goToBrowserBack() {
        this.router.navigate(['edit-page/content'], { queryParamsHandling: 'merge' });
    }

    goToAnalyticsApp() {
        this.router.navigate(['/apps/dotAnalytics-config']);
    }
}
