import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import type { DotCMSRuntime, RequestOptions } from '@dotcms/ai/runtime';

import { downloadAssets, uploadAssets } from './assets-transfer';

/**
 * Exercises `uploadAssets` / `downloadAssets` end to end against a fake runtime and a real
 * temp directory.
 *
 * The sibling spec covers only `splitIncludePatterns` and `includeMatcher`, which left the
 * transfer behaviour itself — the manifest, the per-file failure isolation, and the whole
 * publish-then-verify path — with no coverage at all, in the file carrying the most error
 * handling in the server.
 *
 * A real temp dir rather than a mocked `fs`: these functions walk directories, read bytes and
 * write files, and mocking that surface would mostly test the mock.
 */

const SITE = '//demo.dotcms.com/application/themes/travel';

interface FakeOptions {
    /** Per-path handler. Return a value, or throw to simulate a failure. */
    onRequest?: (options: RequestOptions) => unknown;
}

function fakeRuntime(options?: FakeOptions) {
    const calls: RequestOptions[] = [];
    let uploadCount = 0;

    const request = jest.fn(async (opts: RequestOptions) => {
        calls.push(opts);

        const custom = options?.onRequest?.(opts);
        if (custom !== undefined) {
            return custom;
        }

        // Default happy path: every upload gets an identifier, everything reads back live.
        if (opts.path === '/api/v2/assets/publish' || opts.path === '/api/v2/assets/save') {
            uploadCount += 1;

            return { entity: { identifier: `id-${uploadCount}` } };
        }
        if (opts.path?.startsWith('/api/v1/content/')) {
            return { entity: { live: true } };
        }
        if (opts.path === '/api/v1/workflow/actions/default/fire/PUBLISH') {
            return { entity: {} };
        }

        return {};
    });

    return { runtime: { request } as unknown as DotCMSRuntime, calls };
}

/** The binary envelope `downloadAssetBytes` expects back from an asset read. */
function binary(text: string) {
    const base64 = Buffer.from(text, 'utf8').toString('base64');

    return {
        __dotcmsBinary: true as const,
        contentType: 'text/css',
        base64,
        byteLength: Buffer.byteLength(text)
    };
}

/** Count how many requests hit a given path. */
function callsTo(calls: RequestOptions[], path: string): number {
    return calls.filter((call) => call.path === path).length;
}

