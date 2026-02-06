import { inject, Injectable, Type } from '@angular/core';
import { Route, Router } from '@angular/router';

import { LoggerService } from '@dotcms/dotcms-js';
import { DotMenuItem } from '@dotcms/dotcms-models';

import { DotRemoteModuleWrapperComponent } from './dot-remote-module-wrapper.component';
import { MenuGuardService } from './guards/menu-guard.service';

export interface DynamicRouteConfig {
    path: string;
    component?: Type<unknown>;
    loadComponent?: () => Promise<Type<unknown>>;
    loadChildren?: () => Promise<Route[]>;
    canActivate?: boolean;
    data?: Record<string, unknown>;
}

/** Tracks containers that have already been initialized to avoid double-init errors. */
const initializedContainers = new Set<string>();

/**
 * Registry of known Angular module paths to their import functions.
 * Add entries here for modules that can be dynamically loaded.
 */
const ANGULAR_MODULE_REGISTRY: Record<string, () => Promise<Route[]>> = {
    // Example entries - add your dynamic portlet modules here
    // '@dotcms/portlets/my-custom': () => import('@dotcms/portlets/my-custom').then(m => m.routes)
};

/**
 * Dynamically loads a remote module using Module Federation.
 * Use this for external plugins that are deployed separately.
 *
 * @param remoteEntry - URL to the remote entry file (e.g., 'http://localhost:4201/remoteEntry.js')
 * @param remoteName - Name of the remote module
 * @param exposedModule - Name of the exposed module (e.g., './Routes')
 */
async function loadRemoteModule(
    remoteEntry: string,
    remoteName: string,
    exposedModule: string
): Promise<Route[]> {
    // Ensure webpack Module Federation globals exist before loading the remote.
    // The remote's webpack runtime references these when resolving shared modules.
    // If the host doesn't use MF, we provide no-op stubs so the remote
    // falls back to its own bundled dependencies.
    ensureWebpackSharingGlobals();

    // Dynamically load the remote entry script
    await loadRemoteEntry(remoteEntry, remoteName);

    // Wait for the container to be initialized.
    // Webpack's library type hoists the declaration (creating the key on window)
    // but the actual assignment happens asynchronously after chunks load.
    const container = await waitForContainer(remoteName);

    if (!container) {
        console.error(
            `[DynamicRoute] Container '${remoteName}' not found on window after timeout.`
        );

        return [];
    }

    // Initialize the container exactly once — calling init() twice throws.
    if (!initializedContainers.has(remoteName)) {
        const win = window as unknown as Record<string, unknown>;
        const shareScopes = win['__webpack_share_scopes__'] as Record<string, unknown>;
        await container.init(shareScopes['default'] || {});
        initializedContainers.add(remoteName);
    }

    // Get the exposed module
    const factory = await container.get(exposedModule);
    const Module = factory();

    // Check if the remote exports a mount function (mount/unmount pattern).
    // This is used when the remote has its own Angular runtime and needs
    // to bootstrap independently inside a host-provided DOM element.
    const mountFn = Module['mount'] as ((el: HTMLElement) => Promise<() => void>) | undefined;

    if (typeof mountFn === 'function') {
        return [
            {
                path: '',
                component: DotRemoteModuleWrapperComponent,
                data: { mount: mountFn }
            }
        ];
    }

    // Fall back to standard route exports
    return (Module['remoteRoutes'] || Module['routes'] || Module['default'] || []) as Route[];
}

/**
 * Ensures the webpack Module Federation sharing globals exist on window.
 * Remote entries reference __webpack_init_sharing__ and __webpack_share_scopes__
 * during their initialization. If the host doesn't use MF, we provide stubs
 * so the remote falls back to its own bundled dependencies.
 */
function ensureWebpackSharingGlobals(): void {
    const win = window as unknown as Record<string, unknown>;

    if (typeof win['__webpack_init_sharing__'] !== 'function') {
        win['__webpack_share_scopes__'] = { default: {} };
        win['__webpack_init_sharing__'] = (name: string) => {
            if (!(win['__webpack_share_scopes__'] as Record<string, unknown>)[name]) {
                (win['__webpack_share_scopes__'] as Record<string, unknown>)[name] = {};
            }

            return Promise.resolve();
        };
    }
}

/**
 * Waits for a Module Federation container to be assigned on window.
 * The var library type hoists the declaration synchronously, but the
 * assignment can be deferred until webpack finishes loading chunks.
 */
function waitForContainer(
    remoteName: string,
    timeout = 5000
): Promise<
    | {
          init: (shareScope: unknown) => Promise<void>;
          get: (module: string) => Promise<() => Record<string, unknown>>;
      }
    | undefined
