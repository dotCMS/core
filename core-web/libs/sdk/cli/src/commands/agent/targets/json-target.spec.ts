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
        const written = await writeJsonTarget({ target, scope: 'folder', url: URL_, token: TOKEN });
        await expect(fs.stat(written)).resolves.toBeDefined();
    });

    it('writes the entry under the container key that editor reads', async () => {
        const written = await writeJsonTarget({
            target: getTarget('vscode'), scope: 'folder', url: URL_, token: TOKEN
        });
        const doc = await readJson(written);
        expect(doc['servers']).toBeDefined();
        expect(doc['mcpServers']).toBeUndefined();
    });

    it('writes the standard stdio shape with the env var names the server reads', async () => {
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN
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
            target: getTarget('opencode'), scope: 'folder', url: URL_, token: TOKEN
        });
        const entry = (await readJson(written))['mcp']['dotcms'];
        expect(entry.type).toBe('local');
        expect(entry.command).toEqual(['npx', '-y', '@dotcms/mcp-server@latest']);
        expect(entry.enabled).toBe(true);
        expect(entry.environment).toEqual({ DOTCMS_URL: URL_, AUTH_TOKEN: TOKEN });
    });

    it('writes 2-space indented JSON', async () => {
        const written = await writeJsonTarget({
            target: getTarget('cursor'), scope: 'folder', url: URL_, token: TOKEN
        });
        expect(await fs.readFile(written, 'utf8')).toContain('\n  "mcpServers"');
    });
});
