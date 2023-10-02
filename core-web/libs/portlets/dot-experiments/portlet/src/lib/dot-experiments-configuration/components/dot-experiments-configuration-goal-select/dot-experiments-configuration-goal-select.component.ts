import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SidebarModule } from 'primeng/sidebar';

import { takeUntil } from 'rxjs/operators';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    GOAL_TYPES,
    Goals,
    GOALS_METADATA_MAP,
    MAX_INPUT_DESCRIPTIVE_LENGTH,
    StepStatus
} from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotMessagePipe, DotTrimInputDirective } from '@dotcms/ui';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';
import {
    DotSidebarDirective,
    SIDEBAR_SIZES
} from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { DotValidators } from '@shared/validators/dotValidators';

import { DotExperimentsOptionsModule } from '../../../shared/ui/dot-experiment-options/dot-experiments-options.module';
import { DotExperimentsGoalConfigurationReachPageComponent } from '../../../shared/ui/dot-experiments-goal-configuration-reach-page/dot-experiments-goal-configuration-reach-page.component';
import { DotExperimentsGoalConfigurationUrlParameterComponentComponent } from '../../../shared/ui/dot-experiments-goal-configuration-url-parameter-component/dot-experiments-goal-configuration-url-parameter-component.component';
import { DotExperimentsGoalsComingSoonComponent } from '../../../shared/ui/dot-experiments-goals-coming-soon/dot-experiments-goals-coming-soon.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-goal-select',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DotMessagePipe,
        DotFieldValidationMessageModule,
        DotSidebarHeaderComponent,
        DotSidebarDirective,
        DotExperimentsOptionsModule,
        DotDropdownDirective,
        DotAutofocusDirective,
        SidebarModule,
        ButtonModule,
        SelectButtonModule,
        CardModule,
        InputTextModule,
        DropdownModule,
        DotExperimentsGoalConfigurationReachPageComponent,
        DotExperimentsGoalConfigurationUrlParameterComponentComponent,
        DotExperimentsGoalsComingSoonComponent,
        DotTrimInputDirective
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
    protected readonly maxNameLength = MAX_INPUT_DESCRIPTIVE_LENGTH;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore = inject(
        DotExperimentsConfigurationStore
    );
    vm$: Observable<{ experimentId: string; goals: Goals; status: StepStatus }> =
        this.dotExperimentsConfigurationStore.goalsStepVm$;
    private readonly dotMessageService: DotMessageService = inject(DotMessageService);

    protected readonly defaultNameValuesByType: Partial<Record<GOAL_TYPES, string>> = {
        [GOAL_TYPES.EXIT_RATE]: this.dotMessageService.get(
            'experiments.goal.conditions.detect.exit.rate'
        ),
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
        this.listenType();
    }

    private initForm(): void {
        this.form = new FormGroup({
            primary: new FormGroup({
                name: new FormControl('', {
                    nonNullable: true,
                    validators: [
                        Validators.required,
                        Validators.maxLength(this.maxNameLength),
                        DotValidators.noWhitespace
                    ]
                }),
                type: new FormControl('', {
                    nonNullable: true,
                    validators: [Validators.required]
                }),
                conditions: new FormArray([])
            })
        });
    }

    private listenType() {
        this.form
            .get('primary.type')
            .valueChanges.pipe(takeUntil(this.destroy$))
            .subscribe((value) => {
                this.defineDefaultName(value);
                this.resetGoalConditions();
            });
    }

    private resetGoalConditions(): void {
        (this.form.get('primary.conditions') as FormArray).clear();
    }

    /**
     * If the user don't set a custom name set the default name based on goal selection.
     * @param {string} typeValue
     * @memberOf DotExperimentsConfigurationGoalSelectComponent
     */
    private defineDefaultName(typeValue: GOAL_TYPES): void {
        const nameControl = this.form.get('primary.name') as FormControl;

        if (
            nameControl.value === '' ||
            Object.values(this.defaultNameValuesByType).includes(nameControl.value)
        ) {
            nameControl.setValue(this.defaultNameValuesByType[typeValue]);
        }
    }
}
