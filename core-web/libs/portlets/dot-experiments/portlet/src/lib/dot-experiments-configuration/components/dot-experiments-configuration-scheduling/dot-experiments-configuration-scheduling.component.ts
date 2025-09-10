import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import {
    ComponentStatus,
    ExperimentSteps,
    RangeOfDateAndTime,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotDynamicDirective, DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationSchedulingAddComponent } from '../dot-experiments-configuration-scheduling-add/dot-experiments-configuration-scheduling-add.component';

@Component({
    selector: 'dot-experiments-configuration-scheduling',
    imports: [
        CommonModule,
        DotDynamicDirective,
        DotMessagePipe,
        DotIconModule,
        // PrimeNg
        CardModule,
        ButtonModule,
        TooltipModule
    ],
    templateUrl: './dot-experiments-configuration-scheduling.component.html',
    styleUrls: ['./dot-experiments-configuration-scheduling.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationSchedulingComponent {
    private readonly dotExperimentsConfigurationStore = inject(DotExperimentsConfigurationStore);

    vm$: Observable<{
        experimentId: string;
        scheduling: RangeOfDateAndTime;
        status: StepStatus;
        isExperimentADraft: boolean;
        disabledTooltipLabel: string | null;
    }> = this.dotExperimentsConfigurationStore.schedulingStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );

    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    private componentRef: ComponentRef<DotExperimentsConfigurationSchedulingAddComponent>;

    /**
     * Open the sidebar to set the Scheduling
     * @returns void
     * @memberof DotExperimentsConfigurationSchedulingComponent
     */
    setupSchedule() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.SCHEDULING);
    }

    private handleSidebar(status: StepStatus) {
        if (status && status.isOpen && status.status != ComponentStatus.SAVING) {
            this.loadSidebarComponent(status);
        } else {
            this.removeSidebarComponent();
        }
    }

    private loadSidebarComponent(status: StepStatus): void {
        if (this.shouldLoadSidebar(status)) {
            this.sidebarHost.viewContainerRef.clear();
            this.componentRef =
                this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationSchedulingAddComponent>(
                    DotExperimentsConfigurationSchedulingAddComponent
                );
        }
    }

    private removeSidebarComponent() {
        if (this.componentRef) {
            this.sidebarHost.viewContainerRef.clear();
        }
    }

    private shouldLoadSidebar(status: StepStatus): boolean {
        return status && status.isOpen && status.status != ComponentStatus.SAVING;
    }
}
