import { vi } from 'vitest';

import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { uploadAssets } from './upload-assets';

import { ValidationError, type DotCMSRuntime } from '../../runtime';

/**
 * How upload_assets walks `src`, against directory listings a real disk will not produce on
 * demand. `readdir()` is forbidden: it loads a directory's whole listing before the scan limit
 * can count it. `opendir()` serves whichever listing the test installs in `listing`, and
 * counts the directories open at once.
 */
interface FakeEntry {
    name: string;
    isDirectory: () => boolean;
    isFile: () => boolean;
}

const file = (name: string): FakeEntry => ({
    name,
    isDirectory: () => false,
    isFile: () => true
});
const directory = (name: string): FakeEntry => ({
    name,
    isDirectory: () => true,
    isFile: () => false
});

let listing: (path: string) => Iterable<FakeEntry>;
let entriesRead = 0;
let open = 0;
let mostOpen = 0;

vi.mock('node:fs/promises', async (importOriginal) => {
    const actual = await importOriginal<typeof import('node:fs/promises')>();

    return {
        ...actual,
        readdir: async () => {
            throw new Error('readdir() loads the whole directory; the walk must stream it');
        },
        opendir: async (path: string) => {
            open++;
            mostOpen = Math.max(mostOpen, open);
            let closed = false;
            const close = async () => {
                if (!closed) {
                    closed = true;
                    open--;
                }
            };

            return {
                async *[Symbol.asyncIterator]() {
                    try {
                        for (const entry of listing(path)) {
                            entriesRead++;
                            yield entry;
                        }
                    } finally {
                        // As a real Dir does: iteration that ends, breaks or throws closes it.
                        await close();
                    }
                },
                close
            };
        }
    };
});

describe('uploadAssets — walking src', () => {
    let src: string;
    const request = vi.fn();
    const dotcms = { request } as unknown as DotCMSRuntime;

    beforeEach(async () => {
        src = await mkdtemp(join(tmpdir(), 'dot-walk-'));
        entriesRead = 0;
        open = 0;
        mostOpen = 0;
        request.mockReset();
    });

    afterEach(async () => {
        await rm(src, { recursive: true, force: true });
    });

    it('stops reading a vast directory as soon as the scan limit is passed', async () => {
        listing = function* () {
            for (let n = 0; ; n++) yield file(`f${n}.txt`);
        };

        const error = await uploadAssets({
            dotcms,
            src,
            dest: '//demo.dotcms.com/application',
            include: '*.css',
            publish: false,
            verify: false,
            maxScannedEntries: 100
        }).catch((e: unknown) => e);

        expect(error).toBeInstanceOf(ValidationError);
        expect(entriesRead).toBe(101);
        expect(request).not.toHaveBeenCalled();
        expect(open).toBe(0);
    });

    it('keeps one directory open at a time, however deep the tree', async () => {
        // 80 levels, each holding one file and the next level. Recursing while the parent was
        // still being read held every ancestor open — EMFILE around depth 20 under a
        // 32-descriptor limit, though the entry count was well inside the scan limit.
        const DEPTH = 80;
        listing = function* (path) {
            const depth = path.slice(src.length).split('/').filter(Boolean).length;
            yield file(`f${depth}.txt`);
            if (depth < DEPTH) yield directory('d');
        };

        const error = await uploadAssets({
            dotcms,
            src,
            dest: '//demo.dotcms.com/application',
            include: '*.css',
            publish: false,
            verify: false
        }).catch((e: unknown) => e);

        // Nothing matched `*.css`: a clean empty result, having walked all 80 levels.
        expect(error).not.toBeInstanceOf(Error);
        expect(entriesRead).toBe(DEPTH * 2 + 1);
        expect(mostOpen).toBe(1);
        expect(open).toBe(0);
    });
});