describe('uploadAssets', () => {
    let src: string;

    beforeEach(async () => {
        src = await mkdtemp(join(tmpdir(), 'dot-upload-'));
        await writeFile(join(src, 'style.css'), '.a{color:red}');
        await writeFile(join(src, 'main.vtl'), '#set($x = 1)');
    });

    afterEach(async () => {
        await rm(src, { recursive: true, force: true });
    });

    it('uploads every file and reports them in the manifest', async () => {
        const { runtime, calls } = fakeRuntime();

        const manifest = await uploadAssets({
            dotcms: runtime,
            src,
            dest: SITE,
            publish: true,
            verify: false
        });

        expect(manifest.count).toBe(2);
        expect(manifest.failures).toEqual([]);
        expect(manifest.files.map((file) => file.path).sort()).toEqual(['main.vtl', 'style.css']);
        expect(callsTo(calls, '/api/v2/assets/publish')).toBe(2);
    });

    it('records a per-file failure without abandoning the rest of the batch', async () => {
        let seen = 0;
        const { runtime } = fakeRuntime({
            onRequest: (opts) => {
                if (opts.path === '/api/v2/assets/publish') {
                    seen += 1;
                    if (seen === 1) {
                        throw new Error('HTTP 400 Bad Request');
                    }
                }

                return undefined;
            }
        });

        const manifest = await uploadAssets({
            dotcms: runtime,
            src,
            dest: SITE,
            publish: true,
            verify: false
        });

        expect(manifest.failures).toHaveLength(1);
        expect(manifest.count).toBe(1);
    });

    it('rejects a destination that is not host-qualified', async () => {
        const { runtime } = fakeRuntime();

        await expect(
            uploadAssets({
                dotcms: runtime,
                src,
                dest: '/application/themes/travel',
                publish: true,
                verify: false
            })
        ).rejects.toThrow(/host-qualified/i);
    });

    it('distinguishes a bad include pattern from an empty source dir', async () => {
        const { runtime } = fakeRuntime();

        const manifest = await uploadAssets({
            dotcms: runtime,
            src,
            dest: SITE,
            include: '*.png',
            publish: true,
            verify: false
        });

        expect(manifest.count).toBe(0);
        // "matched 0 of 2" rather than "no files found" — a mistyped glob must not read as
        // silent success in an unattended run.
        expect(manifest.warnings.join(' ')).toMatch(/matched 0 of 2/);
    });

    describe('publish + verify', () => {
        it('does not let a failed liveness read destroy the report of completed writes', async () => {
            // The headline case. Every file uploaded and published; one liveness GET then
            // fails. Before this was guarded the throw escaped uploadAssets entirely and the
            // caller was told the operation failed — so its next move was to re-upload
            // everything that had in fact already landed.
            const { runtime } = fakeRuntime({
                onRequest: (opts) => {
                    if (opts.path?.startsWith('/api/v1/content/')) {
                        throw new Error('HTTP 500 Server Error');
                    }

                    return undefined;
                }
            });

            const manifest = await uploadAssets({
                dotcms: runtime,
                src,
                dest: SITE,
                publish: true,
                verify: true
            });

            expect(manifest.count).toBe(2);
            expect(manifest.files).toHaveLength(2);
            expect(manifest.warnings.join(' ')).toMatch(/[Cc]ould not check/);
            // An unreadable status is NOT a confirmed failure, so it must not be reported as
            // not-live.
            expect(manifest.notLive).toEqual([]);
        });

        it('reports files whose identifier never parsed instead of silently skipping them', async () => {
            // With an unexpected publish envelope every identifier is undefined. Filtering
            // them out silently produced "2 files, 0 failures, 0 notLive" — indistinguishable
            // from a fully verified publish when nothing at all was verified.
            const { runtime, calls } = fakeRuntime({
                onRequest: (opts) =>
                    opts.path === '/api/v2/assets/publish' ? { entity: {} } : undefined
            });

            const manifest = await uploadAssets({
                dotcms: runtime,
                src,
                dest: SITE,
                publish: true,
                verify: true
            });

            expect(manifest.count).toBe(2);
            expect(manifest.warnings.join(' ')).toMatch(/could NOT be verified/i);
            // Nothing was checkable, so no liveness read should have been attempted.
            expect(calls.filter((call) => call.path?.startsWith('/api/v1/content/'))).toHaveLength(
                0
            );
        });

        it('re-fires PUBLISH for a file that is not live, then re-checks it', async () => {
            let liveChecks = 0;
            const { runtime, calls } = fakeRuntime({
                onRequest: (opts) => {
                    if (opts.path?.startsWith('/api/v1/content/')) {
                        liveChecks += 1;

                        // Not live on the first pass, live once re-published.
                        return { entity: { live: liveChecks > 2 } };
                    }

                    return undefined;
                }
            });

            const manifest = await uploadAssets({
                dotcms: runtime,
                src,
                dest: SITE,
                publish: true,
                verify: true
            });

            expect(callsTo(calls, '/api/v1/workflow/actions/default/fire/PUBLISH')).toBe(2);
            expect(manifest.notLive).toEqual([]);
        });

        it('keeps going when one re-publish fails, and says which one', async () => {
            let fires = 0;
            const { runtime } = fakeRuntime({
                onRequest: (opts) => {
                    if (opts.path?.startsWith('/api/v1/content/')) {
                        return { entity: { live: false } };
                    }
                    if (opts.path === '/api/v1/workflow/actions/default/fire/PUBLISH') {
                        fires += 1;
                        if (fires === 1) {
                            throw new Error('HTTP 400 locked by another workflow');
                        }
                    }

                    return undefined;
                }
            });

            const manifest = await uploadAssets({
                dotcms: runtime,
                src,
                dest: SITE,
                publish: true,
                verify: true
            });

            // The first fire failed; the rest were still attempted rather than abandoned.
            expect(fires).toBeGreaterThan(1);
            expect(manifest.warnings.join(' ')).toMatch(/Re-publish failed/);
            expect(manifest.notLive).toHaveLength(2);
        });

        it('skips verification entirely when publish is off', async () => {
            const { runtime, calls } = fakeRuntime();

            await uploadAssets({
                dotcms: runtime,
                src,
                dest: SITE,
                publish: false,
                verify: true
            });

            expect(callsTo(calls, '/api/v2/assets/save')).toBe(2);
            expect(calls.filter((call) => call.path?.startsWith('/api/v1/content/'))).toHaveLength(
                0
            );
        });
    });

    it('warns when a 0-byte file had to be uploaded as a newline', async () => {
        // The remote asset then DIFFERS from the source, which is invisible to the caller
        // unless it is said out loud.
        await writeFile(join(src, 'empty.vtl'), '');
        const { runtime } = fakeRuntime({
            onRequest: (opts) => {
                if (opts.path === '/api/v2/assets/publish') {
                    const data = (opts.formData as { file?: { data?: string } })?.file?.data;
                    if (data === '') {
                        throw new Error('HTTP 400 empty body rejected');
                    }
                }

                return undefined;
            }
        });

        const manifest = await uploadAssets({
            dotcms: runtime,
            src,
            dest: SITE,
            publish: true,
            verify: false
        });

        const empty = manifest.files.find((file) => file.path === 'empty.vtl');
        expect(manifest.warnings.join(' ')).toMatch(/0 bytes[\s\S]*single newline/);
        expect(empty?.bytes).toBe(1);
    });
});

