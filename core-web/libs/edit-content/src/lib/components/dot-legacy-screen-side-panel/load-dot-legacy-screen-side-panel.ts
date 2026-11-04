import { Type } from '@angular/core';

/**
 * Loads the generic legacy-screen side panel's content, for `DotSidePanelService` or
 * `injectSidePanel`. It arrives in its own chunk on the first open.
 *
 * The import lives here, inside the library, so openers never import this library dynamically:
 * they import it statically for everything else, and the two may not be mixed.
 *
 * @returns the panel's content component
 */
export function loadDotLegacyScreenSidePanel(): Promise<Type<unknown>> {
    return import('./dot-legacy-screen-side-panel.component').then(
        (m) => m.DotLegacyScreenSidePanelComponent
    );
}
