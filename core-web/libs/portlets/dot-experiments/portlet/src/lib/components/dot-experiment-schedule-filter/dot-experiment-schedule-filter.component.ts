import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DatePickerModule } from 'primeng/datepicker';
import { PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { DotChipFilterComponent } from '@dotcms/ui';

import { ExperimentsListSchedulePeriod } from '../../shared/models';
import {
    formatScheduleBound,
    parseScheduleBound
} from '../../util/dot-experiments-list-store.util';

/**
 * Chip narrowing the list to experiments scheduled to start inside a period.
 *
 * A calendar in range mode, not a list of relative windows. #37307 asks for five fixed options
 * ("the last 3 months" and friends), and this deliberately diverges: Content Drive filters dates
 * with an absolute from-to calendar (`dot-field-filter`), and a second listing in the same admin
 * filtering dates a different way would create exactly the inconsistency this work exists to
 * close. A range also covers every window the issue asks for, and periods that are not "the last
 * N months". See User Story 2 in the spec.
 *
 * It cannot reuse `dot-field-filter` itself: that chip is driven by a `DotCMSContentTypeField` and
 * switches on the field's type, and an experiment is not a contentlet with a content type.
 *
 * No period is the absence of a range, not a range defaulted to today (FR-025): the shared chip
 * reads "active" from having selections, so a chip always holding one would claim a filter it is
 * not applying.
 */
@Component({
    selector: 'dot-experiment-schedule-filter',
    imports: [FormsModule, DatePickerModule, PopoverModule, DotChipFilterComponent],
    templateUrl: './dot-experiment-schedule-filter.component.html'
})
export class DotExperimentScheduleFilterComponent {
    /** Lower bound in force, as a local calendar date; `null` leaves that side open. */
    readonly $from = input<string | null>(null, { alias: 'from' });

    /** Upper bound in force. */
    readonly $to = input<string | null>(null, { alias: 'to' });

    /** Emits the whole period: the calendar hands over a range, never one bound at a time. */
    readonly selectionChange = output<ExperimentsListSchedulePeriod>();

    readonly #dotMessageService = inject(DotMessageService);

    /**
     * Flattens the inline picker's own panel chrome so it reads as one surface with the popover
     * instead of a panel inside a panel — the picker draws its own border, rounding and shadow,
     * and the popover already provides all three.
     *
     * The same treatment `dot-field-filter` applies in `@dotcms/ui`, deliberately copied rather
     * than shared: it is two Tailwind overrides, and a styling token exported across libs for two
     * consumers buys less than it costs. A third consumer is the moment to extract it.
     *
     * The width cap is not decoration either: the month and year grids are wider than the day
     * grid, so without it the panel grows when one is opened and the popover jumps sideways.
     */
    protected readonly datePickerPt = {
        panel: { class: '!border-0 !rounded-none !shadow-none !max-w-96' }
    };

    protected readonly $title = computed<string>(() =>
        this.#dotMessageService.get('experiments.list.filter.schedule')
    );

    /** What the chip reads while unfiltered. Not a range of its own — see the class note. */
    protected readonly $emptyLabel = computed<string>(() =>
        this.#dotMessageService.get('experiments.list.filter.schedule.any')
    );

    /**
     * What the chip renders, and what makes it read as active.
     *
     * Three phrasings rather than one with blanks: a chip reading "Scheduled 2026-06-01 to" while
     * the second end is still being picked looks broken, and an open upper bound is a state the
     * calendar passes through every time.
     */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const from = this.$from();
        const to = this.$to();

        if (from && to) {
            return [
                this.#dotMessageService.get('experiments.list.filter.schedule.range', from, to)
            ];
        }

        if (from) {
            return [
                this.#dotMessageService.get('experiments.list.filter.schedule.from-only', from)
            ];
        }

        if (to) {
            return [this.#dotMessageService.get('experiments.list.filter.schedule.to-only', to)];
        }

        return [];
    });

    /**
     * Bound to the calendar, which speaks `Date` while the address speaks days.
     *
     * Re-seeds from the applied period whenever the parent changes it (URL hydration,
     * back/forward), while staying writable by the calendar.
     */
    protected readonly $range = linkedSignal<Date[] | null>(() =>
        toCalendarRange(parseScheduleBound(this.$from()), parseScheduleBound(this.$to()))
    );

    /**
     * The calendar's range changed.
     *
     * PrimeNG emits `[start]`, then `[start, end]`, and `null` when cleared, so a half-picked
     * range arrives here as a real state rather than as an error — and it is applied, because "on
     * or after this date" is a useful filter in its own right (FR-020).
     */
    onRangeChange(dates: (Date | null)[] | null): void {
        const [from, to] = dates ?? [];
        this.$range.set(toCalendarRange(from ?? null, to ?? null));

        this.selectionChange.emit({
            from: from ? formatScheduleBound(from) : null,
            to: to ? formatScheduleBound(to) : null
        });
    }

    onRemove(): void {
        this.onRangeChange(null);
    }
}

/**
 * The calendar's own model for a period.
 *
 * `null` when empty, and **not** `[null, null]`: PrimeNG's range branch tests `value.length` — two
 * nulls pass that — and then reads `value[0].getTime()`, so a two-null array throws on the very
 * first click and the picker never selects anything. This is not defensive padding; it is the
 * shape the component contract requires, and it cost a browser session to find because a unit test
 * asserting the model's shape asserts our assumption rather than PrimeNG's.
 *
 * A half-picked period is a one-element array, which is also what the picker produces itself after
 * the first click.
 *
 * An open **lower** bound cannot be drawn at all — range mode has no way to express "up to this
 * day, from anywhere" — so the calendar renders unselected while the chip still names the bound
 * and the filter still applies. Only an address can reach that state; picking on the calendar
 * always sets the lower bound first.
 */
function toCalendarRange(from: Date | null, to: Date | null): Date[] | null {
    if (!from) {
        return null;
    }

    return to ? [from, to] : [from];
}
