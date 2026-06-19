
import {
    DotcmsClient,
    type RenderSources,
    type ScanFinding,
    type ScanResult,
    type SourceRef
} from './dotcms-client';
import {
    generateFix,
    triageViolation,
    type FixInput,
    type FixOutput,
    type TriageDecision,
    type TriageInput
} from './triage';

import type { FixReport, FixRequest, FixResult } from './contract';
import type { LanguageModel } from 'ai';

/**
 * The loop (plan §5), shape (B): a deterministic skeleton that calls the LLM
 * only for triage/attribution and minimal-diff generation. All sequencing,
 * guards, caps, and report assembly are plain code here so the guards
 * (attribution-evidence gate, auto-revert-on-regression) are real, testable
 * code paths.
 *
 *   SCAN(live) → SCAN(EDIT_MODE baseline) → LOCATE → READ candidates
 *   → per violation: TRIAGE → evidence gate → FIX → SAVE-WORKING
 *   → RE-SCAN(EDIT_MODE) → auto-revert if worse → REPORT
 *
 * Re-scan basis is EDIT_MODE-before vs EDIT_MODE-after (S0: EDIT_MODE chrome
 * adds ~48 phantom violations; same chrome both sides cancels). The final §6
 * `scan` delta reports the live-before count and the EDIT_MODE-after count for
 * the human-facing number, while the auto-revert decision uses the EDIT_MODE
 * baseline so a fix is never mis-judged a regression.
 */

export interface RunFixCaps {
    /** Max source files the run will edit. */
    maxFiles: number;
    /** Max bytes of any single file edit. */
    maxFileBytes: number;
    /** Max violations triaged (protects against huge scans). */
    maxViolations: number;
}

export const DEFAULT_CAPS: RunFixCaps = {
    maxFiles: 20,
    maxFileBytes: 512 * 1024,
    maxViolations: 100
};

export interface RunFixDeps {
    client: DotcmsClient;
    model?: LanguageModel; // injected for tests / provider swap
    caps?: RunFixCaps;
    /** Hook for the future SSE layer; no-op by default (plan §8.4). */
    onStep?: (phase: string, message: string) => void;
    /** Override the LLM triage call (tests / alternate strategies). */
    triage?: (input: TriageInput, model?: LanguageModel) => Promise<TriageDecision>;
    /** Override the LLM fix-generation call. */
    fix?: (input: FixInput, model?: LanguageModel) => Promise<FixOutput>;
}

const noop = () => undefined;

/** Build the two scan URLs from the resolved page (plan §8.2 string assembly). */
function scanUrls(req: FixRequest): { live: string; editMode: string } {
    const { dotcmsBaseUrl, page } = req;
    const base = `${dotcmsBaseUrl}${page.uri}?host_id=${page.hostId}`;
    const live = base;
    let editMode = `${base}&mode=EDIT_MODE`;
    // language_id only needed for multilingual pages (S0 finding (c)).
    if (page.languageId && page.languageId !== 1) {
        editMode += `&language_id=${page.languageId}`;
    }
    return { live, editMode };
}

/**
 * Editable theme source extensions the agent will consider fixing. The theme block
 * returns ALL files; we keep markup (vtl) and stylesheets (css and its preprocessors)
 * and skip everything else. JS is intentionally excluded — the agent does not edit
 * JavaScript and theme JS bundles dominate token cost; JS-injected DOM issues are
 * handled via report-only triage. Images/fonts/etc. are not text-editable.
 */
const EDITABLE_THEME_EXTENSIONS = new Set(['vtl', 'css', 'scss', 'sass', 'dotsass', 'less']);

/** Candidate source refs from /_render-sources: editable theme files + container VTLs. */
function collectCandidates(sources: RenderSources): SourceRef[] {
    const refs: SourceRef[] = (sources.theme?.files ?? []).filter((f) =>
        EDITABLE_THEME_EXTENSIONS.has((f.extension ?? '').toLowerCase())
    );
    for (const container of Object.values(sources.containers ?? {})) {
        for (const ct of container.contentTypes ?? []) {
            if (ct.path) {
                refs.push({ identifier: ct.identifier, path: ct.path });
            }
        }
    }
    // De-dup by path.
    const seen = new Set<string>();
    return refs.filter((r) => (seen.has(r.path) ? false : (seen.add(r.path), true)));
}