> {
    const win = window as unknown as Record<string, unknown>;

    // Already available
    if (win[remoteName]) {
        return Promise.resolve(
            win[remoteName] as {
                init: (shareScope: unknown) => Promise<void>;
                get: (module: string) => Promise<() => Record<string, unknown>>;
            }
        );
    }

    // Poll until available or timeout
    return new Promise((resolve) => {
        const interval = 50;
        let elapsed = 0;
        const timer = setInterval(() => {
            elapsed += interval;

            if (win[remoteName]) {
                clearInterval(timer);
                resolve(
                    win[remoteName] as {
                        init: (shareScope: unknown) => Promise<void>;
                        get: (module: string) => Promise<() => Record<string, unknown>>;
                    }
                );
            } else if (elapsed >= timeout) {
                clearInterval(timer);
                resolve(undefined);
            }
        }, interval);
    });
}

/**
 * Loads the remote entry script into the page.
 */
function loadRemoteEntry(remoteEntry: string, remoteName: string): Promise<void> {
    return new Promise((resolve, reject) => {
        // Check if already loaded
        if ((window as unknown as Record<string, unknown>)[remoteName]) {
            resolve();

            return;
        }

        const script = document.createElement('script');
        script.src = remoteEntry;
        script.type = 'text/javascript';
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error(`Failed to load remote entry: ${remoteEntry}`));
        document.head.appendChild(script);
    });
}

/**
 * Service for dynamically registering Angular routes at runtime.
 * Use this to add custom portlet routes without rebuilding the application.
 *
 * @example
 * // Register a component directly
 * dynamicRouteService.registerRoute({
 *     path: 'my-portlet',
 *     component: MyPortletComponent
 * });
 *
 * @example
 * // Register with lazy loading
 * dynamicRouteService.registerRoute({
 *     path: 'my-portlet',
 *     loadComponent: () => import('./my-portlet.component').then(m => m.MyPortletComponent)
 * });
 */
@Injectable({ providedIn: 'root' })
export class DynamicRouteService {
    private readonly router = inject(Router);
    private readonly logger = inject(LoggerService);
    private readonly registeredRoutes = new Set<string>();

    /**
     * Register a new Angular route dynamically.
     * The route will be added to the main authenticated layout.
     *
     * @param config - The route configuration
     * @returns true if the route was registered, false if it already exists
     */
    registerRoute(config: DynamicRouteConfig): boolean {
        if (this.registeredRoutes.has(config.path)) {
            this.logger.warn(this, `Route '${config.path}' is already registered. Skipping.`);

            return false;
        }

        const mainRoute = this.findMainLayoutRoute();

        if (!mainRoute?.children) {
            this.logger.error(
                this,
                'Could not find main layout route. Dynamic route registration failed.'
            );

            return false;
        }

        const newRoute = this.buildRoute(config);

        // Insert at the beginning to take precedence over catch-all routes
        mainRoute.children.unshift(newRoute);
        this.registeredRoutes.add(config.path);

        // Reset router configuration to apply changes
        this.router.resetConfig(this.router.config);

        this.logger.info(this, `Registered dynamic route: '${config.path}'`);

        return true;
    }

    /**
     * Unregister a previously registered dynamic route.
     *
     * @param path - The path of the route to remove
     * @returns true if the route was removed, false if it wasn't found
     */
    unregisterRoute(path: string): boolean {
        if (!this.registeredRoutes.has(path)) {
            return false;
        }

        const mainRoute = this.findMainLayoutRoute();

        if (mainRoute?.children) {
            const index = mainRoute.children.findIndex((r) => r.path === path);

            if (index !== -1) {
                mainRoute.children.splice(index, 1);
                this.registeredRoutes.delete(path);
                this.router.resetConfig(this.router.config);
                this.logger.info(this, `Unregistered dynamic route: '${path}'`);

                return true;
            }
        }

        return false;
    }

    /**
     * Check if a route is already registered.
     *
     * @param path - The path to check
     * @returns true if the route exists
     */
    isRouteRegistered(path: string): boolean {
        return this.registeredRoutes.has(path) || this.routeExistsInConfig(path);
    }

    /**
     * Get all dynamically registered route paths.
     *
     * @returns Array of registered paths
     */
    getRegisteredRoutes(): string[] {
        return Array.from(this.registeredRoutes);
    }

    /**
     * Register a module loader for a specific Angular module path.
     * This allows runtime registration of lazy-loadable modules.
     *
     * @param modulePath - The module path identifier (e.g., "@dotcms/portlets/my-custom")
     * @param loader - Function that returns a Promise of routes
     */
    registerModuleLoader(modulePath: string, loader: () => Promise<Route[]>): void {
        ANGULAR_MODULE_REGISTRY[modulePath] = loader;
        this.logger.info(this, `Registered module loader for: '${modulePath}'`);
    }

