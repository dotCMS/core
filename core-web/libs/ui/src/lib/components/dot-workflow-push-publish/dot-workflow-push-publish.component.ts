import { catchError, of, take } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';

import {
    DotFormatDateService,
    DotMessageService,
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@dotcms/data-access';
import { DotcmsConfigService, DotTimeZone } from '@dotcms/dotcms-js';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { PushPublishEnvSelectorComponent } from '../dot-push-publish-env-selector/dot-push-publish-env-selector.component';

/** What the user can ask a push publish for. Values are the backend's `iWantTo` vocabulary. */
export type DotWorkflowPushPublishAction = 'publish' | 'expire' | 'publishexpire';

/**
 * The push publish payload, in the shape the backend's `PushPublishBean` expects.
 *
 * Emitted already converted — dates split into `yyyy-MM-dd` + `HH-mm` pairs, environments comma-joined
 * into `whereToSend` — so a consumer can hand it straight to a fire request without repeating the
 * transformation that `DotWorkflowEventHandlerService.processWorkflowPayload` does today.
 */
export interface DotWorkflowPushPublishValue {
    /** Comma-joined environment ids. */
    whereToSend: string;
    iWantTo: DotWorkflowPushPublishAction;
    publishDate: string;
    publishTime: string;
    expireDate: string;
    expireTime: string;
    filterKey: string;
    timezoneId: string;
}

/**
 * Collects a workflow action's `pushPublish` input: where to send it, when, and under which filter.
 *
 * **Deliberately not the legacy `DotPushPublishFormComponent`.** That one is bound to the single-item
 * push publish *dialog*: it takes a `DotPushPublishDialogData` (asset identifier, `restricted`, `cats`,
 * `removeOnly`), and when that data carries `customCode` it replaces the entire form with plugin-supplied
 * HTML parsed into a sibling div. Neither belongs in a step embedded in someone else's dialog — a
 * plugin's markup rendering inside a host's frame has no contract about size or validity. The env
 * selector *is* shared with it, so the two cannot drift on how environments are chosen.
 *
 * The `iWantTo` choice drives which fields matter: expiring needs no publish date and takes no filter,
 * publishing needs no expiry. Rather than enabling and disabling controls, this computes what is
 * required and what gets emitted, so the value is always consistent with the choice.
 *
 * Presentational and dialog-free: it emits its value and validity outward and renders no header, footer
 * or submit control.
 */
@Component({
    selector: 'dot-workflow-push-publish',
    imports: [
        FormsModule,
        SelectModule,
        SelectButtonModule,
        DatePickerModule,
        PushPublishEnvSelectorComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-workflow-push-publish.component.html',
    providers: [DotPushPublishFiltersService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotWorkflowPushPublishComponent {
    readonly #filtersService = inject(DotPushPublishFiltersService);
    readonly #configService = inject(DotcmsConfigService);
    readonly #formatDateService = inject(DotFormatDateService);
    readonly #dotMessageService = inject(DotMessageService);

    /** Freezes every field, used while an action is in flight. */
    readonly disabled = input<boolean>(false);

    /** The collected payload, emitted on every change. */
    readonly valueChange = output<DotWorkflowPushPublishValue>();
    /** Whether the payload is complete enough to fire; lets a host gate its own Continue. */
    readonly validChange = output<boolean>();

    protected readonly $iWantTo = signal<DotWorkflowPushPublishAction>('publish');
    protected readonly $publishDate = signal<Date>(new Date());
    protected readonly $expireDate = signal<Date>(new Date());
    protected readonly $filterKey = signal<string>('');
    protected readonly $timezoneId = signal<string>('');
    protected readonly $environments = signal<string[]>([]);

    /** Reveals the timezone select, which is collapsed behind a link until asked for. */
    protected readonly $showTimezone = signal<boolean>(false);

    protected readonly $filterOptions = signal<SelectItem[]>([]);
    protected readonly $timezoneOptions = signal<SelectItem[]>([]);

    /**
     * Yesterday, matching the legacy form.
     *
     * Not today: a user in a timezone behind the server's would otherwise be unable to pick their own
     * current day.
     */
    protected readonly minDate = ((): Date => {
        const date = new Date();
        date.setDate(date.getDate() - 1);

        return date;
    })();

    protected readonly pushActions: SelectItem[] = [
        {
            label: this.#dotMessageService.get('contenttypes.content.push_publish.action.push'),
            value: 'publish'
        },
        {
            label: this.#dotMessageService.get('contenttypes.content.push_publish.action.remove'),
            value: 'expire'
        },
        {
            label: this.#dotMessageService.get(
                'contenttypes.content.push_publish.action.pushremove'
            ),
            value: 'publishexpire'
        }
    ];

    /** Expiring needs no publish date. */
    protected readonly $needsPublishDate = computed(() => this.$iWantTo() !== 'expire');
    /** Publishing needs no expiry date. */
    protected readonly $needsExpireDate = computed(() => this.$iWantTo() !== 'publish');
    /**
     * A filter shapes what gets pushed, so it is meaningless when only expiring.
     *
     * The legacy form clears the value as well as hiding the control; this emits `''` for the same
     * reason — a filter left over from a previous choice must not ride along.
     */
    protected readonly $filtersEnabled = computed(() => this.$iWantTo() !== 'expire');

    /** The local timezone's label, shown beside the reveal link. */
    protected readonly $localTimezoneLabel = computed(
        () =>
            this.$timezoneOptions().find((option) => option.value === this.$timezoneId())?.label ??
            ''
    );

    /**
     * Valid once at least one environment is chosen and every date the choice needs is set.
     *
     * An environment is the one thing with no sensible default: dates default to now and the filter to
     * the server's default, but there is no "somewhere" to push to.
     */
    protected readonly $valid = computed(() => {
        if (!this.$environments().length) {
            return false;
        }

        if (this.$needsPublishDate() && !this.$publishDate()) {
            return false;
        }

        return !(this.$needsExpireDate() && !this.$expireDate());
    });

    constructor() {
        this.loadFilters();
        this.loadTimezones();

        // One effect publishes both outputs, so a host can never see a value and a validity that
        // disagree about the same state.
        effect(() => {
            this.valueChange.emit(this.buildValue());
            this.validChange.emit(this.$valid());
        });
    }

    protected onIWantToChange(iWantTo: DotWorkflowPushPublishAction): void {
        this.$iWantTo.set(iWantTo);
    }

    protected onEnvironmentsChange(environments: string[]): void {
        this.$environments.set(environments ?? []);
    }

    protected toggleTimezone(event: Event): void {
        event.preventDefault();
        this.$showTimezone.update((shown) => !shown);
    }

    /**
     * Assembles the backend payload.
     *
     * Both date pairs are always sent, even the one the choice does not need — which is what the legacy
     * path does, and the backend reads only the ones `iWantTo` makes relevant.
     */
    private buildValue(): DotWorkflowPushPublishValue {
        const publishDate = this.$publishDate() ?? new Date();
        const expireDate = this.$expireDate() ?? new Date();

        return {
            whereToSend: this.$environments().join(),
            iWantTo: this.$iWantTo(),
            publishDate: this.#formatDateService.format(publishDate, 'yyyy-MM-dd'),
            publishTime: this.#formatDateService.format(publishDate, 'HH-mm'),
            expireDate: this.#formatDateService.format(expireDate, 'yyyy-MM-dd'),
            expireTime: this.#formatDateService.format(expireDate, 'HH-mm'),
            filterKey: this.$filtersEnabled() ? this.$filterKey() : '',
            timezoneId: this.$timezoneId()
        };
    }

    /**
     * Loads the push publish filters and arms the server's default.
     *
     * A failed lookup leaves no options and no filter, which the backend treats as "no filter" — the
     * step stays usable rather than blocking the action behind an error.
     */
    private loadFilters(): void {
        this.#filtersService
            .get()
            .pipe(
                take(1),
                catchError(() => of([] as DotPushPublishFilter[]))
            )
            .subscribe((filters) => {
                this.$filterOptions.set(
                    filters.map((item) => ({ label: item.title, value: item.key }))
                );
                this.$filterKey.set(filters.find(({ defaultFilter }) => defaultFilter)?.key ?? '');
            });
    }

    /** Loads timezones and defaults to the browser's, which is what the user means by "now". */
    private loadTimezones(): void {
        this.#configService
            .getTimeZones()
            .pipe(
                take(1),
                catchError(() => of([] as DotTimeZone[]))
            )
            .subscribe((timezones) => {
                this.$timezoneOptions.set(
                    timezones.map((item) => ({ label: item.label, value: item.id }))
                );

                const local = Intl.DateTimeFormat().resolvedOptions().timeZone;
                // Only default when the browser's zone is one the server knows; otherwise leave it
                // unset rather than guessing a zone the backend would reject.
                if (timezones.some((item) => item.id === local)) {
                    this.$timezoneId.set(local);
                }
            });
    }
}
