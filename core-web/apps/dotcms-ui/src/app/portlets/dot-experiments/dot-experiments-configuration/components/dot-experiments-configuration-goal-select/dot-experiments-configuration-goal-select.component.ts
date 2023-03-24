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
import { DotMessagePipe } from '@dotcms/app/view/pipes';
import {
    ComponentStatus,
    GOAL_TYPES,
    Goals,
    GOALS_METADATA_MAP,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
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
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotMessagePipe: DotMessagePipe,
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
                name: new FormControl(
                    this.dotMessagePipe.transform('experiments.configure.goals.name.default'),
                    {
                        nonNullable: true,
                        validators: [Validators.required]
                    }
                ),
                type: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
                conditions: new FormArray([
                    new FormGroup({
                        parameter: new FormControl(''),
                        operator: new FormControl(''),
                        value: new FormControl('')
                    })
                ])
            })
        });
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
