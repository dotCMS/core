import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { RippleModule } from 'primeng/ripple';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotExperiment, DotExperimentStatusList } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsConfigurationGoalsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationSchedulingComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-scheduling/dot-experiments-configuration-scheduling.component';
import { DotExperimentsConfigurationSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsConfigurationTrafficComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-traffic/dot-experiments-configuration-traffic.component';
import { DotExperimentsConfigurationVariantsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants/dot-experiments-configuration-variants.component';
import {
    ConfigurationViewModel,
    DotExperimentsConfigurationStore
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsExperimentSummaryComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    standalone: true,
    imports: [
        AsyncPipe,
        NgIf,
        DotMessagePipeModule,
        DotExperimentsUiHeaderComponent,
        DotExperimentsExperimentSummaryComponent,
        DotExperimentsConfigurationVariantsComponent,
        DotExperimentsConfigurationGoalsComponent,
        DotExperimentsConfigurationTrafficComponent,
        DotExperimentsConfigurationSchedulingComponent,
        DotExperimentsConfigurationSkeletonComponent,
        CardModule,
        ButtonModule,
        RippleModule
    ],
    selector: 'dot-experiments-configuration',
    templateUrl: './dot-experiments-configuration.component.html',
    styleUrls: ['./dot-experiments-configuration.component.scss'],
    providers: [DotExperimentsConfigurationStore, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent implements OnInit {
    vm$: Observable<ConfigurationViewModel> = this.dotExperimentsConfigurationStore.vm$;
    experimentStatus = DotExperimentStatusList;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly router: Router,
        private readonly route: ActivatedRoute,
        private readonly confirmationService: ConfirmationService,
        private readonly dotMessagePipe: DotMessagePipe
    ) {}

    ngOnInit(): void {
        this.dotExperimentsConfigurationStore.loadExperiment(
            this.route.snapshot.params.experimentId
        );
    }

    /**
     * Go to Experiment List
     * @param {string} pageId
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
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
     * Run the Experiment
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    runExperiment(experiment: DotExperiment) {
        this.dotExperimentsConfigurationStore.startExperiment(experiment);
    }

    /**
     * Stop the Experiment
     * @param {MouseEvent} $event
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    stopExperiment($event: MouseEvent, experiment: DotExperiment) {
        this.confirmationService.confirm({
            target: $event.target,
            message: this.dotMessagePipe.transform('experiments.action.stop.delete-confirm'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessagePipe.transform('stop'),
            rejectLabel: this.dotMessagePipe.transform('dot.common.dialog.reject'),
            accept: () => {
                this.dotExperimentsConfigurationStore.stopExperiment(experiment);
            }
        });
    }

    /**
     * Stop the Schedule Experiment and set the status to Draft
     * @param {MouseEvent} $event
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    stopScheduleExperiment($event: MouseEvent, experiment: DotExperiment) {
        this.confirmationService.confirm({
            target: $event.target,
            message: this.dotMessagePipe.transform('experiments.action.stop.schedule-confirm'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessagePipe.transform('dot.common.dialog.accept'),
            rejectLabel: this.dotMessagePipe.transform('dot.common.dialog.reject'),
            accept: () => {
                this.dotExperimentsConfigurationStore.cancelSchedule(experiment);
            }
        });
    }
}
