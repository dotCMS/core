import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import {
    FormArray,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SidebarModule } from 'primeng/sidebar';

import { takeUntil } from 'rxjs/operators';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    GOAL_TYPES,
    GoalConditionsControlsNames,
    Goals,
    GOALS_METADATA_MAP,
    MAX_INPUT_DESCRIPTIVE_LENGTH,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';
import { DotExperimentsGoalConfigurationUrlParameterComponentComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-goal-configuration-url-parameter-component/dot-experiments-goal-configuration-url-parameter-component.component';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';
import {
    DotSidebarDirective,
    SIDEBAR_SIZES
} from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { DotValidators } from '@shared/validators/dotValidators';

import { DotExperimentsOptionsModule } from '../../../shared/ui/dot-experiment-options/dot-experiments-options.module';
import { DotExperimentsGoalConfigurationReachPageComponent } from '../../../shared/ui/dot-experiments-goal-configuration-reach-page/dot-experiments-goal-configuration-reach-page.component';
import { DotExperimentsGoalsComingSoonComponent } from '../../../shared/ui/dot-experiments-goals-coming-soon/dot-experiments-goals-coming-soon.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-goal-select',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotMessagePipeModule,
        DotFieldValidationMessageModule,
        DotSidebarHeaderComponent,
        DotSidebarDirective,
        DotExperimentsOptionsModule,
        DotDropdownDirective,
        DotAutofocusModule,
        //PrimeNg
        SidebarModule,
        ButtonModule,
        SelectButtonModule,
        CardModule,
        InputTextModule,
        DropdownModule,
        DotExperimentsGoalConfigurationReachPageComponent,
        DotExperimentsGoalConfigurationUrlParameterComponentComponent,
        DotExperimentsGoalsComingSoonComponent
    ],
    templateUrl: './dot-experiments-configuration-goal-select.component.html',
    styleUrls: ['./dot-experiments-configuration-goal-select.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationGoalSelectComponent implements OnInit, OnDestroy {
    sidebarSizes = SIDEBAR_SIZES;
    form: FormGroup;
    goals = GOALS_METADATA_MAP;
    goalsTypes = GOAL_TYPES;
    statusList = ComponentStatus;
    vm$: Observable<{ experimentId: string; goals: Goals; status: StepStatus }> =
        this.dotExperimentsConfigurationStore.goalsStepVm$;
    protected readonly maxNameLength = MAX_INPUT_DESCRIPTIVE_LENGTH;

    protected readonly defaultNameValuesByType: Partial<Record<GOAL_TYPES, string>> = {
        [GOAL_TYPES.BOUNCE_RATE]: this.dotMessageService.get(
            'experiments.goal.conditions.minimize.bounce.rate'
        ),
        [GOAL_TYPES.REACH_PAGE]: this.dotMessageService.get(
            'experiments.goal.conditions.maximize.reach.page'
        ),
        [GOAL_TYPES.URL_PARAMETER]: this.dotMessageService.get(
            'experiments.goal.conditions.detect.queryparam.in.url'
        )
    };

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotMessageService: DotMessageService
    ) {}

    /**
     * Get the conditions FormArray
     *
     * @memberOf DotExperimentsConfigurationGoalSelectComponent
     * @return FormArray
     */
    get conditionsFormArray() {
        return this.form.get('primary.conditions') as FormArray;
    }

    get goalNameControl(): FormControl {
        return this.form.get('primary.name') as FormControl;
    }

    /**
     * Close the sidebar
     *
     * @memberOf DotExperimentsConfigurationGoalSelectComponent
     * @return void
     */
    closeSidebar(): void {
        this.dotExperimentsConfigurationStore.closeSidebar();
    }

    /**
     * Save the selected Goal
     *
     * @param {string} experimentId
     * @memberOf DotExperimentsConfigurationGoalSelectComponent
     */
    save(experimentId: string): void {
        this.dotExperimentsConfigurationStore.setSelectedGoal({
            experimentId,
            goals: {
                ...this.form.value
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    ngOnInit(): void {
        this.initForm();
        this.listenGoalTypeSelection();
    }

    private listenGoalTypeSelection(): void {
        this.form
            .get('primary.type')
            .valueChanges.pipe(takeUntil(this.destroy$))
            .subscribe((type) => {
                this.removeConditionsControlValidations();
                if (type != GOAL_TYPES.BOUNCE_RATE) {
                    this.addConditionsControlValidations();
                }
            });
    }

    private initForm(): void {
        this.form = new FormGroup({
            primary: new FormGroup({
                name: new FormControl('', {
                    nonNullable: true,
                    validators: [Validators.required, Validators.maxLength(this.maxNameLength)]
                }),
                type: new FormControl('', {
                    nonNullable: true,
                    validators: [Validators.required]
                }),
                conditions: new FormArray([
                    new FormGroup({
                        parameter: new FormControl(''),
                        operator: new FormControl(''),
                        value: new FormControl('')
                    })
                ])
            })
        });

        this.form.get('primary.type').valueChanges.subscribe((value) => {
            this.defineDefaultName(value);
        });
    }

    /**
     * If the user don't set a custom name set the default name based on goal selection.
     * @param {string} typeValue
     * @memberOf DotExperimentsConfigurationGoalSelectComponent
     */
    private defineDefaultName(typeValue: string): void {
        const nameControl = this.form.get('primary.name') as FormControl;

        if (
            nameControl.value === '' ||
            Object.values(this.defaultNameValuesByType).includes(nameControl.value)
        ) {
            nameControl.setValue(this.defaultNameValuesByType[typeValue]);
        }
    }

    /**
     * Add the required validations to the controls of the conditions controls FormArray
     * @private
     */
    private addConditionsControlValidations(): void {
        const selectedGoalType = this.form.get('primary.type').value;

        const RequiredConditionsControlsByGoalTypeRules: Partial<
            Record<GOAL_TYPES, Record<GoalConditionsControlsNames, ValidatorFn[]>>
        > = {
            [GOAL_TYPES.REACH_PAGE]: {
                parameter: [Validators.required],
                operator: [Validators.required],
                value: [Validators.required]
            },
            [GOAL_TYPES.URL_PARAMETER]: {
                parameter: [Validators.required, DotValidators.validQueryParamName],
                operator: [Validators.required],
                value: [Validators.required]
            }
        };

        Object.keys(RequiredConditionsControlsByGoalTypeRules[selectedGoalType]).forEach(
            (controlName) => {
                Object.values(this.conditionsFormArray.controls).forEach(
                    (conditionFormGroup: FormArray) => {
                        conditionFormGroup.controls[controlName].enable();
                        conditionFormGroup.controls[controlName].setValidators([
                            ...RequiredConditionsControlsByGoalTypeRules[selectedGoalType][
                                controlName
                            ]
                        ]);
                        conditionFormGroup.controls[controlName].updateValueAndValidity();
                    }
                );
            }
        );

        this.conditionsFormArray.enable();
    }

    /**
     * Remove the validations to the controls of the conditions controls FormArray
     * @private
     */
    private removeConditionsControlValidations(): void {
        Object.values(this.conditionsFormArray.controls).forEach((controlArray: FormArray) => {
            Object.values(controlArray.controls).forEach((control) => {
                control.reset('');
                control.setValidators([]);
                control.updateValueAndValidity();
            });
        });
        this.conditionsFormArray.disable();
    }
}
