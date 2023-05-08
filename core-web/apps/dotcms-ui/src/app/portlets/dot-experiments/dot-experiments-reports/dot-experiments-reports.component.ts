import { Observable } from 'rxjs';

import {
    AsyncPipe,
    LowerCasePipe,
    NgClass,
    NgIf,
    PercentPipe,
    TitleCasePipe
} from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    inject,
    OnInit,
    ViewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TagModule } from 'primeng/tag';

import { tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_NAME } from '@dotcms/dotcms-models';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotExperimentsPublishVariantComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-publish-variant/dot-experiments-publish-variant.component';
import { DotExperimentsReportsChartComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsReportsSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-skeleton/dot-experiments-reports-skeleton.component';
import {
    DotExperimentsReportsStore,
    VmReportExperiment
} from '@portlets/dot-experiments/dot-experiments-reports/store/dot-experiments-reports-store';
import { DotExperimentsDetailsTableComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsExperimentSummaryComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

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
        DotExperimentsExperimentSummaryComponent,
        DotExperimentsReportsSkeletonComponent,
        DotExperimentsReportsChartComponent,
        DotExperimentsDetailsTableComponent,
        DotExperimentsPublishVariantComponent,
        DotDynamicDirective,
        //PrimeNg
        TagModule,
        ButtonModule,
        RippleModule,
        TitleCasePipe
    ],
    templateUrl: './dot-experiments-reports.component.html',
    styleUrls: ['./dot-experiments-reports.component.scss'],
    providers: [DotExperimentsReportsStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsComponent implements OnInit {
    vm$: Observable<VmReportExperiment> = this.store.vm$.pipe(
        tap(({ showDialog }) => this.handlePromoteDialog(showDialog))
    );
    dotMessageService = inject(DotMessageService);
    readonly chartConfig: { xAxisLabel: string; yAxisLabel: string; title: string } = {
        xAxisLabel: this.dotMessageService.get('experiments.chart.xAxisLabel'),
        yAxisLabel: this.dotMessageService.get('experiments.chart.yAxisLabel'),
        title: this.dotMessageService.get('experiments.reports.chart.title')
    };

    @ViewChild(DotDynamicDirective, { static: true }) dialogHost!: DotDynamicDirective;
    protected readonly defaultVariantName = DEFAULT_VARIANT_NAME;
    private componentRef: ComponentRef<DotExperimentsPublishVariantComponent>;

    constructor(
        private readonly store: DotExperimentsReportsStore,
        private readonly router: Router,
        private readonly route: ActivatedRoute
    ) {}

    ngOnInit(): void {
        this.store.loadExperimentAndResults(this.route.snapshot.params.experimentId);
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
     * Open publish variant dialog
     *
     * @returns void
     * @memberof DotExperimentsReportsComponent
     *
     */
    openPublishVariantDialog() {
        this.store.showPromoteDialog();
    }

    private handlePromoteDialog(showDialog: boolean) {
        if (showDialog) {
            this.loadPromoteVariantComponent();
        } else {
            this.removeSidebarComponent();
        }
    }

    private removeSidebarComponent() {
        if (this.componentRef) {
            this.dialogHost.viewContainerRef.clear();
        }
    }

    private loadPromoteVariantComponent(): void {
        this.dialogHost.viewContainerRef.clear();
        this.componentRef =
            this.dialogHost.viewContainerRef.createComponent<DotExperimentsPublishVariantComponent>(
                DotExperimentsPublishVariantComponent
            );
    }
}
