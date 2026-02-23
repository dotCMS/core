import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Params, Router } from '@angular/router';

import { DEFAULT_PERSONA, PERSONA_KEY } from '../../shared/consts';

/**
 * Query parameters required for EMA (Edit Mode Architecture) pages.
 */
type EmaQueryParams = {
    url: string;
    language_id: number;
    [PERSONA_KEY]: string;
    variantName: string;
};

/**
 * Result of query parameter validation.
 */
type ConfirmQueryParamsResult = {
    /** The missing query parameters with their default values */
    newQueryParams: EmaQueryParams | null;
    /** Whether any required query params were missing and defaults were applied */
    didQueryParamsGetCompleted: boolean;
};

/**
 * Default query parameters required for EMA page rendering.
 * If any of these are missing from the URL, the guard will redirect
 * with these defaults applied.
 */
const DEFAULT_QUERY_PARAMS: ReadonlyArray<{ key: string; value: string }> = [
    {
        key: 'language_id',
        value: '1'
    },
    {
        key: 'url',
        value: '/'
    },
    {
        key: 'com.dotmarketing.persona.id',
        value: DEFAULT_PERSONA.identifier
    }
];

/**
 * Route guard for Edit Mode Architecture (EMA) pages.
 *
 * This guard ensures that all required query parameters are present before
 * allowing navigation to EMA pages. If any required parameters are missing,
 * it redirects to the same route with default values applied.
 *
 * Required query parameters:
 * - `language_id`: The language ID for content rendering (default: 1)
 * - `url`: The page URL path to edit (default: '/')
 * - `com.dotmarketing.persona.id`: The persona identifier (default: DEFAULT_PERSONA)
 *
 * @example
 * // Input: /edit-page/experiments/PAGE_ID?url=/test
 * // Missing: language_id, persona.id
 * // Redirects to: /edit-page/experiments/PAGE_ID?url=/test&language_id=1&com.dotmarketing.persona.id=...
 *
 * @example
 * // Input: /edit-page/experiments/PAGE_ID/EXP_ID/configuration?url=/test
 * // Preserves full nested path on redirect
 */
export const editEmaGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
    const router = inject(Router);
    const childPath = getFullChildPath(route.firstChild);

    const { didQueryParamsGetCompleted, newQueryParams } = confirmQueryParams(route.queryParams);

    if (didQueryParamsGetCompleted && childPath) {
        // Return UrlTree to redirect - this tells Angular to cancel current navigation
        // and start a new one atomically, avoiding the flash caused by competing navigations
        return router.createUrlTree([`/edit-page/${childPath}`], {
            queryParams: {
                ...route.queryParams,
                ...newQueryParams
            }
        });
    }

    return true;
};

/**
 * Recursively traverses all child routes to build the complete URL path.
 * This is necessary because Angular's route tree only gives immediate children,
 * not the full nested path.
 *
 * @param route - The starting route snapshot (typically route.firstChild)
 * @returns The full path string joining all descendant URL segments
 *
 * @example
 * // Route tree: edit-page -> experiments -> PAGE_ID -> EXP_ID -> configuration
 * // Returns: 'experiments/PAGE_ID/EXP_ID/configuration'
 */
function getFullChildPath(route: ActivatedRouteSnapshot | null): string {
    const segments: string[] = [];

    let current = route;
    while (current) {
        // Collect all URL segments from this route level
        for (const segment of current.url) {
            segments.push(segment.path);
        }
        // Move to the next child level
        current = current.firstChild;
    }

    return segments.join('/');
}

/**
 * Validates that all required query parameters are present.
 *
 * Checks each parameter in DEFAULT_QUERY_PARAMS:
 * - For 'url': Also checks if the value is empty or whitespace-only
 * - For other params: Checks if the value is falsy
 *
 * @param queryParams - The current route query parameters
 * @returns Object containing:
 *   - `didQueryParamsGetCompleted`: true if any params were missing
 *   - `newQueryParams`: The missing params with their default values, or null if none missing
 */
function confirmQueryParams(queryParams: Params): ConfirmQueryParamsResult {
    const { missing, ...missingQueryParams } = DEFAULT_QUERY_PARAMS.reduce(
        (acc, { key, value }) => {
            // Special handling for 'url': treat empty/whitespace-only as missing
            if (key === 'url' && queryParams[key]?.trim()?.length === 0) {
                acc[key] = '/';
                acc.missing = true;

                return acc;
            }

            // For all params: treat falsy values as missing
            if (!queryParams[key]) {
                acc[key] = value;
                acc.missing = true;

                return acc;
            }

            return acc;
        },
        {
            missing: false
        }
    );

    if (missing) {
        return {
            didQueryParamsGetCompleted: true,
            newQueryParams: missingQueryParams as EmaQueryParams
        };
    }

    return {
        didQueryParamsGetCompleted: false,
        newQueryParams: null
    };
}
