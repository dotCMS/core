import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { DrawerModule } from 'primeng/drawer';
import { SelectButtonModule } from 'primeng/selectbutton';

import { take } from 'rxjs/operators';

import { ComponentStatus, RangeOfDateAndTime, StepStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSidebarDirective, DotSidebarHeaderComponent } from '@dotcms/ui';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-dot-experiments-configuration-scheduling-add',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DotMessagePipe,
        DotSidebarHeaderComponent,
        DotSidebarDirective,
        //PrimeNg
        DrawerModule,
        ButtonModule,
        SelectButtonModule,
        CardModule,
        DatePickerModule
    ],
    templateUrl: './dot-experiments-configuration-scheduling-add.component.html',
    styleUrls: ['./dot-experiments-configuration-scheduling-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationSchedulingAddComponent implements OnInit {
    private readonly dotExperimentsConfigurationStore = inject(DotExperimentsConfigurationStore);

    form: FormGroup;
    scheduling: RangeOfDateAndTime;
    stepStatus = ComponentStatus;

    today = new Date();
    initialDate = new Date();
    maxEndDate: Date;
    minEndDate: Date;

    vm$: Observable<{
        experimentId: string;
        scheduling: RangeOfDateAndTime;
        status: StepStatus;
        schedulingBoundaries: Record<string, number>;
    }> = this.dotExperimentsConfigurationStore.schedulingStepVm$;

    ngOnInit(): void {
        this.setInitialDate();
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

    /**
     * Initial end date should be at least 30 minutes after start date.
     * @returns void
     * @memberof DotExperimentsConfigurationSchedulingAddComponent
     */
    setDateBoundaries(): void {
        this.vm$.pipe(take(1)).subscribe(({ schedulingBoundaries }) => {
            this.setMinEndDate(schedulingBoundaries['EXPERIMENTS_MIN_DURATION']);
            this.setMaxEndDate(schedulingBoundaries['EXPERIMENTS_MAX_DURATION']);
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
