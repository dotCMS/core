import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { provideZoneChangeDetection } from '@angular/core';

import { monacoMock } from '@dotcms/utils-testing';

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
 * from a lifecycle hook and threw `ReferenceError: monaco is not defined`. Angular
 * reports a hook failure asynchronously, so Jest dropped it while Vitest counts it as
 * an unhandled error.
 *
 * The shared mock from utils-testing rather than a local stub: dot-es-search needs the
 * same global, and a second hand-rolled copy is how the two drift apart — the first one
 * here was already missing `onDidBlurEditorText`.
 */
Object.defineProperty(globalThis, 'monaco', {
    writable: true,
    configurable: true,
    value: monacoMock
});
