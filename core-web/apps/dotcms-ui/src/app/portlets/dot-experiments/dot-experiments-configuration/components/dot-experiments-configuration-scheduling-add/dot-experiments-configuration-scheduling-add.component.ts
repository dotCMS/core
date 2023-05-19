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
import {
    ComponentStatus,
    RangeOfDateAndTime,
    StepStatus,
    TIME_14_DAYS,
    TIME_90_DAYS
} from '@dotcms/dotcms-models';
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
    stepStatus = ComponentStatus;

    today = new Date();
    initialDate = new Date();
    minEndDate: Date;
    maxEndDate: Date;

    vm$: Observable<{ experimentId: string; scheduling: RangeOfDateAndTime; status: StepStatus }> =
        this.dotExperimentsConfigurationStore.schedulingStepVm$;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    ngOnInit(): void {
        this.setInitialDate();
        this.initForm();
        this.setDateBoundaries();
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

    /**
     * Set min and max date for the End date
     * @returns void
     * @memberof DotExperimentsConfigurationSchedulingAddComponent
     */
    setDateBoundaries(): void {
        this.setMinEndDate();
        this.setMaxEndDate();
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

    /**
     * Set initial date to the next fixed 30 minutes from now.
     * @returns void
     * @memberof DotExperimentsConfigurationSchedulingAddComponent
     */
    private setInitialDate(): void {
        if (this.initialDate.getMinutes() > 30) {
            this.initialDate.setMinutes(0);
            this.initialDate.setHours(this.initialDate.getHours() + 1);
        } else {
            this.initialDate.setMinutes(30);
        }
    }

    /**
     * Initial end date should be at least 14 days after start date.
     */
    private setMinEndDate(): void {
        if (this.form.value.startDate) {
            this.minEndDate = new Date(this.form.value.startDate.getTime() + TIME_14_DAYS);
        } else {
            this.minEndDate = new Date(Date.now() + TIME_14_DAYS);
        }

        if (this.isStatDateMoreRecent()) {
            this.form.patchValue({
                endDate: null
            });
        }
    }

    /**
     * End date should be at most 90 days after start date.
     * @private
     */
    private setMaxEndDate(): void {
        if (this.form.value.startDate) {
            this.maxEndDate = new Date(this.form.value.startDate.getTime() + TIME_90_DAYS);
        } else {
            this.maxEndDate = new Date(Date.now() + TIME_90_DAYS);
        }

        if (this.isEndDateOutOfBoundaries()) {
            this.form.patchValue({
                endDate: null
            });
        }
    }

    private isStatDateMoreRecent(): boolean {
        return (
            this.form.value.startDate &&
            this.form.value.endDate &&
            this.form.value.startDate.getTime() + TIME_14_DAYS > this.form.value.endDate.getTime()
        );
    }

    private isEndDateOutOfBoundaries(): boolean {
        return (
            this.form.value.startDate &&
            this.form.value.endDate &&
            this.form.value.startDate.getTime() + TIME_90_DAYS < this.form.value.endDate.getTime()
        );
    }
}
