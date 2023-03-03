import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormArray, FormGroup, FormGroupDirective, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { GoalsConditionsOperatorsList, GoalsConditionsParametersList } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';

/**
 * Component with all the inputs of the configuration of Goal Type REACH PAGE
 */
@Component({
    selector: 'dot-experiments-experiment-goal-reach-page-config',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DotDropdownDirective,
        DotMessagePipeModule,
        DotFieldValidationMessageModule,
        //PrimeNg
        ButtonModule,
        DropdownModule,
        InputTextModule
    ],
    templateUrl: './dot-experiments-experiment-goal-reach-page-config.component.html',
    styleUrls: ['./dot-experiments-experiment-goal-reach-page-config.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsExperimentGoalReachPageConfigComponent implements OnInit {
    parametersList = GoalsConditionsParametersList;
    operatorsList = GoalsConditionsOperatorsList;

    form!: FormGroup;

    constructor(private readonly rootFormGroup: FormGroupDirective) {}

    get primaryFormGroup() {
        return this.rootFormGroup.control.get('primary') as FormGroup;
    }

    get conditionsFormArray() {
        return this.rootFormGroup.control.get('primary.conditions') as FormArray;
    }

    /**
     * Get all Control of the FormArray with the index of the array
     * @param index
     */
    conditionsControl(index: number) {
        return this.conditionsFormArray.controls[index] as FormGroup;
    }

    ngOnInit(): void {
        this.form = this.rootFormGroup.control;
    }
}
