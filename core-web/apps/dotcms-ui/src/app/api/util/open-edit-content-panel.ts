import { DestroyRef } from '@angular/core';

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
