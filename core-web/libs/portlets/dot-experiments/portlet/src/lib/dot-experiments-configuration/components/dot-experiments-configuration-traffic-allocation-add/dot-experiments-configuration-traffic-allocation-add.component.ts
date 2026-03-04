import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    inject
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
import { InputTextModule } from 'primeng/inputtext';
import { SliderModule } from 'primeng/slider';

import { take } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DotMessagePipe,
    DotSidebarDirective,
    DotSidebarHeaderComponent,
    SIDEBAR_SIZES
} from '@dotcms/ui';

import {
    ConfigurationTrafficStepViewModel,
    DotExperimentsConfigurationStore
} from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-traffic-allocation-add',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DotMessagePipe,
        DotSidebarHeaderComponent,
        DotSidebarDirective,
        DrawerModule,
        ButtonModule,
        SliderModule,
        InputTextModule
    ],
    templateUrl: './dot-experiments-configuration-traffic-allocation-add.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationTrafficAllocationAddComponent implements OnInit {
    private readonly dotExperimentsConfigurationStore = inject(DotExperimentsConfigurationStore);
    private readonly cd = inject(ChangeDetectorRef);

    form: FormGroup;
    trafficAllocation: string;
    stepStatus = ComponentStatus;
    sidebarSizes = SIDEBAR_SIZES;

    vm$: Observable<ConfigurationTrafficStepViewModel> =
        this.dotExperimentsConfigurationStore.trafficStepVm$;

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
     * Check traffic allocation is higher than 100
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficAllocationAddComponent
     */
    onInputTrafficAllocation(event: Event) {
        const input = event.target as HTMLInputElement;
        const raw = input.value.trim();
        const num = raw === '' ? 1 : Math.min(100, Math.max(1, Number(raw) || 1));
        this.form.patchValue({ trafficAllocation: num }, { emitEvent: true });
        this.cd.detectChanges();
    }

    private initForm() {
        this.vm$.pipe(take(1)).subscribe((data) => {
            this.form = new FormGroup({
                trafficAllocation: new FormControl<number>(data.trafficAllocation, {
                    nonNullable: true,
                    validators: [Validators.required]
                }),
                trafficAllocationInput: new FormControl<number>(data.trafficAllocation, {
                    nonNullable: true,
                    validators: [Validators.required]
                })
            });
        });

        // Workaround: PrimeNG p-slider doesn't properly sync when multiple form controls
        // share the same formControlName. Using separate controls and manually syncing them
        // via valueChanges subscriptions to keep the slider and input in sync.
        this.form.get('trafficAllocationInput')?.valueChanges.subscribe((value) => {
            this.form.get('trafficAllocation')?.setValue(value, { emitEvent: false });
            this.cd.detectChanges();
        });

        this.form.get('trafficAllocation')?.valueChanges.subscribe((value) => {
            this.form.get('trafficAllocationInput')?.setValue(value, { emitEvent: false });
            this.cd.detectChanges();
        });
    }
}
