import { InjectionToken } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { EditContentIdentity } from '../services/host/edit-content-host.model';

/**
 * Lets an opener replace the editor's own navigation for two cases. Only Content Drive provides
 * it (#37759, FR-028, FR-029); every other opener keeps the editor's navigation. Remove with the
 * legacy editor.
 *
 * Each method gets what the editor was opened with (`EditContentHost.resolveIdentity()`), which
 * stays the same across in-place reloads, so the opener can answer only for the editor it opened.
 * Returning `false` declines, and the editor navigates as usual: a nested editor (a related
 * content opened from a field) inherits the provider too, and must not take over its opener's
 * panel.
 */
export interface EditContentNavigationOverride {
    /**
     * "Switch to the old editor" was confirmed and the type now uses the legacy editor.
     *
     * @param opened What the editor was opened with.
     * @param contentlet The content being edited, or `null` for a create that was never saved.
     * @param contentTypeVariable The type that was just set back to the legacy editor.
     * @returns `true` when the opener reopened it; `false` to let the editor navigate.
     */
    switchToLegacyEditor(
        opened: EditContentIdentity,
        contentlet: DotCMSContentlet | null,
        contentTypeVariable: string
    ): boolean;

    /**
     * The content failed to load, after the standard error was shown.
     *
     * @param opened What the editor was opened with.
     * @returns `true` when the opener closed the editor; `false` to let the editor navigate.
     */
    leaveOnLoadError(opened: EditContentIdentity): boolean;
}

/** Provided only by Content Drive. Remove with the legacy editor. */
export const EDIT_CONTENT_NAVIGATION_OVERRIDE = new InjectionToken<EditContentNavigationOverride>(
    'EDIT_CONTENT_NAVIGATION_OVERRIDE'
);
