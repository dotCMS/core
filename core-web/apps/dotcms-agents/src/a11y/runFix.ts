
import { nudgeToClear, parseColor, parseTargetRatio } from './contrast';
import { attribute, parseColorRules, type CssRule } from './css-attribution';
import { extractInlineSourceMap, resolveDeclarationValue } from './css-source-map';
import {
    DotcmsClient,
    type RenderSources,
    type ScanFinding,
    type ScanResult,
    type SourceRef
} from './dotcms-client';
import { runResearch } from './researchLoop';
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
    /** Override the LLM (VTL) fix-generation call. */
    fix?: (input: FixInput, model?: LanguageModel) => Promise<FixOutput>;
    /** Disable PASS 2 agentic research (default on). Set false to run deterministic-only. */
    research?: boolean;
    /** maxSteps for the PASS 2 research loop (default 40). */
    researchMaxSteps?: number;
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
 * VTL candidate refs from /_render-sources: theme VTLs + container VTLs only.
 * This is a SMALL set (~10) — markup the agent may edit for VTL-level violations
 * (missing alt/aria/lang/labels, heading order). We deliberately do NOT include
 * stylesheets here: CSS goes through the lazy deterministic path (attribute the
 * one compiled stylesheet + sourcemap → edit one SCSS source), so the 150+ SCSS
 * partials are never read or sent to the model (plan §5 READ, S1.5).
 */
