
import {
    errMsg,
    isFixableViolation,
    isViolation,
    noop,
    saveMime,
    shortName
} from './agent-utils';
import { colorTokenAt, nudgeToClear, parseColor, parseTargetRatio } from './contrast';
import { attribute, findColorDeclNode, parseColorRules, type CssRule } from './css-attribution';
import { extractInlineSourceMap, resolveDeclarationValue } from './css-source-map';
import { DotcmsClient, type ScanFinding, type ScanResult } from './dotcms-client';
import { runResearch } from './researchLoop';

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
    /** Disable PASS 2 agentic research (default on). Set false to run deterministic-only. */
    research?: boolean;
    /** maxSteps for the PASS 2 research loop (default 40). */
    researchMaxSteps?: number;
}

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


export async function runFix(req: FixRequest, deps: RunFixDeps): Promise<FixReport> {
    const { client } = deps;
    const caps = deps.caps ?? DEFAULT_CAPS;
    const step = deps.onStep ?? noop;
    const { live, editMode } = scanUrls(req);

    // 1. SCAN live (human-facing "before") + EDIT_MODE baseline (the apples-to-
    // apples basis for revert decisions). Independent → run concurrently.
    step('scan', 'Scanning live + working baseline');
    const [liveScan, baselineScan] = await Promise.all([client.scan(live), client.scan(editMode)]);

    // If a render-affecting resource (stylesheet/script) failed to load, the scan
    // measured a broken/unstyled render — fixing against it (esp. contrast) is
    // unsafe. Abort and report inconclusively rather than "fix" a styleless page.
    if (baselineScan.renderReliable === false || liveScan.renderReliable === false) {
        const warned = (baselineScan.renderWarnings ?? liveScan.renderWarnings ?? [])
            .map((w) => `${w.resourceType ?? 'resource'} ${w.status ?? w.errorText ?? 'failed'}: ${w.url}`)
            .join('; ');
        step('scan', `Render unreliable — aborting (a sub-resource failed to load): ${warned}`);
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
            stylesheets,
            currentContent,
            editedPaths,
            caps,
            editModeUrl: editMode,
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
        // What PASS 1 already touched — so PASS 2's file edits aren't double-reported.
        const pass1Files = new Set(results.filter((r) => r.file).map((r) => r.file));
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
                // Record each file the research pass edited (confirmed by the final
                // re-scan below — we don't claim per-violation here).
                for (const p of research.editedPaths) {
                    if (!pass1Files.has(p)) {
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

/** Violation count from the normalized scan (normalizeAxe always sets findings.violations). */
function countViolations(scan: ScanResult): number {
    return scan.findings.violations;
}

interface ProcessCtx {
    finding: ScanFinding;
    req: FixRequest;
    deps: RunFixDeps;
    stylesheets: string[];
    currentContent: Record<string, string>;
    editedPaths: Set<string>;
    caps: RunFixCaps;
    editModeUrl: string;
    baselineViolations: number;
    /** Per-run cache: a stylesheet's parsed color rules + sourcemap, keyed by URL.
     * Contrast violations cluster on one compiled stylesheet, so fetch+parse it
     * once for the whole run instead of per violation. */
    stylesheetCache: Map<string, ParsedStylesheet>;
    /** Set by saveAndRescan: the fresh EDIT_MODE scan taken after the edit (kept or
     * reverted). The main loop reuses it to refresh which violations remain — so a
     * shared-rule edit that clears many violations costs ONE re-scan, not N. */
    lastScan?: ScanResult;
}

interface ParsedStylesheet {
    rules: CssRule[];
    map: ReturnType<typeof extractInlineSourceMap>;
}

/** Fetch + parse a stylesheet once per run (cached): its color rules + inline sourcemap. */
async function loadStylesheet(ctx: ProcessCtx, sheet: string): Promise<ParsedStylesheet> {
    const cached = ctx.stylesheetCache.get(sheet);
    if (cached) {
        return cached;
    }
    const css = await ctx.deps.client.fetchStylesheet(sheet);
    const parsed: ParsedStylesheet = { rules: parseColorRules(css), map: extractInlineSourceMap(css) };
    ctx.stylesheetCache.set(sheet, parsed);
    return parsed;
}

/**
 * Route a violation. PASS 1 handles only color-contrast, deterministically (no
 * LLM, no forced-JSON). Everything else (VTL markup: alt/aria/labels/structure)
 * is left to PASS 2 — the tool-calling research loop — which is both more robust
 * (tool calls, not Output.object) and has more reach (researches across files).
 * So non-contrast violations return `reported` here and flow to PASS 2 as the
 * unresolved set.
 */
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
    // Non-contrast → defer to PASS 2 (agentic, tool-calling). Marked reported so
    // it joins the unresolved set the research loop works on.
    return { ruleId: finding.code, status: 'reported', reason: 'Deferred to agentic research pass.' };
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
    let matches: CssRule[] = [];
    let map: ParsedStylesheet['map'] = null;
    for (const sheet of stylesheets) {
        let parsed: ParsedStylesheet;
        try {
            parsed = await loadStylesheet(ctx, sheet); // cached per run
        } catch (e) {
            return { ...base, status: 'failed', reason: `fetch stylesheet failed: ${errMsg(e)}` };
        }
        const m = attribute(finding.context, parsed.rules);
        if (m.length) {
            matches = m;
            map = parsed.map;
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
        const declNode = findColorDeclNode(cand.rule.node, cand.decl.prop, cand.decl.value);
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

/** Replace only the first occurrence of `needle` (literal) in `haystack`. */
function replaceFirst(haystack: string, needle: string, replacement: string): string {
    const i = haystack.indexOf(needle);
    return i === -1 ? haystack : haystack.slice(0, i) + replacement + haystack.slice(i + needle.length);
}
