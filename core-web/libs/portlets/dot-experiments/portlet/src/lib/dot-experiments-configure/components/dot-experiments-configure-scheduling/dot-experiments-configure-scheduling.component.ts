import { injectDispatch } from '@ngrx/signals/events';

import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, linkedSignal, untracked } from '@angular/core';
import { disabled, form, FormField, maxDate, minDate } from '@angular/forms/signals';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';

import {
    ExperimentsConfigProperties,
    PROP_NOT_FOUND,
    RangeOfDateAndTime,
    TIME_7_DAYS,
    TIME_90_DAYS
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/** Route `data` key `DotExperimentsConfigResolver` publishes the backend's duration limits under. */
const CONFIG_ROUTE_DATA_KEY = 'config';

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

/** Both pickers move in half hours, same as the old screen's. */
const DATE_PICKER_STEP_MINUTE = 30;

/** Half an hour, in minutes: the granularity the initial start date is rounded up to. */
const HALF_AN_HOUR = 30;

/** How long an experiment may run: the backend's own limits, in milliseconds. */
interface ExperimentDurationBounds {
    minDuration: number;
    maxDuration: number;
}

/** Internal form model. `null` in either field means "not scheduled from/until". */
interface SchedulingFormModel {
    startDate: Date | null;
    endDate: Date | null;
}

/**
 * Scheduling card of the Configure screen: when the experiment starts collecting sessions, and
 * when it stops.
 *
 * Both dates are optional. An experiment with no start date begins the moment Start is pressed and
 * runs until it is stopped by hand; a start date in the future schedules it instead, which is what
 * turns the footer's primary button into "Schedule".
 *
 * The end date is bounded by what the backend accepts — `EXPERIMENTS_MIN_DURATION` and
 * `EXPERIMENTS_MAX_DURATION`, resolved into the route's `data` by `DotExperimentsConfigResolver`
 * and defaulting to 7 and 90 days. The pickers keep an out-of-bounds date out of reach, and the
 * validators flag one that arrived from the server anyway rather than silently discarding it, which
 * is what the old screen did.
 */
@Component({
    selector: 'dot-experiments-configure-scheduling',
    imports: [DatePipe, FormField, ButtonModule, DatePickerModule, DotMessagePipe],
    templateUrl: './dot-experiments-configure-scheduling.component.html'
})
export class DotExperimentsConfigureSchedulingComponent {
    readonly store = inject(DotExperimentsConfigureStore);

    readonly DATE_PICKER_STEP_MINUTE = DATE_PICKER_STEP_MINUTE;

    /** Read once: a modal-free screen cannot outlive the resolve that produced these. */
    readonly #durationBounds = resolveDurationBounds(
        inject(ActivatedRoute).snapshot.data[CONFIG_ROUTE_DATA_KEY]
    );

    /** "Now" for the whole session. The pickers offer nothing before it. */
    readonly #now = new Date();

    /** Where an empty start picker opens: the next half hour, as the old screen does. */
    protected readonly initialStartDate = nextHalfHour(this.#now);

    /**
     * The persisted schedule, compared by value.
     *
     * Every autosave answers with a whole new experiment object, so identity would change on a
     * Name PATCH too — and re-seeding the form from it would wipe a half-entered date.
     */
    readonly #$storedScheduling = computed<RangeOfDateAndTime>(
        () => {
            const scheduling = this.store.experiment()?.scheduling;

            return {
                startDate: scheduling?.startDate ?? null,
                endDate: scheduling?.endDate ?? null
            };
        },
        {
            equal: (a, b) => a.startDate === b.startDate && a.endDate === b.endDate
        }
    );

    /** The schedule as edited on screen, re-seeded whenever the persisted one actually changes. */
    protected readonly $model = linkedSignal<RangeOfDateAndTime, SchedulingFormModel>({
        source: this.#$storedScheduling,
        computation: ({ startDate, endDate }) => ({
            startDate: toDate(startDate),
            endDate: toDate(endDate)
        })
    });

    protected readonly formTree = form(this.$model, (f) => {
        minDate(f.startDate, this.#now);
        minDate(f.endDate, () => this.$minEndDate());
        maxDate(f.endDate, () => this.$maxEndDate());
        disabled(f.startDate, { when: () => this.store.$isLocked() });
        disabled(f.endDate, { when: () => this.store.$isLocked() });
    });

    /** An experiment has to run for at least the configured minimum, counted from its start. */
    protected readonly $minEndDate = computed<Date>(
        () => new Date(this.#startOrNow() + this.#durationBounds.minDuration)
    );

    /** And for no longer than the configured maximum, counted from the same point. */
    protected readonly $maxEndDate = computed<Date>(
        () => new Date(this.#startOrNow() + this.#durationBounds.maxDuration)
    );

    protected readonly $startDate = computed<Date | null>(() => this.$model().startDate);

    /** Nothing to clear before a date is set, and nothing may be cleared once locked. */
    protected readonly $canClearSchedule = computed<boolean>(
        () => !this.store.$isLocked() && !!this.$startDate()
    );

    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);

    /**
     * Persists the schedule through the store, which debounces it into a single PATCH.
     *
     * The persisted value is read untracked so the response re-seeding the form is a no-op here
     * rather than a second call, and an out-of-bounds edit is never sent — it is shown instead.
     */
    protected readonly persistSchedulingEffect = effect(() => {
        const { startDate, endDate } = this.$model();
        const isValid = this.formTree().valid();
        const stored = untracked(this.#$storedScheduling);
        const scheduling: RangeOfDateAndTime = {
            startDate: startDate?.getTime() ?? null,
            endDate: endDate?.getTime() ?? null
        };

        if (
            !isValid ||
            (scheduling.startDate === stored.startDate && scheduling.endDate === stored.endDate)
        ) {
            return;
        }

        this.#dispatch.schedulingChanged(scheduling);
    });

    /**
     * Drops both dates, which puts the experiment back to "starts when Start is pressed".
     *
     * `null` rather than a pair of nulls: it is what `PATCH /api/v1/experiments/{id}` reads as
     * "no schedule at all".
     */
    protected clearSchedule(): void {
        this.#dispatch.schedulingChanged(null);
        this.$model.set({ startDate: null, endDate: null });
    }

    /** The end date is measured from the start, or from now while no start has been chosen. */
    #startOrNow(): number {
        return this.$model().startDate?.getTime() ?? this.#now.getTime();
    }
}

/**
 * The backend reports both limits in days, and `NOT_FOUND` when the property is unset — which is
 * also what an unresolved route means, so both fall back to the same defaults.
 */
function resolveDurationBounds(
    configProps: Record<string, string | boolean> | undefined
): ExperimentDurationBounds {
    return {
        minDuration: toMilliseconds(
            configProps?.[ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION],
            TIME_7_DAYS
        ),
        maxDuration: toMilliseconds(
            configProps?.[ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION],
            TIME_90_DAYS
        )
    };
}

function toMilliseconds(days: string | boolean | undefined, fallback: number): number {
    if (typeof days !== 'string' || days === PROP_NOT_FOUND) {
        return fallback;
    }

    const parsed = Number(days);

    return Number.isFinite(parsed) && parsed > 0 ? parsed * MILLISECONDS_PER_DAY : fallback;
}

/** The next half hour from `date`, so a start date never opens on a minute already gone. */
function nextHalfHour(date: Date): Date {
    const initialDate = new Date(date);

    if (initialDate.getMinutes() > HALF_AN_HOUR) {
        initialDate.setMinutes(0);
        initialDate.setHours(initialDate.getHours() + 1);
    } else {
        initialDate.setMinutes(HALF_AN_HOUR);
    }

    return initialDate;
}

function toDate(timestamp: number | null): Date | null {
    return timestamp ? new Date(timestamp) : null;
}
