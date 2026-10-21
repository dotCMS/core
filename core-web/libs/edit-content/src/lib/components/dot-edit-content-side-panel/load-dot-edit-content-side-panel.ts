import { Type } from '@angular/core';

/**
 * Loads the Edit Content side panel's content, for `DotSidePanelService` or `injectSidePanel`. The
 * editor and its Block Editor dependency chain arrive in their own chunk on the first open, so the
 * portlets that open it stay light.
 *
 * The import lives here, inside the library, so openers never import this library dynamically:
 * they import it statically for everything else, and the two may not be mixed.
 *
 * @returns the panel's content component
 */
export function loadDotEditContentSidePanel(): Promise<Type<unknown>> {
    return import('./dot-edit-content-side-panel.component').then(
        (m) => m.DotEditContentSidePanelComponent
    );
}
