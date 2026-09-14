import { InjectionToken, Signal } from '@angular/core';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

/**
 * The field-filter chips' lifecycle, owned by the surface rendering them.
 *
 * **Why this is not part of {@link DotFilterFacade}.** That contract carries filter *values* — a
 * key and what is selected under it. The field filters need two things that are not values:
 *
 * - **Which chips are shown.** A chip is added before it filters anything, so that a fresh chip
 *   does not reload the list on the way in. That state cannot live in the filter bag without
 *   putting an empty criterion there, which is exactly what the facade's O3 forbids.
 * - **The active content type's field metadata.** One fetch feeds three consumers: this menu (which
 *   fields are still addable), the chips (which control each field renders), and the surface's own
 *   request builder (which reshapes a raw value into the `userSearchable` payload). The fetch is
 *   the menu's, so the metadata has to travel outwards.
 *
 * Both surfaces implement it over their own store, next to their facade. A surface that does not
 * offer the "More" overflow implements nothing and provides nothing — the token is only injected
 * by the field-filter chips themselves.
 */
export interface DotFieldFilterHost {
    /** The field variables with a chip on screen, in the order the editor added them. */
    readonly $activeFields: Signal<string[]>;

    /**
     * Eligible field metadata for the active content type, as published by {@link setFields}.
     *
     * Empty while nothing is loaded, which is a different state from "loaded and nothing is
     * eligible" only to the surface — the menu tells them apart through {@link $loadingFields}.
     */
    readonly $fields: Signal<DotCMSContentTypeField[]>;

    /**
     * Shows a chip for `variable` without filtering anything yet.
     *
     * Adding the same variable twice is a no-op: the menu already hides what is active, and a
     * double-add would mint a second chip writing the same key.
     */
    addField(variable: string): void;

    /**
     * Publishes one field fetch.
     *
     * Both lists come from the same response, and the split is what keeps surface-specific
     * knowledge out of the shared menu: `eligible` are the fields it may offer as filters, `all` is
     * the raw field list, which Content Drive additionally mines for its table's "Show In List"
     * columns. A surface with no such use ignores `all`.
     */
    setFields(fields: { eligible: DotCMSContentTypeField[]; all: DotCMSContentTypeField[] }): void;

    /**
     * Drops every field filter, every chip and the cached metadata.
     *
     * Called when the active content type changes — the previous type's fields do not exist on the
     * new one, so a surviving `us.*` value would filter on a field the editor can no longer see.
     */
    clearFields(): void;
}

export const DOT_FIELD_FILTER_HOST = new InjectionToken<DotFieldFilterHost>(
    'DOT_FIELD_FILTER_HOST'
);
