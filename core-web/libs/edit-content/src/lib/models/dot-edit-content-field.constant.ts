import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

import { FIELD_TYPES } from './dot-edit-content-field.enum';

export const CALENDAR_FIELD_TYPES = [FIELD_TYPES.DATE, FIELD_TYPES.DATE_AND_TIME, FIELD_TYPES.TIME];

export const FLATTENED_FIELD_TYPES = [
    FIELD_TYPES.CHECKBOX,
    FIELD_TYPES.MULTI_SELECT,
    FIELD_TYPES.TAG
];

export const UNCASTED_FIELD_TYPES = [FIELD_TYPES.BLOCK_EDITOR];

export const TAB_FIELD_CLAZZ = 'com.dotcms.contenttype.model.field.ImmutableTabDividerField';

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
    language: 'text',
    fontSize: 14
};
