import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { EMPTY, Subscription } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotContentSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    AgentHeartbeat,
    AgentProgress,
    AgentRunStep,
    DotCMSContentlet
} from '@dotcms/dotcms-models';
import { DotPageScannerService, PageScannerA11yResponse } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';

import { A11yGroup, buildA11yGroups } from '../models/a11y-groups';
import {
    impactToSeverity,
    SEVERITY_RANK,
    severityBreakdown,
    type SeverityCounts
} from '../models/a11y-severity';
import {
    AgentFixRequest,
    FixReport,
    FixResult,
    NEEDS_ATTENTION_STATUSES,
    RESEARCH_RULE_ID,
    StudioPageRow,
    StudioPhase
} from '../models/accessibility-studio.models';
import { DotA11yAgentService } from '../services/dot-a11y-agent.service';

/**
 * Status of rehydrating the selected page from a deep link (`/agents/a11y/<path>`
 * opened cold): `loading` while the single-page fetch is in flight, `not-found`
 * when the path resolves to no page (so the run screen can bounce back to the
 * picker), `idle` otherwise.
 */
type RehydrateStatus = 'idle' | 'loading' | 'not-found';

interface A11yRunState {
    /** Studio state machine (§7). Starts at `ready` (a page is being opened). */
    phase: StudioPhase;
    /** The page this run is against. */
    selected: StudioPageRow | null;
    /** Deep-link rehydration status for {@link A11yRunStore.openPageByUri}. */
    rehydrateStatus: RehydrateStatus;
    /** Per-run opt-out: when true, the agent reports CSS contrast instead of fixing it (§3). */
    skipCss: boolean;
    /**
     * The real axe scan result of the PREVIEW (working) render — populated by
     * runScan() via DotPageScannerService. This is the scan that owns the whole
     * UI: score donut, issue list, fix flow, and the preview-frame markers.
     */
    scanResult: PageScannerA11yResponse | null;
    /**
     * The axe scan result of the LIVE (published) render. Used ONLY for the
     * side-by-side comparison — it draws the live-frame marker layer so the user
     * sees the violations that still exist on the published page (which may lag
     * the preview when fixes aren't published yet). Feeds no other widget.
     */
    liveScanResult: PageScannerA11yResponse | null;
    /** Live agent activity log — appended from SSE `phase` events during a fix run. */
    steps: AgentRunStep[];
    /**
     * Live violation count from SSE `progress` events — the authoritative running
     * score while fixing (baseline → current, cleared so far). Null until the
     * first progress frame arrives.
     */
    progress: AgentProgress | null;
    /**
     * The current run's id — captured from the stream's first `run` event, used to
     * target the /stop request at this specific run. Null when no run is active.
     */
    runId: string | null;
    /**
     * Latest keep-alive tick from SSE `heartbeat` events — how long the run and the
     * current action have been going. Drives the "still working…" indicator so a
     * long, quiet step (a model call) doesn't look hung. Null between runs.
     */
    heartbeat: AgentHeartbeat | null;
    /** Set when a fix run fails — surfaced inline so the user can retry. */
    fixError: string | null;
    /** The §6 run report — populated when the fix pass completes (SSE `done`). */
    report: FixReport | null;
    /**
     * Monotonic counter bumped whenever the working (preview) render changes — each
     * mid-fix re-scan and the terminal report. The preview iframe keys its URL off
     * this so it reloads to show the agent's applied fixes visually. Not otherwise
     * meaningful; only its changes matter.
     */
    previewRevision: number;
}

const initialState: A11yRunState = {
    phase: 'ready',
    selected: null,
    rehydrateStatus: 'idle',
    skipCss: false,
    scanResult: null,
    liveScanResult: null,
    steps: [],
    progress: null,
    runId: null,
    heartbeat: null,
    fixError: null,
    report: null,
    previewRevision: 0
};

/** The run-state reset shared by opening a page and starting a fresh scan. */
const runReset = (): Partial<A11yRunState> => ({
    scanResult: null,
    liveScanResult: null,
    steps: [],
    progress: null,
    runId: null,
    heartbeat: null,
    fixError: null,
    report: null,
    previewRevision: 0
});

