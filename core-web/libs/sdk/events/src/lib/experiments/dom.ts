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

const MARK_SELECTOR = `[${EXPERIMENT_ATTRIBUTE}]`;

const revealedRules = new Set<string>();

// Where the browser has adopted style sheets, reveals go to one: a change through the CSSOM,
// which no Content-Security-Policy blocks, unlike a style element the script creates.
let revealSheet: CSSStyleSheet | undefined;

/**
 * Tells whether an element is on screen as far as React is concerned: not inside a route that
 * Activity keeps in the page with `display: none`, nor inside the hidden container where a
 * Suspense boundary's content waits while it streams in.
 *
 * @param element - The element
 * @returns False when an ancestor keeps it out of view
 */
const isRendered = (element: HTMLElement): boolean => {
    for (let node: HTMLElement | null = element; node; node = node.parentElement) {
        if (node.hidden || node.style.display === 'none') {
            return false;
        }
    }

    return true;
};

/**
 * Reads the experiments the current page renders, from their marks: each experiment and
 * rendered variant once, in document order.
 *
 * @returns The experiments and the variants the server rendered
 */
export const readPageExperiments = (): PageExperimentMark[] => {
    const marks = new Map<string, PageExperimentMark>();

    document.querySelectorAll<HTMLElement>(MARK_SELECTOR).forEach((element) => {
        const experimentId = element.getAttribute(EXPERIMENT_ATTRIBUTE);

        if (!experimentId || !isRendered(element)) {
            return;
        }

        const variant = element.getAttribute(VARIANT_ATTRIBUTE) || DEFAULT_VARIANT;
        marks.set(`${experimentId}\n${variant}`, { experimentId, variant });
    });

    return Array.from(marks.values());
};

/**
 * Calls back when marks are inserted or rewritten, so the engine decides content that arrives
 * after its first decision: Suspense content that streams in, or a route that changes only its
 * query and keeps its elements.
 *
 * @param changed - Called once per batch of DOM changes that touches a mark
 * @returns Stops observing
 */
export const observeMarks = (changed: () => void): (() => void) => {
    const touchesMark = (record: MutationRecord): boolean =>
        record.type === 'attributes' ||
        Array.from(record.addedNodes).some(
            (node) =>
                node instanceof Element &&
                (node.matches(MARK_SELECTOR) || !!node.querySelector(MARK_SELECTOR))
        );
    const observer = new MutationObserver((records) => {
        if (records.some(touchesMark)) {
            changed();
        }
    });

    observer.observe(document.documentElement, {
        childList: true,
        subtree: true,
        attributeFilter: [EXPERIMENT_ATTRIBUTE, VARIANT_ATTRIBUTE]
    });

    return () => observer.disconnect();
};

/**
 * Adds a reveal rule: to the adopted style sheet where the browser has them, or else to a
 * style element in the head.
 *
 * @param rule - The rule
 */
const addRevealRule = (rule: string): void => {
    if ('adoptedStyleSheets' in document) {
        try {
            revealSheet ??= new CSSStyleSheet();

            if (!document.adoptedStyleSheets.includes(revealSheet)) {
                document.adoptedStyleSheets = [...document.adoptedStyleSheets, revealSheet];
            }

            revealSheet.insertRule(rule, revealSheet.cssRules.length);

            return;
        } catch {
            // A style element instead
        }
    }

    let style = document.getElementById(REVEAL_STYLE_ID);

    if (!style) {
        style = document.createElement('style');
        style.id = REVEAL_STYLE_ID;
        document.head.appendChild(style);
    }

    style.appendChild(document.createTextNode(rule));
};

/**
 * Shows rows the renderer hid. With an id and a variant, only that experiment's rows rendered
 * with that variant, so a route kept in the page with another render stays hidden; without an
 * id, all of them (engine off, inside UVE).
 *
 * The rule beats the renderer's hiding rule through `:root` and `!important`, whatever the
 * order of the two in the document.
 *
 * @param experimentId - The experiment whose rows to show
 * @param variant - The variant they were rendered with
 */
export const revealRows = (experimentId?: string, variant?: string): void => {
    if (typeof document === 'undefined') {
        return;
    }

    const selector = experimentId
        ? `[${EXPERIMENT_ATTRIBUTE}="${CSS.escape(experimentId)}"]` +
          (variant ? `[${VARIANT_ATTRIBUTE}="${CSS.escape(variant)}"]` : '')
        : MARK_SELECTOR;
    const rule = `:root ${selector}{visibility:visible !important;animation:none !important}`;

    if (revealedRules.has(rule)) {
        return;
    }

    revealedRules.add(rule);
    addRevealRule(rule);

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
 * A held call never settles, and after `QUIET_MS` only new calls go out.
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
