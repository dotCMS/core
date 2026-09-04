import * as os from 'node:os';

import { TARGETS, getTarget } from './registry';

import type { TargetId } from './types';

const ALL: TargetId[] = ['claude-code', 'cursor', 'vscode', 'codex', 'antigravity', 'devin', 'opencode'];

function withPlatform(platform: NodeJS.Platform, home: string, fn: () => void) {
    const desc = Object.getOwnPropertyDescriptor(process, 'platform');
    Object.defineProperty(process, 'platform', { value: platform, configurable: true });
    const homeSpy = jest.spyOn(os, 'homedir').mockReturnValue(home);
    try { fn(); } finally {
        if (desc) Object.defineProperty(process, 'platform', desc);
        homeSpy.mockRestore();
    }
}

describe('target registry', () => {
    afterEach(() => { jest.restoreAllMocks(); delete process.env['CODEX_HOME']; });

    it('ships exactly the seven supported editors (SC-006)', () => {
        expect(TARGETS.map((t) => t.id).sort()).toEqual([...ALL].sort());
    });

    it('uses the container key each editor actually reads (FR-014)', () => {
        expect(getTarget('cursor').containerKey).toBe('mcpServers');
        expect(getTarget('claude-code').containerKey).toBe('mcpServers');
        // The two that differ, and are the easiest to get wrong:
        expect(getTarget('vscode').containerKey).toBe('servers');
        expect(getTarget('opencode').containerKey).toBe('mcp');
        expect(getTarget('codex').containerKey).toBe('mcp_servers');
    });

    it('marks OpenCode as structurally different, not merely differently keyed', () => {
        expect(getTarget('opencode').entryShape).toBe('opencode-local');
        expect(getTarget('cursor').entryShape).toBe('stdio');
    });

    it('uses TOML only for Codex', () => {
        expect(getTarget('codex').format).toBe('toml');
        for (const id of ALL.filter((i) => i !== 'codex')) {
            expect(getTarget(id).format).toBe('json');
        }
    });

    describe('VS Code global path differs on all three platforms (FR-012)', () => {
        it('macOS', () => {
            withPlatform('darwin', '/Users/dev', () => {
                expect(getTarget('vscode').configPath('global')).toBe(
                    '/Users/dev/Library/Application Support/Code/User/mcp.json'
                );
            });
        });

        it('linux', () => {
            withPlatform('linux', '/home/dev', () => {
                expect(getTarget('vscode').configPath('global')).toBe(
                    '/home/dev/.config/Code/User/mcp.json'
                );
            });
        });

        it('windows', () => {
            const old = process.env['APPDATA'];
            process.env['APPDATA'] = 'C:\\Users\\dev\\AppData\\Roaming';
            withPlatform('win32', 'C:\\Users\\dev', () => {
                expect(getTarget('vscode').configPath('global')).toContain('Code');
                expect(getTarget('vscode').configPath('global')).toContain('mcp.json');
            });
            if (old === undefined) delete process.env['APPDATA']; else process.env['APPDATA'] = old;
        });
    });

    it('honours $CODEX_HOME when set', () => {
        withPlatform('darwin', '/Users/dev', () => {
            expect(getTarget('codex').configPath('global')).toBe('/Users/dev/.codex/config.toml');
            process.env['CODEX_HOME'] = '/custom/codex';
            expect(getTarget('codex').configPath('global')).toBe('/custom/codex/config.toml');
        });
    });

    it('resolves folder scope relative to the working directory, not $HOME', () => {
        withPlatform('darwin', '/Users/dev', () => {
            const p = getTarget('cursor').configPath('folder', '/work/project');
            expect(p).toBe('/work/project/.cursor/mcp.json');
            expect(p).not.toContain('/Users/dev');
        });
    });

    it('gives every target a folder path and a global path, or an explicit null', () => {
        for (const id of ALL) {
            const t = getTarget(id);
            for (const scope of ['folder', 'global'] as const) {
                const p = t.configPath(scope, '/work/project');
                expect(p === null || typeof p === 'string').toBe(true);
            }
        }
    });

    it('names an unknown id rather than returning undefined (FR-032)', () => {
        expect(() => getTarget('not-an-editor' as TargetId)).toThrow(/not-an-editor/);
    });
});
