import { MenuItem } from 'primeng/api';

import { MenuItemEntity } from '@dotcms/dotcms-models';

/**
 * Helper functions available to route handlers for building breadcrumbs.
 */
interface BreadcrumbHelpers {
    /**
     * Sets the entire breadcrumb array.
     */
    setBreadcrumbs: (breadcrumbs: MenuItem[]) => void;
    /**
     * Adds a new breadcrumb to the end of the breadcrumb array.
     */
    addNewBreadcrumb: (item: MenuItem) => void;
}

/**
 * Handler function for special route cases.
 * Receives the URL, menu items, current breadcrumbs, and helper functions. (setBreadcrumbs, addNewBreadcrumb)
 * Returns true if the handler processed the URL, false otherwise.
 */
export type RouteHandler = (
    url: string,
    menu: MenuItemEntity[],
    breadcrumbs: MenuItem[],
    helpers: BreadcrumbHelpers
) => boolean;

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
 *     handler: (url, menu, breadcrumbs, { setBreadcrumbs, addNewBreadcrumb }) => {
 *         // Your logic here
 *         return true;
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
        handler: (_url, menu, breadcrumbs, { setBreadcrumbs }) => {
            const templatesItem = menu.find((item) => item.menuLink === '/templates');

            if (templatesItem) {
                // Only build base breadcrumb if it doesn't exist yet
                const hasTemplatesBreadcrumb = breadcrumbs.some(
                    (crumb) => crumb.url === '/dotAdmin/#/templates'
                );

                if (!hasTemplatesBreadcrumb) {
                    setBreadcrumbs([
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
                return true;
            }
            return false;
        }
    },

    /**
     * Handles /content?filter= routes.
     */
    contentFilter: {
        test: (url: string) => /\/content\?filter=.+$/.test(url),

        handler: (url, _menu, _breadcrumbs, { addNewBreadcrumb }) => {
            const queryIndex = url.indexOf('?');
            if (queryIndex === -1) {
                return false;
            }

            const queryString = url.substring(queryIndex + 1);
            const params = new URLSearchParams(queryString);
            const filter = params.get('filter');

            if (!filter) {
                return false;
            }

            const newUrl = `/dotAdmin/#${url}`;
            addNewBreadcrumb({
                label: filter,
                target: '_self',
                url: newUrl
            });
            return true;
        }
    }
};

/**
 * Processes a URL using the special route handlers hashmap.
 * Iterates through all handlers and executes the first one that matches.
 *
 * @param url - The URL to process
 * @param menu - Array of menu items
 * @param breadcrumbs - Current breadcrumbs array
 * @param helpers - Helper functions for building breadcrumbs
 * @returns true if a handler processed the URL, false otherwise
 */
export function processSpecialRoute(
    url: string,
    menu: MenuItemEntity[],
    breadcrumbs: MenuItem[],
    helpers: BreadcrumbHelpers
): boolean {
    const handler = Object.values(ROUTE_HANDLERS).find((config) => config.test(url));

    if (!handler) {
        return false;
    }

    return handler.handler(url, menu, breadcrumbs, helpers);
}
