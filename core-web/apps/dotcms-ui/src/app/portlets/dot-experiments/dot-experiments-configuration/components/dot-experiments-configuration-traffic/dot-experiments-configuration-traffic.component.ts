import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import {
    ExperimentSteps,
    ComponentStatus,
    StepStatus,
    TrafficProportion,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsConfigurationTrafficAllocationAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-traffic-allocation-add/dot-experiments-configuration-traffic-allocation-add.component';
import { DotExperimentsConfigurationTrafficSplitAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-traffic-split-add/dot-experiments-configuration-traffic-split-add.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

enum TrafficConfig {
    ALLOCATION = 'ALLOCATION',
    SPLIT = 'SPLIT'
}

@Component({
    selector: 'dot-experiments-configuration-traffic',
    standalone: true,
    imports: [
        CommonModule,
        DotMessagePipeModule,
        DotDynamicDirective,
        // PrimeNg
        CardModule,
        ButtonModule,
        DotIconModule,
        TooltipModule
    ],
    templateUrl: './dot-experiments-configuration-traffic.component.html',
    styleUrls: ['./dot-experiments-configuration-traffic.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationTrafficComponent {
    vm$: Observable<{
        experimentId: string;
        trafficProportion: TrafficProportion;
        trafficAllocation: number;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.dotExperimentsConfigurationStore.trafficStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );

    splitEvenly = TrafficProportionTypes.SPLIT_EVENLY;

    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    private componentRef: ComponentRef<
        | DotExperimentsConfigurationTrafficAllocationAddComponent
        | DotExperimentsConfigurationTrafficSplitAddComponent
    >;
    private trafficConfig: TrafficConfig;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    /**
     * Open sidebar to set Traffic Allocation
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficComponent
     */
    changeTrafficAllocation() {
        this.trafficConfig = TrafficConfig.ALLOCATION;
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.TRAFFIC);
    }

    /**
     * Open sidebar to set Traffic Proportion
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficComponent
     */
    changeTrafficProportion() {
        this.trafficConfig = TrafficConfig.SPLIT;
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.TRAFFIC);
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
                this.trafficConfig == TrafficConfig.SPLIT
                    ? this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationTrafficSplitAddComponent>(
                          DotExperimentsConfigurationTrafficSplitAddComponent
                      )
                    : this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationTrafficAllocationAddComponent>(
                          DotExperimentsConfigurationTrafficAllocationAddComponent
                      );
        }
    }

    private removeSidebarComponent() {
        if (this.componentRef) {
            this.sidebarHost.viewContainerRef.clear();
        }
    }
}
