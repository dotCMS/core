import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

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
