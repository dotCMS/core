import { Observable } from 'rxjs';

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotExperiment, DotExperimentStatusList } from '@dotcms/dotcms-models';
import {
    ConfigurationViewModel,
    DotExperimentsConfigurationStore
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';

@Component({
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
                editPageTab: null,
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
}
