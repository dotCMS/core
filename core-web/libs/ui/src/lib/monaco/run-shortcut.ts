/**
 * Keyboard shortcut that runs the query/script typed into a Monaco editor:
 * `Cmd + Enter` on macOS, `Ctrl + Enter` everywhere else.
 *
 * Monaco is loaded at runtime through ngx-monaco-editor's AMD loader, so the
 * key constants are read from `window.monaco` once the editor exists instead of
 * importing `monaco-editor` (which would pull the whole bundle into the lib).
 */

/** The subset of Monaco's global namespace needed to build the keybinding. */
interface MonacoKeyNamespace {
    KeyMod: { CtrlCmd: number };
    KeyCode: { Enter: number };
}

/** The subset of `monaco.editor.IStandaloneCodeEditor` used to register the action. */
export interface DotMonacoRunShortcutEditor {
    addAction(descriptor: {
        id: string;
        label: string;
        keybindings?: number[];
        run: () => void;
    }): unknown;
}

/** Monaco action id for the run shortcut; one per editor instance. */
export const DOT_MONACO_RUN_ACTION_ID = 'dotcms.run';

/**
 * Binds `Cmd/Ctrl + Enter` to `run` on the given editor.
 *
 * The binding is registered as an editor action, so it is scoped to that editor
 * and only fires while it has focus — other editors on the page (read-only
 * result viewers, for instance) are unaffected. It also overrides Monaco's own
 * `Ctrl + Enter` "insert line after" binding inside that editor.
 *
 * Callers are responsible for guarding `run` against in-flight requests.
 *
 * @param editor the editor instance emitted by `ngx-monaco-editor`'s `(onInit)`
 * @param run    what the shortcut does, usually the same handler as the Run button
 * @param label  human-readable name shown in Monaco's command palette
 */
export function registerDotMonacoRunShortcut(
    editor: DotMonacoRunShortcutEditor,
    run: () => void,
    label = 'Run'
): void {
    const monaco = (globalThis as { monaco?: MonacoKeyNamespace }).monaco;

    if (!monaco) {
        return;
    }

    editor.addAction({
        id: DOT_MONACO_RUN_ACTION_ID,
        label,
        keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter],
        run
    });
}

/**
 * The shortcut as it should be shown to the user: `⌘ + Enter` on Apple
 * platforms, `Ctrl + Enter` otherwise.
 *
 * @param navigator the browser navigator; absent outside a browser, which falls back to `Ctrl`
 */
export function getDotMonacoRunShortcutLabel(navigator?: Navigator | null): string {
    const platform =
        (navigator as (Navigator & { userAgentData?: { platform?: string } }) | null | undefined)
            ?.userAgentData?.platform ??
        navigator?.platform ??
        '';

    return /mac|iphone|ipad|ipod/i.test(platform) ? '⌘ + Enter' : 'Ctrl + Enter';
}
