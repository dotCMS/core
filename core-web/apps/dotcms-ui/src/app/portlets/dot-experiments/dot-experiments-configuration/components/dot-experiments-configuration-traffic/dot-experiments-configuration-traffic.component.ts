import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

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
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

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
        DotIconModule
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
    }> = this.dotExperimentsConfigurationStore.trafficStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );

    splitEvenly = TrafficProportionTypes.SPLIT_EVENLY;

    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    private componentRef: ComponentRef<DotExperimentsConfigurationTrafficAllocationAddComponent>;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    changeTrafficAllocation() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.TRAFFIC);
    }

    changeTrafficProportion() {
        //to be implemented
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
                this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationTrafficAllocationAddComponent>(
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
