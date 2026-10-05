/** App key of the UVE app, whose `configuration` param gets the route editor. */
export const UVE_APP_KEY = 'dotema-config-v2';

/** The catch-all pattern, matched by every page path. */
export const UVE_CATCH_ALL_PATTERN = '.*';

/** One editable entry of the UVE `config` array. */
export interface UveRoute {
    /** Local id used only to track cards in the template; never saved. */
    id: number;
    pattern: string;
    url: string;
    allowedDevURLs: string[];
    /** Keys of the entry the form doesn't edit, kept so saving doesn't drop them. */
    extra: Record<string, unknown>;
    /** Keys of `options` other than `allowedDevURLs`, kept for the same reason. */
    extraOptions: Record<string, unknown>;
}

/** Validation problems of one route, keyed by field. */
export interface UveRouteErrors {
    pattern?: string;
    url?: string;
    allowedDevURLs: (string | null)[];
}

let nextRouteId = 0;

/**
 * Creates an empty route.
 *
 * @param pattern the starting pattern
 */
export function createUveRoute(pattern = ''): UveRoute {
    return {
        id: nextRouteId++,
        pattern,
        url: '',
        allowedDevURLs: [],
        extra: {},
        extraOptions: {}
    };
}

const isPlainObject = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

/**
 * Reads a saved UVE configuration into routes.
 * Returns `null` when the text isn't JSON or doesn't have the `{ "config": [...] }` shape,
 * so the caller can fall back to the raw JSON editor instead of losing data.
 *
 * @param value the saved JSON string
 */
export function parseUveConfig(value: string): UveRoute[] | null {
    if (!value || !value.trim()) {
        return [];
    }

    let parsed: unknown;
    try {
        parsed = JSON.parse(value);
    } catch {
        return null;
    }

    if (!isPlainObject(parsed) || !Array.isArray(parsed.config)) {
        return null;
    }

    const routes: UveRoute[] = [];
    for (const entry of parsed.config) {
        if (!isPlainObject(entry)) {
            return null;
        }

        const { pattern, url, options, ...extra } = entry;
        const { allowedDevURLs, ...extraOptions } = isPlainObject(options) ? options : {};

        if (
            (pattern !== undefined && typeof pattern !== 'string') ||
            (url !== undefined && typeof url !== 'string') ||
            (options !== undefined && !isPlainObject(options)) ||
            (allowedDevURLs !== undefined &&
                (!Array.isArray(allowedDevURLs) ||
                    allowedDevURLs.some((devUrl) => typeof devUrl !== 'string')))
        ) {
            return null;
        }

        routes.push({
            ...createUveRoute(),
            pattern: (pattern as string | undefined) ?? '',
            url: (url as string | undefined) ?? '',
            allowedDevURLs: (allowedDevURLs as string[] | undefined) ?? [],
            extra,
            extraOptions
        });
    }

    return routes;
}

/**
 * Turns routes back into the JSON string the UVE reads.
 *
 * @param routes the routes, in match order
 */
export function serializeUveConfig(routes: UveRoute[]): string {
    const config = routes.map((route) => {
        const devUrls = route.allowedDevURLs.map((devUrl) => devUrl.trim()).filter(Boolean);
        const options = devUrls.length
            ? { ...route.extraOptions, allowedDevURLs: devUrls }
            : route.extraOptions;

        return {
            ...route.extra,
            pattern: route.pattern.trim(),
            url: route.url.trim(),
            ...(Object.keys(options).length ? { options } : {})
        };
    });

    return JSON.stringify({ config }, null, 4);
}

/**
 * Tells whether a value is an absolute http(s) URL.
 *
 * @param value the text to check
 */
export function isHttpUrl(value: string): boolean {
    try {
        const { protocol } = new URL(value);

        return protocol === 'http:' || protocol === 'https:';
    } catch {
        return false;
    }
}

/**
 * Returns the message keys for everything wrong in a route.
 *
 * @param route the route to check
 */
export function validateUveRoute(route: UveRoute): UveRouteErrors {
    const errors: UveRouteErrors = { allowedDevURLs: [] };
    const pattern = route.pattern.trim();
    const url = route.url.trim();

    if (!pattern) {
        errors.pattern = 'apps.uve.route.error.required';
    } else {
        try {
            new RegExp(pattern);
        } catch {
            errors.pattern = 'apps.uve.route.error.pattern';
        }
    }

    if (!url) {
        errors.url = 'apps.uve.route.error.required';
    } else if (!isHttpUrl(url)) {
        errors.url = 'apps.uve.route.error.url';
    }

    errors.allowedDevURLs = route.allowedDevURLs.map((devUrl) =>
        devUrl.trim() && !isHttpUrl(devUrl.trim()) ? 'apps.uve.route.error.url' : null
    );

    return errors;
}

/**
 * Tells whether a route has any validation problem.
 *
 * @param errors the result of {@link validateUveRoute}
 */
export function hasUveRouteErrors(errors: UveRouteErrors): boolean {
    return !!errors.pattern || !!errors.url || errors.allowedDevURLs.some(Boolean);
}
