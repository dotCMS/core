import { MenuItem } from 'primeng/api';

import { MenuItemEntity } from '@dotcms/dotcms-models';

/**
 * Helper functions available to route handlers for building breadcrumbs.
 */
interface BreadcrumbHelpers {
    /**
     * Sets the entire breadcrumb array.
     */
    set: (breadcrumbs: MenuItem[]) => void;
    /**
     * Adds a new breadcrumb to the end of the breadcrumb array.
     */
    append: (item: MenuItem) => void;
}

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
    /**
     * Helper functions for building breadcrumbs.
     */
    helpers: BreadcrumbHelpers;
}

/**
 * Handler function for special route cases.
 * Receives parameters object with URL, menu items, breadcrumbs, and helper functions.
 */
export type RouteHandler = (params: ProcessSpecialRouteParams) => void;

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
 *     handler: ({ url, menu, breadcrumbs, helpers: { set, append } }) => {
 *         // Your logic here
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
        handler: ({ menu, breadcrumbs, helpers: { set } }) => {
            const templatesItem = menu.find((item) => item.menuLink === '/templates');

            if (templatesItem) {
                // Only build base breadcrumb if it doesn't exist yet
                const hasTemplatesBreadcrumb = breadcrumbs.some(
                    (crumb) => crumb.url === '/dotAdmin/#/templates'
                );

                if (!hasTemplatesBreadcrumb) {
                    set([
                        {
                            label: 'Home',
                            disabled: true
                        },
                        {
                            label: templatesItem.parentMenuLabel,
                            disabled: true
                        },
                        {
                            label: templatesItem.label,
                            target: '_self',
                            url: '/dotAdmin/#/templates'
                        }
                    ]);
                }
            }
        }
    },

    /**
     * Handles /content?filter= routes.
     */
    contentFilter: {
        test: (url: string) => /\/content\?filter=.+$/.test(url),

        handler: ({ url, helpers: { append } }) => {
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
            append({
                label: filter,
                target: '_self',
                url: newUrl
            });
        }
    }
};

/**
 * Processes a URL using the special route handlers hashmap.
 * Iterates through all handlers and executes the first one that matches.
 *
 * @param params - Object containing url, menu, breadcrumbs, and helpers
 */
export function processSpecialRoute(params: ProcessSpecialRouteParams): void {
    const { url } = params;
    const handler = Object.values(ROUTE_HANDLERS).find((config) => config.test(url));

    if (handler) {
        handler.handler(params);
    }
}
