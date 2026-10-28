// All the custom events that come from the JSP Iframe
/** The `ng-event`s the legacy screens the page editor opens in the side panel report. */
export enum NG_CUSTOM_EVENTS {
    CONTENT_SEARCH_SELECT = 'select-contentlet',
    CREATE_CONTENTLET = 'create-contentlet-from-edit-page',
    SAVE_PAGE = 'save-page',
    SAVE_MENU_ORDER = 'save-menu-order',
    ERROR_SAVING_MENU_ORDER = 'error-saving-menu-order',
    CANCEL_SAVING_MENU_ORDER = 'cancel-save-menu-order',
    LANGUAGE_IS_CHANGED = 'language-is-changed'
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
    /** User is dragging an iframe resize handle. Overlays hide; bounds re-emit on release. */
    RESIZING = 'resizing',
    INLINE_EDITING = 'inline-editing'
}

export enum CommonErrors {
    'NOT_FOUND' = '404',
    'ACCESS_DENIED' = '403'
}

export enum PALETTE_CLASSES {
    OPEN = 'open',
    CLOSED = 'closed'
}

export enum CONTAINER_INSERT_ERROR {
    CONTAINER_LIMIT_REACHED = 'CONTAINER_LIMIT_REACHED',
    DUPLICATE_CONTENT = 'DUPLICATE_CONTENT'
}
