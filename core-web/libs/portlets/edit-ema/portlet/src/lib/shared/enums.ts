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

// We had a problem with the reloads because we had all these states in one level, but loading and error are global states and not part of the editor state
// idle, dragging, out-of-bounds, scroll-drag are status of the editor and are isolated from loading/loaded/error but they should respond to the UVE_STATUS
// The editor should wait for the UVE to be loaded before altering its status
// The current state of the editor
export enum EDITOR_STATE {
    LOADING = 'loading', // Delete this one and use the UVE_STATUS
    IDLE = 'idle',
    DRAGGING = 'dragging',
    ERROR = 'error', // Delete this one and use the UVE_STATUS
    OUT_OF_BOUNDS = 'out-of-bounds',
    SCROLL_DRAG = 'scroll-drag'
}

export enum EDITOR_MODE {
    // I will merge this in just one
    EDIT = 'edit',
    EDIT_VARIANT = 'edit-variant',

    // This could be part of edit
    INLINE_EDITING = 'inline-editing',

    // This can be merge in just one too
    PREVIEW_VARIANT = 'preview-variant',
    DEVICE = 'device',
    SOCIAL_MEDIA = 'social-media',

    // This and readonly could be just one
    LOCKED = 'locked'
}

// Not sure if this is needed after the refactor
export enum PAGE_MODE {
    EDIT = 'EDIT_MODE',
    PREVIEW = 'PREVIEW_MODE',
    LIVE = 'LIVE'
}
