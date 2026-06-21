import { colorTokenAt, nudgeToClear, parseColor, parseTargetRatio } from './contrast';
import {
    attribute,
    attributeByTarget,
    findColorDeclNode,
    parseColorRules,
    parseCustomProperties,
    resolveVarValue,
    type CssRule
} from './css-attribution';
import { extractInlineSourceMap, resolveDeclarationValue } from './css-source-map';

import { CSS_RULE_CODES, countViolations } from '../../domain/policies';
import { errMsg, noop, saveMime, shortName } from '../../shared/agent-utils';
import { type RunFixCaps, type RunFixDeps } from '../types';

import type { FixRequest, FixResult } from '../../domain/contract';
import type { ScanFinding, ScanResult } from '../../dotcms/dotcms-client';

/**
 * The deterministic CSS contrast engine (PASS 1, plan §1.5) — NO LLM.
 *
 * Attribute a contrast finding against the ONE compiled stylesheet the page
 * loaded, resolve the winning rule's color (following `var(--x)`) to a concrete
 * value, nudge it with WCAG math until the pair clears, and edit the smallest
 * source of truth — the SCSS source via sourcemap, the custom-property definition,
 * or the compiled `.css` directly. Each edit is saved to WORKING, re-scanned, and
 * auto-reverted if it makes things worse.
 *
 * The orchestrator (run-fix) owns sequencing + the report; this module owns the
 * per-violation CSS decision and the guarded save/rescan.
 */

/** Per-violation working context, threaded through the CSS pass. */
export interface ProcessCtx {
    finding: ScanFinding;
    req: FixRequest;
    deps: RunFixDeps;
    stylesheets: string[];
    currentContent: Record<string, string>;
    editedPaths: Set<string>;
    caps: RunFixCaps;
    previewUrl: string;
    baselineViolations: number;
    /** Per-run cache: a stylesheet's parsed color rules + sourcemap, keyed by URL.
     * Contrast violations cluster on one compiled stylesheet, so fetch+parse it
     * once for the whole run instead of per violation. */
    stylesheetCache: Map<string, ParsedStylesheet>;
    /** Set by saveAndRescan: the fresh PREVIEW scan taken after the edit (kept or
     * reverted). The main loop reuses it to refresh which violations remain — so a
     * shared-rule edit that clears many violations costs ONE re-scan, not N. */
    lastScan?: ScanResult;
}

export interface ParsedStylesheet {
    rules: CssRule[];
    map: ReturnType<typeof extractInlineSourceMap>;
    /** Custom-property name→value map, to resolve `var(--x)` to a concrete color. */
    vars: Map<string, string>;
    /** The raw CSS text — used to edit directly when there's no sourcemap. */
    css: string;
}

