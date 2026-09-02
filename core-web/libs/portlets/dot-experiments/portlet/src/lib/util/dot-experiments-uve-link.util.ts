import { UVE_MODE } from '@dotcms/types';

import { DotExperimentConfigurePage } from '../shared/models';

/**
 * Query param naming which configuration screen opened a variant, so the return leg can land back
 * on it (#37005, FR-005).
 *
 * A query param rather than session or local storage, deliberately: it has to survive a reload and
 * a pasted link, and it has to be *absent* rather than stale when someone deep-links straight into
 * a variant — that absence is what makes the switch the fallback. Storage would fail both.
 */
export const EXPERIMENT_RETURN_PARAM = 'experimentReturn';

/** The only origin this work sets. The legacy card sets none, which is what keeps FR-018 true. */
export const EXPERIMENT_RETURN_PORTLET = 'portlet';

/** Route the Universal Visual Editor serves page content on. */
const UVE_CONTENT_ROUTE = '/edit-page/content';

/**
 * "No persona" — the default UVE itself applies.
 *
 * The literal rather than an import: the source of truth is `DEFAULT_PERSONA.identifier` in the
 * edit-ema portlet's own `shared/consts.ts`, which is not public API of that lib and would be a
 * cross-feature deep import. `libs/edit-content/src/lib/utils/functions.util.ts` already inlines
 * the same string twice when it builds UVE links; this at least names it.
 *
 * Unlike `language_id`, leaving this out would be harmless — `editEmaGuard` fills a missing persona
 * with exactly this value. It is sent anyway so the link is complete on arrival and the guard has
 * no reason to bounce the navigation.
 */
const NO_PERSONA_ID = 'modes.persona.no.persona';

/**
 * Query params UVE reads when opening a page. Named explicitly rather than typed as a
 * `Record<string, …>` so a caller cannot quietly add or misspell one, and so the specs can assert
 * `link.queryParams.language_id` instead of index-signature bracket access.
 */
export interface DotPageEditorQueryParams {
    url: string;
    language_id: number;
    'com.dotmarketing.persona.id': string;
}

/** The variant round-trip adds the experiment context and the origin marker on top. */
export interface DotVariantEditorQueryParams extends DotPageEditorQueryParams {
    variantName: string;
    experimentId: string;
    mode: UVE_MODE;
    [EXPERIMENT_RETURN_PARAM]: typeof EXPERIMENT_RETURN_PORTLET;
}

export interface DotEditorLink<T extends DotPageEditorQueryParams = DotPageEditorQueryParams> {
    commands: string[];
    queryParams: T;
}

export interface VariantEditorLinkParams {
    page: DotExperimentConfigurePage | null;
    variantId: string;
    experimentId: string;
    mode: UVE_MODE;
}

/**
 * The page half of the destination, or `null` when the page data cannot produce a complete one.
 *
 * Returning `null` rather than a partial object is the point (FR-004). `editEmaGuard`
 * *substitutes* defaults for missing params instead of rejecting — `url` becomes `/`, `language_id`
 * becomes `1` — so a partially-formed link does not fail loudly, it opens a plausible-looking wrong
 * page. Refusing here is what lets the caller say why instead.
 */
function pageParamsOf(page: DotExperimentConfigurePage | null): DotPageEditorQueryParams | null {
    const path = page?.path?.trim();

    // `== null` on purpose: language 0 is not a real language id, but neither is it what this
    // guard is for — the case being caught is an unresolved page, which arrives as
    // undefined/null. A truthiness check would also reject 0 without saying so.
    if (!path || page?.languageId == null) {
        return null;
    }

    return {
        url: path,
        language_id: page.languageId,
        'com.dotmarketing.persona.id': NO_PERSONA_ID
    };
}

/**
 * Deep link that opens a variant of an experiment's page in the Universal Visual Editor.
 *
 * Built entirely from the experiment data the portlet already holds — never from
 * `window.location` (FR-003). The legacy card re-parsed the address bar for a `url=` fragment
 * because it was mounted *inside* `/edit-page/experiments/...` and could rely on UVE having put
 * those params there. This screen sits at `/experiments/:id/configuration`, where they do not
 * exist, so the address bar has nothing to offer even if it were acceptable to read.
 *
 * @returns the link, or `null` when the action must be refused with a stated reason.
 */
export function buildVariantEditorLink({
    page,
    variantId,
    experimentId,
    mode
}: VariantEditorLinkParams): DotEditorLink<DotVariantEditorQueryParams> | null {
    const pageParams = pageParamsOf(page);

    // The experiment id is as load-bearing as the page: the return leg resolves by experiment
    // identity, not by page, because a page may host several (FR-005).
    if (!pageParams || !variantId || !experimentId) {
        return null;
    }

    return {
        commands: [UVE_CONTENT_ROUTE],
        queryParams: {
            ...pageParams,
            variantName: variantId,
            experimentId,
            mode,
            [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
        }
    };
}

/**
 * Deep link back to a page in the editor, with no experiment context — the page-filter chip's
 * return affordance on the site-wide list (FR-024).
 *
 * The same page params as {@link buildVariantEditorLink} without the variant, experiment, mode or
 * origin marker: this is a return to the page, not the start of a round-trip.
 */
export function buildPageEditorLink(page: DotExperimentConfigurePage | null): DotEditorLink | null {
    const pageParams = pageParamsOf(page);

    return pageParams ? { commands: [UVE_CONTENT_ROUTE], queryParams: pageParams } : null;
}
