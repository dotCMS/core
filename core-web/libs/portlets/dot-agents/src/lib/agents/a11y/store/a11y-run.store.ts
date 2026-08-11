import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, inject, isDevMode } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { AgentHeartbeat, AgentProgress, AgentRunStep } from '@dotcms/dotcms-models';
import { DotPageScannerService, PageScannerA11yResponse } from '@dotcms/portlets/dot-ema/ui';

import { SubscriptionSlot } from './subscription-slot';

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
 * Dev-only port swap for {@link backendOrigin}. Under `nx serve` the app is served
 * on DEV_SERVER_PORT while dotCMS itself answers on DEV_BACKEND_PORT; the agent
 * renders server-side and must be handed the latter. Both are inert in prod, where
 * the portlet is served from the dotCMS origin and no swap happens.
 */
const DEV_SERVER_PORT = '4200';
const DEV_BACKEND_PORT = '8080';

interface A11yRunState {
    /** Studio state machine (§7). Starts at `ready` (a page is being opened). */
    phase: StudioPhase;
    /** The page this run is against. */
    selected: StudioPageRow | null;
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
    /**
     * The one error channel for this screen — a failed scan, a failed fix run, a stream
     * that dropped, or a stop that didn't take. Rendered as a banner at the top of the
     * portlet and cleared whenever a new scan or fix starts.
     *
     * Deliberately NOT routed through `DotHttpErrorManagerService`: a modal error dialog
     * over a long-running agent screen interrupts a run the user is watching, and these
     * failures are all recoverable in place (re-scan, re-run, retry the stop).
     */
    runError: string | null;
    /**
     * The §6 run report — populated when the fix pass completes (SSE `done`).
     *
     * Non-null implies `scan.before` and `scan.after` are present: `DotA11yAgentService`
     * validates the terminal payload and yields null for a status-only frame rather than
     * a partial report. That invariant is what lets every derived count below read
     * `report.scan` behind a plain truthiness check — a status-only `aborted` frame used
     * to satisfy that check and then throw inside the computeds, blanking the run pane.
     */
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
    skipCss: false,
    scanResult: null,
    liveScanResult: null,
    steps: [],
    progress: null,
    runId: null,
    heartbeat: null,
    runError: null,
    report: null,
    previewRevision: 0
};

/**
 * A thrown value as banner text, falling back to `fallback` for anything that isn't an
 * `Error` (an HTTP failure arrives as an `HttpErrorResponse`, whose `message` is the
 * useful part; a non-Error throw has nothing worth showing the user).
 */
const errorText = (error: unknown, fallback: string): string =>
    error instanceof Error && error.message ? error.message : fallback;

/** The run-state reset shared by opening a page and starting a fresh scan. */
const runReset = (): Partial<A11yRunState> => ({
    scanResult: null,
    liveScanResult: null,
    steps: [],
    progress: null,
    runId: null,
    heartbeat: null,
    runError: null,
    report: null,
    previewRevision: 0
});

