import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SidebarModule } from 'primeng/sidebar';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { SelectButtonModule } from 'primeng/selectbutton';
import {
    DefaultGoalConfiguration,
    ExperimentsGoalsList,
    Goals,
    GoalSelectOption,
    Status,
    StepStatus
} from '@dotcms/dotcms-models';
import { CardModule } from 'primeng/card';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { Observable } from 'rxjs';

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
        //PrimeNg
        SidebarModule,
        ButtonModule,
        SelectButtonModule,
        CardModule
    ],
    templateUrl: './dot-experiments-configuration-goal-select.component.html',
    styleUrls: ['./dot-experiments-configuration-goal-select.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationGoalSelectComponent implements OnInit {
    /**
     * Emit when the sidebar is closed
     */
    @Output()
    closedSidebar = new EventEmitter<boolean>();

    form: FormGroup;
    goalsList: Array<GoalSelectOption> = ExperimentsGoalsList;
    statusList = Status;

    vm$: Observable<{ experimentId: string; goals: Goals; status: StepStatus }> =
        this.dotExperimentsConfigurationStore.goalsStepVm$;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    ngOnInit(): void {
        this.initForm();
    }

    save(experimentId: string) {
        const { goal } = this.form.value;
        this.dotExperimentsConfigurationStore.setSelectedGoal({
            experimentId,
            goals: {
                ...DefaultGoalConfiguration,
                primary: {
                    ...DefaultGoalConfiguration.primary,
                    type: goal
                }
            }
        });
    }

    private initForm() {
        this.form = new FormGroup({
            goal: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required]
            })
        });
    }
}