const isViolation = (f: ScanFinding) => f.resultType === 'violation' || f.type === 'error';

const STYLESHEET_EXTENSIONS = ['.css', '.scss', '.sass', '.dotsass', '.less'];

/** Content type used when saving an edited working copy. Stylesheets (including CSS
 * preprocessors) save as text/css; everything else as text/plain. */
function saveMime(path: string): string {
    const lower = path.toLowerCase();
    return STYLESHEET_EXTENSIONS.some((ext) => lower.endsWith(ext)) ? 'text/css' : 'text/plain';
}

export async function runFix(req: FixRequest, deps: RunFixDeps): Promise<FixReport> {
    const { client } = deps;
    const caps = deps.caps ?? DEFAULT_CAPS;
    const step = deps.onStep ?? noop;
    const { live, editMode } = scanUrls(req);

    // 1. SCAN (live) — the human-facing "before" count.
    step('scan', 'Scanning live page');
    const liveScan = await client.scan(live);

    // 1b. SCAN (EDIT_MODE baseline) — the apples-to-apples basis for revert
    // decisions (same editor chrome as the post-edit re-scan).
    step('scan', 'Scanning working baseline');
    const baselineScan = await client.scan(editMode);

    // 2. LOCATE
    step('locate', 'Locating page sources');
    const sources = await client.locate(req.page.uri, req.page.hostId);
    const candidates = collectCandidates(sources);

    // 3. READ candidate sources once. `currentContent` is the single working copy
    // we progressively improve: each fix builds on the previous edit to the same
    // file (and triage/fix always see the latest content). We do NOT guard against
    // pre-existing unpublished edits — the goal is to fix the a11y issue; working-
    // save is non-destructive (dotCMS keeps per-asset version history, plan §3).
    step('read', `Reading ${candidates.length} source files`);
    const currentContent: Record<string, string> = {};
    for (const ref of candidates) {
        try {
            currentContent[ref.path] = await client.read(ref.path);
        } catch {
            // Unreadable candidate — skip; triage will treat it as (not read).
        }
    }

    const violations = liveScan.findings.items
        .filter(isViolation)
        .slice(0, caps.maxViolations);

    const results: FixResult[] = [];
    const editedPaths = new Set<string>();

    for (const finding of violations) {
        const res = await processViolation({
            finding,
            req,
            deps,
            candidates,
            currentContent,
            editedPaths,
            caps,
            editModeUrl: editMode,
            baselineViolations: countViolations(baselineScan)
        });
        results.push(res);
    }

    // Final re-scan for the human-facing "after" — one EDIT_MODE scan after all edits.
    step('rescan', 'Re-scanning working page');
    let afterCount = countViolations(liveScan);
    try {
        const finalScan = await client.scan(editMode);
        // Map EDIT_MODE count back to a live-comparable number by subtracting the
        // chrome baseline delta is not exact; we report the raw EDIT_MODE delta
        // against the EDIT_MODE baseline as the trustworthy signal.
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
        publishRequired: true
    };
}

function countViolations(scan: ScanResult): number {
    return scan.findings?.violations ?? scan.counts?.errors ?? scan.totalIssues ?? 0;
}

interface ProcessCtx {
    finding: ScanFinding;
    req: FixRequest;
    deps: RunFixDeps;
    candidates: SourceRef[];
    currentContent: Record<string, string>;
    editedPaths: Set<string>;
    caps: RunFixCaps;
    editModeUrl: string;
    baselineViolations: number;
}

