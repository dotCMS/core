import { inject, Injectable } from '@angular/core';

import { filter, take } from 'rxjs/operators';

import { LoggerService } from '@dotcms/dotcms-js';
import { DotMenu } from '@dotcms/dotcms-models';

import { DotMenuService } from './dot-menu.service';
import { DynamicRouteService } from './dynamic-route.service';

/**
 * Service that initializes dynamic routes from the menu API.
 * Call `initialize()` after user authentication to register any
 * dynamic Angular portlets defined in the backend.
 *
 * @example
 * // In a component or service after login
 * this.dynamicRouteInitializer.initialize().subscribe(count => {
 *     console.log(`Registered ${count} dynamic routes`);
 * });
 */
@Injectable({ providedIn: 'root' })
export class DynamicRouteInitializerService {
    private readonly menuService = inject(DotMenuService);
    private readonly dynamicRouteService = inject(DynamicRouteService);
    private readonly logger = inject(LoggerService);

    private initialized = false;

    /**
     * Initialize dynamic routes from the menu API.
     * This should be called once after user authentication.
     *
     * @param force - Force re-initialization even if already done
     * @returns Observable that emits the number of routes registered
     */
    initialize(force = false): Promise<number> {
        if (this.initialized && !force) {
            this.logger.info(
                this,
                'Dynamic routes already initialized. Use force=true to re-initialize.'
            );

            return Promise.resolve(0);
        }

        return new Promise((resolve) => {
            this.menuService
                .loadMenu(force)
                .pipe(
                    filter((menus): menus is DotMenu[] => !!menus),
                    take(1)
                )
                .subscribe({
                    next: (menus) => {
                        const allMenuItems = menus.flatMap((menu) => menu.menuItems);
                        const count =
                            this.dynamicRouteService.registerRoutesFromMenuItems(allMenuItems);

                        this.initialized = true;
                        this.logger.info(this, `Initialized ${count} dynamic routes from menu`);
                        resolve(count);
                    },
                    error: (err) => {
                        this.logger.error(this, 'Failed to initialize dynamic routes:', err);
                        resolve(0);
                    }
                });
        });
    }

    /**
     * Check if dynamic routes have been initialized.
     */
    isInitialized(): boolean {
        return this.initialized;
    }

    /**
     * Get list of currently registered dynamic routes.
     */
    getRegisteredRoutes(): string[] {
        return this.dynamicRouteService.getRegisteredRoutes();
    }
}
