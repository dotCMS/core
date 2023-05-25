import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import {
    ComponentStatus,
    ExperimentSteps,
    RangeOfDateAndTime,

} from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipeModule } from "@dotcms/ui";
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import {
    DotExperimentsConfigurationSchedulingAddComponent
} from "../dot-experiments-configuration-scheduling-add/dot-experiments-configuration-scheduling-add.component";
import { DotExperimentsConfigurationStore } from "../../store/dot-experiments-configuration-store";


@Component({
    selector: 'dot-experiments-configuration-scheduling',
    standalone: true,
    imports: [
        CommonModule,
        DotDynamicDirective,
        DotMessagePipeModule,
        DotIconModule,
        // PrimeNg
        CardModule,
        ButtonModule,
        ConfirmPopupModule,
        TooltipModule
    ],
    templateUrl: './dot-experiments-configuration-scheduling.component.html',
    styleUrls: ['./dot-experiments-configuration-scheduling.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationSchedulingComponent {
    vm$: Observable<{
        experimentId: string;
        scheduling: RangeOfDateAndTime;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.dotExperimentsConfigurationStore.schedulingStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );

    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    private componentRef: ComponentRef<DotExperimentsConfigurationSchedulingAddComponent>;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

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
