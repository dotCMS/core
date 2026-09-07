import { Editor } from '@tiptap/core';

/**
 * Resolve the editor's host HTMLElement from `editor.options.element`.
 *
 * v3 widened the `element` type to `HTMLElement | { mount: HTMLElement } | ((editor: HTMLElement) => void)`,
 * so direct property access (`parentElement`, `firstChild`, etc.) no longer typechecks.
 * Callers that previously assumed an `HTMLElement` should funnel through this helper.
 */
export function getEditorElement(editor: Editor): HTMLElement | null {
    const el = editor.options.element as unknown;
    if (!el) {
        return null;
    }

    if (el instanceof HTMLElement) {
        return el;
    }

    if (typeof el === 'object' && el !== null && 'mount' in el) {
        const mount = (el as { mount: unknown }).mount;

        return mount instanceof HTMLElement ? mount : null;
    }

    return null;
}
