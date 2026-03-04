import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentVariantDetail } from '@dotcms/dotcms-models';
import { DotDynamicDirective, DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsExperimentSummaryComponent } from './components/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsReportDailyDetailsComponent } from './components/dot-experiments-report-daily-details/dot-experiments-report-daily-details.component';
import { DotExperimentsReportsChartComponent } from './components/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsReportsSkeletonComponent } from './components/dot-experiments-reports-skeleton/dot-experiments-reports-skeleton.component';
import {
    DotExperimentsReportsStore,
    VmReportExperiment
} from './store/dot-experiments-reports-store';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    selector: 'dot-experiments-reports',
    imports: [
        AsyncPipe,
        DotExperimentsUiHeaderComponent,
        DotExperimentsExperimentSummaryComponent,
        DotExperimentsReportsSkeletonComponent,
        DotExperimentsReportsChartComponent,
        DotDynamicDirective,
        DotMessagePipe,
        DotExperimentsReportDailyDetailsComponent,
        TagModule,
        ButtonModule,
        ConfirmPopupModule,
        TabsModule
    ],
    templateUrl: './dot-experiments-reports.component.html',
    providers: [DotExperimentsReportsStore],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col w-full h-full'
    }
})
export class DotExperimentsReportsComponent implements OnInit {
    private readonly store = inject(DotExperimentsReportsStore);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);

    vm$: Observable<VmReportExperiment> = this.store.vm$;
    readonly axisLabelsProbabilityChart: { xAxisLabel: string; yAxisLabel: string } = {
        xAxisLabel: this.dotMessageService.get('experiments.chart.xAxisLabel'),
        yAxisLabel: this.dotMessageService.get('experiments.chart.yAxisLabel')
    };
    readonly axisLabelsBayesianChart: { xAxisLabel: string; yAxisLabel: string } = {
        xAxisLabel: this.dotMessageService.get('experiments.chart.xAxisLabel.bayesian'),
        yAxisLabel: this.dotMessageService.get('experiments.chart.yAxisLabel.bayesian')
    };

    ngOnInit(): void {
        this.loadExperimentsResults();
    }

    /**
     * Go to Experiment List
     * @param {string} pageId
     * @returns void
     * @memberof DotExperimentsReportsComponent
     */
    goToExperimentList(pageId: string) {
        this.router.navigate(['/edit-page/experiments/', pageId], {
            queryParams: {
                mode: null,
                variantName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge'
        });
    }

    /**
     * Promote Variant
     * @param {MouseEvent} $event
     * @param experimentId
     * @param variant
     * @returns void
     * @memberof DotExperimentsReportsComponent
     */
    promoteVariant($event: MouseEvent, experimentId: string, variant: DotExperimentVariantDetail) {
        this.confirmationService.confirm({
            target: $event.target,
            message: this.dotMessageService.get('experiment.reports.promote.warning'),
            icon: 'pi pi-info-circle',
            acceptLabel: this.dotMessageService.get('Yes'),
            rejectLabel: this.dotMessageService.get('No'),
            accept: () => {
                this.store.promoteVariant({ experimentId, variant });
            }
        });
    }

    /**
     * Load Experiments Resutls.
     * @returns void
     * @memberof DotExperimentsReportsComponent
     */
    loadExperimentsResults() {
        this.store.loadExperimentAndResults(this.route.snapshot.params['experimentId']);
    }
}
