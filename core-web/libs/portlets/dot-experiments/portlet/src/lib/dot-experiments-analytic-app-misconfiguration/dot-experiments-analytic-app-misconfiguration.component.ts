import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    selector: 'dot-experiments-analytic-app-misconfiguration',
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
    private router = inject(Router);
    private location = inject(Location);
    private dotMessageService = inject(DotMessageService);

    emptyConfiguration: PrincipalConfiguration;
    pageTitle: string;

    ngOnInit(): void {
        const location = this.location.getState() as {
            healthStatus: HealthStatusTypes;
        };

        /**
         * With the new UVE changes, probably we enter to this component without the location state
         * so we need to check if the location state is not present and set the default configuration
         *
         * Probably needs a recheck later. This is a hotfix for realease
         */
        const healthStatus = location?.healthStatus || HealthStatusTypes.NOT_CONFIGURED;

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
