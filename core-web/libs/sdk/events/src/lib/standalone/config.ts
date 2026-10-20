import type {
    DotCMSEventsConfig,
    DotCMSEventsImpressionsConfig,
    DotCMSEventsLogLevel,
    DotCMSEventsQueueConfig
} from '../models';

/**
 * The script tag dotCMS injects into traditional pages carries this attribute
 * (`dotCMS/src/main/resources/ca/html/analytics_head.html`).
 */
export const SCRIPT_SELECTOR = 'script[data-analytics-auth]';

const LOG_LEVELS: readonly string[] = ['debug', 'info', 'warn', 'error'];

type JsonRecord = Record<string, unknown>;

const isRecord = (value: unknown): value is JsonRecord =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

/**
 * Parses `data-analytics-config`, the site's advanced config from the Content Analytics app.
 * The app stores what the user typed, so single-quoted JSON (`{'queue': false}`) is read too.
 *
 * @param raw - The attribute's value
 * @returns The parsed object, or null when the value is empty or not JSON
 */
const parseAdvancedConfig = (raw: string | null): JsonRecord | null => {
    if (!raw || !raw.trim()) {
        return null;
    }

    const attempts = [
        raw,
        raw
            .replace(/([{,]\s*)'(.+?)'(\s*:)/g, '$1"$2"$3')
            .replace(/(:\s*)'(.+?)'(\s*[,}\]])/g, '$1"$2"$3')
    ];

    for (const attempt of attempts) {
        try {
            const parsed: unknown = JSON.parse(attempt);

            return isRecord(parsed) ? parsed : null;
        } catch {
            // Try the next form
        }
    }

    console.warn('[dotCMS events] data-analytics-config is not JSON; it was ignored');

    return null;
};

/**
 * Keeps the numeric fields of an object that the SDK knows, dropping the rest.
 *
 * @param value - The object from the advanced config
 * @param keys - The fields to keep
 * @returns The fields that hold numbers, or undefined when none does
 */
const numbers = <T extends string>(value: JsonRecord, keys: readonly T[]) => {
    const picked: Partial<Record<T, number>> = {};

    for (const key of keys) {
        const field = value[key];

        if (typeof field === 'number' && Number.isFinite(field)) {
            picked[key] = field;
        }
    }

    return Object.keys(picked).length > 0 ? picked : undefined;
};

/**
 * The options the advanced config may set, validated: anything else it holds is ignored.
 *
 * @param advanced - The parsed advanced config
 * @returns The options it sets
 */
const fromAdvancedConfig = (advanced: JsonRecord): Partial<DotCMSEventsConfig> => {
    const options: Partial<DotCMSEventsConfig> = {};
    const { queue, impressions, clicks, autoPageView, debug, logLevel } = advanced;

    if (typeof queue === 'boolean') {
        options.queue = queue;
    } else if (isRecord(queue)) {
        const picked: DotCMSEventsQueueConfig | undefined = numbers(queue, [
            'eventBatchSize',
            'flushInterval'
        ]);

        if (picked) {
            options.queue = picked;
        }
    }

    if (typeof impressions === 'boolean') {
        options.impressions = impressions;
    } else if (isRecord(impressions)) {
        const picked: DotCMSEventsImpressionsConfig | undefined = numbers(impressions, [
            'visibilityThreshold',
            'dwellMs',
            'maxNodes'
        ]);

        if (picked) {
            options.impressions = picked;
        }
    }

    if (typeof clicks === 'boolean') {
        options.clicks = clicks;
    }

    if (typeof autoPageView === 'boolean') {
        options.autoPageView = autoPageView;
    }

    if (typeof debug === 'boolean') {
        options.debug = debug;
    }

    if (typeof logLevel === 'string' && LOG_LEVELS.includes(logLevel)) {
        options.logLevel = logLevel as DotCMSEventsLogLevel;
    }

    return options;
};

/**
 * Builds the `dotEvents.init` config from the script tag dotCMS injects into traditional pages,
 * in the attribute names its template prints. An attribute with a value overrides
 * `data-analytics-config`. Experiments run, as in any app: this script replaces dotCMS's own
 * experiments script on these pages.
 *
 * @param script - The script tag, or null when the page has none
 * @param origin - The page's origin: dotCMS serves both the page and the script
 * @returns The config, or null without a script or a `data-analytics-auth`
 */
export const readScriptConfig = (
    script: HTMLScriptElement | null,
    origin: string
): DotCMSEventsConfig | null => {
    const siteAuth = script?.getAttribute('data-analytics-auth')?.trim();

    if (!script || !siteAuth) {
        return null;
    }

    const attribute = (name: string): string | null => {
        const value = script.getAttribute(name);

        return value && value.trim() ? value.trim() : null;
    };
    const advanced = parseAdvancedConfig(script.getAttribute('data-analytics-config'));
    const debug = attribute('data-analytics-debug');
    const autoPageView = attribute('data-analytics-auto-page-view');
    const impressions = attribute('data-analytics-impressions');
    const clicks = attribute('data-analytics-clicks');

    return {
        ...(advanced && fromAdvancedConfig(advanced)),
        dotcmsUrl: origin,
        siteAuth,
        ...(debug && { debug: debug === 'true' }),
        // Opt-out, as in @dotcms/analytics: only "false" turns automatic pageviews off
        ...(autoPageView && { autoPageView: autoPageView !== 'false' }),
        ...(impressions && { impressions: impressions === 'true' }),
        ...(clicks && { clicks: clicks === 'true' })
    };
};
