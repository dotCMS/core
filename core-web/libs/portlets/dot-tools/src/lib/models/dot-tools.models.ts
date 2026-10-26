import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

/**
 * Section as returned by `GET /v1/layouts` per the spec in PR #37645 and by
 * every write that returns the full section list (delete, reorder,
 * setSectionTools).
 *
 * `portletTitles` is aligned by index with `portletIds`: the localized title
 * the backend resolved for each id, falling back to the tool's registered
 * name and then the id (never a raw translation key). It lets the sections
 * panel show tool titles without a separate catalog lookup when the catalog
 * has not loaded yet.
 */
export interface DotToolsSection {
    id: string;
    name: string;
    icon: string;
    tabOrder: number;
    portletIds: string[];
    portletTitles: string[];
}

export interface DotToolsCatalogEntry {
    id: string;
    title: string;
    isCustom: boolean;
}

/**
 * Data view mode is stored and transmitted in lowercase: `list` | `card`.
 * PR #37678 pins this in the `GET /v1/portlet/custom/{id}` and
 * `POST` / `PUT /v1/portlet/custom` shapes. The frontend form's SelectButton
 * options still label them "List" / "Card" — see DOT_TOOLS_DATA_VIEW_MODES.
 */
export type DotToolsDataViewMode = 'list' | 'card';

/** Response of `GET /v1/portlet/custom/{portletId}` per PR #37678. */
export interface DotToolsCustomToolConfig {
    portletId: string;
    portletName: string;
    baseTypes: DotCMSBaseTypesContentTypes[];
    contentTypes: string[];
    dataViewMode: DotToolsDataViewMode;
}

export interface DotToolsSectionForm {
    name: string;
    icon: string;
}

export interface DotToolsToolForm {
    portletId: string;
    portletName: string;
    baseTypes: DotCMSBaseTypesContentTypes[];
    contentTypes: string[];
    dataViewMode: DotToolsDataViewMode;
}
