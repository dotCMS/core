import { Observable } from 'rxjs';

import { AsyncPipe, LowerCasePipe, NgClass, NgIf, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { TagModule } from 'primeng/tag';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotExperimentsConfigurationSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsReportsChartComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsReportsSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-skeleton/dot-experiments-reports-skeleton.component';
import {
    DotExperimentsReportsStore,
    VmReportExperiment
} from '@portlets/dot-experiments/dot-experiments-reports/store/dot-experiments-reports-store';
import { DotExperimentsDetailsTableComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsExperimentSummaryComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    selector: 'dot-experiments-reports',
    standalone: true,
    imports: [
        AsyncPipe,
        NgIf,
        LowerCasePipe,
        PercentPipe,
        NgClass,
        //dotCMS
        DotExperimentsUiHeaderComponent,
        DotPipesModule,
        DotExperimentsConfigurationSkeletonComponent,
        DotExperimentsExperimentSummaryComponent,
        DotExperimentsReportsSkeletonComponent,
        DotExperimentsReportsChartComponent,
        DotExperimentsDetailsTableComponent,
        //PrimeNg
        TagModule
    ],
    templateUrl: './dot-experiments-reports.component.html',
    styleUrls: ['./dot-experiments-reports.component.scss'],
    providers: [DotExperimentsReportsStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsComponent implements OnInit {
    vm$: Observable<VmReportExperiment> = this.store.vm$;
    defaultVariantId = DEFAULT_VARIANT_ID;
    detailData = [
        {
            id: DEFAULT_VARIANT_ID,
            variant_name: 'Default Variant',
            traffic_split: 0.33,
            pageviews: 100,
            sessions: 100,
            clicks: 100,
            best_variant: 0.5,
            improvement: 'Baseline',
            is_winner: false,
            better_than_baseline: false
        },
        {
            id: 'variant-1',
            variant_name: 'Variant 1',
            traffic_split: 0.33,
            pageviews: 100,
            sessions: 100,
            clicks: 100,
            best_variant: 0.5,
            improvement: 0.5,
            is_winner: true,
            better_than_baseline: true
        },
        {
            id: 'variant-2',
            variant_name: 'Variant 2',
            traffic_split: 0.33,
            pageviews: 100,
            sessions: 100,
            clicks: 100,
            best_variant: 0.5,
            improvement: 0.5,
            is_winner: false,
            better_than_baseline: false
        }
    ];

    constructor(
        private readonly store: DotExperimentsReportsStore,
        private readonly router: Router,
        private readonly route: ActivatedRoute
    ) {}

    ngOnInit(): void {
        this.store.loadExperiment(this.route.snapshot.params.experimentId);
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
                editPageTab: null,
                variantName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge'
        });
    }
}
