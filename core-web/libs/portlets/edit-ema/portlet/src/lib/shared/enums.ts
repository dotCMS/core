export enum NOTIFY_CUSTOMER {
    EMA_RELOAD_PAGE = 'ema-reload-page', // We need to reload the ema page
    EMA_REQUEST_BOUNDS = 'ema-request-bounds',
    EMA_EDITOR_PONG = 'ema-editor-pong',
    EMA_SCROLL_INSIDE_IFRAME = 'scroll-inside-iframe',
    SET_PAGE_DATA = 'SET_PAGE_DATA',
    COPY_CONTENTLET_INLINE_EDITING_SUCCESS = 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS'
}

// All the custom events that come from the JSP Iframe
export enum NG_CUSTOM_EVENTS {
    EDIT_CONTENTLET_LOADED = 'edit-contentlet-loaded',
    CONTENT_SEARCH_SELECT = 'select-contentlet',
    CREATE_CONTENTLET = 'create-contentlet-from-edit-page',
    SAVE_PAGE = 'save-page',
    FORM_SELECTED = 'form-selected',
    COMPARE_CONTENTLET = 'compare-contentlet',
    SAVE_MENU_ORDER = 'save-menu-order',
    ERROR_SAVING_MENU_ORDER = 'error-saving-menu-order',
    CANCEL_SAVING_MENU_ORDER = 'cancel-save-menu-order',
    OPEN_WIZARD = 'workflow-wizard',
    DIALOG_CLOSED = 'dialog-closed',
    EDIT_CONTENTLET_UPDATED = 'edit-contentlet-data-updated'
}

// Status of the whole UVE
export enum UVE_STATUS {
    LOADING = 'loading',
    LOADED = 'loaded',
    ERROR = 'error'
}

export enum EDITOR_STATE {
    ERROR = 'error',
    IDLE = 'idle',
    DRAGGING = 'dragging',
    SCROLL_DRAG = 'scroll-drag',
    SCROLLING = 'scrolling',
    INLINE_EDITING = 'inline-editing'
}

export enum PAGE_MODE {
    EDIT = 'EDIT_MODE',
    PREVIEW = 'PREVIEW_MODE',
    LIVE = 'LIVE'
}

export enum CommonErrors {
    'NOT_FOUND' = '404',
    'ACCESS_DENIED' = '403'
}

export enum DialogStatus {
    IDLE = 'IDLE',
    LOADING = 'LOADING',
    INIT = 'INIT'
}

export enum FormStatus {
    DIRTY = 'DIRTY',
    SAVED = 'SAVED',
    PRISTINE = 'PRISTINE'
}
