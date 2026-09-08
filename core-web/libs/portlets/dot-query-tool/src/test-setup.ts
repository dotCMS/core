import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { provideZoneChangeDetection } from '@angular/core';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

class MockResizeObserver {
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
}

Object.defineProperty(window, 'ResizeObserver', {
    writable: true,
    configurable: true,
    value: MockResizeObserver
});

/**
 * `@materia-ui/ngx-monaco-editor` reads a GLOBAL `monaco`, loaded from an asset at
 * runtime and absent under test: its `initEditor()` calls `monaco.editor.create(...)`
 * from ngAfterViewInit and threw `ReferenceError: monaco is not defined`. Angular
 * reports a hook failure asynchronously, so Jest dropped it while Vitest counts it as
 * an unhandled error. The editor is not what these specs assert on — they exercise the
 * page around it — so this is a no-op surface, not a fake editor.
 */
const disposable = { dispose: () => undefined };

// Every editor member the wrapper touches (grepped off its bundle: `this.editor.*`),
// so a missing one cannot resurface as another unhandled error.
const monacoEditorStub = {
    onDidBlurEditorText: () => disposable,
    onDidChangeModelContent: () => disposable,
    onDidChangeModelDecorations: () => disposable,
    getValue: () => '',
    setValue: () => undefined,
    getModel: () => null,
    setModel: () => undefined,
    updateOptions: () => undefined,
    layout: () => undefined,
    dispose: () => undefined
};

Object.defineProperty(globalThis, 'monaco', {
    writable: true,
    configurable: true,
    value: {
        editor: {
            create: () => monacoEditorStub,
            createDiffEditor: () => ({ ...monacoEditorStub, setModel: () => undefined }),
            createModel: () => ({ uri: { path: '' }, dispose: () => undefined }),
            getModels: () => [],
            getModelMarkers: () => [],
            setModelLanguage: () => undefined,
            setTheme: () => undefined
        },
        Uri: { parse: (value: string) => ({ path: value }) }
    }
});
