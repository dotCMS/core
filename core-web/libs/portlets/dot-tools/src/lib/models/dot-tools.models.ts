import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

/**
 * WAITING FOR BACKEND — response shape mirrors the contract pinned in #37353.
 * When the endpoint lands, replace the mock data source in DotToolsStore
 * without changing these types.
 */
export interface DotToolsSection {
    id: string;
    name: string;
    icon: string;
    tabOrder: number;
    portletIds: string[];
}

export interface DotToolsCatalogEntry {
    id: string;
    title: string;
    /**
     * WAITING FOR BACKEND — flag proposed in the comment on #37353. Gates the
     * per-row Edit/Delete menu on the Available Tools panel; first-party tools
     * (isCustom=false) cannot be edited or removed from the catalog.
     */
    isCustom: boolean;
}

export type DotToolsDataViewMode = 'List' | 'Card';

/**
 * WAITING FOR BACKEND — mirrors `CustomPortletForm` (POST/PUT /v1/portlet/custom).
 * The Edit-tool prefill endpoint is the one proposed in the comment on #37353.
 */
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
