import { DestroyRef, Injector } from '@angular/core';

import type { DotEditContentOpenOptions, DotEditContentRequest } from '@dotcms/edit-content';
import type { DotSidePanelRef } from '@dotcms/ui';

/**
 * Opens content in the side panel through `DotEditContentOpenerService`, from the admin app
 * (#38303). The app lazy-loads `@dotcms/edit-content` with its routes, so the library is loaded
 * here on demand instead of imported statically, which would pull it into the main bundle.
 *
 * Opens nothing when the caller's injector was destroyed while the library loaded (a deep-link
 * route left before the editor arrived): a panel bound to a dead injector would close at once.
 *
 * @param request What to open.
 * @param options The caller's injector, which the panel lives under, and its event handlers.
 * @returns The panel, or `null` when the caller went away first.
 */
export async function openEditContentPanel(
    request: DotEditContentRequest,
    options: DotEditContentOpenOptions
): Promise<DotSidePanelRef | null> {
    const { DotEditContentOpenerService } = await import('@dotcms/edit-content');

    if (options.injector.get(DestroyRef, null)?.destroyed) {
        return null;
    }

    return options.injector.get(DotEditContentOpenerService).open(request, options);
}

/**
 * Removes every side panel param, and `extraKeys`, from the URL in place, through
 * `DotPanelUrlStateService` (#38303, FR-049). For a screen whose URL names more than one panel on
 * load: it opens none of them. Loads `@dotcms/edit-content` on demand, as {@link
 * openEditContentPanel} does; reading the URL needs no library, `readPanelUrlIntent` is in
 * `@dotcms/utils`.
 *
 * @param injector The screen's injector.
 * @param extraKeys Params of the screen's own that go with the panel params.
 */
export async function clearPanelUrl(
    injector: Injector,
    extraKeys: readonly string[] = []
): Promise<void> {
    const { DotPanelUrlStateService } = await import('@dotcms/edit-content');

    injector.get(DotPanelUrlStateService).clear(extraKeys);
}
