import * as fs from 'node:fs/promises';
import * as os from 'node:os';
import * as path from 'node:path';

import { CAN_RESTRICT, ensureDir, writeMerged } from './config-file';

let dir: string;
beforeEach(async () => {
    dir = await fs.mkdtemp(path.join(os.tmpdir(), 'dotcms-perm-'));
});
afterEach(async () => {
    await fs.rm(dir, { recursive: true, force: true });
});

const write = (file: string) =>
    writeMerged({ file, containerKey: 'mcpServers', entryKey: 'dotcms', entry: { token: 'x' } });

/** POSIX only: on Windows chmod toggles the read-only bit and never touches ACLs, so asserting
 *  a mode there would assert nothing. The Windows behaviour is covered by the honesty test. */
const posix = CAN_RESTRICT ? describe : describe.skip;

posix('file permissions (FR-021, SC-004)', () => {
    it('writes the file readable and writable by its owner only', async () => {
        const file = path.join(dir, 'nested', 'mcp.json');
        await write(file);
        const mode = (await fs.stat(file)).mode & 0o777;
        expect(mode).toBe(0o600);
    });

    it('restricts directories it creates to the owner', async () => {
        const created = path.join(dir, 'made-by-us');
        await ensureDir(created);
        const mode = (await fs.stat(created)).mode & 0o777;
        expect(mode).toBe(0o700);
    });

    it('reports that permissions were applied', async () => {
        const result = await write(path.join(dir, 'mcp.json'));
        expect(result.permissionsApplied).toBe(true);
    });
});

describe('permission honesty (research R5)', () => {
    /**
     * Asserting `permissionsApplied === CAN_RESTRICT` is `true === true` on POSIX, which a
     * hard-coded `true` satisfies — the assertion could only bite on Windows, where nothing
     * runs it. Forcing the capability off proves the claim tracks reality instead.
     */
    it('does NOT claim permissions were applied when the platform cannot apply them', async () => {
        const result = await writeMerged({
            file: path.join(dir, 'mcp.json'),
            containerKey: 'mcpServers',
            entryKey: 'dotcms',
            entry: { token: 'x' },
            canRestrict: false
        });
        expect(result.permissionsApplied).toBe(false);
    });

    it('claims them when the platform can', async () => {
        const result = await writeMerged({
            file: path.join(dir, 'mcp.json'),
            containerKey: 'mcpServers',
            entryKey: 'dotcms',
            entry: { token: 'x' },
            canRestrict: true
        });
        expect(result.permissionsApplied).toBe(true);
    });
});

/**
 * These exist because an end-to-end run against a folder that already held other MCP servers
 * showed the old writer rebuilding the whole document from its parse tree: values survived,
 * bytes did not. FR-016 and target-configs.md both promise the untouched keys come through
 * "byte-for-byte identical", and for a file a team shares that is the difference between a
 * one-entry diff and a whole-file churn.
 *
 * The TOML writer already worked this way (parse to validate, splice to write). This is the
 * JSON side catching up.
 */
