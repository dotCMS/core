import { processViolation, type ParsedStylesheet, type ProcessCtx } from './css/css-fixer';
import { runResearch } from './research/research-loop';
import { scanUrls } from './scan-url';
import { DEFAULT_CAPS, type RunFixDeps } from './types';

import {
    applicableStylesheets,
    countViolations,
    renderAffectingWarnings
} from '../domain/policies';
import { errMsg, isFixableViolation, isViolation, noop } from '../shared/agent-utils';

import type { FixReport, FixRequest, FixResult } from '../domain/contract';
import type { ScanFinding } from '../dotcms/dotcms-client';

/**
 * The loop (plan §5), shape (B): a deterministic skeleton (this file) that
 * orchestrates two passes — PASS 1 deterministic CSS contrast (fix/css/css-fixer)
 * and PASS 2 agentic research (fix/research) — and assembles the §6 report. All
 * sequencing, guards, and caps live here as plain code; the per-violation CSS
 * decision + guarded save/revert live in css-fixer.
 *
 *   SCAN(live) → SCAN(PREVIEW_MODE baseline) → per violation: route → FIX →
 *   SAVE-WORKING → RE-SCAN(PREVIEW_MODE) → auto-revert if worse → PASS 2 → REPORT
 *
 * Re-scan basis is PREVIEW_MODE — it renders the WORKING/draft version (so the
 * agent's saves are reflected) WITHOUT the editor chrome that EDIT_MODE injects
 * (drag handles / edit buttons + extra container divs, which axe over-flags as
 * region/button-name). We scan PREVIEW before AND after edits so the delta is
 * apples-to-apples and the auto-revert never mis-judges a fix.
 */

export { DEFAULT_CAPS, type RunFixCaps, type RunFixDeps } from './types';

