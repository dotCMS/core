import { injectDispatch } from '@ngrx/signals/events';
import { Observable, of } from 'rxjs';

import { Component, computed, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';

import { DotContentSearchService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { isDotIdentifier } from '@dotcms/utils';

import {
    EXPERIMENTS_URL,
    RESULTS_CONFIRM_DIALOG_KEY,
    STATUS_LABEL_KEYS,
    STATUS_SEVERITIES
} from '../../../shared/constants';
import { DotExperimentConfigurePage, TagSeverity } from '../../../shared/models';
import { dotExperimentsResultsPageEvents } from '../../../store/dot-experiments-results-page.events';
import { DotExperimentsResultsStore } from '../../../store/dot-experiments-results.store';
import { toConfigurePage } from '../../../util/dot-experiments-configure.util';
import { configureCommandsOf, variantsCount } from '../../../util/dot-experiments-list.util';

/** Shape of the `/api/content/_search` entity the page lookup reads contentlets from. */
interface PageLookupEntity {
    jsonObjectView?: { contentlets?: DotCMSContentlet[] };
}

/**
 * Separator of the three parts of the subline: middle dot U+00B7 with a space either side, as the
 * design specifies. Not the en dash the Period uses, and not a pipe.
 */
const SUBLINE_SEPARATOR = ' · ';

/** Trailing part of the subline: the variant count, always in its plural form. */
const SUBLINE_VARIANTS_KEY = 'experiments.results.header.variants';

/**
 * Header of the Results screen: back, name, status, the page the experiment runs on, and the two
 * actions this screen offers.
 *
 * The store is injected rather than received through inputs — the Results shell provides it, so
 * every part of the screen reads the same instance.
 *
 * Only Stop is raised from here, and only while the experiment is RUNNING (AC3). The confirmation
 * goes to the shell's `p-confirmDialog` by key, as the Configure header's does: this component
 * renders no dialog of its own, and the toast that follows belongs to the shell, which listens for
 * the API event.
 */
@Component({
    selector: 'dot-experiments-results-header',
    imports: [ButtonModule, TagModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-experiments-results-header.component.html',
    host: {
        class: 'flex flex-none items-center justify-between gap-6 border-b border-surface-200 bg-white px-8 py-4'
    }
})
export class DotExperimentsResultsHeaderComponent {
    readonly store = inject(DotExperimentsResultsStore);

    readonly $title = computed<string>(() => this.store.experiment()?.name ?? '');

    readonly $statusSeverity = computed<TagSeverity>(
        () => STATUS_SEVERITIES[this.store.$status()] ?? 'secondary'
    );

    readonly $statusLabelKey = computed<string>(
        () => STATUS_LABEL_KEYS.get(this.store.$status()) ?? ''
    );

    /** Stopping ends data collection, so it only applies while data is being collected (AC3). */
    readonly $showStop = computed<boolean>(
        () => this.store.$status() === DotExperimentStatus.RUNNING
    );

    readonly #dispatch = injectDispatch(dotExperimentsResultsPageEvents);
    readonly #router = inject(Router);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #contentSearchService = inject(DotContentSearchService);

    /**
     * The page the experiment runs on, resolved from its `pageId`.
     *
     * `DotExperiment` carries the identifier and nothing else — no title, no path — so the same
     * content search the list uses for its Page column resolves them here. Ancillary to the
     * screen: a page that cannot be resolved leaves the subline reading the variant count alone
     * rather than blocking a report that is otherwise complete.
     */
    readonly #page = toSignal(
        toObservable(computed<string | null>(() => this.store.experiment()?.pageId ?? null)).pipe(
            distinctUntilChanged(),
            switchMap((pageId) => this.#lookupPage(pageId))
        ),
        { initialValue: null }
    );

    /** `{pageTitle} · {pagePath} · {n} Variants`, dropping whichever parts are not known yet (AC2). */
    readonly $subline = computed<string>(() => {
        const page = this.#page();
        const variants = variantsCount(this.store.experiment()?.trafficProportion);

        return [
            page?.title,
            page?.path,
            this.#dotMessageService.get(SUBLINE_VARIANTS_KEY, String(variants))
        ]
            .filter(Boolean)
            .join(SUBLINE_SEPARATOR);
    });

    /** Leaves the Results screen for the list. */
    onBackToList(): void {
        this.#router.navigate([EXPERIMENTS_URL]);
    }

    /** Opens the Configure screen of the experiment being reported on (AC2). */
    onConfiguration(): void {
        const experimentId = this.store.experiment()?.id;

        if (experimentId) {
            this.#router.navigate(configureCommandsOf(experimentId));
        }
    }

    /**
     * Asks before ending the experiment, then hands the transition to the store.
     *
     * The copy says what ending costs — data collection stops there and then — since the button is
     * only reachable while sessions are still being counted (AC3).
     *
     * Raised on the shell's `p-confirmDialog` by `RESULTS_CONFIRM_DIALOG_KEY`, as the Configure
     * header raises its own: this component renders no dialog. The key is what keeps the two
     * confirmations of this screen apart — the summary table mounts an unkeyed dialog for Promote,
     * so a keyed request reaches this one and only this one (AC21).
     */
    confirmStop(): void {
        this.#confirmationService.confirm({
            key: RESULTS_CONFIRM_DIALOG_KEY,
            header: this.#dotMessageService.get('experiments.action.stop-experiment'),
            message: this.#dotMessageService.get('experiments.results.stop.confirm-message'),
            acceptLabel: this.#dotMessageService.get('experiments.action.end'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            rejectButtonStyleClass: 'p-button-secondary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.#dispatch.stopRequested()
        });
    }

    /**
     * Resolves a page identifier to the title and path the subline renders.
     *
     * The identifier is concatenated into a Lucene query, so anything outside the identifier shape
     * is answered as "not found" rather than widening the search — same guard the Configure screen
     * applies to the `?pageId=` it is handed.
     */
    #lookupPage(pageId: string | null): Observable<DotExperimentConfigurePage | null> {
        if (!pageId || !isDotIdentifier(pageId)) {
            return of(null);
        }

        return this.#contentSearchService
            .get<PageLookupEntity>({
                query: `+contentType:htmlpageasset +working:true +identifier:${pageId}`,
                limit: 1
            })
            .pipe(
                map((entity) => entity?.jsonObjectView?.contentlets?.[0]),
                map((contentlet) => (contentlet ? toConfigurePage(contentlet) : null)),
                catchError(() => of(null))
            );
    }
}
