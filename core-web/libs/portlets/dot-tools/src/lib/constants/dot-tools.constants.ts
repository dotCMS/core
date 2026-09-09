import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

/**
 * Catalog pagination. Kept here so the store's slice count, the "Load N more"
 * button label and any future test stay in agreement.
 */
export const CATALOG_INITIAL_LIMIT = 15;
export const CATALOG_LOAD_MORE_STEP = 40;

/**
 * Curated Material Symbols set the Section dialog picks from. Client-owned:
 * the backend accepts any string (see `Layout.description`), so this list is
 * ours to expand. Order is roughly by role (navigation → domain → generic).
 */
export const DOT_TOOLS_SECTION_ICONS: readonly string[] = [
    'rocket_launch',
    'space_dashboard',
    'admin_panel_settings',
    'widgets',
    'language',
    'build',
    'article',
    'perm_media',
    'campaign',
    'schema',
    'terminal',
    'settings',
    'folder',
    'bolt',
    'api',
    'analytics',
    'shopping_cart',
    'group',
    'lock',
    'public',
    'sell',
    'stars',
    'description',
    'dataset',
    'tune',
    'extension',
    'hub',
    'inventory_2',
    'map',
    'payments'
] as const;

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
    id: 'List' | 'Card';
    label: string;
    icon: string;
}> = [
    { id: 'List', label: 'List', icon: 'list' },
    { id: 'Card', label: 'Card', icon: 'grid_view' }
];
