import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';

import {
    GoalsConditionsOperatorsListByType,
    GoalsConditionsParametersListByType
} from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotDropdownDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotExperimentsOptionContentBaseComponent } from '../dot-experiment-options/components/dot-experiments-option-content-base-component/dot-experiments-option-content-base.component';

/**
 * Component with all the inputs of the conditions of Goal Type REACH_PAGE
 */
@Component({
    selector: 'dot-experiments-goal-configuration-reach-page',
    imports: [
        CommonModule,
        DotAutofocusDirective,
        DotDropdownDirective,
        DotFieldRequiredDirective,
        DotFieldValidationMessageComponent,
        DropdownModule,
        DotMessagePipe,
        InputTextModule,
        PaginatorModule,
        ReactiveFormsModule
    ],
    templateUrl: './dot-experiments-goal-configuration-reach-page.component.html',
    styleUrls: ['./dot-experiments-goal-configuration-reach-page.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsGoalConfigurationReachPageComponent
    extends DotExperimentsOptionContentBaseComponent
    implements OnInit
{
    override parametersList = GoalsConditionsParametersListByType['REACH_PAGE'];
    override operatorsList = GoalsConditionsOperatorsListByType['REACH_PAGE'];

    ngOnInit(): void {
        this.setInitialCondition(this.getNewCondition());
    }

    private getNewCondition(): FormGroup {
        return new FormGroup({
            parameter: new FormControl('', Validators.required),
            operator: new FormControl('', Validators.required),
            value: new FormControl('', Validators.required)
        });
    }
}
