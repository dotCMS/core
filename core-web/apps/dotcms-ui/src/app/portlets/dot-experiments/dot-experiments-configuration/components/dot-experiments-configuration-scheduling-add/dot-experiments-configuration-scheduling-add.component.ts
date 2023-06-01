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
import { ComponentStatus, RangeOfDateAndTime, StepStatus } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';
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

    vm$: Observable<{
        experimentId: string;
        scheduling: RangeOfDateAndTime;
        status: StepStatus;
        schedulingBoundaries: Record<string, number>;
    }> = this.dotExperimentsConfigurationStore.schedulingStepVm$;

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
        this.vm$.pipe(take(1)).subscribe(({ schedulingBoundaries }) => {
            this.setMinEndDate(schedulingBoundaries.EXPERIMENTS_MIN_DURATION);
            this.setMaxEndDate(schedulingBoundaries.EXPERIMENTS_MAX_DURATION);
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
     * Initial end date should be at waht is comes from the schedulingBoundaries.
     */
    private setMinEndDate(experimentMinDuration: number): void {
        if (this.form.value.startDate) {
            this.minEndDate = new Date(this.form.value.startDate.getTime() + experimentMinDuration);
        } else {
            this.minEndDate = new Date(Date.now() + experimentMinDuration);
        }

        if (this.isStatDateMoreRecent(experimentMinDuration)) {
            this.form.patchValue({
                endDate: null
            });
        }
    }

    /**
     * End date should be at most what is comes from schedulingBoundaries.
     * @private
     */
    private setMaxEndDate(experimentMaxDuration: number): void {
        if (this.form.value.startDate) {
            this.maxEndDate = new Date(this.form.value.startDate.getTime() + experimentMaxDuration);
        } else {
            this.maxEndDate = new Date(Date.now() + experimentMaxDuration);
        }

        if (this.isEndDateOutOfBoundaries(experimentMaxDuration)) {
            this.form.patchValue({
                endDate: null
            });
        }
    }

    private isStatDateMoreRecent(experimentMinDuration: number): boolean {
        return (
            this.form.value.startDate &&
            this.form.value.endDate &&
            this.form.value.startDate.getTime() + experimentMinDuration >
                this.form.value.endDate.getTime()
        );
    }

    private isEndDateOutOfBoundaries(experimentMaxDuration: number): boolean {
        return (
            this.form.value.startDate &&
            this.form.value.endDate &&
            this.form.value.startDate.getTime() + experimentMaxDuration <
                this.form.value.endDate.getTime()
        );
    }
}