function collectVtlCandidates(sources: RenderSources): SourceRef[] {
    const refs: SourceRef[] = (sources.theme?.files ?? []).filter(
        (f) => (f.extension ?? '').toLowerCase() === 'vtl'
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

/** axe rule ids that are governed by CSS (contrast etc.) → the deterministic CSS path. */
const CSS_RULE_CODES = new Set(['color-contrast', 'color-contrast-enhanced']);

/** Pick the compiled dotCMS stylesheet(s) the page actually loaded (same-origin). */
function applicableStylesheets(scan: ScanResult, dotcmsBaseUrl: string): string[] {
    const origin = (() => {
        try {
            return new URL(dotcmsBaseUrl).origin;
        } catch {
            return '';
        }
    })();
    return (scan.stylesheets ?? []).filter((u) => {
        try {
            return new URL(u).origin === origin;
        } catch {
            return false;
        }
    });
}

const isViolation = (f: ScanFinding) => f.resultType === 'violation' || f.type === 'error';

/**
 * Genuine EDIT_MODE editor chrome — the inline edit/add buttons the editor
 * injects (`data-dot-object="edit-content"` / `"edit-container"` / `"add"`).
 * These are editor UI, in no template, so unfixable noise — drop them.
 *
 * NOTE: only the edit-* / add buttons are dropped. Elements with
 * `data-dot-object="contentlet"` or `"container"` are NOT chrome — those
 * attributes are dotCMS's attribution metadata (content type → VTL) and are
 * useful, not noise (see the scanner data-dot-ancestor spec).
 */
const isEditorChrome = (f: ScanFinding) =>
    /data-dot-object="(edit-content|edit-container|add)"/.test(f.context ?? '');

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

    // 2. LOCATE. VTL candidates are a small set (theme + container VTLs); the SCSS
    // tree is NOT collected — CSS uses the lazy deterministic path (S1.5).
    step('locate', 'Locating page sources');
    const sources = await client.locate(req.page.uri, req.page.hostId);
    const vtlCandidates = collectVtlCandidates(sources);
    const stylesheets = applicableStylesheets(liveScan, req.dotcmsBaseUrl);

    // `currentContent` is the single progressively-improved working copy per file
    // (a later violation on the same file builds on the earlier edit). Files are
    // read LAZILY on first touch — never bulk-read up front. We do not guard
    // against pre-existing unpublished edits (working-save is non-destructive; §3).
    const currentContent: Record<string, string> = {};

    const violations = liveScan.findings.items
        .filter(isViolation)
        .filter((f) => !isEditorChrome(f)) // drop platform edit-mode chrome (unfixable noise)
        .slice(0, caps.maxViolations);

    const results: FixResult[] = [];
    const editedPaths = new Set<string>();

    // A single CSS edit (a shared rule/token) often clears MANY violations at once.
    // So we skip any violation that a previous edit already resolved: after each
    // successful edit we re-scan, and a finding whose (code+selector) no longer
    // appears in the fresh EDIT_MODE scan is counted as cleared — no second edit,
    // no wasted re-scan. `liveSignatures` tracks what's still failing.
    const sig = (f: ScanFinding) => `${f.code}|${f.selector}`;
    let liveSignatures = new Set(violations.map(sig));
    // Running EDIT_MODE baseline — drops as edits clear violations, so each fix's
    // revert guard compares against the current state, not the original page.
    let runningBaseline = countViolations(baselineScan);
    // Rule codes we have actually edited a source for. A collateral clear is only
    // CREDIBLE for the SAME code (a contrast edit can clear other contrast
    // violations via a shared rule, but it cannot fix heading-order/link-name —
    // those disappearing between scans is scan variance, not our doing).
    const editedCodes = new Set<string>();

    for (const finding of violations) {
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
            vtlCandidates,
            stylesheets,
            currentContent,
            editedPaths,
            caps,
            editModeUrl: editMode,
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
            liveSignatures = new Set(
                ctx.lastScan.findings.items.filter(isViolation).map(sig)
            );
            runningBaseline = countViolations(ctx.lastScan);
        }
    }

    // PASS 2 — agentic research on whatever PASS 1 left unresolved. Give the model
    // read/grep/edit/rescan tools and let it discover sources we didn't pre-locate
    // (container VTL colors, markup, structure) — the way the MCP agent did. Safe:
    // typed tools only (no publish/delete), all through the allowlisted sandbox.
    if (deps.research !== false) {
        const resolvedSigs = new Set(
            results.filter((r) => r.status === 'fixed-to-working' && r.file).map((r) => r.ruleId)
        );
        // Re-derive the live unresolved set from the latest signatures.
        const unresolved = violations.filter((v) => liveSignatures.has(sig(v)));
        if (unresolved.length > 0) {
            step('fix', `Agentic research on ${unresolved.length} remaining violation(s)`);
            try {
                const research = await runResearch({
                    violations: unresolved,
                    model: deps.model,
                    maxSteps: deps.researchMaxSteps,
                    deps: {
                        client,
                        page: { uri: req.page.uri, hostId: req.page.hostId },
                        editModeUrl: editMode,
                        editedPaths,
                        cache: currentContent,
                        onStep: step
                    }
                });
                // Record what the research pass touched (honest: a file edit, confirmed
                // by the final re-scan below — we don't claim per-violation here).
                for (const p of research.editedPaths) {
                    if (!resolvedSigs.has(p)) {
                        results.push({
                            ruleId: 'agentic-research',
                            status: 'fixed-to-working',
                            file: p,
                            reason: research.summary.slice(0, 300)
                        });
                    }
                }
            } catch (e) {
                step('fix', `Agentic research failed: ${errMsg(e)}`);
            }
        }
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
    vtlCandidates: SourceRef[];
    stylesheets: string[];
    currentContent: Record<string, string>;
    editedPaths: Set<string>;
    caps: RunFixCaps;
    editModeUrl: string;
    baselineViolations: number;
    /** Set by saveAndRescan: the fresh EDIT_MODE scan taken after the edit (kept or
     * reverted). The main loop reuses it to refresh which violations remain — so a
     * shared-rule edit that clears many violations costs ONE re-scan, not N. */
    lastScan?: ScanResult;
}

/** Route a violation to the deterministic CSS path or the LLM-driven VTL path. */
async function processViolation(ctx: ProcessCtx): Promise<FixResult> {
    const { finding } = ctx;
    const isCss = CSS_RULE_CODES.has(finding.code);
    if (isCss) {
        if (ctx.req.options.skipCss) {
            return {
                ruleId: finding.code,
                status: 'reported',
                reason: 'CSS fix skipped per run option (skipCss).'
            };
        }
        return processCssViolation(ctx);
    }
    return processVtlViolation(ctx);
}

/**
 * CSS path (deterministic, S1.5): attribute against the ONE compiled stylesheet,
 * map the winning rule's value back to its SCSS source via the sourcemap, edit
 * only that one source file. The LLM sees only the matched rule (~100 tokens) —
 * the 150+ SCSS partials are never read or sent.
 */
async function processCssViolation(ctx: ProcessCtx): Promise<FixResult> {
    const { finding, deps, stylesheets, editedPaths, caps } = ctx;
    const client = deps.client;
    const step = deps.onStep ?? noop;
    const base: FixResult = { ruleId: finding.code, status: 'reported' };

    if (stylesheets.length === 0) {
        return { ...base, reason: 'No same-origin stylesheet found on the page to attribute against.' };
    }

    // axe's data is the SOURCE OF TRUTH for which colors are in play.
    const data = finding.data;
    const fg = data?.fgColor ? parseColor(data.fgColor) : null;
    const bg = data?.bgColor ? parseColor(data.bgColor) : null;
    if (!data || (!fg && !bg)) {
        return { ...base, reason: 'Scanner did not provide fg/bg color data for this contrast violation; reported.' };
    }

    // Gather candidate rules that match the element, then pick the (rule, decl)
    // whose VALUE equals axe's flagged fgColor or bgColor — that decl is provably
    // the one axe measured. We do NOT trust specificity ranking to guess the decl;
    // the color identity decides. The OTHER color of the pair is the counterpart.
    let css = '';
    let matches: CssRule[] = [];
    for (const sheet of stylesheets) {
        try {
            css = await client.fetchStylesheet(sheet);
        } catch (e) {
            return { ...base, status: 'failed', reason: `fetch stylesheet failed: ${errMsg(e)}` };
        }
        matches = attribute(finding.context, parseColorRules(css));
        if (matches.length) {
            break;
        }
    }
    if (!matches.length) {
        return { ...base, reason: 'Could not attribute the contrast failure to a CSS rule (no sound match).' };
    }

    // Candidate edits: the decl whose value == fgColor (nudge it against bg) and
    // the decl whose value == bgColor (nudge against fg). We try BOTH and keep the
    // first that yields a real fix — so when one side can't be nudged (e.g. white
    // text against a light bg), we fall back to editing the OTHER side (the bg).
    const sameColor = (a: ReturnType<typeof parseColor>, b: ReturnType<typeof parseColor>) =>
        !!a && !!b && a.r === b.r && a.g === b.g && a.b === b.b;
    interface Candidate {
        rule: CssRule;
        decl: { prop: string; value: string };
        counterpart: NonNullable<ReturnType<typeof parseColor>>;
    }
    const candidates: Candidate[] = [];
    for (const rule of matches) {
        for (const d of rule.decls) {
            const v = parseColor(d.value);
            if (sameColor(v, fg) && bg) {
                candidates.push({ rule, decl: d, counterpart: bg });
            } else if (sameColor(v, bg) && fg) {
                candidates.push({ rule, decl: d, counterpart: fg });
            }
        }
    }
    if (candidates.length === 0) {
        return {
            ...base,
            reason: `No matched CSS rule's color equals axe's flagged fg(${data.fgColor})/bg(${data.bgColor}) — the failing color isn't in an attributable rule (likely inherited/inline/computed); reported.`
        };
    }

    const map = extractInlineSourceMap(css);
    if (!map) {
        return { ...base, reason: 'No sourcemap on the stylesheet; cannot locate the SCSS source to edit.' };
    }
    const targetRatio = parseTargetRatio(data.expectedContrastRatio);

    // Try each candidate; the first that resolves to source AND yields a nudge wins.
    let lastReason = 'No candidate could be fixed.';
    for (const cand of candidates) {
        const editable = parseColor(cand.decl.value);
        const fix = editable ? nudgeToClear(editable, cand.counterpart, targetRatio) : null;
        if (!fix) {
            lastReason = `Cannot reach ${targetRatio}:1 by nudging ${cand.decl.value} (${cand.decl.prop}); trying the counterpart.`;
            continue; // this side is unfixable (e.g. white) — try the other
        }

        // Resolve this decl's value position → its SCSS source.
        const declNode = findNamedColorDeclNode(cand.rule.node, cand.decl.prop, cand.decl.value);
        const declPos = declNode?.source?.start;
        if (!declNode || !declPos) {
            lastReason = `No declaration position for ${cand.decl.prop}; trying the counterpart.`;
            continue;
        }
        const resolved = await resolveDeclarationValue(map, declPos.line, declPos.column, declNode.prop);
        if (!resolved || resolved.content === undefined) {
            lastReason = `Could not map ${cand.rule.selector} (${cand.decl.prop}) to its SCSS source; trying the counterpart.`;
            continue;
        }
        const sourcePath = resolveSourcePath(resolved.source, stylesheets[0]);
        const sourceValue = colorTokenAt(resolved.content, resolved.line, resolved.column);
        if (!sourceValue) {
            lastReason = `Found no editable color token at ${shortName(sourcePath)}:${resolved.line}; trying the counterpart.`;
            continue;
        }
        if (!editedPaths.has(sourcePath) && editedPaths.size >= caps.maxFiles) {
            return { ...base, file: sourcePath, reason: 'Per-run file cap reached' };
        }

        // Read the ONE source file (lazy; prefer the sourcemap's embedded content).
        let original = ctx.currentContent[sourcePath] ?? resolved.content;
        if (ctx.currentContent[sourcePath] === undefined && resolved.content === undefined) {
            try {
                original = await client.read(sourcePath);
            } catch (e) {
                return { ...base, status: 'failed', file: sourcePath, reason: `read source failed: ${errMsg(e)}` };
            }
        }
        if (!original.includes(sourceValue)) {
            lastReason = `Source value ${sourceValue} not found in ${shortName(sourcePath)}; trying the counterpart.`;
            continue;
        }

        step('fix', `Fixing ${finding.code} → ${cand.rule.selector} (${cand.decl.prop}) in ${shortName(sourcePath)}`);
        const edited = replaceFirst(original, sourceValue, fix.newColor);
        if (Buffer.byteLength(edited, 'utf-8') > caps.maxFileBytes) {
            return { ...base, status: 'failed', file: sourcePath, reason: 'Edited file exceeds byte cap' };
        }
        const diff = `- ${cand.decl.prop}: ${sourceValue}\n+ ${cand.decl.prop}: ${fix.newColor}  (${cand.rule.selector}; ${data.contrastRatio ?? '?'}:1 → ${fix.achievedRatio.toFixed(2)}:1, target ${targetRatio}:1)`;
        return saveAndRescan(ctx, {
            path: sourcePath,
            original,
            edited,
            diff,
            blastRadius: 'token',
            review: `CSS color edit in ${shortName(sourcePath)} — may affect every element using this rule/variable`
        });
    }

    return { ...base, reason: `${lastReason} (needs a design decision); reported.` };
}

/**
 * VTL path (LLM-driven): the small VTL candidate set is read lazily; the model
 * triages + attributes, gated on evidence, then produces a minimal file diff.
 */
async function processVtlViolation(ctx: ProcessCtx): Promise<FixResult> {
    const { finding, deps, vtlCandidates, currentContent, editedPaths, caps } = ctx;
    const client = deps.client;
    const step = deps.onStep ?? noop;
    const triageFn = deps.triage ?? triageViolation;
    const fixFn = deps.fix ?? generateFix;
    const base: FixResult = { ruleId: finding.code, status: 'reported' };

    // Lazily read the (small) VTL candidate set for this violation's triage.
    for (const ref of vtlCandidates) {
        if (currentContent[ref.path] === undefined) {
            try {
                currentContent[ref.path] = await client.read(ref.path);
            } catch {
                // skip unreadable candidate
            }
        }
    }

    let decision: TriageDecision;
    try {
        decision = await triageFn(
            { finding, candidates: vtlCandidates, fileContents: currentContent, skipCss: ctx.req.options.skipCss },
            deps.model
        );
    } catch (e) {
        return { ...base, status: 'failed', reason: `triage failed: ${errMsg(e)}` };
    }

    if (decision.fixability !== 'vtl' || !decision.targetPath) {
        return { ...base, reason: decision.reason };
    }
    if (!decision.evidenceFound) {
        return { ...base, reason: `Attribution not provable in source: ${decision.reason}` };
    }

    const path = decision.targetPath;
    const ref = vtlCandidates.find((c) => c.path === path);
    if (!editedPaths.has(path) && editedPaths.size >= caps.maxFiles) {
        return { ...base, file: path, reason: 'Per-run file cap reached' };
    }

    const original = currentContent[path] ?? '';
    step('fix', `Fixing ${finding.code} in ${shortName(path)}`);
    let fix: FixOutput;
    try {
        fix = await fixFn(
            { finding, targetPath: path, originalContent: original, fixability: 'vtl' },
            deps.model
        );
    } catch (e) {
        return { ...base, status: 'failed', file: path, identifier: ref?.identifier, reason: `fix generation failed: ${errMsg(e)}` };
    }
    if (!fix.applied) {
        return { ...base, file: path, identifier: ref?.identifier, reason: fix.reason };
    }
    if (Buffer.byteLength(fix.newContent, 'utf-8') > caps.maxFileBytes) {
        return { ...base, status: 'failed', file: path, identifier: ref?.identifier, reason: 'Edited file exceeds byte cap' };
    }

    return saveAndRescan(ctx, {
        path,
        original,
        edited: fix.newContent,
        diff: fix.diff,
        identifier: ref?.identifier
    });
}

interface SaveSpec {
    path: string;
    original: string;
    edited: string;
    diff: string;
    identifier?: string;
    blastRadius?: 'element-scoped' | 'shared-rule' | 'token';
    review?: string;
}

/** Save-working, verify bytes, re-scan EDIT_MODE, auto-revert on regression. Shared by both paths. */
async function saveAndRescan(ctx: ProcessCtx, spec: SaveSpec): Promise<FixResult> {
    const { deps, editedPaths, currentContent } = ctx;
    const client = deps.client;
    const step = deps.onStep ?? noop;
    const base: FixResult = { ruleId: ctx.finding.code, status: 'reported' };
    const { path, original, edited } = spec;

    let saved;
    try {
        saved = await client.saveWorking(path, edited, saveMime(path));
    } catch (e) {
        return { ...base, status: 'failed', file: path, identifier: spec.identifier, reason: `save failed: ${errMsg(e)}` };
    }
    if (!saved || saved.fileSize <= 0) {
        return { ...base, status: 'failed', file: path, identifier: saved?.identifier ?? spec.identifier, reason: 'save returned 0 bytes; not applied' };
    }
    editedPaths.add(path);
    currentContent[path] = edited;

    step('rescan', `Re-scanning after ${ctx.finding.code}`);
    try {
        const rescan = await client.scan(ctx.editModeUrl);
        const after = countViolations(rescan);
        if (after > ctx.baselineViolations) {
            step('fix', `Reverting ${shortName(path)} (re-scan worse)`);
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
        ctx.baselineViolations = after;
        ctx.lastScan = rescan; // main loop reuses this to skip collateral-cleared violations
    } catch (e) {
        return {
            ...base,
            status: 'fixed-to-working',
            file: path,
            identifier: saved.identifier,
            diff: spec.diff,
            ...(spec.blastRadius ? { blastRadius: spec.blastRadius } : {}),
            ...(spec.review ? { review: spec.review } : {}),
            reason: `Saved; re-scan could not confirm (${errMsg(e)})`
        };
    }

    return {
        ...base,
        status: 'fixed-to-working',
        file: path,
        identifier: saved.identifier,
        diff: spec.diff,
        ...(spec.blastRadius ? { blastRadius: spec.blastRadius } : {}),
        ...(spec.review ? { review: spec.review } : {})
    };
}

/** Resolve a sourcemap `sources[]` entry (theme-relative) to a dotCMS asset path. */
function resolveSourcePath(source: string, stylesheetUrl: string): string {
    // The stylesheet lives at …/themes/<theme>/css/<sheet>; sources are relative
    // to that css/ dir (e.g. "custom-styles/_variables-custom.scss"). Resolve against
    // the stylesheet's directory, then return the host-qualified //host/path form.
    try {
        const sheet = new URL(stylesheetUrl);
        const cssDir = sheet.pathname.slice(0, sheet.pathname.lastIndexOf('/') + 1);
        const abs = new URL(source, 'http://h' + cssDir).pathname; // normalize ../ segments
        return `//${sheet.host}${abs}`;
    } catch {
        return source;
    }
}

const shortName = (p: string) => p.split('/').pop() ?? p;

/** The postcss declaration node matching `prop` + `value` on a rule (for its
 * value position; a rule may have several color decls, so match the exact one). */
function findNamedColorDeclNode(
    rule: CssRule['node'],
    prop: string,
    value: string
): { prop: string; value: string; source?: { start?: { line: number; column: number } } } | undefined {
    const decls = rule.nodes.filter(
        (n): n is import('postcss').Declaration => n.type === 'decl'
    );
    return (
        decls.find((d) => d.prop === prop && d.value === value) ??
        decls.find((d) => d.value === value) ??
        decls.find((d) => /color/i.test(d.prop))
    );
}

/** Extract a CSS color token (hex / rgb()/rgba() / name) at or near a source position. */
function colorTokenAt(content: string, line: number, column: number): string | null {
    const lineText = content.split('\n')[line - 1];
    if (!lineText) {
        return null;
    }
    // Search from the resolved column onward (then the whole line) for a color literal.
    const COLOR = /#[0-9a-fA-F]{3,8}\b|rgba?\([^)]*\)|hsla?\([^)]*\)/;
    const fromCol = lineText.slice(Math.max(0, column));
    const m = fromCol.match(COLOR) ?? lineText.match(COLOR);
    return m ? m[0] : null;
}

/** Replace only the first occurrence of `needle` (literal) in `haystack`. */
function replaceFirst(haystack: string, needle: string, replacement: string): string {
    const i = haystack.indexOf(needle);
    return i === -1 ? haystack : haystack.slice(0, i) + replacement + haystack.slice(i + needle.length);
}

function errMsg(e: unknown): string {
    return e instanceof Error ? e.message : String(e);
}
