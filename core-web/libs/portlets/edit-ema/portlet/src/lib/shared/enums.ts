export enum NOTIFY_CUSTOMER {
    EMA_RELOAD_PAGE = 'ema-reload-page', // We need to reload the ema page
    EMA_REQUEST_BOUNDS = 'ema-request-bounds'
}

// All the custom events that come from the JSP Iframe
export enum NG_CUSTOM_EVENTS {
    EDIT_CONTENTLET_LOADED = 'edit-contentlet-loaded',
    CONTENT_SEARCH_SELECT = 'select-contentlet',
    CREATE_CONTENTLET = 'create-contentlet-from-edit-page',
    SAVE_CONTENTLET = 'save-page'
}
