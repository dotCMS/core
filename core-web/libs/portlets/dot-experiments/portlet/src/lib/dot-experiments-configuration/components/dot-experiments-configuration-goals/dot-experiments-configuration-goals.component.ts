import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    ExperimentSteps,
    GOAL_TYPES,
    Goals,
    GOALS_METADATA_MAP,
    GoalsLevels,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

import { DotExperimentsDetailsTableComponent } from '../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationGoalSelectComponent } from '../dot-experiments-configuration-goal-select/dot-experiments-configuration-goal-select.component';

/**
 * Assign goal to experiment
 */
@Component({
    selector: 'dot-experiments-configuration-goals',
    standalone: true,
    imports: [
        CommonModule,

        DotMessagePipe,
        DotDynamicDirective,
        DotIconModule,
        UiDotIconButtonTooltipModule,
        UiDotIconButtonModule,
        DotExperimentsDetailsTableComponent,
        // PrimeNg
        ButtonModule,
        CardModule,
        TooltipModule,
        ConfirmPopupModule
    ],
    templateUrl: './dot-experiments-configuration-goals.component.html',
    styleUrls: ['./dot-experiments-configuration-goals.component.scss'],
    providers: [DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationGoalsComponent {
    vm$: Observable<{
        experimentId: string;
        goals: Goals | null;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.dotExperimentsConfigurationStore.goalsStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );

    destroy$: Subject<boolean> = new Subject<boolean>();
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    protected readonly GOALS_METADATA_MAP = GOALS_METADATA_MAP;
    protected readonly GOAL_TYPES = GOAL_TYPES;

    private componentRef: ComponentRef<DotExperimentsConfigurationGoalSelectComponent>;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotMessageService: DotMessageService,
        private readonly confirmationService: ConfirmationService,
        private readonly dotMessagePipe: DotMessagePipe
    ) {}

    /**
     * Open the sidebar to select the principal goal
     * @returns void
     * @memberof DotExperimentsConfigurationGoalsComponent
     */
    openSelectGoalSidebar() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.GOAL);
    }

    /**
     * Show confirmation dialog to allow the user confirmation to delete a goal
     * @param {Event} event
     * @param {GoalsLevels} goalLevel
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationGoalsComponent
     */
    deleteGoal(event: Event, goalLevel: GoalsLevels, experimentId: string) {
        this.confirmationService.confirm({
            target: event.target,
            message: this.dotMessagePipe.transform(
                'experiments.configure.action.delete.confirm-question'
            ),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessagePipe.transform('delete'),
            rejectLabel: this.dotMessagePipe.transform('dot.common.dialog.reject'),
            accept: () =>
                this.dotExperimentsConfigurationStore.deleteGoal({ goalLevel, experimentId })
        });
    }

    private handleSidebar(status: StepStatus) {
        if (status && status.isOpen) {
            this.loadSidebarComponent(status);
        } else {
            this.removeSidebarComponent();
        }
    }

    private loadSidebarComponent(status: StepStatus): void {
        if (status && status.isOpen && status.status != ComponentStatus.SAVING) {
            this.sidebarHost.viewContainerRef.clear();
            this.componentRef =
                this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationGoalSelectComponent>(
                    DotExperimentsConfigurationGoalSelectComponent
                );
        }
    }

    private removeSidebarComponent() {
        if (this.componentRef) {
            this.sidebarHost.viewContainerRef.clear();
        }
    }
}
