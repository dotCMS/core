/**
 * Standard Components statuses enum
 * INIT = Initial status of the component
 * LOADING = When you are waiting for a response of data necessary to render the  component
 * LOADED = Finished LOADING (Could use IDLE)
 * SAVING = Status of an action of the component loaded (delete, saving, editing)
 * IDLE = Finished Saving
 **/
export enum ComponentStatus {
    INIT = 'INIT',
    LOADING = 'LOADING',
    LOADED = 'LOADED',
    SAVING = 'SAVING',
    IDLE = 'IDLE'
}

export const enum FeaturedFlags {
    LOAD_FRONTEND_EXPERIMENTS = 'FEATURE_FLAG_EXPERIMENTS',
    DOTFAVORITEPAGE_FEATURE_ENABLE = 'DOTFAVORITEPAGE_FEATURE_ENABLE',
    FEATURE_FLAG_TEMPLATE_BUILDER = 'FEATURE_FLAG_TEMPLATE_BUILDER_2'
}

export type DotDropdownSelectOption<T> = {
    label: string;
    value: T;
    inactive?: boolean;
    description?: string;
};