describe('merging into a document the developer owns (FR-016)', () => {
    const merge = (file: string, entry: unknown = { type: 'stdio', command: 'npx' }) =>
        writeMerged({ file, containerKey: 'mcpServers', entryKey: 'dotcms', entry });

    it('leaves every untouched line byte-for-byte identical', async () => {
        const file = path.join(dir, 'mcp.json');
        const before = [
            '{',
            '  "mcpServers": {',
            '    "github": {',
            '      "command": "npx",',
            '      "args": ["-y", "@modelcontextprotocol/server-github"],',
            '      "env": { "GITHUB_TOKEN": "ghp_existing" }',
            '    },',
            '    "postgres": { "command": "docker", "args": ["run", "-i", "mcp/postgres"] }',
            '  },',
            '  "someUnrelatedSetting": { "keepMe": true, "nested": [1, 2, 3] }',
            '}',
            ''
        ].join('\n');
        await fs.writeFile(file, before, 'utf8');

        await merge(file);
        const after = await fs.readFile(file, 'utf8');

        // Every original line still present verbatim — not merely value-equivalent.
        for (const line of before.split('\n').slice(1, -2)) {
            if (line.trim() === '' || line.trim() === '},') continue;
            expect(after).toContain(line);
        }
        expect(after).toContain('"args": ["-y", "@modelcontextprotocol/server-github"]');
        expect(after).toContain('"nested": [1, 2, 3]');
        // The IMMEDIATELY PRECEDING sibling is the one a formatting-aware edit reflows: our
        // insertion point abuts it. Asserting it survives compact is what makes this test
        // about preservation rather than about values.
        expect(after).toContain(
            '"postgres": { "command": "docker", "args": ["run", "-i", "mcp/postgres"] }'
        );
    });

    it('preserves comments — .vscode/mcp.json is JSONC, not strict JSON', async () => {
        // A commented VS Code config was rejected outright as "not valid JSON", so the tool
        // refused to configure an editor whose file was perfectly valid for that editor.
        const file = path.join(dir, 'mcp.json');
        await fs.writeFile(
            file,
            '{\n  // Shared team servers -- see wiki\n  "servers": {\n    "github": { "command": "npx" }\n  }\n}\n',
            'utf8'
        );

        await writeMerged({
            file,
            containerKey: 'servers',
            entryKey: 'dotcms',
            entry: { command: 'npx' }
        });
        const after = await fs.readFile(file, 'utf8');

        expect(after).toContain('// Shared team servers -- see wiki');
        expect(after).toContain('"github": { "command": "npx" }');
        expect(after).toContain('"dotcms"');
    });

    it('accepts a trailing comma, which JSONC allows and JSON.parse rejects', async () => {
        const file = path.join(dir, 'mcp.json');
        await fs.writeFile(
            file,
            '{\n  "servers": {\n    "github": { "command": "npx" },\n  },\n}\n',
            'utf8'
        );
        await expect(
            writeMerged({
                file,
                containerKey: 'servers',
                entryKey: 'dotcms',
                entry: { command: 'npx' }
            })
        ).resolves.toBeDefined();
        expect(await fs.readFile(file, 'utf8')).toContain('"dotcms"');
    });

    it('matches the indentation already in the file rather than imposing two spaces', async () => {
        const file = path.join(dir, 'mcp.json');
        await fs.writeFile(
            file,
            '{\n    "mcpServers": {\n        "github": { "command": "npx" }\n    }\n}\n',
            'utf8'
        );
        await merge(file);
        const after = await fs.readFile(file, 'utf8');
        expect(after).toContain('\n        "dotcms"');
    });

    it('still creates a well-formed document when the file does not exist', async () => {
        const file = path.join(dir, 'nested', 'mcp.json');
        await merge(file);
        const doc = JSON.parse(await fs.readFile(file, 'utf8'));
        expect(doc.mcpServers.dotcms).toEqual({ type: 'stdio', command: 'npx' });
    });

    it('replaces our own entry without disturbing its neighbours', async () => {
        const file = path.join(dir, 'mcp.json');
        await fs.writeFile(
            file,
            '{\n  "mcpServers": {\n    "github": { "command": "npx" },\n    "dotcms": { "command": "OLD" }\n  }\n}\n',
            'utf8'
        );
        const result = await merge(file);
        const after = await fs.readFile(file, 'utf8');

        expect(result.replacedExisting).toBe(true);
        expect(after).not.toContain('OLD');
        expect(after).toContain('"github": { "command": "npx" }');
    });

    it('is still a named error, and still writes nothing, when the file is genuinely broken', async () => {
        // Tolerating JSONC must not mean tolerating garbage: FR-018 is unchanged.
        const file = path.join(dir, 'mcp.json');
        const broken = '{ "mcpServers": { "github": ';
        await fs.writeFile(file, broken, 'utf8');
        await expect(merge(file)).rejects.toThrow(/not valid JSON/i);
        expect(await fs.readFile(file, 'utf8')).toBe(broken);
    });
});

describe('a document whose root is not an object (FR-018)', () => {
    /**
     * These parse cleanly, so they slipped past the malformed check and then failed the
     * `tree.type !== 'object'` test — which routed them to the FRESH-FILE branch and replaced
     * the developer's file wholesale. Verified: a file containing `[1,2,3]` came back as
     * `{"mcpServers":{...}}`. FR-018 says never overwrite; say what is wrong and stop.
     */
    it.each([
        ['an array', '[1, 2, 3]\n'],
        ['a string', '"hello"\n'],
        ['a number', '42\n'],
        ['null', 'null\n']
    ])('refuses to write into %s, and leaves it byte-for-byte', async (_what, content) => {
        const file = path.join(dir, 'mcp.json');
        await fs.writeFile(file, content, 'utf8');
        await expect(
            writeMerged({ file, containerKey: 'mcpServers', entryKey: 'dotcms', entry: { a: 1 } })
        ).rejects.toThrow(/not valid JSON/i);
        expect(await fs.readFile(file, 'utf8')).toBe(content);
    });
});
