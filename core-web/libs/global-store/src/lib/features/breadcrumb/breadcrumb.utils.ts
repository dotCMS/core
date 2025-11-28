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
