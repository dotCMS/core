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

    function loadMonaco(): void {
        (globalThis as { monaco?: unknown }).monaco = {
            KeyMod: { CtrlCmd: CTRL_CMD },
            KeyCode: { Enter: ENTER }
        };
    }

    function pressShortcut(): void {
        editor.addAction.mock.calls[0][0].run();
    }

    it('binds Cmd/Ctrl + Enter to the run handler once Monaco is loaded', () => {
        loadMonaco();
        const run = vi.fn();

        registerDotMonacoRunShortcut(editor as DotMonacoRunShortcutEditor, run, {
            label: 'Run query'
        });

        expect(editor.addAction).toHaveBeenCalledWith({
            id: DOT_MONACO_RUN_ACTION_ID,
            label: 'Run query',
            keybindings: [CTRL_CMD | ENTER],
            run: expect.any(Function)
        });

        pressShortcut();

        expect(run).toHaveBeenCalledTimes(1);
    });

    it('skips the run handler while canRun returns false', () => {
        loadMonaco();
        const run = vi.fn();
        let allowed = false;

        registerDotMonacoRunShortcut(editor as DotMonacoRunShortcutEditor, run, {
            canRun: () => allowed
        });

        pressShortcut();
        expect(run).not.toHaveBeenCalled();

        allowed = true;
        pressShortcut();
        expect(run).toHaveBeenCalledTimes(1);
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

    it.each([
        [
            'macOS',
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
            '⌘ + Enter'
        ],
        ['Windows', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 'Ctrl + Enter']
    ])('falls back to the user agent on %s when platform is empty', (_os, userAgent, expected) => {
        const navigator = { platform: '', userAgent, userAgentData: { platform: '' } };

        expect(getDotMonacoRunShortcutLabel(navigator as unknown as Navigator)).toBe(expected);
    });

    it('falls back to Ctrl without a navigator', () => {
        expect(getDotMonacoRunShortcutLabel(undefined)).toBe('Ctrl + Enter');
    });
});
