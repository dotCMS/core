import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import {
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';
import { SliderModule } from 'primeng/slider';

import { take } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSidebarDirective, DotSidebarHeaderComponent } from '@dotcms/ui';

import {
    ConfigurationTrafficStepViewModel,
    DotExperimentsConfigurationStore
} from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-traffic-allocation-add',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotMessagePipe,
        DotSidebarHeaderComponent,
        DotSidebarDirective,
        //PrimeNg
        SidebarModule,
        ButtonModule,
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
    stepStatus = ComponentStatus;

    vm$: Observable<ConfigurationTrafficStepViewModel> =
        this.dotExperimentsConfigurationStore.trafficStepVm$;

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

    /**
     * Check allocation is higher than 100
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficAllocationAddComponent
     */
    checkAllocationRange() {
        this.form.setValue({
            trafficAllocation:
                this.form.value.trafficAllocation > 100 ? 100 : this.form.value.trafficAllocation
        });
    }

    private initForm() {
        this.vm$.pipe(take(1)).subscribe((data) => {
            this.form = new FormGroup({
                trafficAllocation: new FormControl<number>(data.trafficAllocation, {
                    nonNullable: true,
                    validators: [Validators.required]
                })
            });
        });
    }
}
