import { Observable } from 'rxjs';

import {
    AsyncPipe,
    LowerCasePipe,
    NgClass,
    NgIf,
    PercentPipe,
    TitleCasePipe
} from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentVariantDailyDetail } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

import { DotExperimentsReportBayesianDetailComponent } from './components/dot-experiments-report-bayesian-detail/dot-experiments-report-bayesian-detail.component';
import { DotExperimentsReportDailyDetailsComponent } from './components/dot-experiments-report-daily-details/dot-experiments-report-daily-details.component';
import { DotExperimentsReportsChartComponent } from './components/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsReportsSkeletonComponent } from './components/dot-experiments-reports-skeleton/dot-experiments-reports-skeleton.component';
import {
    DotExperimentsReportsStore,
    VmReportExperiment
} from './store/dot-experiments-reports-store';

import { DotExperimentsDetailsTableComponent } from '../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsExperimentSummaryComponent } from '../shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    selector: 'dot-experiments-reports',
    standalone: true,
    imports: [
        AsyncPipe,
        NgIf,
        LowerCasePipe,
        PercentPipe,
        NgClass,
        DotExperimentsUiHeaderComponent,
        DotPipesModule,
        DotExperimentsExperimentSummaryComponent,
        DotExperimentsReportsSkeletonComponent,
        DotExperimentsReportsChartComponent,
        DotExperimentsDetailsTableComponent,
        DotDynamicDirective,
        DotMessagePipe,
        DotExperimentsReportDailyDetailsComponent,
        TagModule,
        ButtonModule,
        TitleCasePipe,
        ConfirmPopupModule,
        TabViewModule,
        DotExperimentsReportBayesianDetailComponent
    ],
    templateUrl: './dot-experiments-reports.component.html',
    styleUrls: ['./dot-experiments-reports.component.scss'],
    providers: [DotExperimentsReportsStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsComponent implements OnInit {
    vm$: Observable<VmReportExperiment> = this.store.vm$;

    private dotMessageService = inject(DotMessageService);

    readonly axisLabelsProbabilityChart: { xAxisLabel: string; yAxisLabel: string } = {
        xAxisLabel: this.dotMessageService.get('experiments.chart.xAxisLabel'),
        yAxisLabel: this.dotMessageService.get('experiments.chart.yAxisLabel')
    };

    readonly axisLabelsBayesianChart: { xAxisLabel: string; yAxisLabel: string } = {
        xAxisLabel: this.dotMessageService.get('experiments.chart.xAxisLabel.bayesian'),
        yAxisLabel: this.dotMessageService.get('experiments.chart.yAxisLabel.bayesian')
    };

    constructor(
        private readonly store: DotExperimentsReportsStore,
        private readonly router: Router,
        private readonly route: ActivatedRoute,
        private readonly confirmationService: ConfirmationService
    ) {}

    ngOnInit(): void {
        this.store.loadExperimentAndResults(this.route.snapshot.params['experimentId']);
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
    promoteVariant(
        $event: MouseEvent,
        experimentId: string,
        variant: DotExperimentVariantDailyDetail
    ) {
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
}
