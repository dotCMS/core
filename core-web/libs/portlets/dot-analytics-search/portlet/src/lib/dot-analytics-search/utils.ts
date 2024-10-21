import { JsonObject } from '@angular-devkit/core';
import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

const BASE_MONACO_EDITOR_OPTIONS: MonacoEditorConstructionOptions = {
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
    language: 'json',
    fontSize: 12
};

export const ANALYTICS_MONACO_EDITOR_OPTIONS: MonacoEditorConstructionOptions = {
    ...BASE_MONACO_EDITOR_OPTIONS
};

export const ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS: MonacoEditorConstructionOptions = {
    ...BASE_MONACO_EDITOR_OPTIONS,
    readOnly: true
};

export const isValidJson = (jsonString: string): boolean | JsonObject => {
    try {
        return JSON.parse(jsonString);
    } catch (e) {
        return false;
    }
};