/**
 * The Accessibility Studio **run** store — owns a single page's scan / fix /
 * review / publish lifecycle for the run route (`agents/a11y/<page-path>`). It's
 * provided at {@link DotA11yRunComponent}, so navigating to a different page
 * destroys and recreates it → fresh state per page, no manual reset needed.
 *
 * The page it runs against is handed over by the page list through router state
 * ({@link openSelectedPage}); the URL carries its readable path for display and
 * sharing, but the run route is only reachable through the list.
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
        const scannerService = inject(DotPageScannerService);
        const agentService = inject(DotA11yAgentService);

        // The in-flight scan / fix-stream subscription, held so Stop can cancel it
        // (unsubscribing aborts the underlying fetch). Not reactive UI state.
        const activeScan = new SubscriptionSlot();
        // The comparison-only LIVE scan, tracked separately so Stop cancels it too
        // without coupling it to the UI-driving preview scan's lifecycle.
        const liveScan = new SubscriptionSlot();
        // The mid-fix re-scan of the PREVIEW render, triggered by `progress` frames
        // so the ring + per-severity legend track the agent's live fixes. Held so a
        // newer progress frame supersedes an in-flight one (no stampede / stale writes).
        const fixRescan = new SubscriptionSlot();
        // The in-flight POST /stop. Held so teardown can cancel it, and so a user
        // clicking Stop repeatedly supersedes the previous request instead of piling
        // up one un-cancellable POST per click.
        const stopRequest = new SubscriptionSlot();

        /**
         * The dotCMS backend origin the agent must render + call against.
         *
         * In prod the portlet is served FROM the dotCMS origin, so the browser's own
         * origin is already correct and is returned untouched.
         *
         * Under `nx serve` the app is on the dev-server port but the agent (and the
         * dotCMS scanner it drives) render server-side and can only reach the backend
         * directly, so the port is swapped for the backend's. Only the PORT is
         * rewritten — protocol and hostname are preserved — and only in dev mode, so a
         * hostname that merely contains the dev port's digits (`dev4200.example.com`)
         * can't be corrupted the way a blind string replace would.
         */
        function backendOrigin(): string {
            const origin = window.location.origin;
            if (!isDevMode()) {
                return origin;
            }

            try {
                const url = new URL(origin);
                if (url.port === DEV_SERVER_PORT) {
                    url.port = DEV_BACKEND_PORT;
                }

                return url.origin;
            } catch {
                return origin;
            }
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
            fixRescan.set(
                scannerService
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
                    })
            );
        }

        /** Open a page → studio "ready" (waits for the user to scan). */
        function openPage(selected: StudioPageRow) {
            patchState(store, {
                selected,
                phase: 'ready',
                ...runReset()
            });
        }

        return {
            setSkipCss(skipCss: boolean) {
                patchState(store, { skipCss });
            },

            /**
             * Adopt the page the page list handed over via router state → run screen
             * "ready". The row carries everything the run needs (identifier, host,
             * language), so there is no lookup: the run route is entered only from the
             * list, and a run URL opened cold has no row to adopt (the run screen
             * bounces back to the list instead).
             *
             * A no-op when the SAME page under the SAME site is already selected, so a
             * re-navigation into the route can't reset an in-progress run.
             */
            openSelectedPage(row: StudioPageRow) {
                if (store.selected()?.identifier === row.identifier) {
                    return;
                }
                openPage(row);
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
                    runError: null
                });

                // Primary scan — the PREVIEW (working) render. Owns the phase + all
                // widgets. Failure returns to `ready` so the user can retry.
                activeScan.set(
                    scannerService
                        .checkA11y(buildScanUrl(page, 'EDIT_MODE'))
                        .pipe(
                            take(1),
                            catchError((error: unknown) => {
                                // Return to ready so the user can retry the scan, and
                                // report it in the portlet's own banner.
                                patchState(store, {
                                    phase: 'ready',
                                    runError: errorText(error, 'The scan failed.')
                                });

                                return EMPTY;
                            })
                        )
                        .subscribe((scanResult) => {
                            patchState(store, { scanResult, phase: 'scanned' });
                        })
                );

                // Comparison-only scan — the LIVE (published) render. Draws the
                // live-frame markers and nothing else, so its failure must NOT touch
                // the phase or surface an error dialog: swallow it and leave the live
                // markers empty. Runs in parallel with the primary scan.
                liveScan.set(
                    scannerService
                        .checkA11y(buildScanUrl(page, 'LIVE'))
                        .pipe(
                            take(1),
                            catchError(() => EMPTY)
                        )
                        .subscribe((liveScanResult) => {
                            patchState(store, { liveScanResult });
                        })
                );
            },

            /**
             * Tear down every in-flight subscription without touching state.
             *
             * The in-band transitions (stopScan / done / error) each clean up the
             * subscriptions they own, but component/route teardown does not: navigating
             * away mid-run would otherwise leave the SSE `fetch` ReadableStream open,
             * with its `subscriber.next` closures keeping this store alive and the
             * abandoned run still streaming. Called from the `onDestroy` hook below.
             */
            teardown() {
                activeScan.cancel();
                liveScan.cancel();
                fixRescan.cancel();
                stopRequest.cancel();
            },

            /** Cancel the in-flight scan (unsubscribe aborts the request) → back to ready. */
            stopScan() {
                if (store.phase() !== 'scanning') {
                    return;
                }
                activeScan.cancel();
                // Cancel the parallel comparison scan too.
                liveScan.cancel();
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
                    runError: null,
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

                // Whether this run's outcome has already been recorded — by a terminal
                // frame (`done` / `aborted` / `error`) or by the stream erroring. Read by
                // the `complete` handler below, which fires in BOTH cases (the error path
                // completes too, via the EMPTY that `catchError` returns) and must not
                // overwrite an outcome that is already correct.
                let settled = false;

                activeScan.set(
                    agentService
                        .fixStream(request)
                        .pipe(
                            catchError((error: unknown) => {
                                const message =
                                    error instanceof Error
                                        ? error.message
                                        : 'The agent run failed.';
                                settled = true;
                                patchState(store, {
                                    phase: 'scanned',
                                    runError: message,
                                    // The run may have written fixes before the stream
                                    // died; refresh so they aren't hidden (see `error`).
                                    previewRevision: store.previewRevision() + 1
                                });

                                return EMPTY;
                            })
                        )
                        .subscribe({
                            next: (event) => {
                                switch (event.type) {
                                    case 'run':
                                        // First frame: capture the run id so Stop can target it.
                                        patchState(store, { runId: event.runId });
                                        break;
                                    // `step` is the legacy alias of `phase` — treat identically.
                                    case 'phase':
                                    case 'step':
                                        patchState(store, {
                                            steps: [...store.steps(), event.step]
                                        });
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
                                        settled = true;
                                        fixRescan.cancel();
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
                                        settled = true;
                                        fixRescan.cancel();
                                        patchState(store, {
                                            phase: 'scanned',
                                            runError: event.message,
                                            // Bump like `done`/`aborted` do. A run can fail
                                            // AFTER writing fixes to the working version, and
                                            // without this the preview and changed-files panel
                                            // keep their pre-run state — hiding real unpublished
                                            // edits and, with them, the Publish bar.
                                            previewRevision: store.previewRevision() + 1
                                        });
                                        break;
                                    default:
                                        // Exhaustive: any unhandled event type is ignored.
                                        break;
                                }
                            },
                            complete: () => {
                                // The stream ended. If a terminal frame already landed this
                                // is the normal close and the phase is already correct.
                                if (settled) {
                                    return;
                                }

                                // Otherwise the connection dropped mid-run. Fall back to
                                // `scanned` so the user keeps their scan results and can
                                // retry — staying in `fixing` would spin the "still
                                // working…" indicator forever with no way out, since Stop
                                // targets a run the agent may no longer have.
                                fixRescan.cancel();
                                patchState(store, {
                                    phase: 'scanned',
                                    runError:
                                        'The connection to the agent ended before it reported a result. ' +
                                        'Any fixes it already wrote to the working version are kept — ' +
                                        're-scan to see the current state.',
                                    // Those kept fixes are exactly what the preview and
                                    // changed-files panel must now show (see `error`).
                                    previewRevision: store.previewRevision() + 1
                                });
                            }
                        })
                );
            },

            /**
             * Stop the in-flight agent run. Tells the agent (by run id) to stop; it
             * returns a partial report via the stream's `aborted` event, keeping
             * fixes already applied. We keep the stream subscribed so that terminal
             * event still lands and moves us to the done screen. No-op if the run id
             * hasn't arrived yet (the agent hasn't announced the run).
             *
             * A FAILED stop must be visible. The service already treats 202 and 404 as
             * equivalent (the run is gone either way), so anything still reaching the
             * error path — a 5xx, a network drop — means the agent very likely kept
             * running and kept writing to the working version. Swallowing that would
             * leave the UI claiming a stop that never took, on the one control the user
             * reaches for when they want the agent to stop touching their page.
             */
            stopAgent() {
                const runId = store.runId();
                if (store.phase() !== 'fixing' || !runId) {
                    return;
                }
                stopRequest.set(
                    agentService
                        .stop(runId)
                        .pipe(
                            take(1),
                            catchError(() => {
                                // Stay in `fixing`: the run is, as far as we know, still
                                // going. The stream's own terminal frame (or its close
                                // handler) still owns the phase transition.
                                patchState(store, {
                                    runError:
                                        'Could not stop the agent — it may still be running and ' +
                                        'writing to the working version. Try again in a moment.'
                                });

                                return EMPTY;
                            })
                        )
                        .subscribe()
                );
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
    }),
    withHooks({
        /**
         * Abort any in-flight scan / SSE fix stream when the store is destroyed
         * (route navigation, component teardown), so an abandoned run cannot keep
         * streaming in the background.
         */
        onDestroy(store) {
            store.teardown();
        }
    })
);
