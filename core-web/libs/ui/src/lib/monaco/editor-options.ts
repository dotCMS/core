/**
 * Shared base options for `ngx-monaco-editor` instances used across dotCMS portlets.
 * Consumers add `language`, `wordWrap`, etc. on top of this.
 */
export const DOT_MONACO_BASE_OPTIONS = {
    theme: 'vs',
    minimap: { enabled: false },
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    automaticLayout: true,
    fontSize: 13,
    fontFamily: 'JetBrains Mono, Fira Code, Consolas, monospace'
} as const;

/**
 * Read-only JSON viewer preset with bottom padding so the closing brace
 * doesn't sit flush against the viewport edge when scrolled.
 */
export const DOT_MONACO_RAW_OPTIONS = {
    ...DOT_MONACO_BASE_OPTIONS,
    language: 'json',
    readOnly: true,
    lineNumbers: 'off',
    padding: { top: 12, bottom: 24 }
} as const;
