import { MenuItem } from 'primeng/api';

import { MenuItemEntity } from '@dotcms/dotcms-models';

/**
 * Parameters for processing special routes and route handlers.
 */
export interface ProcessSpecialRouteParams {
    /**
     * The URL to process.
     */
    url: string;
    /**
     * Array of menu items.
     */
    menu: MenuItemEntity[];
    /**
     * Current breadcrumbs array.
     */
    breadcrumbs: MenuItem[];
}

/**
 * Result structure returned by a route handler.
 * Uses discriminated unions to ensure type safety:
 * - 'set' and 'append' types require breadcrumbs array
 * - 'truncate' type requires index and forbids breadcrumbs
 */
export type RouteHandlerResult =
    | {
          type: 'set';
          breadcrumbs: MenuItem[];
      }
    | {
          type: 'append';
          breadcrumbs: MenuItem[];
      }
    | {
          type: 'truncate';
          index: number;
          breadcrumbs?: never;
      };

/**
 * Handler function for special route cases.
 * Receives parameters object with URL, menu items, and breadcrumbs.
 * Returns a RouteHandlerResult or void if no action is needed.
 */
export type RouteHandler = (params: ProcessSpecialRouteParams) => RouteHandlerResult | void;

/**
 * Route handler configuration.
 * Contains a test function to check if the URL matches, and a handler to process it.
 */
interface RouteHandlerConfig {
    /**
     * Test function that returns true if this handler should process the URL.
     * Can use regex, string matching, or any custom logic.
     */
    test: (url: string) => boolean;
    /**
     * Handler function that processes the URL and builds breadcrumbs.
     */
    handler: RouteHandler;
}

/**
 * The Content Drive params that say where the author is browsing. Every other param (the open
 * panel, a folder dialog, …) names something that is only open for a moment.
 */
const CONTENT_DRIVE_BROWSING_PARAMS = ['path', 'filters', 'isTreeExpanded'];

/**
 * The URL a Content Drive crumb links back to: the same folder, filters and tree, without the
 * params that open a panel or a dialog. The crumb outlives them, and once another crumb follows it
 * a click on it would otherwise reopen the content, or start another create, the URL named when
 * the author arrived.
 *
 * The kept params stay exactly as the router wrote them, in the same order, so a click on the
 * crumb comes back as the same URL and truncates the trail to it.
 *
 * @param url A Content Drive URL, as the router reports it.
 * @returns The URL without the panel and dialog params.
 */
function contentDriveCrumbUrl(url: string): string {
    const [urlPath, queryString = ''] = url.split('?');
    const browsing = queryString
        .split('&')
        .filter((param) => CONTENT_DRIVE_BROWSING_PARAMS.includes(param.split('=')[0]));

    return browsing.length > 0 ? `${urlPath}?${browsing.join('&')}` : urlPath;
}

/**
 * Hashmap of special route handlers.
 * Each entry defines a test and handler for a specific route pattern.
 *
 * To add a new special case, simply add a new entry to this map:
 *
 * ```typescript
 * newRoute: {
 *     test: (url: string) => /^\/new-route\/.+$/.test(url),
 *     handler: ({ url, menu, breadcrumbs }) => {
 *         // Your logic here
 *         return { type: 'set' | 'append', breadcrumbs: [...] }
 *     }
 * }
 * ```
 */
