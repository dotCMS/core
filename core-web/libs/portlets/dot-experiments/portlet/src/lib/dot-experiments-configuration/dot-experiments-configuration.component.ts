import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { RippleModule } from 'primeng/ripple';

import {
    ComponentStatus,
    CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatusList
} from '@dotcms/dotcms-models';
import { DotMessagePipe, DotMessagePipeModule } from '@dotcms/ui';

import { DotExperimentsConfigurationGoalsComponent } from './components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationSchedulingComponent } from './components/dot-experiments-configuration-scheduling/dot-experiments-configuration-scheduling.component';
import { DotExperimentsConfigurationSkeletonComponent } from './components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsConfigurationTrafficComponent } from './components/dot-experiments-configuration-traffic/dot-experiments-configuration-traffic.component';
import { DotExperimentsConfigurationVariantsComponent } from './components/dot-experiments-configuration-variants/dot-experiments-configuration-variants.component';
import {
    ConfigurationViewModel,
    DotExperimentsConfigurationStore
} from './store/dot-experiments-configuration-store';

import { DotExperimentsExperimentSummaryComponent } from '../shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotExperimentsInlineEditTextComponent } from '../shared/ui/dot-experiments-inline-edit-text/dot-experiments-inline-edit-text.component';

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
        DotExperimentsInlineEditTextComponent,
        CardModule,
        ButtonModule,
        RippleModule,
        InplaceModule,
        InputTextModule,
        RippleModule,
        MenuModule,
        ConfirmDialogModule
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
    protected readonly ComponentStatus = ComponentStatus;
    confirmDialogKey = CONFIRM_DIALOG_KEY;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly router: Router,
        private readonly route: ActivatedRoute,
        private readonly confirmationService: ConfirmationService,
        private readonly dotMessagePipe: DotMessagePipe
    ) {}

    ngOnInit(): void {
        this.dotExperimentsConfigurationStore.loadExperiment(
            this.route.snapshot.params['experimentId']
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
     * Save the description of the experiment
     * @param {string} description
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    saveDescriptionAction(description: string, experiment: DotExperiment) {
        this.dotExperimentsConfigurationStore.setDescription({
            data: {
                description
            },
            experiment
        });
    }
}
