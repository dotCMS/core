import * as fs from 'node:fs/promises';
import * as os from 'node:os';
import * as path from 'node:path';

import { writeJsonTarget } from './json-target';
import { getTarget } from './registry';

const URL_ = 'https://demo.dotcms.com';
const TOKEN = 'dot_testtoken_1234';

let dir: string;
beforeEach(async () => { dir = await fs.mkdtemp(path.join(os.tmpdir(), 'dotcms-cli-')); });
afterEach(async () => { await fs.rm(dir, { recursive: true, force: true }); });

async function readJson(p: string) { return JSON.parse(await fs.readFile(p, 'utf8')); }

describe('json target — fresh file (FR-019, FR-020)', () => {
    it('creates the file and any missing parent directories', async () => {
        const target = getTarget('cursor');
        const written = await writeJsonTarget({ target, scope: 'folder', url: URL_, token: TOKEN, cwd: dir });
        await expect(fs.stat(written)).resolves.toBeDefined();
    });

    it('writes the entry under the container key that editor reads', async () => {
        const written = await writeJsonTarget({
            target: getTarget('vscode'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const doc = await readJson(written);
        expect(doc['servers']).toBeDefined();
        expect(doc['mcpServers']).toBeUndefined();
    });

    it('writes the standard stdio shape with the env var names the server reads', async () => {
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const entry = (await readJson(written))['mcpServers']['dotcms'];
        expect(entry.type).toBe('stdio');
        expect(entry.command).toBe('npx');
        expect(entry.args).toEqual(['-y', '@dotcms/mcp-server@latest']);
        expect(entry.env).toEqual({ DOTCMS_URL: URL_, AUTH_TOKEN: TOKEN });
        expect(entry.env['DOTCMS_TOKEN']).toBeUndefined();
    });

    it("writes OpenCode's different shape, not merely a different key", async () => {
        const written = await writeJsonTarget({
            target: getTarget('opencode'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const entry = (await readJson(written))['mcp']['dotcms'];
        expect(entry.type).toBe('local');
        expect(entry.command).toEqual(['npx', '-y', '@dotcms/mcp-server@latest']);
        expect(entry.enabled).toBe(true);
        expect(entry.environment).toEqual({ DOTCMS_URL: URL_, AUTH_TOKEN: TOKEN });
    });

    it('writes 2-space indented JSON', async () => {
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        expect(await fs.readFile(written, 'utf8')).toContain('\n  "mcpServers"');
    });
});

describe('json target — merge, never clobber (FR-016)', () => {
    /** A config file is shared property: it already holds servers the developer depends on. */
    const seeded = {
        mcpServers: {
            'some-other-server': { command: 'node', args: ['other.js'] },
            'dotcms-lookalike': { command: 'node', args: ['not-ours.js'] }
        },
        unrelatedSetting: true,
        nested: { deep: { value: 42 } }
    };

    async function seed(target = getTarget('cursor')) {
        const file = target.configPath('folder', dir) as string;
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(file, JSON.stringify(seeded, null, 2), 'utf8');
        return file;
    }

    it('preserves every other server verbatim', async () => {
        await seed();
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const doc = await readJson(written);
        expect(doc['mcpServers']['some-other-server']).toEqual(seeded.mcpServers['some-other-server']);
    });

    it('preserves unrelated top-level settings, including nested ones', async () => {
        await seed();
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const doc = await readJson(written);
        expect(doc['unrelatedSetting']).toBe(true);
        expect(doc['nested']).toEqual({ deep: { value: 42 } });
    });

    it('does not touch a similarly-named sibling server', async () => {
        await seed();
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const doc = await readJson(written);
        expect(doc['mcpServers']['dotcms-lookalike']).toEqual(seeded.mcpServers['dotcms-lookalike']);
    });

    it('replaces an existing dotcms entry rather than duplicating it', async () => {
        const file = await seed();
        await fs.writeFile(
            file,
            JSON.stringify(
                { mcpServers: { ...seeded.mcpServers, dotcms: { command: 'npx', args: ['stale'] } } },
                null,
                2
            ),
            'utf8'
        );
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const entry = (await readJson(written))['mcpServers']['dotcms'];
        expect(entry.args).toEqual(['-y', '@dotcms/mcp-server@latest']);
        expect(Object.keys((await readJson(written))['mcpServers']).filter((k) => k === 'dotcms')).toHaveLength(1);
    });

    it('leaves everything but the dotcms key byte-for-byte identical', async () => {
        const file = await seed();
        const before = JSON.parse(await fs.readFile(file, 'utf8'));
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        });
        const after = await readJson(written);
        delete after['mcpServers']['dotcms'];
        expect(after).toEqual(before);
    });
});

describe('json target — malformed input (FR-018)', () => {
    it('fails with a named error identifying the file and the remedy', async () => {
        const file = getTarget('cursor').configPath('folder', dir) as string;
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(file, '{ not json', 'utf8');
        const err = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        }).catch((e: Error) => e);
        expect((err as Error).message).toContain(file);
        expect((err as Error).message).toMatch(/not valid JSON/i);
        expect((err as Error).message).toMatch(/skip-mcp|fix it/i);
    });

    it('leaves the malformed file byte-for-byte untouched — never overwrites', async () => {
        const file = getTarget('cursor').configPath('folder', dir) as string;
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(file, '{ not json', 'utf8');
        await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN, cwd: dir
        }).catch(() => undefined);
        expect(await fs.readFile(file, 'utf8')).toBe('{ not json');
    });
});
