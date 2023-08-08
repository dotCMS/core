import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import {
    GoalsConditionsOperatorsListByType,
    GoalsConditionsParametersListByType
} from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';

import { DotExperimentsOptionContentBaseComponent } from '../dot-experiment-options/components/dot-experiments-option-content-base-component/dot-experiments-option-content-base.component';

/**
 * Component with all the inputs of the conditions of Goal Type REACH_PAGE
 */
@Component({
    selector: 'dot-experiments-goal-configuration-reach-page',
    standalone: true,
    imports: [
        CommonModule,
        DotAutofocusModule,
        DotDropdownDirective,
        DotFieldRequiredDirective,
        DotFieldValidationMessageModule,
        DotPipesModule,
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
