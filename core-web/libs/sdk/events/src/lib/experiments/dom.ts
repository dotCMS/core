import {
    DECISION_TIMEOUT_MS,
    DEFAULT_VARIANT,
    EXPERIMENT_ATTRIBUTE,
    QUIET_MS,
    REVEAL_STYLE_ID,
    VARIANT_ATTRIBUTE,
    VARIANT_QUERY_PARAM
} from './constants';

import { CONTENTLET_CLASS, CONTENTLET_RESCAN_EVENT } from '../contentlets/constants';

import type { PageExperimentMark } from './models';

const revealedRules = new Set<string>();

/**
 * Reads the experiment the current page carries in its row marks.
 *
 * @returns The experiment and the variant the server rendered, or null
 */
export const readPageExperiment = (): PageExperimentMark | null => {
    const row = document.querySelector<HTMLElement>(`[${EXPERIMENT_ATTRIBUTE}]`);
    const experimentId = row?.getAttribute(EXPERIMENT_ATTRIBUTE);

    if (!row || !experimentId) {
        return null;
    }

    return {
        experimentId,
        variant: row.getAttribute(VARIANT_ATTRIBUTE) || DEFAULT_VARIANT
    };
};

/**
 * Shows rows the renderer hid. With an id, only that experiment's rows; without one, all
 * of them (engine off, inside UVE).
 *
 * The rule is added to a style element in the head. It beats the renderer's hiding rule
 * through `:root` and `!important`, whatever the order of the two in the document.
 *
 * @param experimentId - The experiment whose rows to show
 */
export const revealRows = (experimentId?: string): void => {
    if (typeof document === 'undefined') {
        return;
    }

    const selector = experimentId
        ? `[${EXPERIMENT_ATTRIBUTE}="${CSS.escape(experimentId)}"]`
        : `[${EXPERIMENT_ATTRIBUTE}]`;
    const rule = `:root ${selector}{visibility:visible !important;animation:none !important}`;

    if (revealedRules.has(rule)) {
        return;
    }

    revealedRules.add(rule);

    let style = document.getElementById(REVEAL_STYLE_ID);

    if (!style) {
        style = document.createElement('style');
        style.id = REVEAL_STYLE_ID;
        document.head.appendChild(style);
    }

    style.appendChild(document.createTextNode(rule));

    // The rows show through a style rule, which no tracker's DOM observer notices, and the
    // impression tracker skipped them while they were hidden.
    window.dispatchEvent(new CustomEvent(CONTENTLET_RESCAN_EVENT));
};

/** The style element that hides the contentlet wrappers while a new visitor is decided. */
const CONTENTLET_HIDING_STYLE_ID = 'dotcms-experiment-contentlets';

/**
 * Hides the contentlet wrappers dotCMS prints on the pages it renders: they hold what a variant
 * changes. Like the markup's rule, it shows them on its own after `DECISION_TIMEOUT_MS`.
 */
export const hideContentlets = (): void => {
    if (document.getElementById(CONTENTLET_HIDING_STYLE_ID)) {
        return;
    }

    const style = document.createElement('style');
    style.id = CONTENTLET_HIDING_STYLE_ID;
    style.textContent =
        `.${CONTENTLET_CLASS}{visibility:hidden;animation:dotcms-experiment-timeout 0s linear ${DECISION_TIMEOUT_MS / 1000}s forwards}` +
        '@keyframes dotcms-experiment-timeout{to{visibility:visible}}';
    document.head.appendChild(style);
};

/**
 * Shows the contentlet wrappers `hideContentlets` hid, and asks the impression tracker to scan
 * again for them.
 */
export const revealContentlets = (): void => {
    const style = document.getElementById(CONTENTLET_HIDING_STYLE_ID);

    if (!style) {
        return;
    }

    style.remove();
    window.dispatchEvent(new CustomEvent(CONTENTLET_RESCAN_EVENT));
};

/**
 * The variant a page dotCMS rendered shows: the one its contentlet wrappers carry in
 * `data-dot-variant`, or, when the variant changes none of them (or the wrappers are not
 * parsed yet), the one its URL asks for. dotCMS renders the variant `variantName` names.
 *
 * @param href - The page's URL
 * @returns The rendered variant
 */
export const readRenderedVariant = (href: string): string => {
    const wrapped = Array.from(
        document.querySelectorAll<HTMLElement>(`.${CONTENTLET_CLASS}[${VARIANT_ATTRIBUTE}]`)
    )
        .map((wrapper) => wrapper.getAttribute(VARIANT_ATTRIBUTE))
        .find((variant) => !!variant && variant !== DEFAULT_VARIANT);

    return wrapped ?? new URL(href).searchParams.get(VARIANT_QUERY_PARAM) ?? DEFAULT_VARIANT;
};

/**
 * Holds the page's new `fetch` calls, except `keepalive` ones (an analytics flush), for
 * `QUIET_MS`. A page being replaced runs until the variant's document arrives, and a hydrated
 * app goes on prefetching its links; held, they do not compete with the variant's server render.
 */
export const holdRequests = (): void => {
    const send = window.fetch;

    window.fetch = (input, init) =>
        init?.keepalive ? send.call(window, input, init) : new Promise<Response>(() => undefined);
    setTimeout(() => {
        window.fetch = send;
    }, QUIET_MS);
};

/**
 * Replaces the page with a variant's URL: stops its loading, navigates, and holds its new
 * requests, as the boot script does when it decides from storage.
 *
 * @param url - The variant's URL
 */
export const leavePageFor = (url: string): void => {
    window.stop();
    window.location.replace(url);
    holdRequests();
};
