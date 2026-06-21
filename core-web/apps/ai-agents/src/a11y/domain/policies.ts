import type { RenderWarning, ScanResult } from '../dotcms/dotcms-client';

/**
 * Pure decision rules for the fix loop — no I/O, no LLM. Kept in the domain layer
 * so the orchestrator reads as policy + sequencing, and these rules are unit-
 * testable in isolation.
 */

/** axe rule ids governed by CSS (contrast etc.) → routed to the deterministic CSS path. */
export const CSS_RULE_CODES = new Set(['color-contrast', 'color-contrast-enhanced']);

/** Resource types whose load failure makes the rendered page untrustworthy to fix against. */
const RENDER_AFFECTING_TYPES = new Set(['stylesheet', 'script', 'document']);

/** Violation count from a normalized scan (normalizeAxe always sets findings.violations). */
export function countViolations(scan: ScanResult): number {
    return scan.findings.violations;
}

/** Pick the compiled dotCMS stylesheet(s) the page actually loaded (same-origin). */
export function applicableStylesheets(scan: ScanResult, dotcmsBaseUrl: string): string[] {
    let origin = '';
    try {
        origin = new URL(dotcmsBaseUrl).origin;
    } catch {
        origin = '';
    }
    return (scan.stylesheets ?? []).filter((u) => {
        try {
            return new URL(u).origin === origin;
        } catch {
            return false;
        }
    });
}

/**
 * The render-affecting resource failures in a scan (stylesheet/script/document).
 * A 404 on a decorative image / xhr / favicon doesn't make the render untrustworthy,
 * so we key on resource type rather than the scanner's coarse `renderReliable` flag.
 */
export function renderAffectingWarnings(scan: ScanResult): RenderWarning[] {
    return (scan.renderWarnings ?? []).filter((w) =>
        RENDER_AFFECTING_TYPES.has((w.resourceType ?? '').toLowerCase())
    );
}