export async function runFix(req: FixRequest, deps: RunFixDeps): Promise<FixReport> {
    const { client, signal } = deps;
    const caps = deps.caps ?? DEFAULT_CAPS;
    const step = deps.onStep ?? noop;
    const aborted = () => signal?.aborted ?? false;
    const { live, preview } = scanUrls(req);

    // 1. SCAN live (human-facing "before") + PREVIEW_MODE baseline (working content,
    // no editor chrome — the apples-to-apples basis for revert decisions).
    // Independent → run concurrently.
    step('scan', 'Scanning live + working (preview) baseline');
    const [liveScan, baselineScan] = await Promise.all([client.scan(live), client.scan(preview)]);

    // Abort only when a RENDER-AFFECTING resource (stylesheet/script/document)
    // failed to load — that means the scan measured a broken/unstyled render and
    // fixing against it (esp. contrast) is unsafe. A 404 on a decorative image /
    // xhr / favicon shouldn't block a run (see domain/policies).
    const blocking = [
        ...renderAffectingWarnings(baselineScan),
        ...renderAffectingWarnings(liveScan)
    ];
    if (blocking.length > 0) {
        const warned = blocking
            .map(
                (w) =>
                    `${w.resourceType ?? 'resource'} ${w.status ?? w.errorText ?? 'failed'}: ${w.url}`
            )
            .join('; ');
        step(
            'scan',
            `Render unreliable — aborting (a stylesheet/script failed to load): ${warned}`
        );
        return {
            runId: req.runId,
            page: { uri: req.page.uri, host: req.page.host, languageId: req.page.languageId },
            scan: {
                before: { violations: countViolations(liveScan) },
                after: { violations: countViolations(liveScan) }
            },
            results: [
                {
                    ruleId: 'render-unreliable',
                    status: 'reported',
                    reason: `Scan render was unreliable — a render-affecting resource failed to load, so results aren't trustworthy. No fixes attempted. Failed: ${warned}`
                }
            ],
            changedFiles: [], // aborted before any fix
            publishRequired: true
        };
    }

    const stylesheets = applicableStylesheets(liveScan, req.dotcmsBaseUrl);

    // `currentContent` is the single progressively-improved working copy per file
    // (a later violation on the same file builds on the earlier edit). Files are
    // read LAZILY on first touch — never bulk-read up front. We do not guard
    // against pre-existing unpublished edits (working-save is non-destructive; §3).
    // PASS 1 fixes only CSS (deterministic); PASS 2 re-discovers VTL sources via
    // its own tools, so the loop doesn't pre-locate here.
    const currentContent: Record<string, string> = {};
    // Fetch+parse each compiled stylesheet at most once per run (contrast violations
    // cluster on the same sheet — without this it re-downloads + re-parses per violation).
    const stylesheetCache = new Map<string, ParsedStylesheet>();

    const violations = liveScan.findings.items
        .filter(isFixableViolation) // real violations, excluding editor chrome
        .slice(0, caps.maxViolations);

    const results: FixResult[] = [];
    const editedPaths = new Set<string>();

    // A single CSS edit (a shared rule/token) often clears MANY violations at once.
    // So we skip any violation that a previous edit already resolved: after each
    // successful edit we re-scan, and a finding whose (code+selector) no longer
    // appears in the fresh PREVIEW scan is counted as cleared — no second edit,
    // no wasted re-scan. `liveSignatures` tracks what's still failing.
    const sig = (f: ScanFinding) => `${f.code}|${f.selector}`;
    let liveSignatures = new Set(violations.map(sig));
    // Running PREVIEW baseline — drops as edits clear violations, so each fix's
    // revert guard compares against the current state, not the original page.
    let runningBaseline = countViolations(baselineScan);
    // Rule codes we have actually edited a source for. A collateral clear is only
    // CREDIBLE for the SAME code (a contrast edit can clear other contrast
    // violations via a shared rule, but it cannot fix heading-order/link-name —
    // those disappearing between scans is scan variance, not our doing).
    const editedCodes = new Set<string>();

    for (const finding of violations) {
        // SAFE checkpoint: stop processing further violations if the user hit Stop.
        // Between-violations only — a fix (save → rescan → keep/revert) is atomic, so
        // the working version is always in a consistent state here. Fixes already
        // applied stay; the report below is partial.
        if (aborted()) {
            step('fix', 'Stopped by user — keeping fixes applied so far');
            break;
        }

        // No longer present in the latest re-scan. Only credit it as fixed if we
        // edited a source for this SAME rule code; otherwise it vanished for
        // reasons we can't claim (scan variance) → report honestly.
        if (!liveSignatures.has(sig(finding))) {
            if (editedCodes.has(finding.code)) {
                results.push({
                    ruleId: finding.code,
                    status: 'fixed-to-working',
                    reason: 'Cleared by an earlier fix to a shared rule/variable.'
                });
            } else {
                results.push({
                    ruleId: finding.code,
                    status: 'reported',
                    reason: 'No longer detected after fixes (not attributable to an edit; scan variance).'
                });
            }
            continue;
        }

        const ctx: ProcessCtx = {
            finding,
            req,
            deps,
            stylesheets,
            currentContent,
            editedPaths,
            caps,
            previewUrl: preview,
            stylesheetCache,
            baselineViolations: runningBaseline
        };
        const res = await processViolation(ctx);
        results.push(res);

        // An applied edit already triggered ONE re-scan inside saveAndRescan
        // (the revert guard). Reuse THAT scan — no extra scan — to refresh which
        // violations remain (so a shared-rule edit that cleared many is reflected)
        // and to carry the new baseline forward.
        if (res.status === 'fixed-to-working' && res.file && ctx.lastScan) {
            editedCodes.add(finding.code);
            liveSignatures = new Set(ctx.lastScan.findings.items.filter(isViolation).map(sig));
            runningBaseline = countViolations(ctx.lastScan);
        }
    }

    // PASS 2 — agentic research on whatever PASS 1 left unresolved. Give the model
    // read/grep/edit/rescan tools and let it discover sources we didn't pre-locate
    // (container VTL colors, markup, structure) — the way the MCP agent did. Safe:
    // typed tools only (no publish/delete), all through the allowlisted sandbox.
    // Skipped entirely if the user stopped during PASS 1 (research is the slow part).
    if (deps.research !== false && !aborted()) {
        // What PASS 1 already touched — so PASS 2's file edits aren't double-reported.
        const pass1Files = new Set(results.filter((r) => r.file).map((r) => r.file));
        const unresolved = violations.filter((v) => liveSignatures.has(sig(v)));
        if (unresolved.length > 0) {
            step('fix', `Agentic research on ${unresolved.length} remaining violation(s)`);
            // Record a file the research pass saved as a fixed result (confirmed by
            // the final re-scan below — we don't claim per-violation here).
            const recordEdit = (summary: string) => (p: string) => {
                if (!pass1Files.has(p)) {
                    results.push({
                        ruleId: 'agentic-research',
                        status: 'fixed-to-working',
                        file: p,
                        reason: summary.slice(0, 300)
                    });
                }
            };
            try {
                const research = await runResearch({
                    violations: unresolved,
                    model: deps.model,
                    maxSteps: deps.researchMaxSteps,
                    signal, // Stop cancels the in-flight model call mid-research.
                    deps: {
                        client,
                        page: { uri: req.page.uri, hostId: req.page.hostId },
                        previewUrl: preview,
                        editedPaths,
                        cache: currentContent,
                        onStep: step
                    }
                });
                research.editedPaths.forEach(recordEdit(research.summary));
            } catch (e) {
                // On Stop, generateText throws AbortError — that's expected, not a
                // failure. Either way, keep crediting files the model already saved
                // (editedPaths is mutated by the save tool before any abort).
                if (aborted()) {
                    step('fix', 'Stopped by user — keeping research fixes applied so far');
                    [...editedPaths].forEach(recordEdit('Saved before the run was stopped.'));
                } else {
                    step('fix', `Agentic research failed: ${errMsg(e)}`);
                }
            }
        }
    }

    // Final re-scan for the human-facing "after" — one PREVIEW scan after all edits.
    step('rescan', 'Re-scanning working page');
    let afterCount = countViolations(liveScan);
    try {
        const finalScan = await client.scan(preview);
        // PREVIEW matches the live structure (no editor chrome), so the working
        // delta maps directly: after = live-before − (preview-baseline − preview-final).
        const baseline = countViolations(baselineScan);
        const finalEdit = countViolations(finalScan);
        afterCount = Math.max(0, countViolations(liveScan) - (baseline - finalEdit));
    } catch {
        step('rescan', 'Final re-scan failed; reporting per-fix re-scans only');
    }

    return {
        runId: req.runId,
        page: { uri: req.page.uri, host: req.page.host, languageId: req.page.languageId },
        scan: {
            before: { violations: countViolations(liveScan) },
            after: { violations: afterCount }
        },
        results,
        // The authoritative set of files left changed (reverted files already removed
        // from editedPaths). Same on a completed or stopped run.
        changedFiles: [...editedPaths],
        publishRequired: true
    };
}
