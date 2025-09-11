/**
 * Standard Components statuses enum
 * INIT = Initial/clean status of the component
 * LOADING = When you are waiting for a response of data necessary to render the  component
 *      |-> LOADED = Finished LOADING (Could use IDLE)
 *      |-> IDLE = Finished Loading or Saving
 * SAVING = Status of an action of the component loaded (delete, saving, editing)
 *      |-> IDLE = Finished delete, saving, editing
 * ERROR = Error state for the component
 **/
export enum ComponentStatus {
    INIT = 'INIT',
    LOADING = 'LOADING',
    LOADED = 'LOADED',
    SAVING = 'SAVING',
    IDLE = 'IDLE',
    ERROR = 'ERROR'
}

export const enum FeaturedFlags {
    LOAD_FRONTEND_EXPERIMENTS = 'FEATURE_FLAG_EXPERIMENTS',
    DOTFAVORITEPAGE_FEATURE_ENABLE = 'DOTFAVORITEPAGE_FEATURE_ENABLE',
    FEATURE_FLAG_TEMPLATE_BUILDER = 'FEATURE_FLAG_TEMPLATE_BUILDER_2',
    FEATURE_FLAG_SEO_IMPROVEMENTS = 'FEATURE_FLAG_SEO_IMPROVEMENTS',
    FEATURE_FLAG_SEO_PAGE_TOOLS = 'FEATURE_FLAG_SEO_PAGE_TOOLS',
    FEATURE_FLAG_EDIT_URL_CONTENT_MAP = 'FEATURE_FLAG_EDIT_URL_CONTENT_MAP',
    FEATURE_FLAG_CONTENT_EDITOR2_ENABLED = 'CONTENT_EDITOR2_ENABLED',
    FEATURE_FLAG_CONTENT_EDITOR2_CONTENT_TYPE = 'CONTENT_EDITOR2_CONTENT_TYPE',
    FEATURE_FLAG_ANNOUNCEMENTS = 'FEATURE_FLAG_ANNOUNCEMENTS',
    FEATURE_FLAG_NEW_EDIT_PAGE = 'FEATURE_FLAG_NEW_EDIT_PAGE',
    FEATURE_FLAG_UVE_PREVIEW_MODE = 'FEATURE_FLAG_UVE_PREVIEW_MODE'
}

export const enum DotConfigurationVariables {
    CONTENT_PALETTE_HIDDEN_CONTENT_TYPES = 'CONTENT_PALETTE_HIDDEN_CONTENT_TYPES',
    WYSIWYG_IMAGE_URL_PATTERN = 'WYSIWYG_IMAGE_URL_PATTERN',
    DOT_DEFAULT_CONTAINER = 'DOT_DEFAULT_CONTAINER'
}

export const FEATURE_FLAG_NOT_FOUND = 'NOT_FOUND';

export type DotDropdownGroupSelectOption<T> = {
    label: string;
    items: DotDropdownSelectOption<T>[];
};

export type DotDropdownSelectOption<T> = {
    label: string;
    value: T;
    inactive?: boolean;
    description?: string;
};

export enum DialogStatus {
    HIDE = 'HIDE',
    SHOW = 'SHOW'
}
