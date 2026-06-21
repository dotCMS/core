import type { RenderSources, ScanFinding, SourceRef } from '../dotcms/dotcms-client';

/**
 * Shared helpers used by both the deterministic loop (runFix) and the agentic
 * research tools (tools.ts) — kept here so there is a single source of truth for
 * the scan predicates, the stylesheet/editable extension lists, and the small
 * string utilities (they had drifted across the two modules).
 */

const noop = (): void => undefined;
export { noop };

/** `e.message` when it's an Error, else the stringified value. */
export const errMsg = (e: unknown): string => (e instanceof Error ? e.message : String(e));

/** Basename of a host-qualified asset path. */
export const shortName = (p: string): string => p.split('/').pop() ?? p;

// ── Extensions ───────────────────────────────────────────────────────────────

/** Stylesheet extensions (with leading dot) — drive the save MIME type. */
export const STYLESHEET_EXTENSIONS = ['.css', '.scss', '.sass', '.dotsass', '.less'] as const;

/** Editable theme source extensions (no dot) the agent may read/edit. */
export const EDITABLE_EXTENSIONS = ['vtl', 'css', 'scss', 'sass', 'dotsass', 'less'] as const;

/** Content type for a saved working copy: stylesheets → text/css, else text/plain. */
export const saveMime = (path: string): string => {
    const lower = path.toLowerCase();
    return STYLESHEET_EXTENSIONS.some((ext) => lower.endsWith(ext)) ? 'text/css' : 'text/plain';
};

// ── Scan-finding predicates ──────────────────────────────────────────────────

/** A real failure axe reported (vs. needs-review/notices). */
export const isViolation = (f: ScanFinding): boolean =>
    f.resultType === 'violation' || f.type === 'error';

/**
 * Genuine EDIT_MODE editor chrome — the editor's inline edit/add buttons
 * (`data-dot-object="edit-content"` / `"edit-container"` / `"add"`). They live in
 * no template, so they're unfixable noise — drop them. NOTE: `"contentlet"` /
 * `"container"` wrappers are NOT chrome; those carry dotCMS attribution metadata
 * (content type → VTL) and are useful, not noise.
 */
export const isEditorChrome = (f: ScanFinding): boolean =>
    /data-dot-object="(edit-content|edit-container|add)"/.test(f.context ?? '');

/** A violation the agent should act on: a real failure that isn't editor chrome. */
export const isFixableViolation = (f: ScanFinding): boolean => isViolation(f) && !isEditorChrome(f);

// ── Source-ref collection ────────────────────────────────────────────────────

/**
 * Collect de-duped source refs the agent may edit: theme files matching `exts`
 * (extension without dot) plus every container content-type VTL. PASS 1 uses the
 * full editable set; callers can narrow `exts` (e.g. `['vtl']`).
 */
export function collectSourceRefs(
    sources: RenderSources,
    exts: readonly string[] = EDITABLE_EXTENSIONS
): SourceRef[] {
    const refs: SourceRef[] = (sources.theme?.files ?? []).filter((f) =>
        exts.includes((f.extension ?? '').toLowerCase())
    );
    for (const container of Object.values(sources.containers ?? {})) {
        for (const ct of container.contentTypes ?? []) {
            if (ct.path) {
                refs.push({ identifier: ct.identifier, path: ct.path });
            }
        }
    }
    const seen = new Set<string>();
    return refs.filter((r) => (seen.has(r.path) ? false : (seen.add(r.path), true)));
}
