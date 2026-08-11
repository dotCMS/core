import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { takeUntil } from 'rxjs/operators';

import { GOAL_OPERATORS, GoalsConditionsOperatorsListByType } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotDropdownDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotExperimentsOptionContentBaseComponent } from '../dot-experiment-options/components/dot-experiments-option-content-base-component/dot-experiments-option-content-base.component';

const PARAMETER_QUERY_PARAMETER = 'queryParameter';

@Component({
    selector: 'dot-experiments-goal-configuration-url-parameter-component',
    templateUrl: './dot-experiments-goal-configuration-url-parameter-component.component.html',
    imports: [
        ReactiveFormsModule,
        DotDropdownDirective,
        DotFieldValidationMessageComponent,
        DotMessagePipe,
        SelectModule,
        DotAutofocusDirective,
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
     * `value` control not neededπ
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
