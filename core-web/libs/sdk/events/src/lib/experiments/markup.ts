import { buildExperimentBootScript } from './boot';
import { DEFAULT_VARIANT, EXPERIMENT_ATTRIBUTE, HIDING_RULE, VARIANT_ATTRIBUTE } from './constants';

import type { DotCMSEventsExperimentMarkup, DotCMSEventsExperimentPage } from '../models';

/**
 * Builds what a page prints so its experiment runs: the attributes for the element that wraps
 * what the experiment varies, the rule that keeps it hidden, and the boot script that decides
 * the visitor's variant while the HTML is parsed. Render it on the server, with the style and
 * the script before the element. `DotCMSExperiment` (`@dotcms/events/react`) does it for React.
 *
 * @param page - The page asset, requested for the variant in the URL's `variantName`
 * @returns The markup, or null when no experiment runs on the page
 *
 * @example
 * ```ts
 * const markup = experimentMarkup(pageAsset);
 * // <style>{markup.style}</style><script>{markup.script}</script><div {...markup.attributes}>…</div>
 * ```
 */
export const experimentMarkup = (
    page: DotCMSEventsExperimentPage | null | undefined
): DotCMSEventsExperimentMarkup | null => {
    const experimentId = page?.runningExperimentId;

    if (!experimentId) {
        return null;
    }

    const variant = page?.viewAs?.variantId || DEFAULT_VARIANT;

    return {
        attributes: { [EXPERIMENT_ATTRIBUTE]: experimentId, [VARIANT_ATTRIBUTE]: variant },
        style: HIDING_RULE,
        script: buildExperimentBootScript({ experimentId, variant })
    };
};
