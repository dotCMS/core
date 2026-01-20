import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import {
    ComponentStatus,
    ExperimentSteps,
    GOAL_TYPES,
    Goals,
    GOALS_METADATA_MAP,
    GoalsLevels,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotDynamicDirective, DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsDetailsTableComponent } from '../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationGoalSelectComponent } from '../dot-experiments-configuration-goal-select/dot-experiments-configuration-goal-select.component';

/**
 * Assign goal to experiment
 */
@Component({
    selector: 'dot-experiments-configuration-goals',
    imports: [
        CommonModule,
        DotMessagePipe,
        DotDynamicDirective,
        DotExperimentsDetailsTableComponent,
        ButtonModule,
        CardModule,
        TooltipModule,
        ConfirmPopupModule
    ],
    templateUrl: './dot-experiments-configuration-goals.component.html',
    providers: [DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationGoalsComponent {
    private readonly dotExperimentsConfigurationStore = inject(DotExperimentsConfigurationStore);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessagePipe = inject(DotMessagePipe);

    vm$: Observable<{
        experimentId: string;
        goals: Goals | null;
        status: StepStatus;
        isExperimentADraft: boolean;
        disabledTooltipLabel: null | string;
    }> = this.dotExperimentsConfigurationStore.goalsStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );

    destroy$: Subject<boolean> = new Subject<boolean>();
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    protected readonly GOALS_METADATA_MAP = GOALS_METADATA_MAP;
    protected readonly GOAL_TYPES = GOAL_TYPES;

    private componentRef: ComponentRef<DotExperimentsConfigurationGoalSelectComponent>;

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
