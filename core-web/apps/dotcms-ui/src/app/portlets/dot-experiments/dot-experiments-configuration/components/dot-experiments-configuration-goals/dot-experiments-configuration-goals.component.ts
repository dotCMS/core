import {
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    OnDestroy,
    ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import {
    ExperimentSteps,
    Goals,
    GOALS_METADATA_MAP,
    Status,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotExperimentsConfigurationGoalSelectComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goal-select/dot-experiments-configuration-goal-select.component';
import { Observable, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { DotIconModule } from '@dotcms/ui';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';

/**
 * Assign goal to experiment
 */
@Component({
    selector: 'dot-experiments-configuration-goals',
    standalone: true,
    imports: [
        CommonModule,

        DotMessagePipeModule,
        DotDynamicDirective,
        DotIconModule,
        // PrimeNg
        ButtonModule,
        CardModule
    ],
    templateUrl: './dot-experiments-configuration-goals.component.html',
    styleUrls: ['./dot-experiments-configuration-goals.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationGoalsComponent implements OnDestroy {
    vm$: Observable<{ goals: Goals; status: StepStatus }> =
        this.dotExperimentsConfigurationStore.goalsStepVm$.pipe(
            tap(({ status }) => this.loadSidebarComponent(status))
        );

    goalTypeMap = GOALS_METADATA_MAP;
    destroy$: Subject<boolean> = new Subject<boolean>();
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    private componentRef: ComponentRef<DotExperimentsConfigurationGoalSelectComponent>;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Open the sidebar to select the principal goal
     * @returns void
     * @memberof DotExperimentsConfigurationGoalsComponent
     */
    openSelectGoalSidebar() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.GOAL);
    }

    private listenOutputsSidebar(): void {
        this.componentRef.instance.closedSidebar.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.dotExperimentsConfigurationStore.closeSidebar();
            this.sidebarHost.viewContainerRef.clear();
        });
    }

    private loadSidebarComponent(status: StepStatus): void {
        if (status && status.isOpen && status.status != Status.SAVING) {
            this.sidebarHost.viewContainerRef.clear();
            this.componentRef =
                this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationGoalSelectComponent>(
                    DotExperimentsConfigurationGoalSelectComponent
                );

            this.listenOutputsSidebar();
        }
    }
}
