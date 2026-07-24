import { AxeImpact, AxeRule, PageScannerA11yResponse } from '@dotcms/portlets/dot-ema/ui';

/**
 * Scan-result display models for the Studio, modeled on edit-ema's
 * dot-page-scanner-a11y-report (its `models.ts` + component are not exported, so
 * the shapes + grouping are replicated here from the public scanner service).
 */

/**
 * UI classification derived from the axe section a rule came from:
 * confirmed `violations` → `error`, `incomplete` (needs review) → `warning`.
 */
export type A11yFindingType = 'error' | 'warning';

/** A flattened element flagged by an axe rule, ready for display. */
export interface A11yGroupItem {
    /** Outer HTML of the offending element. */
    context: string;
    /** First CSS selector axe reported for the element. */
    selector: string;
}

/** One axe rule grouped with every element it flagged. */
export interface A11yGroup {
    code: string;
    type: A11yFindingType;
    message: string;
    impact: AxeImpact | null;
    helpUrl: string;
    items: A11yGroupItem[];
    count: number;
}

/**
 * Flatten an axe result into display groups — `violations` as errors,
 * `incomplete` as warnings. One axe rule maps to one group (its `nodes` are the
 * flagged elements). Mirrors DotPageScannerA11yReportComponent.buildA11yGroups.
 */
export function buildA11yGroups(data: PageScannerA11yResponse | null): A11yGroup[] {
    const axe = data?.axe;
    if (!axe) {
        return [];
    }
    return [...mapRules(axe.violations ?? [], 'error'), ...mapRules(axe.incomplete ?? [], 'warning')];
}

function mapRules(rules: AxeRule[], type: A11yFindingType): A11yGroup[] {
    return rules.map((rule) => ({
        code: rule.id,
        type,
        message: rule.description ?? rule.help ?? '',
        impact: rule.impact ?? null,
        helpUrl: rule.helpUrl ?? '',
        items: (rule.nodes ?? []).map((node) => ({
            context: node.html,
            selector: node.target?.join(', ') ?? ''
        })),
        count: rule.nodes?.length ?? 0
    }));
}
