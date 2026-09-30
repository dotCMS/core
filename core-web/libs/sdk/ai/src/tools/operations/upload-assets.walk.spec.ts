import { vi } from 'vitest';

import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { uploadAssets } from './upload-assets';

import { ValidationError, type DotCMSRuntime } from '../../runtime';

/**
 * The scan limit only bounds a walk if the walk reads a directory a piece at a time.
 * `readdir()` loads a directory's whole listing into one array before anything can count it,
 * so a single directory of millions of entries cost unbounded time and memory whatever the
 * limit said. Here `src` lists an endless directory through `opendir()`, and `readdir()` —
 * which would have to load it all — is not allowed at all.
 */
let entriesRead = 0;

vi.mock('node:fs/promises', async (importOriginal) => {
    const actual = await importOriginal<typeof import('node:fs/promises')>();

    return {
        ...actual,
        readdir: async () => {
            throw new Error('readdir() loads the whole directory; the walk must stream it');
        },
        opendir: async () => ({
            async *[Symbol.asyncIterator]() {
                for (let n = 0; ; n++) {
                    entriesRead++;
                    yield { name: `f${n}.txt`, isDirectory: () => false, isFile: () => true };
                }
            },
            close: async () => undefined
        })
    };
});

describe('uploadAssets — walking a vast directory', () => {
    let src: string;

    beforeEach(async () => {
        src = await mkdtemp(join(tmpdir(), 'dot-walk-'));
        entriesRead = 0;
    });

    afterEach(async () => {
        await rm(src, { recursive: true, force: true });
    });

    it('stops reading the directory as soon as the scan limit is passed', async () => {
        const request = vi.fn();

        const error = await uploadAssets({
            dotcms: { request } as unknown as DotCMSRuntime,
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
    });
});
