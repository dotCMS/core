// All the custom events that come from the JSP Iframe
export enum NG_CUSTOM_EVENTS {
  EDIT_CONTENTLET_LOADED = 'edit-contentlet-loaded',
  CONTENT_SEARCH_SELECT = 'select-contentlet',
  CREATE_CONTENTLET = 'create-contentlet-from-edit-page',
  SAVE_PAGE = 'save-page',
  FORM_SELECTED = 'form-selected',
}

// The current state of the editor
export enum EDITOR_STATE {
  LOADING = 'loading',
  LOADED = 'loaded',
  ERROR = 'error',
}