describe('downloadAssets', () => {
    let dest: string;

    beforeEach(async () => {
        dest = await mkdtemp(join(tmpdir(), 'dot-download-'));
    });

    afterEach(async () => {
        await rm(dest, { recursive: true, force: true });
    });

    /** A `_search` page followed by the per-asset byte reads. */
    function searchRuntime(assets: Array<{ identifier: string; path: string }>, bytes = 'body') {
        return fakeRuntime({
            onRequest: (opts) => {
                if (opts.path === '/api/content/_search') {
                    return { entity: { jsonObjectView: { contentlets: assets } } };
                }
                if (opts.path?.startsWith('/api/v2/assets/')) {
                    return binary(bytes);
                }

                return undefined;
            }
        });
    }

    it('writes each enumerated asset to disk and reports it', async () => {
        const { runtime } = searchRuntime([
            { identifier: 'a1', path: '//demo.dotcms.com/application/themes/travel/style.css' }
        ]);

        const manifest = await downloadAssets({
            dotcms: runtime,
            path: '//demo.dotcms.com/application/themes/travel',
            dest,
            recursive: true,
            overwrite: 'overwrite'
        });

        expect(manifest.count).toBe(1);
        expect(await readFile(join(dest, 'style.css'), 'utf8')).toBe('body');
    });

    it('explains a zero-match instead of reporting an empty success', async () => {
        const { runtime } = searchRuntime([]);

        const manifest = await downloadAssets({
            dotcms: runtime,
            path: '//demo.dotcms.com/application/themes/nope',
            dest,
            recursive: true,
            overwrite: 'overwrite'
        });

        expect(manifest.count).toBe(0);
        expect(manifest.warnings.length).toBeGreaterThan(0);
    });

    it('records a per-asset failure rather than aborting the batch', async () => {
        const { runtime } = fakeRuntime({
            onRequest: (opts) => {
                if (opts.path === '/api/content/_search') {
                    return {
                        entity: {
                            jsonObjectView: {
                                contentlets: [
                                    { identifier: 'a1', path: '//demo.dotcms.com/a/one.css' },
                                    { identifier: 'a2', path: '//demo.dotcms.com/a/two.css' }
                                ]
                            }
                        }
                    };
                }
                if (opts.path === '/api/v2/assets/a1') {
                    throw new Error('HTTP 404 Not Found');
                }
                if (opts.path?.startsWith('/api/v2/assets/')) {
                    return binary('ok');
                }

                return undefined;
            }
        });

        const manifest = await downloadAssets({
            dotcms: runtime,
            path: '//demo.dotcms.com/a',
            dest,
            recursive: true,
            overwrite: 'overwrite'
        });

        expect(manifest.failures).toHaveLength(1);
        expect(manifest.count).toBe(1);
    });

    it('stops paginating when a page adds nothing new', async () => {
        // The termination guard. If the backend ignores `offset` every page comes back full
        // of the same identifiers: the short-page exit never fires, `seen` de-dupes so the
        // result stops growing, and the loop would spin forever issuing identical POSTs.
        const page = Array.from({ length: 500 }, (_, i) => ({
            identifier: `id-${i}`,
            path: `//demo.dotcms.com/a/file-${i}.css`
        }));
        const { runtime, calls } = fakeRuntime({
            onRequest: (opts) => {
                if (opts.path === '/api/content/_search') {
                    // Always the SAME page, regardless of offset.
                    return { entity: { jsonObjectView: { contentlets: page } } };
                }
                if (opts.path?.startsWith('/api/v2/assets/')) {
                    return binary('x');
                }

                return undefined;
            }
        });

        const manifest = await downloadAssets({
            dotcms: runtime,
            path: '//demo.dotcms.com/a',
            dest,
            recursive: true,
            overwrite: 'overwrite'
        });

        // Two searches: the first yields 500 new ids, the second adds none and breaks.
        expect(callsTo(calls, '/api/content/_search')).toBe(2);
        expect(manifest.count).toBe(500);
    });
});
