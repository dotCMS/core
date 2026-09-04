import * as fs from 'node:fs/promises';
import * as os from 'node:os';
import * as path from 'node:path';

import { protectFromVersionControl } from './gitignore';

let dir: string;
beforeEach(async () => { dir = await fs.mkdtemp(path.join(os.tmpdir(), 'dotcms-vcs-')); });
afterEach(async () => { await fs.rm(dir, { recursive: true, force: true }); });

const asRepo = async () => fs.mkdir(path.join(dir, '.git'), { recursive: true });
const gitignore = () => fs.readFile(path.join(dir, '.gitignore'), 'utf8');

describe('names the files a token went into (FR-023)', () => {
    it('names every one of them, in a repository', async () => {
        await asRepo();
        const files = [path.join(dir, '.cursor', 'mcp.json'), path.join(dir, '.mcp.json')];
        const out = await protectFromVersionControl({ files, cwd: dir, confirmExclude: async () => true });
        expect(out.files).toEqual(expect.arrayContaining(files));
    });

    it('offers exclusion and writes .gitignore when accepted', async () => {
        await asRepo();
        const files = [path.join(dir, '.cursor', 'mcp.json')];
        const confirm = jest.fn().mockResolvedValue(true);
        const out = await protectFromVersionControl({ files, cwd: dir, confirmExclude: confirm });
        expect(confirm).toHaveBeenCalled();
        expect(out.excluded).toBe(true);
        expect(await gitignore()).toContain('.cursor/mcp.json');
    });

    it('does not write .gitignore when declined', async () => {
        await asRepo();
        const out = await protectFromVersionControl({
            files: [path.join(dir, '.mcp.json')], cwd: dir, confirmExclude: async () => false
        });
        expect(out.excluded).toBe(false);
        await expect(gitignore()).rejects.toThrow();
    });

    it('appends without duplicating an entry that is already excluded', async () => {
        await asRepo();
        await fs.writeFile(path.join(dir, '.gitignore'), 'node_modules\n.mcp.json\n', 'utf8');
        await protectFromVersionControl({
            files: [path.join(dir, '.mcp.json')], cwd: dir, confirmExclude: async () => true
        });
        const content = await gitignore();
        expect(content.match(/^\.mcp\.json$/gm) ?? []).toHaveLength(1);
        expect(content).toContain('node_modules');
    });
});

describe('a directory not under version control (FR-023a)', () => {
    it('still names the files rather than silently skipping the step', async () => {
        const files = [path.join(dir, '.cursor', 'mcp.json')];
        const out = await protectFromVersionControl({ files, cwd: dir, confirmExclude: async () => true });
        expect(out.inRepository).toBe(false);
        expect(out.files).toEqual(files);
    });

    it('warns the files are unprotected', async () => {
        const out = await protectFromVersionControl({
            files: [path.join(dir, '.mcp.json')], cwd: dir, confirmExclude: async () => true
        });
        expect(out.warnings.join(' ')).toMatch(/not.*version control|unprotected/i);
    });

    it('writes no .gitignore where there is no repository', async () => {
        await protectFromVersionControl({
            files: [path.join(dir, '.mcp.json')], cwd: dir, confirmExclude: async () => true
        });
        await expect(gitignore()).rejects.toThrow();
    });
});

describe('a repo-root .mcp.json is conventionally committed (FR-024)', () => {
    it('warns explicitly that this file is normally committed', async () => {
        await asRepo();
        const out = await protectFromVersionControl({
            files: [path.join(dir, '.mcp.json')], cwd: dir, confirmExclude: async () => true
        });
        expect(out.warnings.join(' ')).toMatch(/normally committed|conventionally committed/i);
    });

    it('does not warn that way for a file with no such convention', async () => {
        await asRepo();
        const out = await protectFromVersionControl({
            files: [path.join(dir, '.cursor', 'mcp.json')], cwd: dir, confirmExclude: async () => true
        });
        expect(out.warnings.join(' ')).not.toMatch(/normally committed/i);
    });
});
