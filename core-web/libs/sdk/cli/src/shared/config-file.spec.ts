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
