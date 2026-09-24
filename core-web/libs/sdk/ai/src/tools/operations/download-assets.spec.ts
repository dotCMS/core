import { vi } from 'vitest';

import { mkdir, mkdtemp, readdir, readFile, rm, stat, symlink, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { DOWNLOAD_ASSETS_ENDPOINTS, downloadAssets } from './download-assets';

import { ValidationError, type DotCMSRuntime, type RequestOptions } from '../../runtime';
import { unlistedCalls } from '../toolkit/endpoints';

/** Every request the fake sees, checked against the download_assets tool's endpoints. */
const seen: RequestOptions[] = [];

/**
 * Exercises `downloadAssets` end to end against a fake runtime and a real temp directory: the
 * enumeration, the per-asset failure isolation, pagination, and the local-root boundary. (The
 * glob filter itself is covered by `shared/glob.spec.ts`.)
 *
 * A real temp dir rather than a mocked `fs`: this function creates directories and writes
 * files, and mocking that surface would mostly test the mock.
 */

interface FakeOptions {
    /** Per-path handler. Return a value, or throw to simulate a failure. */
    onRequest?: (options: RequestOptions) => unknown;
}

/** An asset body the fake serves as a stream — and only to a request that asked to stream. */
interface FakeBody {
    fakeBody: () => ReadableStream<Uint8Array>;
}

function isFakeBody(value: unknown): value is FakeBody {
    return typeof (value as FakeBody | undefined)?.fakeBody === 'function';
}

function fakeRuntime(options?: FakeOptions) {
    const calls: RequestOptions[] = [];

    const request = vi.fn(async (opts: RequestOptions) => {
        seen.push(opts);
        calls.push(opts);

        const out = options?.onRequest?.(opts);
        if (isFakeBody(out)) {
            // The real core hands a successful body to `onBody`. An asset read without one
            // would be buffering the whole file, which is exactly what this operation must not do.
            if (!opts.onBody) {
                throw new Error('asset bytes must be streamed through onBody, not buffered');
            }
            return opts.onBody(out.fakeBody(), { contentType: 'text/css' });
        }

        return out ?? {};
    });

    return { runtime: { request } as unknown as DotCMSRuntime, calls };
}

/** An asset read that streams `text`. */
function binary(text: string): FakeBody {
    return {
        fakeBody: () =>
            new ReadableStream({
                start(controller) {
                    controller.enqueue(new TextEncoder().encode(text));
                    controller.close();
                }
            })
    };
}

/** An asset read that streams `first`, then fails — a connection dropped mid-transfer. */
function brokenBody(first: string): FakeBody {
    return {
        fakeBody: () => {
            let sent = false;

            return new ReadableStream({
                pull(controller) {
                    if (!sent) {
                        sent = true;
                        controller.enqueue(new TextEncoder().encode(first));
                        return;
                    }
                    controller.error(new Error('socket hang up'));
                }
            });
        }
    };
}

/** Count how many requests hit a given path. */
function callsTo(calls: RequestOptions[], path: string): number {
    return calls.filter((call) => call.path === path).length;
}

describe('downloadAssets', () => {
    let dest: string;

    // Everything a download requests must be an endpoint the download_assets tool owns.
    afterEach(() => {
        expect(unlistedCalls(DOWNLOAD_ASSETS_ENDPOINTS, seen.splice(0))).toEqual([]);
    });

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

    it('skips an existing file without downloading its bytes', async () => {
        await writeFile(join(dest, 'style.css'), 'local');
        const { runtime, calls } = searchRuntime([
            { identifier: 'a1', path: '//demo.dotcms.com/application/themes/travel/style.css' }
        ]);

        const manifest = await downloadAssets({
            dotcms: runtime,
            path: '//demo.dotcms.com/application/themes/travel',
            dest,
            recursive: true,
            overwrite: 'skip'
        });

        expect(manifest.skipped).toEqual([{ path: 'style.css', reason: 'exists' }]);
        expect(callsTo(calls, '/api/v2/assets/a1')).toBe(0);
        expect(await readFile(join(dest, 'style.css'), 'utf8')).toBe('local');
    });

    it('leaves no partial file when a transfer breaks, and keeps the file it would replace', async () => {
        // The bytes land in a temporary file that is renamed into place only once complete.
        // Written in place, a dropped connection would truncate the existing file to whatever
        // had arrived — the one outcome worse than not downloading at all.
        await writeFile(join(dest, 'style.css'), 'the good copy');
        const { runtime } = fakeRuntime({
            onRequest: (opts) => {
                if (opts.path === '/api/content/_search') {
                    return {
                        entity: {
                            jsonObjectView: {
                                contentlets: [
                                    {
                                        identifier: 'a1',
                                        path: '//demo.dotcms.com/application/style.css'
                                    }
                                ]
                            }
                        }
                    };
                }
                if (opts.path?.startsWith('/api/v2/assets/')) {
                    return brokenBody('.half{');
                }

                return undefined;
            }
        });

        const manifest = await downloadAssets({
            dotcms: runtime,
            path: '//demo.dotcms.com/application',
            dest,
            recursive: true,
            overwrite: 'overwrite'
        });

        expect(manifest.failures).toEqual([
            { path: 'style.css', error: expect.stringContaining('socket hang up') }
        ]);
        expect(await readFile(join(dest, 'style.css'), 'utf8')).toBe('the good copy');
        expect(await readdir(dest)).toEqual(['style.css']);
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
        // Stopping here never reached the end of the folder, so it must not read as complete.
        expect(manifest.warnings.join(' ')).toMatch(/INCOMPLETE/);
    });

    describe('with a filter', () => {
        // The filters run on our side, after the search. A page can be full of assets that the
        // search returned and none of them selected — that page still moved the walk forward.

        /** A `_search` backend that honours `offset`, serving `pages` in order. */
        function pagedRuntime(pages: Array<Array<{ identifier: string; path: string }>>) {
            return fakeRuntime({
                onRequest: (opts) => {
                    if (opts.path === '/api/content/_search') {
                        const offset = (opts.body as { offset: number }).offset;
                        const contentlets = pages[offset / 500] ?? [];

                        return { entity: { jsonObjectView: { contentlets } } };
                    }
                    if (opts.path?.startsWith('/api/v2/assets/')) {
                        return binary('x');
                    }

                    return undefined;
                }
            });
        }

        /** A full page of `.css` assets at `//demo.dotcms.com/a/<dir>`, ids unique per `tag`. */
        function cssPage(tag: string, dir = '') {
            return Array.from({ length: 500 }, (_, i) => ({
                identifier: `${tag}-${i}`,
                path: `//demo.dotcms.com/a/${dir}${tag}-${i}.css`
            }));
        }

        it('keeps paginating past a full page where nothing matched `include`', async () => {
            const { runtime, calls } = pagedRuntime([
                cssPage('p1'),
                [{ identifier: 'vtl-1', path: '//demo.dotcms.com/a/main.vtl' }]
            ]);

            const manifest = await downloadAssets({
                dotcms: runtime,
                path: '//demo.dotcms.com/a',
                dest,
                recursive: true,
                overwrite: 'overwrite',
                include: '*.vtl'
            });

            expect(callsTo(calls, '/api/content/_search')).toBe(2);
            expect(manifest.files.map((file) => file.path)).toEqual(['main.vtl']);
            expect(manifest.warnings).toEqual([]);
        });

        it('keeps paginating past a full page of nested assets when not recursive', async () => {
            const { runtime } = pagedRuntime([
                cssPage('p1', 'nested/'),
                [{ identifier: 'top-1', path: '//demo.dotcms.com/a/top.css' }]
            ]);

            const manifest = await downloadAssets({
                dotcms: runtime,
                path: '//demo.dotcms.com/a',
                dest,
                recursive: false,
                overwrite: 'overwrite'
            });

            expect(manifest.files.map((file) => file.path)).toEqual(['top.css']);
        });

        it('reports a walk that ran out of pages as incomplete, not as a clean result', async () => {
            // Every page full and unique: the scan cap stops the walk with results left over.
            const pages = Array.from({ length: 20 }, (_, n) => cssPage(`p${n}`));
            const { runtime, calls } = pagedRuntime(pages);

            const manifest = await downloadAssets({
                dotcms: runtime,
                path: '//demo.dotcms.com/a',
                dest,
                recursive: true,
                overwrite: 'overwrite',
                include: 'p1-0.css'
            });

            expect(callsTo(calls, '/api/content/_search')).toBe(10);
            expect(manifest.count).toBe(1);
            expect(manifest.warnings.join(' ')).toMatch(/INCOMPLETE/);
        });

        it('does not blame the path when the cap stopped the walk before any match', async () => {
            const pages = Array.from({ length: 20 }, (_, n) => cssPage(`p${n}`));
            const { runtime } = pagedRuntime(pages);

            const manifest = await downloadAssets({
                dotcms: runtime,
                path: '//demo.dotcms.com/a',
                dest,
                recursive: true,
                overwrite: 'overwrite',
                include: '*.vtl'
            });

            const warnings = manifest.warnings.join(' ');
            expect(warnings).toMatch(/INCOMPLETE/);
            // "No assets matched — check the path" would send the caller after a path that is fine.
            expect(warnings).not.toMatch(/check the path/);
        });
    });

    describe('with a root', () => {
        // `dest` comes from the model, and so do the asset contents. The root is what stops a
        // hosted server's download_assets from planting files in any directory it can write.
        let root: string;
        const THEME = '//demo.dotcms.com/application/themes/travel';
        const ONE_ASSET = [{ identifier: 'a1', path: `${THEME}/style.css` }];

        beforeEach(async () => {
            root = await mkdtemp(join(tmpdir(), 'dot-root-'));
        });

        afterEach(async () => {
            await rm(root, { recursive: true, force: true });
        });

        it('downloads into a directory inside the root, creating it', async () => {
            const { runtime } = searchRuntime(ONE_ASSET);
            const inside = join(root, 'new', 'theme');

            const manifest = await downloadAssets({
                dotcms: runtime,
                root,
                path: THEME,
                dest: inside,
                recursive: true,
                overwrite: 'overwrite'
            });

            expect(manifest.count).toBe(1);
            expect(await readFile(join(inside, 'style.css'), 'utf8')).toBe('body');
        });

        it('refuses a dest outside the root, and creates nothing there', async () => {
            const { runtime, calls } = searchRuntime(ONE_ASSET);
            const outside = join(dest, 'would-be-created'); // `dest` is a sibling temp dir

            const error = await downloadAssets({
                dotcms: runtime,
                root,
                path: THEME,
                dest: outside,
                recursive: true,
                overwrite: 'overwrite'
            }).catch((e: unknown) => e);

            expect(error).toBeInstanceOf(ValidationError);
            expect((error as Error).message).toContain('`dest` must be inside');
            await expect(stat(outside)).rejects.toThrow();
            expect(calls).toEqual([]);
        });

        it('refuses a dest reached through a symlinked directory, before creating anything', async () => {
            // root/escape -> a directory outside the root. `root/escape/new` looks inside as a
            // string; mkdir would have created `new` OUTSIDE.
            await symlink(dest, join(root, 'escape'));
            const { runtime } = searchRuntime(ONE_ASSET);

            const error = await downloadAssets({
                dotcms: runtime,
                root,
                path: THEME,
                dest: join(root, 'escape', 'new'),
                recursive: true,
                overwrite: 'overwrite'
            }).catch((e: unknown) => e);

            expect(error).toBeInstanceOf(ValidationError);
            await expect(stat(join(dest, 'new'))).rejects.toThrow();
        });

        it('refuses to write through a symlink planted inside dest', async () => {
            // An existing root/theme/style.css -> a file outside. writeFile follows the link, so
            // the download would have overwritten the outside file.
            const theme = join(root, 'theme');
            await mkdir(theme);
            const victim = join(dest, 'victim.txt');
            await writeFile(victim, 'untouched');
            await symlink(victim, join(theme, 'style.css'));
            const { runtime } = searchRuntime(ONE_ASSET);

            const manifest = await downloadAssets({
                dotcms: runtime,
                root,
                path: THEME,
                dest: theme,
                recursive: true,
                overwrite: 'overwrite'
            });

            expect(manifest.count).toBe(0);
            expect(manifest.failures).toEqual([
                { path: 'style.css', error: expect.stringContaining('symlink') }
            ]);
            expect(await readFile(victim, 'utf8')).toBe('untouched');
        });
    });
});
