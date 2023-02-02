import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SidebarModule } from 'primeng/sidebar';

import { take } from 'rxjs/operators';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { RangeOfDateAndTime, Status, StepStatus } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

@Component({
    selector: 'dot-dot-experiments-configuration-scheduling-add',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotFieldValidationMessageModule,
        DotMessagePipeModule,
        DotSidebarHeaderComponent,
        DotSidebarDirective,
        //PrimeNg
        SidebarModule,
        ButtonModule,
        SelectButtonModule,
        CardModule,
        CalendarModule
    ],
    templateUrl: './dot-experiments-configuration-scheduling-add.component.html',
    styleUrls: ['./dot-experiments-configuration-scheduling-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationSchedulingAddComponent implements OnInit {
    form: FormGroup;
    scheduling: RangeOfDateAndTime;
    stepStatus = Status;

    today = new Date();

    vm$: Observable<{ experimentId: string; scheduling: RangeOfDateAndTime; status: StepStatus }> =
        this.dotExperimentsConfigurationStore.schedulingStepVm$;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    ngOnInit(): void {
        this.initForm();
    }

    /**
     * Close sidebar
     * @returns void
     * @memberof DotExperimentsConfigurationSchedulingAddComponent
     */
    closeSidebar() {
        this.dotExperimentsConfigurationStore.closeSidebar();
    }

    /**
     * Save selected Scheduling.
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationSchedulingAddComponent
     */
    save(experimentId: string) {
        const { startDate, endDate } = this.form.value;
        this.dotExperimentsConfigurationStore.setSelectedScheduling({
            scheduling: {
                startDate: startDate ? startDate.getTime() : startDate,
                endDate: endDate ? endDate.getTime() : endDate
            },
            experimentId
        });
    }

    private initForm() {
        this.vm$.pipe(take(1)).subscribe((data) => {
            this.form = new FormGroup({
                startDate: new FormControl<Date>(
                    data.scheduling?.startDate ? new Date(data.scheduling.startDate) : null
                ),
                endDate: new FormControl<Date>(
                    data.scheduling?.endDate ? new Date(data.scheduling.endDate) : null
                )
            });
        });
    }
}