/** One violation through triage → guards → fix → save → re-scan → revert. */
async function processViolation(ctx: ProcessCtx): Promise<FixResult> {
    const { finding, deps, candidates, currentContent, editedPaths, caps } = ctx;
    const client = deps.client;
    const step = deps.onStep ?? noop;
    const base: FixResult = { ruleId: finding.code, status: 'reported' };

    const triageFn = deps.triage ?? triageViolation;
    const fixFn = deps.fix ?? generateFix;

    // TRIAGE + ATTRIBUTE
    let decision: TriageDecision;
    try {
        decision = await triageFn(
            { finding, candidates, fileContents: currentContent, skipCss: ctx.req.options.skipCss },
            deps.model
        );
    } catch (e) {
        return { ...base, status: 'failed', reason: `triage failed: ${errMsg(e)}` };
    }

    if (decision.fixability === 'report-only' || !decision.targetPath) {
        return { ...base, status: 'reported', reason: decision.reason };
    }

    // Attribution evidence gate (§5/§9): don't guess-edit.
    if (!decision.evidenceFound) {
        return {
            ...base,
            status: 'reported',
            reason: `Attribution not provable in source: ${decision.reason}`
        };
    }

    const path = decision.targetPath;
    const ref = candidates.find((c) => c.path === path);

    // Per-run file cap.
    if (!editedPaths.has(path) && editedPaths.size >= caps.maxFiles) {
        return { ...base, status: 'reported', file: path, reason: 'Per-run file cap reached' };
    }

    // The current working copy of this file (the original live read, or the result
    // of a previous in-run edit to the same file). A regression reverts to exactly
    // this — the state immediately before this fix.
    const original = currentContent[path] ?? '';

    // GENERATE FIX
    step('fix', `Fixing ${finding.code} in ${path}`);
    let fix: FixOutput;
    try {
        fix = await fixFn(
            { finding, targetPath: path, originalContent: original, fixability: decision.fixability },
            deps.model
        );
    } catch (e) {
        return { ...base, status: 'failed', file: path, identifier: ref?.identifier, reason: `fix generation failed: ${errMsg(e)}` };
    }

    if (!fix.applied) {
        return { ...base, status: 'reported', file: path, identifier: ref?.identifier, reason: fix.reason };
    }
    if (Buffer.byteLength(fix.newContent, 'utf-8') > caps.maxFileBytes) {
        return { ...base, status: 'failed', file: path, identifier: ref?.identifier, reason: 'Edited file exceeds byte cap' };
    }

    // SAVE-WORKING
    let saved;
    try {
        saved = await client.saveWorking(path, fix.newContent, saveMime(path));
    } catch (e) {
        return { ...base, status: 'failed', file: path, identifier: ref?.identifier, reason: `save failed: ${errMsg(e)}` };
    }
    // Verify persisted bytes (§5 step 5).
    if (!saved || saved.fileSize <= 0) {
        return { ...base, status: 'failed', file: path, identifier: saved?.identifier ?? ref?.identifier, reason: 'save returned 0 bytes; not applied' };
    }
    editedPaths.add(path);
    currentContent[path] = fix.newContent; // later violations on this file build on this edit

    // RE-SCAN (EDIT_MODE) → auto-revert if worse.
    step('rescan', `Re-scanning after ${finding.code}`);
    try {
        const rescan = await client.scan(ctx.editModeUrl);
        const after = countViolations(rescan);
        if (after > ctx.baselineViolations) {
            // Regression — revert this asset to its prior (live) content.
            step('fix', `Reverting ${path} (re-scan worse)`);
            await client.saveWorking(path, original, saveMime(path));
            editedPaths.delete(path);
            currentContent[path] = original;
            return {
                ...base,
                status: 'regressed',
                file: path,
                identifier: saved.identifier,
                reverted: true,
                reason: `Re-scan showed +${after - ctx.baselineViolations} violations — reverted to prior version`
            };
        }
        // Improvement becomes the new baseline for subsequent violations.
        ctx.baselineViolations = after;
    } catch (e) {
        // Re-scan failure is not a regression proof; keep the edit but note it.
        return {
            ...base,
            status: 'fixed-to-working',
            file: path,
            identifier: saved.identifier,
            diff: fix.diff,
            reason: `Saved; re-scan could not confirm (${errMsg(e)})`
        };
    }

    const result: FixResult = {
        ...base,
        status: 'fixed-to-working',
        file: path,
        identifier: saved.identifier,
        diff: fix.diff
    };
    if (decision.fixability === 'css') {
        result.blastRadius = 'shared-rule';
        result.review = 'CSS edit — may affect other pages using this rule';
    }
    return result;
}

function errMsg(e: unknown): string {
    return e instanceof Error ? e.message : String(e);
}