/** Projects a search contentlet into the run's page shape. */
function toPageRow(content: DotCMSContentlet): StudioPageRow {
    return {
        identifier: content.identifier,
        title: content.title || content.url || content.identifier,
        // Prefer the urlMap for URL-mapped content (e.g. Blog): it's the real
        // navigable path visitors use. Fall back to `url` for plain pages.
        path: content['urlMap'] || content.url || '',
        type: content.contentType,
        languageId: content.languageId,
        hostId: content.host,
        hostName: content.hostName,
        modDate: content.modDate,
        modUserName: content.modUserName,
        live: !!content.live
    };
}

/**
 * The Accessibility Studio **run** store — owns a single page's scan / fix /
 * review / publish lifecycle for the run route (`agents/a11y/<page-path>`). It's
 * provided at {@link DotA11yRunComponent}, so navigating to a different page
 * destroys and recreates it → fresh state per page, no manual reset needed.
 *
 * The page it runs against comes from the URL: {@link openPageByUri} rehydrates
 * the selection from the path on cold load / refresh. The picker never hands
 * state to this store — the URL is the single source of truth (see [[A11yPickerStore]]).
 */
export const A11yRunStore = signalStore(
    withState<A11yRunState>(initialState),
    withComputed((store) => {
        // Hoisted so every derived count reads ONE memoized traversal of the axe
        // payload. Sibling computeds in the returned literal can't reference each
        // other, so the shared derivations live here.
        const a11yGroups = computed<A11yGroup[]>(() => buildA11yGroups(store.scanResult()));
        const errorGroups = computed<A11yGroup[]>(() =>
            a11yGroups().filter((g) => g.type === 'error')
        );
        const warningGroups = computed<A11yGroup[]>(() =>
            a11yGroups().filter((g) => g.type === 'warning')
        );
        /** Element count across groups (a group's `count` is its flagged-node total). */
        const elementCount = (groups: A11yGroup[]) =>
            groups.reduce((total, g) => total + g.count, 0);

        return {
            // `phase` IS the interface for single-state questions — consumers compare
            // it directly (`phase() === 'scanning'`) rather than going through a
            // per-phase boolean. What lives here is only the phase SETS: groups that
            // carry domain meaning an enum comparison can't express, and that would
            // otherwise be spelled out inline in every consumer.
            /** A scan or fix run is in flight (the working copy may still be changing). */
            isWorking: computed(() => ['scanning', 'fixing'].includes(store.phase())),
            /** A scan has produced (or is producing) results, so the score is meaningful. */
            hasResults: computed(() =>
                ['scanned', 'fixing', 'done', 'published'].includes(store.phase())
            ),
            /** A fix run has started, so before→after figures are meaningful. */
            runStarted: computed(() => ['fixing', 'done', 'published'].includes(store.phase())),
            /** The run reached a terminal state — its report is final. */
            finished: computed(() => ['done', 'published'].includes(store.phase())),
            /** Real axe findings grouped per rule (violations → error, incomplete → warning). */
            a11yGroups,
            /**
             * The LIVE (published) render's findings, grouped per rule — drives ONLY the
             * live-frame marker layer for the side-by-side comparison. Empty until the
             * live scan lands (it runs alongside the preview scan on Scan / Re-scan).
             */
            liveA11yGroups: computed<A11yGroup[]>(() => buildA11yGroups(store.liveScanResult())),
            /** Real axe error-element count (confirmed violations). */
            errorCount: computed(() => elementCount(errorGroups())),
            /** Real axe warning-element count (incomplete / needs review). */
            warningCount: computed(() => elementCount(warningGroups())),
            /**
             * Axe `incomplete` groups (needs manual review) — one per rule, sorted by
             * occurrence count. The agent doesn't fix these (axe couldn't confirm them),
             * so the panel lists them separately with an explanation.
             */
            reviewGroups: computed<A11yGroup[]>(() =>
                [...warningGroups()].sort((a, b) => b.count - a.count)
            ),
            /**
             * The BASELINE violation count — the "before" side of the before→after
             * comparison. It must stay pinned to the ORIGINAL scan even as the preview
             * is re-scanned mid-fix (which mutates `scanResult` and would otherwise drag
             * this down). Source order: the report's frozen `scan.before` once the run
             * completes; the agent's `progress.baseline` while fixing; otherwise the
             * pre-run scan's error count.
             */
            beforeCount: computed(() => {
                const report = store.report();
                if (report) {
                    return report.scan.before.violations;
                }
                const progress = store.progress();
                if (store.phase() === 'fixing' && progress) {
                    return progress.baseline;
                }
                return elementCount(errorGroups());
            }),
            /** Violations remaining after the fix pass. */
            afterCount: computed(() => store.report()?.scan.after.violations ?? 0),
            /**
             * The applied fixes, one row per distinct edit. The agent emits a
             * `fixed-to-working` row per violating ELEMENT, so a single CSS edit matching 5
             * elements arrives 5 times — deduped here on rule + file + diff (the diff text
             * already carries its own "[5 instances]" note, so nothing is lost). Research
             * rows are excluded: they're a step, not a fix.
             */
            fixedResults: computed<FixResult[]>(() => {
                const rows =
                    store
                        .report()
                        ?.results.filter(
                            (r) => r.status === 'fixed-to-working' && r.ruleId !== RESEARCH_RULE_ID
                        ) ?? [];

                return [
                    ...new Map(
                        rows.map((r) => [`${r.ruleId}|${r.file ?? ''}|${r.diff ?? ''}`, r])
                    ).values()
                ];
            }),
            reportedResults: computed<FixResult[]>(
                () =>
                    store
                        .report()
                        ?.results.filter((r) => NEEDS_ATTENTION_STATUSES.includes(r.status)) ?? []
            ),
            latestStep: computed<AgentRunStep | null>(() => {
                const steps = store.steps();
                return steps.length ? steps[steps.length - 1] : null;
            }),
            /**
             * Confirmed-violation groups (axe `error`s), one per rule, sorted for the
             * "BY ISSUE TYPE" list: highest severity first, then most occurrences.
             */
            issueTypeRows: computed<A11yGroup[]>(() => {
                const rank = (g: A11yGroup) => SEVERITY_RANK[impactToSeverity(g.impact)];
                return [...errorGroups()].sort((a, b) => rank(a) - rank(b) || b.count - a.count);
            }),
            /** Open issues broken down by severity (element counts) — drives the donut + legend. */
            severityCounts: computed<SeverityCounts>(() => severityBreakdown(errorGroups())),
            /**
             * Live "open" count for the score widget. After the run finishes it's the
             * report's authoritative after-count; while fixing it's the live count from
             * the agent's `progress` stream (`current`) so the donut animates down as
             * fixes land; before any run it's the scan's before-count.
             */
            openCount: computed<number>(() => {
                const report = store.report();
                if (report) {
                    return report.scan.after.violations;
                }
                if (store.phase() === 'fixing') {
                    // Authoritative live count straight from the agent's `progress`
                    // events. Before the first progress frame lands, fall back to the
                    // baseline (the scan's before-count).
                    const progress = store.progress();
                    if (progress) {
                        return Math.max(0, progress.current);
                    }
                }
                return elementCount(errorGroups());
            }),
            /**
             * How many violations have been cleared. While fixing it's the agent's live
             * `progress.cleared`; once the run completes it's derived from the report's
             * before/after rescan (`before - after`).
             *
             * Deliberately NOT a count of `fixed-to-working` rows: a run makes two passes
             * (deterministic, then agentic), and the rows only log the deterministic pass —
             * one row per violating element, so a single CSS edit fanning out to 5 elements
             * emits 5 rows. Fixes the agentic pass lands have no row at all. The rescan is
             * the only number that reflects both passes.
             */
            fixedCount: computed<number>(() => {
                const report = store.report();
                if (report) {
                    return Math.max(
                        0,
                        report.scan.before.violations - report.scan.after.violations
                    );
                }
                if (store.phase() === 'fixing') {
                    return Math.max(0, store.progress()?.cleared ?? 0);
                }
                return 0;
            }),
            /**
             * How many violations still need attention — the rescan's after-count, not a
             * row count. `reported` rows are a pass-1 handoff marker ("the agentic pass will
             * take this"), not an outcome, so counting them reported work the agent then went
             * on to fix. Only the post-both-passes rescan knows what actually survived.
             */
            reportedCount: computed<number>(() => store.report()?.scan.after.violations ?? 0)
        };
    }),
    withMethods((store) => {
        const contentSearchService = inject(DotContentSearchService);
        const scannerService = inject(DotPageScannerService);
        const agentService = inject(DotA11yAgentService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const globalStore = inject(GlobalStore);

        // The in-flight scan / fix-stream subscription, held so Stop can cancel it
        // (unsubscribing aborts the underlying fetch). Not reactive UI state.
        let activeSub: Subscription | null = null;
        // The comparison-only LIVE scan, tracked separately so Stop cancels it too
        // without coupling it to the UI-driving preview scan's lifecycle.
        let liveScanSub: Subscription | null = null;
        // The mid-fix re-scan of the PREVIEW render, triggered by `progress` frames
        // so the ring + per-severity legend track the agent's live fixes. Held so a
        // newer progress frame supersedes an in-flight one (no stampede / stale writes).
        let fixRescanSub: Subscription | null = null;

        /**
         * The dotCMS backend origin the agent must render + call against. In prod the
         * portlet is served FROM the dotCMS origin, so `window.location.origin` is
         * already correct and the agent trusts it verbatim. The dev split below is the
         * ONLY adjustment — see backendOrigin().
         */
        function backendOrigin(): string {
            // ====================================================================
            // ⚠️ DEV-ONLY HACK — REMOVE BEFORE PRODUCTION ⚠️
            // --------------------------------------------------------------------
            // The Angular dev server runs on :4200, but the agent (and the dotCMS
            // scanner it drives) render/call the page server-side and can only reach
            // the BE on :8080. Rewrite the dev-server origin → the BE origin so the
            // agent receives a backend-reachable dotcmsBaseUrl. Mirrors the same
            // :4200→:8080 hack in DotPageScannerService.checkA11y. In prod there is
            // no split, so this is a no-op. MUST be replaced by an env-aware origin.
            // ====================================================================
            return window.location.origin.replace('4200', '8080');
        }

        /**
         * Build the absolute URL the scanner renders + checks. It must be on the
         * backend origin (never the content-site hostname, which may not be publicly
         * reachable) with `host_id` to disambiguate the site.
         *
         * `mode` selects which render to scan:
         *   - `EDIT_MODE` (default) — the working version. NOTE: DotPageScannerService
         *     rewrites EDIT_MODE → PREVIEW_MODE at its chokepoint (editor chrome would
         *     produce phantom violations), so this scans the PREVIEW render — the same
         *     one the left "with fixes" frame shows. This is the scan that owns the UI.
         *   - `LIVE` — the published render, for the comparison-only live scan. `LIVE`
         *     passes through the scanner untouched.
         * Mirrors DotEmaShellComponent.handleScannerToolClick.
         */
        function buildScanUrl(
            page: StudioPageRow,
            mode: 'EDIT_MODE' | 'LIVE' = 'EDIT_MODE'
        ): string {
            const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
            const url = new URL(path, backendOrigin());
            url.searchParams.set('host_id', page.hostId);
            url.searchParams.set('language_id', String(page.languageId));
            url.searchParams.set('mode', mode);
            return url.toString();
        }

        /**
         * Re-scan the PREVIEW render mid-fix and fold the fresh axe result into
         * `scanResult`, so the score ring, per-severity legend, and issue list track
         * the agent's live fixes as they land. Triggered by each `progress` frame.
         *
         * A newer call supersedes an in-flight one (unsubscribe aborts it), so bursts
         * of progress frames don't stampede the scanner or write stale results. Stays
         * in the `fixing` phase throughout; failures are swallowed (the run continues
         * and the next progress frame retries). Guarded to the fixing phase so a late
         * response after done/abort can't overwrite the report-driven widgets.
         */
        function rescanPreviewDuringFix() {
            const page = store.selected();
            if (store.phase() !== 'fixing' || !page) {
                return;
            }
            fixRescanSub?.unsubscribe();
            fixRescanSub = scannerService
                .checkA11y(buildScanUrl(page, 'EDIT_MODE'))
                .pipe(
                    take(1),
                    catchError(() => EMPTY)
                )
                .subscribe((scanResult) => {
                    // Only apply if we're still fixing (a done/abort may have landed).
                    if (store.phase() === 'fixing') {
                        // Bump previewRevision so the preview iframe reloads and shows
                        // the fixes that this re-scan just picked up.
                        patchState(store, {
                            scanResult,
                            previewRevision: store.previewRevision() + 1
                        });
                    }
                });
        }

        /** Open a page → studio "ready" (waits for the user to scan). */
        function openPage(selected: StudioPageRow) {
            patchState(store, {
                selected,
                phase: 'ready',
                rehydrateStatus: 'idle',
                ...runReset()
            });
        }

        return {
            setSkipCss(skipCss: boolean) {
                patchState(store, { skipCss });
            },

            /**
             * Rehydrate the selected page from a deep link (`/agents/a11y/<path>`
             * opened cold or refreshed): fetch the single page by its URI, then
             * `openPage` it → run screen "ready". If the path resolves to no page,
             * flag `not-found` so the run screen can bounce back to the picker.
             *
             * A no-op only when the SAME page under the SAME site is already
             * selected, so in-session navigation into the run route doesn't refetch
             * or reset an in-progress run. But a site change with the same path must
             * re-resolve (the path points at a different page per site) — the caller
             * re-runs this on `currentSiteId` changes, so the host check reloads it.
             *
             * Paths are unique per-site, so the lookup is host-scoped — matching the
             * picker query. Until the current site is known the lookup would be
             * ambiguous, so we wait (the run effect re-runs when the site resolves).
             */
            openPageByUri(uri: string) {
                const siteId = globalStore.currentSiteId();
                if (store.selected()?.path === uri && store.selected()?.hostId === siteId) {
                    return;
                }
                if (!siteId) {
                    // Site not resolved yet — stay in loading; the caller re-runs.
                    patchState(store, { rehydrateStatus: 'loading' });

                    return;
                }
                patchState(store, { rehydrateStatus: 'loading' });

                // Escape Lucene special characters in the path (it comes from the URL).
                const safeUri = uri.replace(/[+\-&|!(){}[\]^"~*?:\\]/g, '\\$&');
                // Match the picker query shape (pages + urlmapped, working, not
                // deleted, host-scoped) but pinned to this exact path / urlmap.
                const query =
                    `+working:true +(urlmap:* OR basetype:5) +deleted:false ` +
                    `+conhost:${siteId} +(path:"${safeUri}" OR urlmap:"${safeUri}")`;

                contentSearchService
                    .get<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>({
                        query,
                        limit: 1,
                        offset: 0
                    })
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { rehydrateStatus: 'not-found' });

                            return EMPTY;
                        })
                    )
                    .subscribe((entity) => {
                        const content = entity?.jsonObjectView?.contentlets?.[0];
                        if (!content) {
                            patchState(store, { rehydrateStatus: 'not-found' });

                            return;
                        }
                        // openPage resets rehydrateStatus → 'idle'.
                        openPage(toPageRow(content));
                    });
            },

            /**
             * Run the REAL axe scan via DotPageScannerService against the page's
             * EDIT_MODE render, then store the result and move to "scanned".
             */
            runScan() {
                const page = store.selected();
                // Allow scanning from `ready` (first scan) and `scanned` (the re-scan
                // button) — both transition into `scanning`.
                if ((store.phase() !== 'ready' && store.phase() !== 'scanned') || !page) {
                    return;
                }
                // Drop any prior scan/report so the widgets reflect the fresh scan.
                patchState(store, {
                    phase: 'scanning',
                    scanResult: null,
                    liveScanResult: null,
                    report: null,
                    fixError: null
                });

                // Primary scan — the PREVIEW (working) render. Owns the phase + all
                // widgets. Failure returns to `ready` so the user can retry.
                activeSub = scannerService
                    .checkA11y(buildScanUrl(page, 'EDIT_MODE'))
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            // Return to ready so the user can retry the scan.
                            patchState(store, { phase: 'ready' });

                            return EMPTY;
                        })
                    )
                    .subscribe((scanResult) => {
                        patchState(store, { scanResult, phase: 'scanned' });
                    });

                // Comparison-only scan — the LIVE (published) render. Draws the
                // live-frame markers and nothing else, so its failure must NOT touch
                // the phase or surface an error dialog: swallow it and leave the live
                // markers empty. Runs in parallel with the primary scan.
                liveScanSub = scannerService
                    .checkA11y(buildScanUrl(page, 'LIVE'))
                    .pipe(
                        take(1),
                        catchError(() => EMPTY)
                    )
                    .subscribe((liveScanResult) => {
                        patchState(store, { liveScanResult });
                    });
            },

            /** Cancel the in-flight scan (unsubscribe aborts the request) → back to ready. */
            stopScan() {
                if (store.phase() !== 'scanning') {
                    return;
                }
                activeSub?.unsubscribe();
                activeSub = null;
                // Cancel the parallel comparison scan too.
                liveScanSub?.unsubscribe();
                liveScanSub = null;
                patchState(store, { phase: 'ready' });
            },

            /**
             * Run the real fix pass: POST the page to the agent and stream its
             * progress over SSE. Each `phase` event appends to the live activity
             * log; `progress` updates the live violation count; `done` sets the §6
             * report and moves to "done"; `error` returns to "scanned" so the user
             * can retry. The browser holds no token — the dev/prod proxy injects the
             * bearer (see DotA11yAgentService).
             */
            startFix() {
                const page = store.selected();
                if (store.phase() !== 'scanned' || !page) {
                    return;
                }
                patchState(store, {
                    phase: 'fixing',
                    steps: [],
                    progress: null,
                    runId: null,
                    heartbeat: null,
                    fixError: null,
                    report: null,
                    previewRevision: 0
                });

                // The Java proxy (plan §8.1) resolves page details and builds the full
                // FixRequest; the Studio sends only the identifier, languageId, and skipCss.
                const request: AgentFixRequest = {
                    identifier: page.identifier,
                    languageId: page.languageId,
                    skipCss: store.skipCss()
                };

                activeSub = agentService
                    .fixStream(request)
                    .pipe(
                        catchError((error: unknown) => {
                            const message =
                                error instanceof Error ? error.message : 'The agent run failed.';
                            patchState(store, { phase: 'scanned', fixError: message });

                            return EMPTY;
                        })
                    )
                    .subscribe((event) => {
                        switch (event.type) {
                            case 'run':
                                // First frame: capture the run id so Stop can target it.
                                patchState(store, { runId: event.runId });
                                break;
                            // `step` is the legacy alias of `phase` — treat identically.
                            case 'phase':
                            case 'step':
                                patchState(store, { steps: [...store.steps(), event.step] });
                                break;
                            case 'progress':
                                // Live violation count → drives the score donut down.
                                patchState(store, { progress: event.progress });
                                // Re-scan the preview so the ring segments, legend, and
                                // issue list reflect the fixes that just landed (the
                                // progress totals alone carry no per-severity split).
                                rescanPreviewDuringFix();
                                break;
                            case 'heartbeat':
                                // Keep-alive while the agent is thinking between
                                // actions — drives the "still working…" indicator so a
                                // long, quiet step doesn't look hung.
                                patchState(store, { heartbeat: event.heartbeat });
                                break;
                            case 'done':
                            case 'aborted':
                                // done = full run; aborted = stopped early with a partial
                                // report (fixes already applied are kept). Both land on
                                // the done screen with the report the agent returned.
                                // Cancel any pending mid-fix rescan first so it can't
                                // overwrite the report-driven widgets afterwards.
                                fixRescanSub?.unsubscribe();
                                fixRescanSub = null;
                                patchState(store, {
                                    phase: 'done',
                                    report: event.result,
                                    // Final reload so the preview reflects the finished
                                    // working render.
                                    previewRevision: store.previewRevision() + 1
                                });
                                break;
                            case 'error':
                                // Terminal error event from the agent.
                                fixRescanSub?.unsubscribe();
                                fixRescanSub = null;
                                patchState(store, {
                                    phase: 'scanned',
                                    fixError: event.message
                                });
                                break;
                            default:
                                // Exhaustive: any unhandled event type is ignored.
                                break;
                        }
                    });
            },

            /**
             * Stop the in-flight agent run. Tells the agent (by run id) to stop; it
             * returns a partial report via the stream's `aborted` event, keeping
             * fixes already applied. We keep the stream subscribed so that terminal
             * event still lands and moves us to the done screen. No-op if the run id
             * hasn't arrived yet (the agent hasn't announced the run).
             */
            stopAgent() {
                const runId = store.runId();
                if (store.phase() !== 'fixing' || !runId) {
                    return;
                }
                agentService
                    .stop(runId)
                    .pipe(
                        take(1),
                        catchError(() => EMPTY)
                    )
                    .subscribe();
            },

            /**
             * Promote the working version to live (the only publish; human-triggered).
             *
             * Not gated on the `done` phase: the changed files a user publishes may
             * predate this run (an earlier run, a manual edit), so the files panel
             * offers Publish whenever a working-vs-live delta exists — including
             * before any scan. Blocked only while a run is in flight, where the
             * working copy is still being written.
             */
            publish() {
                if (store.isWorking()) {
                    return;
                }
                patchState(store, { phase: 'published' });
            },

            /**
             * Discard the working fixes. Returns to `scanned` when a scan's results
             * are still on screen, otherwise to `ready` — going to `scanned` from
             * `ready` would show a results view for a scan that never ran.
             */
            discard() {
                if (store.isWorking()) {
                    return;
                }
                patchState(store, { phase: store.scanResult() ? 'scanned' : 'ready' });
            }
        };
    })
);
