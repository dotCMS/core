import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
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
export class DotExperimentsAnalyticAppMisconfigurationComponent implements OnInit {
    emptyConfiguration: PrincipalConfiguration;
    pageTitle: string;

    constructor(
        private router: Router,
        private location: Location,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit(): void {
        const { healthStatus } = this.location.getState() as {
            healthStatus: HealthStatusTypes;
        };

        if (healthStatus === HealthStatusTypes.NOT_CONFIGURED) {
            this.setConfiguration(
                'experiments.analytics-app-no-configured.title',
                'experiments.analytics-app-no-configured.subtitle'
            );
            this.pageTitle = 'experiments.analytics-app-no-configured.title';
        }

        if (healthStatus === HealthStatusTypes.CONFIGURATION_ERROR) {
            this.setConfiguration(
                'experiments.analytics-app-misconfiguration.title',
                'experiments.analytics-app-misconfiguration.subtitle'
            );
            this.pageTitle = 'experiments.analytics-app-misconfiguration.title';
        }
    }

    goToBrowserBack() {
        this.router.navigate(['edit-page/content'], { queryParamsHandling: 'merge' });
    }

    private setConfiguration(title, subtitle): void {
        this.emptyConfiguration = {
            title: this.dotMessageService.get(title),
            subtitle: this.dotMessageService.get(subtitle),
            icon: 'pi-chart-line'
        };
    }
}
