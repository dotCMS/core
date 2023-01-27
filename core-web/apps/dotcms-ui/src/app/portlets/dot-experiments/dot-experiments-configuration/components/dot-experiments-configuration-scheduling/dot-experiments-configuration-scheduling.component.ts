import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { tap } from 'rxjs/operators';

import { ExperimentSteps, RangeOfDateAndTime, Status, StepStatus } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsConfigurationSchedulingAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-scheduling-add/dot-experiments-configuration-scheduling-add.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

@Component({
    selector: 'dot-experiments-configuration-scheduling',
    standalone: true,
    imports: [
        CommonModule,
        DotDynamicDirective,
        DotMessagePipeModule,
        // PrimeNg
        CardModule,
        ButtonModule,
        ConfirmPopupModule
    ],
    templateUrl: './dot-experiments-configuration-scheduling.component.html',
    styleUrls: ['./dot-experiments-configuration-scheduling.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationSchedulingComponent {
    vm$: Observable<{ experimentId: string; scheduling: RangeOfDateAndTime; status: StepStatus }> =
        this.dotExperimentsConfigurationStore.schedulingStepVm$.pipe(
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
        if (status && status.isOpen) {
            this.loadSidebarComponent(status);
        } else {
            this.removeSidebarComponent();
        }
    }

    private loadSidebarComponent(status: StepStatus): void {
        if (status && status.isOpen && status.status != Status.SAVING) {
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
}
