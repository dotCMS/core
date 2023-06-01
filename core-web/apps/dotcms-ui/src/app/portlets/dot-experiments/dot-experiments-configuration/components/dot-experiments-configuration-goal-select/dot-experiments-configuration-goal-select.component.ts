import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit
} from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

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
    Goals,
    GOALS_METADATA_MAP,
    MAX_INPUT_LENGTH,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsOptionsModule } from '@portlets/dot-experiments/shared/ui/dot-experiment-options/dot-experiments-options.module';
import { DotExperimentsGoalConfigurationReachPageComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-goal-configuration-reach-page/dot-experiments-goal-configuration-reach-page.component';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';
import {
    DotSidebarDirective,
    SIDEBAR_SIZES
} from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

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
        DotExperimentsGoalConfigurationReachPageComponent
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
    protected readonly maxNameLength = MAX_INPUT_LENGTH;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private BOUNCE_RATE_LABEL = this.dotMessageService.get(
        'experiments.goal.conditions.minimize.bounce.rate'
    );
    private REACH_PAGE_LABEL = this.dotMessageService.get(
        'experiments.goal.conditions.maximize.reach.page'
    );

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotMessageService: DotMessageService,
        private readonly cdr: ChangeDetectorRef
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
                if (type === GOAL_TYPES.REACH_PAGE || type === GOAL_TYPES.CLICK_ON_ELEMENT) {
                    this.addConditionsControlValidations();
                } else {
                    this.removeConditionsControlValidations();
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
            nameControl.value === this.BOUNCE_RATE_LABEL ||
            nameControl.value === this.REACH_PAGE_LABEL
        ) {
            nameControl.setValue(
                typeValue === GOAL_TYPES.BOUNCE_RATE
                    ? this.BOUNCE_RATE_LABEL
                    : this.REACH_PAGE_LABEL
            );
        }
    }

    private addConditionsControlValidations(): void {
        Object.values(this.conditionsFormArray.controls).forEach((controlArray: FormArray) => {
            Object.values(controlArray.controls).forEach((control) => {
                control.setValidators([Validators.required]);
                control.updateValueAndValidity();
            });
        });
        this.conditionsFormArray.enable();
    }

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
