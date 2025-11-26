import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

import { SelectItem } from 'primeng/api';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { PrincipalConfiguration } from '@dotcms/ui';

import { CustomFieldConfig } from './dot-edit-content-custom-field.interface';
import { FIELD_TYPES } from './dot-edit-content-field.enum';

export const CALENDAR_FIELD_TYPES = [FIELD_TYPES.DATE, FIELD_TYPES.DATE_AND_TIME, FIELD_TYPES.TIME];

export const CALENDAR_FIELD_TYPES_WITH_TIME = [FIELD_TYPES.DATE_AND_TIME, FIELD_TYPES.TIME];

export const FLATTENED_FIELD_TYPES = [
    FIELD_TYPES.CHECKBOX,
    FIELD_TYPES.MULTI_SELECT,
    FIELD_TYPES.TAG
];

export const UNCASTED_FIELD_TYPES = [FIELD_TYPES.BLOCK_EDITOR, FIELD_TYPES.KEY_VALUE];

export const TAB_FIELD_CLAZZ = 'com.dotcms.contenttype.model.field.ImmutableTabDividerField';

/**
 * Enum for representing the available languages supported in the Monaco Editor.
 * Each enum value corresponds to a specific language identifier.
 */
export enum AvailableLanguageMonaco {
    PlainText = 'plaintext',
    Javascript = 'javascript',
    Markdown = 'markdown',
    Html = 'html',
    Velocity = 'velocity',
    Json = 'json'
}

/**
 * An array of objects representing options for languages available in Monaco Editor.
 *
 * Each element in the array contains a label for display purposes and a corresponding value from the AvailableLanguageMonaco enumeration.
 *
 * The options provided include:
 * - Html
 * - Javascript
 * - Markdown
 * - Plain Text
 *
 * @type {SelectItem[]}
 */
export const MonacoLanguageOptions: SelectItem[] = [
    { label: 'Html', value: AvailableLanguageMonaco.Html },
    { label: 'Javascript', value: AvailableLanguageMonaco.Javascript },
    { label: 'Markdown', value: AvailableLanguageMonaco.Markdown },
    { label: 'Plain Text', value: AvailableLanguageMonaco.PlainText }
];

/**
 * The default language setting for the Monaco Editor.
 */
export const DEFAULT_MONACO_LANGUAGE = AvailableLanguageMonaco.Html;

export const DEFAULT_MONACO_CONFIG: MonacoEditorConstructionOptions = {
    theme: 'vs',
    minimap: {
        enabled: false
    },
    cursorBlinking: 'solid',
    overviewRulerBorder: false,
    mouseWheelZoom: false,
    lineNumbers: 'on',
    roundedSelection: false,
    automaticLayout: true,
    fixedOverflowWidgets: true,
    language: DEFAULT_MONACO_LANGUAGE,
    fontSize: 14
};

/**
 * Represent the able messages to use in the component DotEmptyContainerComponent
 */
export const CATEGORY_FIELD_EMPTY_MESSAGES: Record<
    ComponentStatus.ERROR | 'empty' | 'noResults',
    PrincipalConfiguration
> = {
    empty: {
        title: 'edit.content.category-field.search.empty.title',
        icon: 'pi-folder-open',
        subtitle: 'edit.content.category-field.search.empty.legend'
    },
    noResults: {
        title: 'edit.content.category-field.search.not-found.title',
        icon: 'pi-exclamation-circle',
        subtitle: 'edit.content.category-field.search.not-found.legend'
    },
    [ComponentStatus.ERROR]: {
        title: 'edit.content.category-field.search.error.title',
        icon: 'pi-exclamation-triangle',
        subtitle: 'edit.content.category-field.search.error.legend'
    }
};

/**
 * Represents the route for the content search page.
 * This constant is used for navigation to the content listing page,
 */
export const CONTENT_SEARCH_ROUTE = '/c/content';

/**
 * Default dialog/modal dimensions used across the application.
 * These constants provide consistent sizing for different dialog types.
 */
export const DIALOG_DIMENSIONS = {
    /** Modal width for custom fields displayed as dialogs */
    MODAL_WIDTH: '500px',
    /** Modal height for custom fields displayed as dialogs */
    MODAL_HEIGHT: '500px'
} as const;

/**
 * Default configuration for custom fields.
 * Used both for fields without field variables and as fallback for configured fields.
 */
export const DEFAULT_CUSTOM_FIELD_CONFIG: CustomFieldConfig = {
    showAsModal: false,
    width: DIALOG_DIMENSIONS.MODAL_WIDTH,
    height: DIALOG_DIMENSIONS.MODAL_HEIGHT
};

/**
 * Key name for the custom field options in field variables.
 * This is the key that should be used when storing JSON configuration in field variables.
 */
export const CUSTOM_FIELD_OPTIONS_KEY = 'customFieldOptions';
