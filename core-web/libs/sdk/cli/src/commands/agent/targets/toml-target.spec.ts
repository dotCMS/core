import { parse } from 'smol-toml';

import * as fs from 'node:fs/promises';
import * as os from 'node:os';
import * as path from 'node:path';


import { getTarget } from './registry';
import { writeTomlTarget } from './toml-target';

const URL_ = 'https://demo.dotcms.com';
const TOKEN = 'dot_testtoken_1234';

let dir: string;
beforeEach(async () => { dir = await fs.mkdtemp(path.join(os.tmpdir(), 'dotcms-toml-')); });
afterEach(async () => { await fs.rm(dir, { recursive: true, force: true }); });

const codex = () => getTarget('codex');
const write = () => writeTomlTarget({ target: codex(), scope: 'folder', url: URL_, token: TOKEN, cwd: dir });
async function seed(content: string) {
    const file = codex().configPath('folder', dir) as string;
    await fs.mkdir(path.dirname(file), { recursive: true });
    await fs.writeFile(file, content, 'utf8');
    return file;
}

describe('toml target — fresh file', () => {
    it('creates the file and its parent directory', async () => {
        const written = await write();
        await expect(fs.stat(written)).resolves.toBeDefined();
    });

    it('writes [mcp_servers.dotcms] with the env table the server reads', async () => {
        const written = await write();
        const doc = parse(await fs.readFile(written, 'utf8')) as Record<string, never>;
        const entry = doc['mcp_servers']['dotcms'];
        expect(entry['command']).toBe('npx');
        expect(entry['args']).toEqual(['-y', '@dotcms/mcp-server@latest']);
        expect(entry['env']).toEqual({ DOTCMS_URL: URL_, AUTH_TOKEN: TOKEN });
    });
});

describe('toml target — round-trips a file we did not write (FR-016, research R6)', () => {
    /** The reason this is a real TOML parser and not text splicing: replacing a table in place
     *  requires understanding where tables begin and end. */
    const existing = `# Codex configuration — hand written
model = "o3"

[mcp_servers.other-server]
command = "node"
args = ["other.js"]

# a comment attached to a later table
[some.unrelated.table]
value = 42
`;

    it('preserves unrelated top-level keys', async () => {
        await seed(existing);
        const written = await write();
        const doc = parse(await fs.readFile(written, 'utf8')) as Record<string, never>;
        expect(doc['model']).toBe('o3');
    });

    it('preserves other MCP servers verbatim', async () => {
        await seed(existing);
        const written = await write();
        const doc = parse(await fs.readFile(written, 'utf8')) as Record<string, never>;
        expect(doc['mcp_servers']['other-server']).toEqual({ command: 'node', args: ['other.js'] });
    });

    it('preserves unrelated nested tables', async () => {
        await seed(existing);
        const written = await write();
        const doc = parse(await fs.readFile(written, 'utf8')) as Record<string, never>;
        expect(doc['some']['unrelated']['table']).toEqual({ value: 42 });
    });

    it('replaces an existing dotcms table rather than appending a second one', async () => {
        await seed(`${existing}
[mcp_servers.dotcms]
command = "npx"
args = ["stale"]
`);
        const written = await write();
        const raw = await fs.readFile(written, 'utf8');
        const doc = parse(raw) as Record<string, never>;
        expect(doc['mcp_servers']['dotcms']['args']).toEqual(['-y', '@dotcms/mcp-server@latest']);
        expect(raw.match(/\[mcp_servers\.dotcms\]/g) ?? []).toHaveLength(1);
    });
});

describe('toml target — malformed input (FR-018)', () => {
    it('fails with a named error and leaves the file untouched', async () => {
        const file = await seed('this = = not toml');
        const err = await write().catch((e: Error) => e);
        expect((err as Error).message).toContain(file);
        expect((err as Error).message).toMatch(/not valid TOML/i);
        expect(await fs.readFile(file, 'utf8')).toBe('this = = not toml');
    });
});