/** Fetch + parse a stylesheet once per run (cached): color rules, vars, inline sourcemap. */
async function loadStylesheet(ctx: ProcessCtx, sheet: string): Promise<ParsedStylesheet> {
    const cached = ctx.stylesheetCache.get(sheet);
    if (cached) {
        return cached;
    }
    const css = await ctx.deps.client.fetchStylesheet(sheet);
    const parsed: ParsedStylesheet = {
        rules: parseColorRules(css),
        map: extractInlineSourceMap(css),
        vars: parseCustomProperties(css),
        css
    };
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
export async function processViolation(ctx: ProcessCtx): Promise<FixResult> {
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
    return {
        ruleId: finding.code,
        status: 'reported',
        reason: 'Deferred to agentic research pass.'
    };
}

/**
 * CSS path (deterministic, S1.5): attribute against the ONE compiled stylesheet,
 * map the winning rule's value back to its SCSS source via the sourcemap, edit
 * only that one source file. The LLM sees only the matched rule (~100 tokens) —
 * the 150+ SCSS partials are never read or sent.
 */
async function processCssViolation(ctx: ProcessCtx): Promise<FixResult> {
    const { finding, deps, stylesheets, editedPaths, caps } = ctx;
    const step = deps.onStep ?? noop;
    const base: FixResult = { ruleId: finding.code, status: 'reported' };

    if (stylesheets.length === 0) {
        return {
            ...base,
            reason: 'No same-origin stylesheet found on the page to attribute against.'
        };
    }

    // axe's data is the SOURCE OF TRUTH for which colors are in play.
    const data = finding.data;
    const fg = data?.fgColor ? parseColor(data.fgColor) : null;
    const bg = data?.bgColor ? parseColor(data.bgColor) : null;
    if (!data || (!fg && !bg)) {
        return {
            ...base,
            reason: 'Scanner did not provide fg/bg color data for this contrast violation; reported.'
        };
    }

    // Gather candidate rules that match the element, then pick the (rule, decl)
    // whose VALUE equals axe's flagged fgColor or bgColor — that decl is provably
    // the one axe measured. We do NOT trust specificity ranking to guess the decl;
    // the color identity decides. The OTHER color of the pair is the counterpart.
    let matches: CssRule[] = [];
    let map: ParsedStylesheet['map'] = null;
    let vars: Map<string, string> = new Map();
    let matchedSheet: ParsedStylesheet | null = null;
    for (const sheet of stylesheets) {
        let parsed: ParsedStylesheet;
        try {
            parsed = await loadStylesheet(ctx, sheet); // cached per run
        } catch (e) {
            return { ...base, status: 'failed', reason: `fetch stylesheet failed: ${errMsg(e)}` };
        }
        // Prefer axe's full target path (handles combinator rules like `.site-nav a`);
        // fall back to element-HTML matching if the target is empty/unparseable.
        const byTarget = finding.selector ? attributeByTarget(finding.selector, parsed.rules) : [];
        const m = byTarget.length > 0 ? byTarget : attribute(finding.context, parsed.rules);
        if (m.length) {
            matches = m;
            map = parsed.map;
            vars = parsed.vars;
            matchedSheet = parsed;
            break;
        }
    }
    if (!matches.length || !matchedSheet) {
        return {
            ...base,
            reason: 'Could not attribute the contrast failure to a CSS rule (no sound match).'
        };
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
        /** The decl's value resolved through `var()` to a concrete color literal. */
        resolvedValue: string;
        /** The custom-property name to edit, if the decl was `var(--name)`; else null. */
        varName: string | null;
        counterpart: NonNullable<ReturnType<typeof parseColor>>;
    }
    // Match on the RESOLVED color (var(--x) → its literal) so we match what axe
    // measured. When the decl is a var, remember the property name — the fix edits
    // the custom-property definition, not the var() reference.
    const varNameOf = (value: string): string | null => {
        const m = value.match(/^\s*var\(\s*(--[\w-]+)/);
        return m ? m[1] : null;
    };
    const candidates: Candidate[] = [];
    for (const rule of matches) {
        for (const d of rule.decls) {
            const resolvedValue = resolveVarValue(d.value, vars);
            const v = parseColor(resolvedValue);
            const varName = varNameOf(d.value);
            if (sameColor(v, fg) && bg) {
                candidates.push({ rule, decl: d, resolvedValue, varName, counterpart: bg });
            } else if (sameColor(v, bg) && fg) {
                candidates.push({ rule, decl: d, resolvedValue, varName, counterpart: fg });
            }
        }
    }
    if (candidates.length === 0) {
        return {
            ...base,
            reason: `No matched CSS rule's color equals axe's flagged fg(${data.fgColor})/bg(${data.bgColor}) — the failing color isn't in an attributable rule (likely inherited/inline/computed); reported.`
        };
    }

    const targetRatio = parseTargetRatio(data.expectedContrastRatio);

    // Try each candidate; the first that resolves to an editable source AND yields a
    // nudge wins. The nudge is computed from the RESOLVED color (the concrete value
    // axe measured), not the declared value (which may be `var(--x)`).
    let lastReason = 'No candidate could be fixed.';
    for (const cand of candidates) {
        const editable = parseColor(cand.resolvedValue);
        const fix = editable ? nudgeToClear(editable, cand.counterpart, targetRatio) : null;
        if (!fix) {
            lastReason = `Cannot reach ${targetRatio}:1 by nudging ${cand.resolvedValue} (${cand.decl.prop}); trying the counterpart.`;
            continue; // this side is unfixable (e.g. white) — try the other
        }

        // Decide WHERE the literal lives and WHAT to replace:
        //   var(--x)  → edit the custom-property definition's literal
        //   sourcemap → map the decl position back to its SCSS source
        //   plain css → edit the literal in the .css directly
        const located = await locateEditTarget(ctx, {
            cand,
            map,
            sheet: matchedSheet,
            stylesheetUrl: stylesheets[0]
        });
        if ('reason' in located) {
            lastReason = located.reason;
            continue;
        }
        const { sourcePath, original, literal } = located;

        if (!editedPaths.has(sourcePath) && editedPaths.size >= caps.maxFiles) {
            return { ...base, file: sourcePath, reason: 'Per-run file cap reached' };
        }
        if (!original.includes(literal)) {
            lastReason = `Source value ${literal} not found in ${shortName(sourcePath)}; trying the counterpart.`;
            continue;
        }

        const where = cand.varName ? `${cand.varName} (${cand.rule.selector})` : cand.rule.selector;
        // Enrich the live step so the UI shows the actual change as it happens —
        // the before→after color + contrast ratio, not just the selector.
        const beforeRatio = data.contrastRatio != null ? `${data.contrastRatio}:1` : '?';
        step(
            'fix',
            `Fixing ${finding.code} → ${where}: ${literal} → ${fix.newColor} (${beforeRatio} → ${fix.achievedRatio.toFixed(2)}:1) in ${shortName(sourcePath)}`
        );
        const edited = replaceFirst(original, literal, fix.newColor);
        if (Buffer.byteLength(edited, 'utf-8') > caps.maxFileBytes) {
            return {
                ...base,
                status: 'failed',
                file: sourcePath,
                reason: 'Edited file exceeds byte cap'
            };
        }
        const diff = `- ${literal}\n+ ${fix.newColor}  (${where}; ${data.contrastRatio ?? '?'}:1 → ${fix.achievedRatio.toFixed(2)}:1, target ${targetRatio}:1)`;
        return saveAndRescan(ctx, {
            path: sourcePath,
            original,
            edited,
            diff,
            blastRadius: cand.varName ? 'token' : 'shared-rule',
            review: cand.varName
                ? `CSS custom property ${cand.varName} in ${shortName(sourcePath)} — affects every element using this token`
                : `CSS color edit (${cand.rule.selector}) in ${shortName(sourcePath)} — may affect every element matching this rule`
        });
    }

    return { ...base, reason: `${lastReason} (needs a design decision); reported.` };
}

interface EditTarget {
    /** Host-qualified path of the file to edit. */
    sourcePath: string;
    /** Current content of that file. */
    original: string;
    /** The exact color literal to replace in that content. */
    literal: string;
}

/**
 * Resolve a contrast candidate to a concrete (file, literal) edit target, choosing
 * the deepest available source:
 *   1. var(--x)        → the custom-property DEFINITION's literal (in its source file)
 *   2. sourcemap (SCSS) → the decl value mapped back to its .scss source line
 *   3. plain .css       → the literal in the compiled .css directly
 *
 * Returns `{ reason }` when this candidate can't be located (so the caller tries
 * the counterpart). The var() path takes priority because editing the token fixes
 * the value at its single source of truth.
 */
async function locateEditTarget(
    ctx: ProcessCtx,
    args: {
        cand: {
            decl: { prop: string; value: string };
            resolvedValue: string;
            varName: string | null;
            rule: CssRule;
        };
        map: ParsedStylesheet['map'];
        sheet: ParsedStylesheet;
        stylesheetUrl: string;
    }
): Promise<EditTarget | { reason: string }> {
    const { cand, map, sheet, stylesheetUrl } = args;

    const contentHost = ctx.req.page.host;
    // The compiled stylesheet's own host-qualified asset path (host from the page
    // request, not the runtime origin in the stylesheet URL).
    const cssPath = cssAssetPath(stylesheetUrl, contentHost);

    // 1. var(--x): edit the custom-property definition's literal. Locate the
    //    `--name: <color>` decl's position; map it to source if there's a sourcemap,
    //    else edit it in the .css directly. The resolved color IS the literal to find.
    if (cand.varName) {
        const literal = cand.resolvedValue;
        const defNode = findVarDefinition(sheet.css, cand.varName);
        // Map the var definition's position to source when a sourcemap exists.
        if (map && defNode) {
            const resolved = await resolveDeclarationValue(
                map,
                defNode.line,
                defNode.column,
                cand.varName
            );
            if (resolved && resolved.content !== undefined) {
                const sourcePath = resolveSourcePath(resolved.source, stylesheetUrl, contentHost);
                const sourceLiteral =
                    colorTokenAt(resolved.content, resolved.line, resolved.column) ?? literal;
                const original = await readSource(ctx, sourcePath, resolved.content);
                return { sourcePath, original, literal: sourceLiteral };
            }
        }
        // No sourcemap (plain .css) → edit the literal in the stylesheet itself.
        const original = await readSource(ctx, cssPath, sheet.css);
        return { sourcePath: cssPath, original, literal };
    }

    // 2. sourcemap (SCSS): map the decl value position back to its .scss source.
    const declNode = findColorDeclNode(cand.rule.node, cand.decl.prop, cand.decl.value);
    const declPos = declNode?.source?.start;
    if (map && declNode && declPos) {
        const resolved = await resolveDeclarationValue(
            map,
            declPos.line,
            declPos.column,
            declNode.prop
        );
        if (resolved && resolved.content !== undefined) {
            const sourcePath = resolveSourcePath(resolved.source, stylesheetUrl, contentHost);
            const sourceLiteral = colorTokenAt(resolved.content, resolved.line, resolved.column);
            if (sourceLiteral) {
                const original = await readSource(ctx, sourcePath, resolved.content);
                return { sourcePath, original, literal: sourceLiteral };
            }
        }
    }

    // 3. plain .css (no sourcemap): edit the literal in the compiled .css directly.
    const original = await readSource(ctx, cssPath, sheet.css);
    return { sourcePath: cssPath, original, literal: cand.resolvedValue };
}

/** Host-qualify the compiled stylesheet's path: `//<contentHost>/<pathname>`. */
function cssAssetPath(stylesheetUrl: string, contentHost: string): string {
    try {
        return `//${contentHost}${new URL(stylesheetUrl).pathname}`;
    } catch {
        return stylesheetUrl;
    }
}

/** Locate a `--name: <value>` definition's 1-based line/column in raw CSS, or null. */
function findVarDefinition(css: string, name: string): { line: number; column: number } | null {
    const lines = css.split('\n');
    for (let i = 0; i < lines.length; i++) {
        const idx = lines[i].indexOf(`${name}:`);
        if (idx !== -1) {
            return { line: i + 1, column: idx };
        }
    }
    return null;
}

/** Read a source file: prefer in-run edits, then sourcemap-embedded content, then fetch. */
async function readSource(ctx: ProcessCtx, path: string, embedded: string): Promise<string> {
    if (ctx.currentContent[path] !== undefined) {
        return ctx.currentContent[path];
    }
    if (embedded !== undefined) {
        return embedded;
    }
    return ctx.deps.client.read(path);
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

/** Save-working, verify bytes, re-scan PREVIEW, auto-revert on regression. Shared by both paths. */
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
        return {
            ...base,
            status: 'failed',
            file: path,
            identifier: spec.identifier,
            reason: `save failed: ${errMsg(e)}`
        };
    }
    if (!saved || saved.fileSize <= 0) {
        return {
            ...base,
            status: 'failed',
            file: path,
            identifier: saved?.identifier ?? spec.identifier,
            reason: 'save returned 0 bytes; not applied'
        };
    }
    editedPaths.add(path);
    currentContent[path] = edited;

    step('rescan', `Re-scanning after ${ctx.finding.code}`);
    try {
        const rescan = await client.scan(ctx.previewUrl);
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
function resolveSourcePath(source: string, stylesheetUrl: string, contentHost: string): string {
    // The stylesheet lives at …/themes/<theme>/css/<sheet>; sources are relative
    // to that css/ dir (e.g. "custom-styles/_variables-custom.scss"). Resolve against
    // the stylesheet's directory, then return the host-qualified //host/path form.
    // The host is the CONTENT host (from the page request) — the stylesheet URL's
    // host may be the runtime origin (localhost:8080), not a valid asset host.
    try {
        const sheet = new URL(stylesheetUrl);
        const cssDir = sheet.pathname.slice(0, sheet.pathname.lastIndexOf('/') + 1);
        const abs = new URL(source, 'http://h' + cssDir).pathname; // normalize ../ segments
        return `//${contentHost}${abs}`;
    } catch {
        return source;
    }
}

/** Replace only the first occurrence of `needle` (literal) in `haystack`. */
function replaceFirst(haystack: string, needle: string, replacement: string): string {
    const i = haystack.indexOf(needle);
    return i === -1
        ? haystack
        : haystack.slice(0, i) + replacement + haystack.slice(i + needle.length);
}
