export enum CUSTOMER_ACTIONS {
    EDIT_CONTENTLET = 'edit-contentlet', // The customer hit edit button
    ADD_CONTENTLET = 'add-contentlet', // The customer hit add button
    DELETE_CONTENTLET = 'delete-contentlet', // The customer hit delete button
    SET_URL = 'set-url', // User navigate internally within the ema
    NOOP = 'noop'
}

export enum NOTIFY_CUSTOMER {
    EMA_RELOAD_PAGE = 'ema-reload-page' // We need to reload the ema page
}

// All the custom events that come from the JSP Iframe
export enum NG_CUSTOM_EVENTS {
    EDIT_CONTENTLET_LOADED = 'edit-contentlet-loaded',
    CONTENT_SEARCH_SELECT = 'select-contentlet',
    CONTENTLET_UPDATED = 'save-page'
}
