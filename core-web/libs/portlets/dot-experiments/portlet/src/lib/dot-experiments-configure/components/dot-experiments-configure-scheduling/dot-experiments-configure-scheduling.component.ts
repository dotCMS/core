import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { FieldTree, FormField } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';

import { DotMessagePipe } from '@dotcms/ui';

import { SchedulingDateBounds, SchedulingFormSlice } from '../../../shared/models';

/** Both pickers move in half hours, same as the old screen's. */
const DATE_PICKER_STEP_MINUTE = 30;

/**
 * Scheduling card of the Configure screen: when the experiment starts collecting sessions, and
 * when it stops.
 *
 * Both dates are optional. An experiment with no start date begins the moment Start is pressed and
 * runs until it is stopped by hand; a start date in the future schedules it instead, which is what
 * turns the footer's primary button into "Schedule".
 *
 * The card is a slice of the shell's root form and nothing else: it does not read the store and
 * does not persist anything. Clearing the schedule is an edit like any other — it empties both
 * dates, and the shell's one autosave sends the `null` that `PATCH /api/v1/experiments/{id}` reads
 * as "no schedule at all".
 *
 * The window the end date may fall in comes from the backend's `EXPERIMENTS_MIN_DURATION` and
 * `EXPERIMENTS_MAX_DURATION`, resolved by the shell and handed over as bounds, so the pickers and
 * the form's rules can never disagree about it.
 */
@Component({
    selector: 'dot-experiments-configure-scheduling',
    imports: [DatePipe, Card, FormField, ButtonModule, DatePickerModule, DotMessagePipe],
    templateUrl: './dot-experiments-configure-scheduling.component.html'
})
export class DotExperimentsConfigureSchedulingComponent {
    /** The scheduling slice of the root form: the card's whole editable surface. */
    readonly $field = input.required<FieldTree<SchedulingFormSlice>>({ alias: 'field' });

    /** Where the two pickers may go, resolved from the backend's duration limits by the shell. */
    readonly $bounds = input.required<SchedulingDateBounds>({ alias: 'bounds' });

    readonly DATE_PICKER_STEP_MINUTE = DATE_PICKER_STEP_MINUTE;

    protected readonly $startDate = computed<Date | null>(() => this.$field()().value().startDate);

    /** Read off the field: the schema disables the slice, so the card need not ask the store. */
    protected readonly $isLocked = computed<boolean>(() => this.$field()().disabled());

    /**
     * Nothing to clear before a date is set, and nothing may be cleared once locked.
     *
     * Either date counts. An end date on its own is a schedule the backend keeps — `toRange` sends
     * `{ startDate: null, endDate }`, which reads as "start when Start is pressed, stop then" — so
     * keying this on the start date alone hid the control for a schedule that was really there.
     */
    protected readonly $canClearSchedule = computed<boolean>(() => {
        const { startDate, endDate } = this.$field()().value();

        return !this.$isLocked() && (!!startDate || !!endDate);
    });

    /**
     * Whatever the form says is wrong with each date, message included — the bounds copy quotes the
     * bounds, and those live with the rules that enforce them rather than being re-formatted here.
     */
    protected readonly $startDateErrors = computed(() => this.$field().startDate().errors());

    protected readonly $endDateErrors = computed(() => this.$field().endDate().errors());

    /**
     * Drops both dates, which puts the experiment back to "starts when Start is pressed".
     *
     * Written as one value: the two dates are one choice, and clearing them one at a time would
     * report an intermediate schedule the user never asked for.
     */
    protected clearSchedule(): void {
        this.$field()().value.set({ startDate: null, endDate: null });
    }
}
