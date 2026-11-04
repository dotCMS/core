import { vi } from 'vitest';

import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { downloadAssets } from './download-assets';

import type { DotCMSRuntime, RequestOptions } from '../../runtime';

/**
 * `FileHandle.write()` may legally write fewer bytes than it was given; it reports how many
 * in `bytesWritten`. A disk rarely does it on demand, so this file's `open` hands back the real
 * handle with a `write` that stores at most 3 bytes a call. Counting the chunk instead of the
 * bytes written, the download would report success for a file silently cut short.
 */
vi.mock('node:fs/promises', async (importOriginal) => {
    const actual = await importOriginal<typeof import('node:fs/promises')>();

    return {
        ...actual,
        open: async (...args: Parameters<typeof actual.open>) => {
            const handle = await actual.open(...args);
            const write = (buffer: Uint8Array, offset = 0, length = buffer.byteLength - offset) =>
                handle.write(buffer, offset, Math.min(length, 3));

            return new Proxy(handle, {
                get: (target, key) => {
                    if (key === 'write') return write;
                    const value = Reflect.get(target, key);
                    return typeof value === 'function' ? value.bind(target) : value;
                }
            });
        }
    };
});

const BODY = 'a body longer than three bytes, written in pieces';

function streamingRuntime(): DotCMSRuntime {
    const request = vi.fn(async (opts: RequestOptions) => {
        if (!opts.onBody) {
            throw new Error('expected a streamed asset read');
        }

        return opts.onBody(
            new ReadableStream({
                start(controller) {
                    controller.enqueue(new TextEncoder().encode(BODY));
                    controller.close();
                }
            }),
            { contentType: 'text/plain' }
        );
    });

    return { request, loadContext: vi.fn() } as unknown as DotCMSRuntime;
}

describe('downloadAssets — short writes', () => {
    let dest: string;

    beforeEach(async () => {
        dest = await mkdtemp(join(tmpdir(), 'dot-short-write-'));
    });

    afterEach(async () => {
        await rm(dest, { recursive: true, force: true });
    });

    it('keeps writing until every byte of each chunk is on disk', async () => {
        const manifest = await downloadAssets({
            dotcms: streamingRuntime(),
            path: '//demo.dotcms.com/application/notes.txt',
            kind: 'asset',
            dest,
            recursive: true,
            overwrite: 'overwrite'
        });

        expect(manifest.failures).toEqual([]);
        expect(await readFile(join(dest, 'notes.txt'), 'utf8')).toBe(BODY);
        expect(manifest.files[0]?.bytes).toBe(Buffer.byteLength(BODY));
    });
});
