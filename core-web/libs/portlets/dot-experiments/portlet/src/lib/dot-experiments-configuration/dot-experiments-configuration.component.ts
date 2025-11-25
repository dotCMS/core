import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';

import {
    ComponentStatus,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatus
} from '@dotcms/dotcms-models';
import { DotAddToBundleComponent, DotRemoveConfirmPopupWithEscapeDirective } from '@dotcms/ui';

import { DotExperimentsConfigurationGoalsComponent } from './components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationSchedulingComponent } from './components/dot-experiments-configuration-scheduling/dot-experiments-configuration-scheduling.component';
import { DotExperimentsConfigurationSkeletonComponent } from './components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsConfigurationTrafficComponent } from './components/dot-experiments-configuration-traffic/dot-experiments-configuration-traffic.component';
import { DotExperimentsConfigurationVariantsComponent } from './components/dot-experiments-configuration-variants/dot-experiments-configuration-variants.component';
import {
    ConfigurationViewModel,
    DotExperimentsConfigurationStore
} from './store/dot-experiments-configuration-store';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotExperimentsInlineEditTextComponent } from '../shared/ui/dot-experiments-inline-edit-text/dot-experiments-inline-edit-text.component';

@Component({
    standalone: true,
    imports: [
        AsyncPipe,
        NgIf,
        DotExperimentsUiHeaderComponent,
        DotExperimentsConfigurationVariantsComponent,
        DotExperimentsConfigurationGoalsComponent,
        DotExperimentsConfigurationTrafficComponent,
        DotExperimentsConfigurationSchedulingComponent,
        DotExperimentsConfigurationSkeletonComponent,
        DotExperimentsInlineEditTextComponent,
        DotAddToBundleComponent,
        DotRemoveConfirmPopupWithEscapeDirective,
        CardModule,
        ButtonModule,
        InplaceModule,
        InputTextModule,
        MenuModule,
        ConfirmDialogModule,
        ConfirmPopupModule
    ],
    selector: 'dot-experiments-configuration',
    templateUrl: './dot-experiments-configuration.component.html',
    styleUrls: ['./dot-experiments-configuration.component.scss'],
    providers: [DotExperimentsConfigurationStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent implements OnInit {
    vm$: Observable<ConfigurationViewModel> = this.dotExperimentsConfigurationStore.vm$;
    experimentStatus = DotExperimentStatus;
    confirmDialogKey = CONFIGURATION_CONFIRM_DIALOG_KEY;
    protected readonly ComponentStatus = ComponentStatus;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly router: Router,
        private readonly route: ActivatedRoute
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
