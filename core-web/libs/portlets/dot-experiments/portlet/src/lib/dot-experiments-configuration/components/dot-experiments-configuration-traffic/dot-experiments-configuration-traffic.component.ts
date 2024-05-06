import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import {
    ComponentStatus,
    ExperimentSteps,
    StepStatus,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import { DotDynamicDirective, DotIconModule, DotMessagePipe } from '@dotcms/ui';

import {
    ConfigurationTrafficStepViewModel,
    DotExperimentsConfigurationStore
} from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationTrafficAllocationAddComponent } from '../dot-experiments-configuration-traffic-allocation-add/dot-experiments-configuration-traffic-allocation-add.component';
import { DotExperimentsConfigurationTrafficSplitAddComponent } from '../dot-experiments-configuration-traffic-split-add/dot-experiments-configuration-traffic-split-add.component';

@Component({
    selector: 'dot-experiments-configuration-traffic',
    standalone: true,
    imports: [
        CommonModule,
        DotMessagePipe,
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
    vm$: Observable<ConfigurationTrafficStepViewModel> =
        this.dotExperimentsConfigurationStore.trafficStepVm$.pipe(
            tap(({ status }) => this.handleSidebar(status))
        );

    splitEvenly = TrafficProportionTypes.SPLIT_EVENLY;

    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    private componentRef: ComponentRef<
        | DotExperimentsConfigurationTrafficAllocationAddComponent
        | DotExperimentsConfigurationTrafficSplitAddComponent
    >;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    /**
     * Open sidebar to set Traffic Allocation
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficComponent
     */
    changeTrafficAllocation() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.TRAFFIC_LOAD);
    }

    private handleSidebar(status: StepStatus) {
        if (status && status.isOpen && status.status != ComponentStatus.SAVING) {
            this.loadSidebarComponent(status);
        } else {
            this.removeSidebarComponent();
        }
    }

    private loadSidebarComponent(status: StepStatus): void {
        this.sidebarHost.viewContainerRef.clear();
        this.componentRef =
            status.experimentStep == ExperimentSteps.TRAFFICS_SPLIT
                ? this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationTrafficSplitAddComponent>(
                      DotExperimentsConfigurationTrafficSplitAddComponent
                  )
                : this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationTrafficAllocationAddComponent>(
                      DotExperimentsConfigurationTrafficAllocationAddComponent
                  );
    }

    private removeSidebarComponent() {
        if (this.componentRef) {
            this.sidebarHost.viewContainerRef.clear();
        }
    }
}