    /**
     * Register routes from menu items that have angularModule defined.
     * This processes menu items from the backend and registers their routes dynamically.
     *
     * Supports two formats for angularModule:
     * 1. Local module: "@dotcms/portlets/my-custom"
     * 2. Remote module: "remote:http://localhost:4201/remoteEntry.js|myPlugin|./Routes"
     *
     * @param menuItems - Array of menu items from the backend
     * @returns Number of routes successfully registered
     */
    registerRoutesFromMenuItems(menuItems: DotMenuItem[]): number {
        let registered = 0;

        for (const item of menuItems) {
            if (item.angular && item.angularModule) {
                const path = this.extractPathFromUrl(item.url);
                let success = false;

                if (item.angularModule.startsWith('remote:')) {
                    // Remote Module Federation format:
                    // remote:<remoteEntry>|<remoteName>|<exposedModule>
                    success = this.registerRemoteModuleFromString(path, item.angularModule, {
                        portletId: item.id,
                        label: item.label
                    });
                } else {
                    // Local module format: @dotcms/portlets/my-custom
                    const loader = ANGULAR_MODULE_REGISTRY[item.angularModule];

                    if (loader) {
                        success = this.registerRoute({
                            path,
                            loadChildren: loader,
                            data: {
                                portletId: item.id,
                                label: item.label
                            }
                        });
                    } else {
                        this.logger.warn(
                            this,
                            `No module loader registered for '${item.angularModule}' (portlet: ${item.id})`
                        );
                    }
                }

                if (success) {
                    registered++;
                }
            }
        }

        return registered;
    }

    /**
     * Parse and register a remote module from the angular-module string format.
     * Format: remote:<remoteEntry>|<remoteName>|<exposedModule>
     *
     * @param path - The route path
     * @param angularModule - The angular-module string from backend
     * @param data - Additional route data
     * @returns true if successfully registered
     */
    private registerRemoteModuleFromString(
        path: string,
        angularModule: string,
        data?: Record<string, unknown>
    ): boolean {
        // Parse: remote:http://localhost:4201/remoteEntry.js|myPlugin|./Routes
        const parts = angularModule.substring('remote:'.length).split('|');

        if (parts.length !== 3) {
            this.logger.error(
                this,
                `Invalid remote module format: '${angularModule}'. Expected: remote:<url>|<name>|<module>`
            );

            return false;
        }

        const [remoteEntry, remoteName, exposedModule] = parts;

        return this.registerRemoteModule({
            path,
            remoteEntry,
            remoteName,
            exposedModule,
            data
        });
    }

    /**
     * Extract the path from a URL (removes leading slash).
     */
    private extractPathFromUrl(url: string): string {
        return url.startsWith('/') ? url.substring(1) : url;
    }

    /**
     * Register a remote module from an external Module Federation plugin.
     * This allows loading Angular modules from separately deployed applications.
     *
     * @param config - Configuration for the remote module
     * @returns true if registration was successful
     *
     * @example
     * dynamicRouteService.registerRemoteModule({
     *     path: 'my-external-portlet',
     *     remoteEntry: 'http://localhost:4201/remoteEntry.js',
     *     remoteName: 'myPlugin',
     *     exposedModule: './Routes'
     * });
     */
    registerRemoteModule(config: {
        path: string;
        remoteEntry: string;
        remoteName: string;
        exposedModule: string;
        data?: Record<string, unknown>;
    }): boolean {
        return this.registerRoute({
            path: config.path,
            loadChildren: () =>
                loadRemoteModule(config.remoteEntry, config.remoteName, config.exposedModule),
            data: config.data
        });
    }

    /**
     * Find the main authenticated layout route where portlets are mounted.
     * This is the route with path '' that has children (PORTLETS_IFRAME + PORTLETS_ANGULAR).
     */
    private findMainLayoutRoute(): Route | undefined {
        return this.router.config.find(
            (route) =>
                route.path === '' &&
                route.children &&
                route.children.length > 0 &&
                route.canActivate &&
                route.canActivate.length > 0
        );
    }

    /**
     * Check if a route already exists in the router configuration.
     */
    private routeExistsInConfig(path: string): boolean {
        const mainRoute = this.findMainLayoutRoute();

        return mainRoute?.children?.some((r) => r.path === path) ?? false;
    }

    /**
     * Build a Route object from the configuration.
     */
    private buildRoute(config: DynamicRouteConfig): Route {
        const route: Route = {
            path: config.path,
            data: {
                reuseRoute: false,
                ...config.data
            }
        };

        // Add menu guard if requested (default: true)
        if (config.canActivate !== false) {
            route.canActivate = [MenuGuardService];
            route.canActivateChild = [MenuGuardService];
        }

        // Set up the route loader
        if (config.component) {
            route.component = config.component;
        } else if (config.loadComponent) {
            route.loadComponent = config.loadComponent;
        } else if (config.loadChildren) {
            route.loadChildren = config.loadChildren;
        }

        return route;
    }
}
