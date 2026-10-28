import { Type } from '@angular/core';

/**
 * Loads the legacy (JSP) editor side panel's content, for `DotSidePanelService` or
 * `injectSidePanel`. It arrives in its own chunk on the first open. Remove with the legacy editor.
 *
 * The import lives here, inside the library, so openers never import this library dynamically:
 * they import it statically for everything else, and the two may not be mixed.
 *
 * @returns the panel's content component
 */
export function loadDotLegacyEditorSidePanel(): Promise<Type<unknown>> {
    return import('./dot-legacy-editor-side-panel.component').then(
        (m) => m.DotLegacyEditorSidePanelComponent
    );
}
