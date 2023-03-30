import { merge, Observable } from 'rxjs';

import { AsyncPipe, LowerCasePipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotResultSimpleVariant } from '@dotcms/dotcms-models';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotExperimentsConfigurationSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsPublishVariantComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-publish-variant/dot-experiments-publish-variant.component';
import { DotExperimentsReportsChartComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsReportsSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-skeleton/dot-experiments-reports-skeleton.component';
import {
    DotExperimentsReportsStore,
    VmReportExperiment
} from '@portlets/dot-experiments/dot-experiments-reports/store/dot-experiments-reports-store';
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
        //dotCMS
        DotExperimentsUiHeaderComponent,
        DotPipesModule,
        DotExperimentsConfigurationSkeletonComponent,
        DotExperimentsExperimentSummaryComponent,
        DotExperimentsReportsSkeletonComponent,
        DotExperimentsReportsChartComponent,
        DotExperimentsPublishVariantComponent,
        DotDynamicDirective,
        //PrimeNg
        TagModule,
        ButtonModule
    ],
    templateUrl: './dot-experiments-reports.component.html',
    styleUrls: ['./dot-experiments-reports.component.scss'],
    providers: [DotExperimentsReportsStore, DotMessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsComponent implements OnInit {
    vm$: Observable<VmReportExperiment> = this.store.vm$;

    @ViewChild(DotDynamicDirective, { static: true }) host!: DotDynamicDirective;

    constructor(
        private readonly store: DotExperimentsReportsStore,
        private readonly router: Router,
        private readonly route: ActivatedRoute,

        private dotMessageService: DotMessageService
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

    /**
     * Load modal to publish the selected variant
     * @param {DotResultSimpleVariant[]} variants
     * @returns void
     * @memberof DotExperimentsReportsComponent
     *
     */
    loadPublishVariant(variants: DotResultSimpleVariant[]): void {
        const viewContainerRef = this.host.viewContainerRef;
        viewContainerRef.clear();
        const componentRef =
            viewContainerRef.createComponent<DotExperimentsPublishVariantComponent>(
                DotExperimentsPublishVariantComponent
            );

        componentRef.instance.data = variants;

        merge(componentRef.instance.hide, componentRef.instance.publish)
            .pipe(take(1))
            .subscribe((variant: string) => {
                if (variant) {
                    this.store.promoteVariant(variant);
                }

                viewContainerRef.clear();
            });
    }
}
