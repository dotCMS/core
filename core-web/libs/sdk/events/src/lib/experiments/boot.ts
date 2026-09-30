import {
    BOOT_STATE_KEY,
    DEFAULT_VARIANT,
    EXPERIMENT_ATTRIBUTE,
    QUIET_MS,
    REVEAL_STYLE_ID,
    STORAGE_KEYS,
    VARIANT_ATTRIBUTE,
    VARIANT_QUERY_PARAM
} from './constants';
import { decideVariant } from './decision';

import type { ExperimentBootState } from './models';

/**
 * What the boot script needs, embedded as JSON because the function runs detached.
 */
export interface ExperimentBootConfig {
    id: string;
    rendered: string;
    assignmentsKey: string;
    disabledUntilKey: string;
    bootKey: string;
    styleId: string;
    revealRule: string;
    param: string;
    defaultVariant: string;
    quietMs: number;
}

/**
 * Applies, while the HTML is parsed and before the app's scripts load, what `decide` says from
 * what the engine stored on an earlier page: it shows the content, or stops the page and
 * replaces it with the assigned variant. It never calls dotCMS: when nothing is stored, the
 * content stays hidden and the engine decides once it loads.
 *
 * It is printed into the page with `Function.prototype.toString`, next to `decideVariant`, so
 * it must not reference anything outside itself: the decision arrives as `decide`, and every
 * value in `config`.
 *
 * @param decide - `decideVariant`, the decision the engine makes too
 * @param config - The page's experiment and the shared names
 * @param w - The window, a parameter so tests can pass their own
 */
export function dotcmsExperimentBoot(
    decide: typeof decideVariant,
    config: ExperimentBootConfig,
    w: Window = window
): void {
    const reveal = (): void => {
        const doc = w.document;
        const Sheet = (w as unknown as typeof globalThis).CSSStyleSheet;

        // An adopted style sheet where the browser has them: no Content-Security-Policy blocks
        // a change through the CSSOM, while it blocks a style element without the page's nonce
        if ('adoptedStyleSheets' in doc && typeof Sheet === 'function') {
            try {
                const sheet = new Sheet();
                const sheets = Array.prototype.slice.call(doc.adoptedStyleSheets);

                sheet.replaceSync(config.revealRule);
                sheets.push(sheet);
                doc.adoptedStyleSheets = sheets;

                return;
            } catch {
                // A style element instead
            }
        }

        let style = doc.getElementById(config.styleId);

        if (!style) {
            style = doc.createElement('style');
            style.id = config.styleId;
            doc.head.appendChild(style);
        }

        style.appendChild(doc.createTextNode(config.revealRule));
    };

    try {
        // Inside the UVE editor nothing is decided and nothing stays hidden
        if (w.parent !== w) {
            reveal();

            return;
        }

        const read = (key: string): unknown => {
            try {
                return JSON.parse(w.localStorage.getItem(key) || 'null');
            } catch {
                return null;
            }
        };
        const now = Date.now();
        const decision = decide({
            experimentId: config.id,
            rendered: config.rendered,
            href: w.location.href,
            stored: read(config.assignmentsKey),
            disabled: Number(read(config.disabledUntilKey) || 0) > now,
            now,
            param: config.param,
            defaultVariant: config.defaultVariant
        });

        // A new visitor: the engine asks dotCMS once it loads
        if (decision.kind === 'unknown') {
            return;
        }

        if (decision.kind !== 'redirect') {
            reveal();

            return;
        }

        const state: ExperimentBootState = { redirectedTo: decision.url };
        (w as unknown as Record<string, unknown>)[config.bootKey] = state;

        // No more parsing, scripts or images on the page being replaced. It still runs until
        // the variant's document arrives: its new requests wait, except keepalive ones (an
        // analytics flush), and all of them go out again if it is still here after quietMs.
        w.stop();
        w.location.replace(decision.url);

        const send = w.fetch;

        w.fetch = (input, init) =>
            init && init.keepalive
                ? send.call(w, input, init)
                : new Promise<Response>(() => undefined);
        w.setTimeout(() => {
            w.fetch = send;
        }, config.quietMs);
    } catch {
        // Storage blocked: the engine decides when it loads
    }
}

/** The page's experiment, as the markup passes it to `buildExperimentBootScript`. */
export interface ExperimentBootOptions {
    /** The experiment running on the page. */
    experimentId: string;
    /** The variant the server rendered. */
    variant: string;
}

/** Escapes a value for a CSS attribute selector in double quotes. */
const escapeAttribute = (value: string): string => value.replace(/["\\]/g, '\\$&');

/**
 * Builds the configuration the boot script runs with.
 *
 * @param options - The experiment and the variant the server rendered
 * @returns The configuration
 */
export const createExperimentBootConfig = ({
    experimentId,
    variant
}: ExperimentBootOptions): ExperimentBootConfig => ({
    id: experimentId,
    rendered: variant,
    assignmentsKey: STORAGE_KEYS.assignments,
    disabledUntilKey: STORAGE_KEYS.disabledUntil,
    bootKey: BOOT_STATE_KEY,
    styleId: REVEAL_STYLE_ID,
    // Keyed by the rendered variant too: a route kept in the page with another render stays hidden
    revealRule: `:root [${EXPERIMENT_ATTRIBUTE}="${escapeAttribute(experimentId)}"][${VARIANT_ATTRIBUTE}="${escapeAttribute(variant)}"]{visibility:visible !important;animation:none !important}`,
    param: VARIANT_QUERY_PARAM,
    defaultVariant: DEFAULT_VARIANT,
    quietMs: QUIET_MS
});

/**
 * Builds the inline script printed before an experiment's content, right after the rule
 * that hides it: the boot function, called with the decision function and its config.
 *
 * @param options - The experiment and the variant the server rendered
 * @returns The script's source
 */
export const buildExperimentBootScript = (options: ExperimentBootOptions): string => {
    // `<` is escaped so no value can close the script element
    const json = JSON.stringify(createExperimentBootConfig(options)).replace(/</g, '\\u003c');

    return `(${dotcmsExperimentBoot.toString()})(${decideVariant.toString()}, ${json});`;
};
