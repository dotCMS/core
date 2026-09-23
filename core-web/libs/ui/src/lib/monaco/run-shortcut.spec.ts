import { vi } from 'vitest';

import {
    DOT_MONACO_RUN_ACTION_ID,
    DotMonacoRunShortcutEditor,
    getDotMonacoRunShortcutLabel,
    registerDotMonacoRunShortcut
} from './run-shortcut';

const CTRL_CMD = 2048;
const ENTER = 3;

describe('registerDotMonacoRunShortcut', () => {
    let editor: { addAction: ReturnType<typeof vi.fn> };

    beforeEach(() => {
        editor = { addAction: vi.fn() };
    });

    afterEach(() => {
        delete (globalThis as { monaco?: unknown }).monaco;
    });

    it('binds Cmd/Ctrl + Enter to the run handler once Monaco is loaded', () => {
        (globalThis as { monaco?: unknown }).monaco = {
            KeyMod: { CtrlCmd: CTRL_CMD },
            KeyCode: { Enter: ENTER }
        };
        const run = vi.fn();

        registerDotMonacoRunShortcut(editor as DotMonacoRunShortcutEditor, run, 'Run query');

        expect(editor.addAction).toHaveBeenCalledWith({
            id: DOT_MONACO_RUN_ACTION_ID,
            label: 'Run query',
            keybindings: [CTRL_CMD | ENTER],
            run
        });
    });

    it('does nothing when Monaco is not loaded', () => {
        registerDotMonacoRunShortcut(editor as DotMonacoRunShortcutEditor, vi.fn());

        expect(editor.addAction).not.toHaveBeenCalled();
    });
});

describe('getDotMonacoRunShortcutLabel', () => {
    it.each(['MacIntel', 'iPhone', 'iPad'])('shows ⌘ on %s', (platform) => {
        expect(getDotMonacoRunShortcutLabel({ platform } as Navigator)).toBe('⌘ + Enter');
    });

    it.each(['Win32', 'Linux x86_64'])('shows Ctrl on %s', (platform) => {
        expect(getDotMonacoRunShortcutLabel({ platform } as Navigator)).toBe('Ctrl + Enter');
    });

    it('prefers userAgentData.platform when the browser exposes it', () => {
        const navigator = { platform: 'Win32', userAgentData: { platform: 'macOS' } };

        expect(getDotMonacoRunShortcutLabel(navigator as unknown as Navigator)).toBe('⌘ + Enter');
    });

    it('falls back to Ctrl without a navigator', () => {
        expect(getDotMonacoRunShortcutLabel(undefined)).toBe('Ctrl + Enter');
    });
});
