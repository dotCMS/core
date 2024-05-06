import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import {
    AbstractControl,
    FormArray,
    FormBuilder,
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SidebarModule } from 'primeng/sidebar';

import { take } from 'rxjs/operators';

import { ComponentStatus, TrafficProportionTypes, Variant } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSidebarDirective, DotSidebarHeaderComponent } from '@dotcms/ui';

import {
    ConfigurationTrafficStepViewModel,
    DotExperimentsConfigurationStore
} from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-traffic-split-add',
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
        RadioButtonModule,
        InputNumberModule,
        FormsModule
    ],
    templateUrl: './dot-experiments-configuration-traffic-split-add.component.html',
    styleUrls: ['./dot-experiments-configuration-traffic-split-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationTrafficSplitAddComponent implements OnInit {
    form: FormGroup;
    stepStatus = ComponentStatus;
    splitEvenly = TrafficProportionTypes.SPLIT_EVENLY;
    customPercentages = TrafficProportionTypes.CUSTOM_PERCENTAGES;

    vm$: Observable<ConfigurationTrafficStepViewModel> =
        this.dotExperimentsConfigurationStore.trafficStepVm$;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private fb: FormBuilder
    ) {}

    get variants(): FormArray {
        return this.form.get('variants') as FormArray;
    }

    ngOnInit(): void {
        this.initForm();
    }

    /**
     * Save modification in traffic allocation.
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficSplitAddComponent
     */
    save(experimentId: string) {
        this.dotExperimentsConfigurationStore.setSelectedTrafficProportion({
            trafficProportion: this.form.value,
            experimentId
        });
    }

    /**
     * Close sidebar
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficSplitAddComponent
     */
    closeSidebar() {
        this.dotExperimentsConfigurationStore.closeSidebar();
    }

    /**
     * Split variant propotion
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficSplitAddComponent
     */
    splitVariantsEvenly() {
        const amountOfVariants = this.variants.length;
        this.variants.controls.forEach((variant) => {
            variant.get('weight').setValue(Math.trunc((100 / amountOfVariants) * 100) / 100);
        });
    }

    /**
     * This is needed because a current bug in primeng
     * https://github.com/primefaces/primeng/pull/11093
     * @param {number} arrayIndex
     * @param {number} value
     * @returns void
     * @memberof DotExperimentsConfigurationTrafficSplitAddComponent
     */
    checkControl(arrayIndex: number, value: number): void {
        ((this.form.get('variants') as FormArray).controls[arrayIndex] as FormGroup).controls[
            'weight'
        ].setValue(value);
    }

    private initForm() {
        this.vm$.pipe(take(1)).subscribe((data) => {
            this.form = this.fb.group({
                type: new FormControl<TrafficProportionTypes>(data.trafficProportion.type, {
                    nonNullable: true,
                    validators: [Validators.required]
                }),
                variants: this.fb.array<Variant>([])
            });

            data.trafficProportion.variants.forEach((variant) => {
                this.variants.push(this.addVariantToForm(variant));
            });
        });
    }

    private addVariantToForm(variant: Variant): FormGroup {
        return this.fb.group(
            {
                id: variant.id,
                name: variant.name,
                weight: [Math.trunc(variant.weight * 100) / 100, [Validators.required]],
                url: variant.url
            },
            { nonNullable: true, validators: [this.trafficSplitCheck()] }
        );
    }

    private trafficSplitCheck(): ValidatorFn {
        return (_control: AbstractControl): ValidationErrors | null => {
            let sum = 0;
            this.variants.controls.forEach((variant) => {
                sum += variant.get('weight').value;
                variant.setErrors(null);
            });

            return Math.round(sum) == 100 ? null : { trafficSplit: 'invalid' };
        };
    }
}
