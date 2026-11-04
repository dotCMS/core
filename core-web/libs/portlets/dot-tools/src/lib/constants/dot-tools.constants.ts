import { DOT_MATERIAL_ICONS, DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { DotToolsDataViewMode } from '../models/dot-tools.models';

/**
 * Catalog pagination. Kept here so the store's slice count, the "Load N more"
 * button label and any future test stay in agreement.
 */
export const CATALOG_INITIAL_LIMIT = 15;
export const CATALOG_LOAD_MORE_STEP = 40;

/**
 * Re-exported for the Section dialog's icon picker. The authoritative
 * list lives in `@dotcms/dotcms-models` so this portlet and the Stencil
 * `dot-material-icon-picker` share one source of truth — add new icons
 * there, not here.
 */
export const DOT_TOOLS_SECTION_ICONS = DOT_MATERIAL_ICONS;

export const DOT_TOOLS_BASE_TYPES: ReadonlyArray<{
    id: DotCMSBaseTypesContentTypes;
    label: string;
}> = [
    { id: DotCMSBaseTypesContentTypes.CONTENT, label: 'Content' },
    { id: DotCMSBaseTypesContentTypes.WIDGET, label: 'Widget' },
    { id: DotCMSBaseTypesContentTypes.FORM, label: 'Form' },
    { id: DotCMSBaseTypesContentTypes.FILEASSET, label: 'File Asset' },
    { id: DotCMSBaseTypesContentTypes.HTMLPAGE, label: 'HTML Page' },
    { id: DotCMSBaseTypesContentTypes.PERSONA, label: 'Persona' },
    { id: DotCMSBaseTypesContentTypes.VANITY_URL, label: 'Vanity URL' },
    { id: DotCMSBaseTypesContentTypes.KEY_VALUE, label: 'Key/Value' },
    { id: DotCMSBaseTypesContentTypes.DOTASSET, label: 'DotAsset' }
];

export const DOT_TOOLS_DATA_VIEW_MODES: ReadonlyArray<{
    id: DotToolsDataViewMode;
    label: string;
    icon: string;
}> = [
    { id: 'list', label: 'List', icon: 'list' },
    { id: 'card', label: 'Card', icon: 'grid_view' }
];

/**
 * What `resolveSectionIcon` returns for a section whose stored icon is not
 * a Material Icon id this portlet knows about — a `fa-*` string the legacy
 * migration never caught, an empty / hand-typed value, or a glyph that
 * fell out of the font. `side_navigation` reads as "something lives on the
 * side nav here" without implying what the section does, which is the
 * useful shape of a stand-in when the real id can't be rendered. It is
 * NOT in the picker list — the Material Symbols font (which this portlet
 * renders with) is a superset of the classic Material Icons set shown by
 * the picker, so a glyph outside the picker still paints correctly.
 */
export const DOT_TOOLS_FALLBACK_ICON = 'side_navigation';

/**
 * Build the lookup set once at module load. 1766-ish names, so an O(1)
 * `Set.has` per render is cheaper than a scan every time the sidebar
 * paints.
 */
const DOT_TOOLS_SECTION_ICON_SET: ReadonlySet<string> = new Set(DOT_TOOLS_SECTION_ICONS);

/**
 * Returns a Material Icon name the Material Symbols font will render. Legacy
 * sections may carry a `fa-*` string the `Task210316UpdateLayoutIcons` task
 * never migrated, a deprecated glyph that fell out of Material Symbols, an
 * empty string, or a hand-typed value from before our picker existed — all
 * paint as nothing (or as the raw text) if we blindly interpolate them, so
 * the fallback kicks in for every id outside the authoritative picker set.
 */
export function resolveSectionIcon(icon: string | null | undefined): string {
    if (typeof icon === 'string' && DOT_TOOLS_SECTION_ICON_SET.has(icon)) {
        return icon;
    }

    return DOT_TOOLS_FALLBACK_ICON;
}
