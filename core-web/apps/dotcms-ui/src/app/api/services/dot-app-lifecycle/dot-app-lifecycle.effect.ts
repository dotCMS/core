import { Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DotRouterService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

/**
 * App-level effect for cross-cutting behaviors that must run for the entire
 * application lifetime, independent of any single component.
 *
 * Provided eagerly via `provideAppInitializer` so it is instantiated at startup.
 * Currently navigates away from the edit page when another user/tab switches
 * the current site via WebSocket; future app-initialization/lifecycle concerns
 * belong here too.
 */
@Injectable({ providedIn: 'root' })
export class DotAppLifecycleEffect {
    readonly #dotRouterService = inject(DotRouterService);

    constructor() {
        inject(GlobalStore)
            .switchSiteEvent$()
            .pipe(takeUntilDestroyed())
            .subscribe(() => {
                if (this.#dotRouterService.isEditPage()) {
                    this.#dotRouterService.goToSiteBrowser();
                }
            });
    }
}
