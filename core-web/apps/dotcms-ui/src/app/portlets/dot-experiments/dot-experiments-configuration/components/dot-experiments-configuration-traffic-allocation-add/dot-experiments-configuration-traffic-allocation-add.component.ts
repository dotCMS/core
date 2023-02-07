import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SidebarModule } from 'primeng/sidebar';
import { SliderModule } from 'primeng/slider';

import { take } from 'rxjs/operators';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { Status, StepStatus, TrafficProportion } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

@Component({
    selector: 'dot-dot-experiments-configuration-traffic-allocation-add',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotFieldValidationMessageModule,
        DotMessagePipeModule,
        DotSidebarHeaderComponent,
        DotSidebarDirective,
        //PrimeNg
        SidebarModule,
        ButtonModule,
        SelectButtonModule,
        CardModule,
        SliderModule,
        InputTextModule,
        FormsModule
    ],
    templateUrl: './dot-experiments-configuration-traffic-allocation-add.component.html',
    styleUrls: ['./dot-experiments-configuration-traffic-allocation-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationTrafficAllocationAddComponent implements OnInit {
    form: FormGroup;
    trafficAllocation: string;
    stepStatus = Status;

    vm$: Observable<{
        experimentId: string;
        trafficProportion: TrafficProportion;
        trafficAllocation: string;
        status: StepStatus;
    }> = this.dotExperimentsConfigurationStore.trafficStepVm$;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    ngOnInit(): void {
        this.initForm();
    }

    /**
     * Save modification in traffic allocation.
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficAllocationAddComponent
     */
    save(experimentId: string) {
        const { trafficAllocation } = this.form.value;
        this.dotExperimentsConfigurationStore.setSelectedAllocation({
            trafficAllocation,
            experimentId
        });
    }

    /**
     * Close sidebar
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficAllocationAddComponent
     */
    closeSidebar() {
        this.dotExperimentsConfigurationStore.closeSidebar();
    }

    private initForm() {
        this.vm$.pipe(take(1)).subscribe((data) => {
            this.form = new FormGroup({
                trafficAllocation: new FormControl<string>(data.trafficAllocation)
            });
        });
    }
}
