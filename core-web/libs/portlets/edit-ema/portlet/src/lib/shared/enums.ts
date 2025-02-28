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
    EDIT_CONTENTLET_UPDATED = 'edit-contentlet-data-updated',
    LANGUAGE_IS_CHANGED = 'language-is-changed',
    UPDATE_WORKFLOW_ACTION = 'update-workflow-action'
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
