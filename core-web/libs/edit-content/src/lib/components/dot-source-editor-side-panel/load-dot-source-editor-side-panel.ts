import { Type } from '@angular/core';

/**
 * Loads the Edit Code panel's content, for `DotSidePanelService` or `injectSidePanel`. Monaco and
 * the editor arrive in their own chunk on the first open, so the portlets that open it stay light.
 *
 * The import lives here, inside the library, so openers never import this library dynamically:
 * they import it statically for everything else, and the two may not be mixed.
 *
 * @returns the panel's content component
 */
export function loadDotSourceEditorSidePanel(): Promise<Type<unknown>> {
    return import('./dot-source-editor-side-panel.component').then(
        (m) => m.DotSourceEditorSidePanelComponent
    );
}
