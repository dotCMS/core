import { NgForOf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

import { takeUntil } from 'rxjs/operators';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { GOAL_OPERATORS, GoalsConditionsOperatorsListByType } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';

import { DotExperimentsOptionContentBaseComponent } from '../dot-experiment-options/components/dot-experiments-option-content-base-component/dot-experiments-option-content-base.component';

const PARAMETER_QUERY_PARAMETER = 'queryParameter';

@Component({
    standalone: true,
    selector: 'dot-experiments-goal-configuration-url-parameter-component',
    templateUrl: './dot-experiments-goal-configuration-url-parameter-component.component.html',
    styleUrls: ['./dot-experiments-goal-configuration-url-parameter-component.component.scss'],
    imports: [
        ReactiveFormsModule,
        NgForOf,
        DotDropdownDirective,
        DotFieldValidationMessageModule,
        DotMessagePipe,
        DropdownModule,
        DotFieldRequiredDirective,
        DotAutofocusModule,
        InputTextModule
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsGoalConfigurationUrlParameterComponentComponent
    extends DotExperimentsOptionContentBaseComponent
    implements OnInit
{
    override operatorsList = GoalsConditionsOperatorsListByType['URL_PARAMETER'];

    ngOnInit(): void {
        this.setInitialCondition(this.getNewCondition());
        this.listenOperatorChanges();
    }

    private getNewCondition(): FormGroup {
        return new FormGroup({
            parameter: new FormControl(PARAMETER_QUERY_PARAMETER, Validators.required),
            operator: new FormControl('', Validators.required),
            value: new FormGroup({
                name: new FormControl('', Validators.required),
                value: new FormControl('', Validators.required)
            })
        });
    }

    /**
     * When the `operator` control value is EXIST
     * `value` control not needed
     * @private
     */
    private listenOperatorChanges(): void {
        this.conditionsFormArray.controls.forEach((condition: FormGroup) => {
            condition
                .get('operator')
                .valueChanges.pipe(takeUntil(this.destroy$))
                .subscribe((value: string) => {
                    const valueControl: AbstractControl = condition.get('value.value');
                    if (value === GOAL_OPERATORS.EXISTS) {
                        valueControl.disable();
                    } else {
                        valueControl.enable();
                    }

                    valueControl.reset();
                });
        });
    }
}
