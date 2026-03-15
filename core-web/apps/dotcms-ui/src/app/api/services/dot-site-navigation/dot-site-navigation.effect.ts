import { Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DotRouterService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

/**
 * App-level effect that navigates away from the edit page whenever
 * another user/tab switches the current site via WebSocket.
 *
 * Provided eagerly in app.config.ts so it is active for the full
 * application lifetime without being tied to any particular component.
 */
@Injectable({ providedIn: 'root' })
export class DotSiteNavigationEffect {
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
