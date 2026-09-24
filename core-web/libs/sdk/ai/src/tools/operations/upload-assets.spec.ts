import { vi } from 'vitest';

import { mkdir, mkdtemp, rm, symlink, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { UPLOAD_ASSETS_ENDPOINTS, uploadAssets } from './upload-assets';

import { ValidationError, type DotCMSRuntime, type RequestOptions } from '../../runtime';
import { unlistedCalls } from '../toolkit/endpoints';

/** Every request the fake sees, checked against the upload_assets tool's endpoints. */
const seen: RequestOptions[] = [];

/**
 * Exercises `uploadAssets` end to end against a fake runtime and a real temp directory: the
 * manifest, the per-file failure isolation, and the whole publish-then-verify path. (The glob
 * filter itself is covered by `shared/glob.spec.ts`.)
 *
 * A real temp dir rather than a mocked `fs`: this function walks directories and reads bytes,
 * and mocking that surface would mostly test the mock.
 */

const SITE = '//demo.dotcms.com/application/themes/travel';

interface FakeOptions {
    /** Per-path handler. Return a value, or throw to simulate a failure. */
    onRequest?: (options: RequestOptions) => unknown;
}

function fakeRuntime(options?: FakeOptions) {
    const calls: RequestOptions[] = [];
    let uploadCount = 0;

    const request = vi.fn(async (opts: RequestOptions) => {
        seen.push(opts);
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

/** Count how many requests hit a given path. */
function callsTo(calls: RequestOptions[], path: string): number {
    return calls.filter((call) => call.path === path).length;
}

describe('uploadAssets', () => {
    let src: string;

    // Everything an upload requests must be an endpoint the upload_assets tool owns.
    afterEach(() => {
        expect(unlistedCalls(UPLOAD_ASSETS_ENDPOINTS, seen.splice(0))).toEqual([]);
    });

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

    it('streams each file from disk as a Blob, never as base64 in memory', async () => {
        const { runtime, calls } = fakeRuntime();

        await uploadAssets({ dotcms: runtime, src, dest: SITE, publish: true, verify: false });

        const parts = calls
            .filter((call) => call.path === '/api/v2/assets/publish')
            .map((call) => call.formData?.['file'] as { name: string; data?: string; blob?: Blob });
        expect(parts.map((part) => part.data)).toEqual([undefined, undefined]);
        const sent = await Promise.all(
            parts.map(async (part) => [part.name, await part.blob?.text()])
        );
        expect(Object.fromEntries(sent)).toEqual({
            'main.vtl': '#set($x = 1)',
            'style.css': '.a{color:red}'
        });
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
                    const blob = (opts.formData as { file?: { blob?: Blob } })?.file?.blob;
                    if (blob?.size === 0) {
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

    describe('with a root', () => {
        // `src` comes from the model. The root is what stops a hosted server's upload_assets
        // from reading any directory the process can — and then serving it back through dotCMS.
        let root: string;

        beforeEach(async () => {
            root = await mkdtemp(join(tmpdir(), 'dot-root-'));
        });

        afterEach(async () => {
            await rm(root, { recursive: true, force: true });
        });

        it('uploads from a directory inside the root', async () => {
            const inside = join(root, 'theme');
            await mkdir(inside);
            await writeFile(join(inside, 'style.css'), '.a{}');
            const { runtime } = fakeRuntime();

            const manifest = await uploadAssets({
                dotcms: runtime,
                root,
                src: inside,
                dest: SITE,
                publish: true,
                verify: false
            });

            expect(manifest.count).toBe(1);
        });

        it('refuses a src outside the root before reading or requesting anything', async () => {
            const { runtime, calls } = fakeRuntime();

            const error = await uploadAssets({
                dotcms: runtime,
                root,
                src, // a sibling temp dir, outside the root
                dest: SITE,
                publish: true,
                verify: false
            }).catch((e: unknown) => e);

            expect(error).toBeInstanceOf(ValidationError);
            expect((error as Error).message).toContain('`src` must be inside');
            expect(calls).toEqual([]);
        });

        it('refuses a src that is a symlink inside the root pointing out of it', async () => {
            const link = join(root, 'looks-inside');
            await symlink(src, link);
            const { runtime, calls } = fakeRuntime();

            const error = await uploadAssets({
                dotcms: runtime,
                root,
                src: link,
                dest: SITE,
                publish: true,
                verify: false
            }).catch((e: unknown) => e);

            expect(error).toBeInstanceOf(ValidationError);
            expect(calls).toEqual([]);
        });
    });
});