export const ROUTE_HANDLERS: Record<string, RouteHandlerConfig> = {
    /**
     * Handles /templates/edit/:id routes.
     */
    templatesEdit: {
        test: (url: string) => /^\/templates\/edit\/[a-zA-Z0-9-]+$/.test(url),
        handler: ({ menu, breadcrumbs }): RouteHandlerResult | void => {
            const templatesItem = menu.find((item) => item.menuLink === '/templates');

            if (templatesItem) {
                // Only build base breadcrumb if it doesn't exist yet
                const hasTemplatesBreadcrumb = breadcrumbs.some(
                    (crumb) => crumb.url === '/dotAdmin/#/templates'
                );

                if (!hasTemplatesBreadcrumb) {
                    return {
                        type: 'set',
                        breadcrumbs: [
                            {
                                label: templatesItem.parentMenuLabel,
                                disabled: true
                            },
                            {
                                label: templatesItem.label,
                                target: '_self',
                                url: '/dotAdmin/#/templates'
                            }
                        ]
                    };
                }
            }
        }
    },

    /**
     * Handles Content Drive URLs that carry query params (`path`, `filters`, `editContent`, …).
     *
     * They never match the menu item, because a URL with params and no `mId` reads as an old
     * bookmark, and Content Drive is reached that way all the time: Content Search and Site Browser
     * links redirect to it, and its own URLs are shared (#37759). Without this the trail stayed as it
     * was: the previous page's title after a redirect, and a blank one in a new tab.
     *
     * - An empty trail (a new tab) starts from Content Drive's place in the menu.
     * - A trail ending on another portlet (a redirect) keeps it, with Content Drive appended.
     * - A trail already ending on Content Drive (a reload whose URL moved on) is left alone.
     *
     * The crumb keeps only the browsing params (see {@link contentDriveCrumbUrl}).
     *
     * The bare `/content-drive`, and a menu click (`?mId=`), match the menu item before this runs.
     */
    contentDrive: {
        test: (url: string) => /^\/content-drive\?/.test(url),

        handler: ({ url, menu, breadcrumbs }): RouteHandlerResult | void => {
            const contentDrive = menu.find((item) => item.menuLink === '/content-drive');

            if (!contentDrive) {
                return;
            }

            const crumb: MenuItem = {
                label: contentDrive.label,
                target: '_self',
                url: `/dotAdmin/#${contentDriveCrumbUrl(url)}`
            };

            if (breadcrumbs.length === 0) {
                return {
                    type: 'set',
                    breadcrumbs: [{ label: contentDrive.parentMenuLabel, disabled: true }, crumb]
                };
            }

            const lastUrl = (breadcrumbs.at(-1)?.url ?? '').replace(/^.*#/, '');

            if (lastUrl.startsWith('/content-drive')) {
                return;
            }

            return { type: 'append', breadcrumbs: [crumb] };
        }
    },

    /**
     * Handles /content?filter= routes.
     */
    contentFilter: {
        test: (url: string) => /\/content\?filter=.+$/.test(url),

        handler: ({ url }): RouteHandlerResult | void => {
            const queryIndex = url.indexOf('?');
            if (queryIndex === -1) {
                return;
            }

            const queryString = url.substring(queryIndex + 1);
            const params = new URLSearchParams(queryString);
            const filter = params.get('filter');

            if (!filter) {
                return;
            }

            const newUrl = `/dotAdmin/#${url}`;
            return {
                type: 'append',
                breadcrumbs: [
                    {
                        label: filter,
                        target: '_self',
                        url: newUrl
                    }
                ]
            };
        }
    }
};

/**
 * Rule that defines when the last breadcrumb should be replaced instead of appended.
 */
interface ReplaceLastCrumbRule {
    test: (item: MenuItem, last: MenuItem) => boolean;
}

/**
 * Rules to determine if the incoming breadcrumb should replace the last one.
 * Add new rules here as new patterns emerge.
 */
const REPLACE_LAST_CRUMB_RULES: Record<string, ReplaceLastCrumbRule> = {
    contentEdit: {
        test: (item, last) => {
            const regex = /\/content[/?].+/;
            const normalize = (url: string | undefined) => (url ?? '').replace(/^.*#/, '');
            return regex.test(normalize(item.url)) && regex.test(normalize(last.url));
        }
    },
    analyticsTab: {
        // Menu item ids for analytics tabs are strings (e.g. 'analytics-engagement'). Strict type
        // check avoids RegExp.test() coercing non-strings (e.g. 0 → "0", null → "null").
        test: (item, last) => {
            const regex = /^analytics-/;
            const itemId = item.id;
            const lastId = last.id;
            if (typeof itemId !== 'string' || typeof lastId !== 'string') {
                return false;
            }
            return regex.test(itemId) && regex.test(lastId);
        }
    }
};

/**
 * Returns true if the incoming breadcrumb item should replace the current last crumb
 * instead of being appended as a new one.
 */
export function shouldReplaceLastCrumb(item: MenuItem, last: MenuItem): boolean {
    return Object.values(REPLACE_LAST_CRUMB_RULES).some((rule) => rule.test(item, last));
}

/**
 * Processes a URL using the special route handlers hashmap.
 * Iterates through all handlers and executes the first one that matches.
 * First checks if the URL already exists in breadcrumbs and returns truncate if found.
 *
 * @param params - Object containing url, menu, and breadcrumbs
 * @returns RouteHandlerResult if a handler matches and produces a result, undefined otherwise
 */
export function processSpecialRoute(params: ProcessSpecialRouteParams): RouteHandlerResult | void {
    const { url, breadcrumbs } = params;
    const newUrl = `/dotAdmin/#${url}`;

    // Check if the URL already exists in breadcrumbs
    const existingIndex = breadcrumbs.findIndex((crumb) => {
        return crumb.url === newUrl;
    });

    if (existingIndex > -1) {
        return {
            type: 'truncate',
            index: existingIndex
        };
    }

    // If not found, continue with special route handlers
    const handlerConfig = Object.values(ROUTE_HANDLERS).find((config) => config.test(url));

    if (handlerConfig) {
        return handlerConfig.handler(params);
    }
}
