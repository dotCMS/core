import { AxeRule, PageScannerA11yResponse } from './dot-page-scanner.service';
import { A11yFindingType, A11yGroup } from './models';

/**
 * Flatten an axe scan result into the display groups both scanner surfaces render:
 * `violations` become errors, `incomplete` (needs manual review) become warnings. One axe
 * rule maps to one group, and its `nodes` are the flagged elements.
 *
 * Lives here — beside the models and the service whose response it consumes — because two
 * features need it: UVE's scanner report panel and the Accessibility Studio. It was
 * previously implemented twice, and the copies had already drifted: the same `target.join`
 * bug had to be fixed in both, and one copy declared `impact` as `AxeImpact | null` when
 * `AxeImpact` already includes `null`.
 */
export function buildA11yGroups(data: PageScannerA11yResponse | null): A11yGroup[] {
    const axe = data?.axe;
    if (!axe) {
        return [];
    }

    return [
        ...mapRules(axe.violations ?? [], 'error'),
        ...mapRules(axe.incomplete ?? [], 'warning')
    ];
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
            // LAST entry, not a join. axe's `target` is an ancestor CHAIN — one entry per
            // frame or shadow-root boundary crossed on the way to the element — so the
            // element's own selector is the last one. Joining them produced a selector LIST,
            // which `querySelector` resolves to whichever matches FIRST: for
            // `['iframe#promo', 'button.cta']` that is the iframe, so an overlay drawn from
            // this selector outlined the whole embed instead of the button inside it.
            selector: node.target?.at(-1) ?? ''
        })),
        count: rule.nodes?.length ?? 0
    }));
}
