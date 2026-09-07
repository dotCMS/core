import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotPageSourcesService } from './dot-page-sources.service';

import { PageRenderSourcesView, PageSourceFile } from '../models/page-render-sources.models';

/** A `_render-sources` response with a theme CSS, a container VTL, and a widget VTL. */
const RENDER_SOURCES: PageRenderSourcesView = {
    page: { identifier: 'page-1', uri: '//demo/index', languageId: 1 },
    theme: {
        id: 'theme-1',
        name: 'awazon',
        folderPath: '//demo/application/themes/awazon/',
        files: [
            {
                path: '//demo/application/themes/awazon/css/awazon.css',
                identifier: 'css-1',
                extension: 'css'
            },
            {
                path: '//demo/application/themes/awazon/header.vtl',
                identifier: 'vtl-theme-1',
                extension: 'vtl'
            }
        ]
    },
    containers: {
        '//demo/application/containers/awazon-content/': {
            source: 'FILE',
            contentTypes: [
                {
                    contentTypeVar: 'AwazonBookList',
                    path: '//demo/application/containers/awazon-content/AwazonBookList.vtl',
                    identifier: 'vtl-container-1'
                },
                // A DB content type — no file, must be dropped.
                { contentTypeVar: 'AwazonNoFile' }
            ]
        }
    },
    widgets: [
        {
            contentTypeVar: 'AwazonNewsletter',
            title: 'Newsletter',
            contentletId: 'w-1',
            contentletInode: 'w-inode-1',
            source: 'FILE',
            path: '//demo/application/containers/awazon-content/AwazonNewsletter.vtl',
            identifier: 'vtl-widget-1'
        },
        // A CODE widget — no file, must be dropped.
        {
            contentTypeVar: 'AwazonInline',
            title: 'Inline',
            contentletId: 'w-2',
            contentletInode: 'w-inode-2',
            source: 'CODE'
        }
    ]
};

/** Build a versions response grouped by ISO code (as the backend returns it). */
function versionsResponse(
    rows: Array<{ inode: string; working?: boolean; live?: boolean; languageId?: number }>
) {
    return {
        entity: {
            versions: {
                'en-us': rows.map((r) => ({
                    inode: r.inode,
                    identifier: 'ignored',
                    working: !!r.working,
                    live: !!r.live,
                    languageId: r.languageId ?? 1,
                    fileAssetVersion: `/dA/${r.inode}/fileAsset/file.vtl`
                }))
            }
        }
    };
}

describe('DotPageSourcesService', () => {
    let spectator: SpectatorHttp<DotPageSourcesService>;

    const createHttp = createHttpFactory(DotPageSourcesService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('getPageSources', () => {
        it('flattens theme, FILE container, and FILE widget files (dropping DB/CODE)', () => {
            let result: PageSourceFile[] = [];
            spectator.service.getPageSources('/index', 'host-1', 1).subscribe((r) => (result = r));

            const req = spectator.expectOne(
                '/api/v1/page/_render-sources/index?host_id=host-1&language_id=1',
                HttpMethod.GET
            );
            req.flush({ entity: RENDER_SOURCES });

            // css-1 + vtl-theme-1 (theme) + vtl-container-1 (FILE container) + vtl-widget-1 (FILE widget)
            expect(result.map((f) => f.identifier).sort()).toEqual([
                'css-1',
                'vtl-container-1',
                'vtl-theme-1',
                'vtl-widget-1'
            ]);
            // The DB content type (AwazonNoFile) and CODE widget (AwazonInline) are dropped.
            expect(result.length).toBe(4);
        });

        it('derives name, extension, and origin per file', () => {
            let result: PageSourceFile[] = [];
            spectator.service.getPageSources('/index', 'host-1', 1).subscribe((r) => (result = r));
            spectator
                .expectOne(
                    '/api/v1/page/_render-sources/index?host_id=host-1&language_id=1',
                    HttpMethod.GET
                )
                .flush({ entity: RENDER_SOURCES });

            const css = result.find((f) => f.identifier === 'css-1');
            expect(css).toMatchObject({ name: 'awazon.css', extension: 'css', origin: 'theme' });

            const widget = result.find((f) => f.identifier === 'vtl-widget-1');
            expect(widget).toMatchObject({
                name: 'AwazonNewsletter.vtl',
                extension: 'vtl',
                origin: 'widget'
            });
        });

        it('strips a leading slash from the uri before the render-sources path', () => {
            spectator.service.getPageSources('/about/team', 'host-1', 2).subscribe();
            spectator.expectOne(
                '/api/v1/page/_render-sources/about/team?host_id=host-1&language_id=2',
                HttpMethod.GET
            );
        });
    });

    describe('getDiffFiles', () => {
        const files: PageSourceFile[] = [
            {
                identifier: 'vtl-1',
                path: '//demo/a.vtl',
                name: 'a.vtl',
                extension: 'vtl',
                origin: 'container'
            }
        ];

        it('returns only files whose working and live text differ, with line counts', () => {
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 1).subscribe((r) => (result = r));

            // versions lookup for the file
            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush(
                    versionsResponse([
                        { inode: 'working-inode', working: true },
                        { inode: 'live-inode', live: true }
                    ])
                );

            // working + live text fetches run concurrently (forkJoin) — grab both
            // via the backend matcher so neither auto-verify races the other.
            spectator.controller
                .match('/dA/working-inode/fileAsset/file.vtl')[0]
                .flush('line1\nline2\nnewline');
            spectator.controller
                .match('/dA/live-inode/fileAsset/file.vtl')[0]
                .flush('line1\nline2');

            expect(result).toEqual([
                expect.objectContaining({
                    identifier: 'vtl-1',
                    working: 'line1\nline2\nnewline',
                    live: 'line1\nline2',
                    added: 1,
                    removed: 0
                })
            ]);
        });

        it('drops files whose working and live text are identical', () => {
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 1).subscribe((r) => (result = r));

            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush(
                    versionsResponse([
                        { inode: 'working-inode', working: true },
                        { inode: 'live-inode', live: true }
                    ])
                );
            spectator.controller.match('/dA/working-inode/fileAsset/file.vtl')[0].flush('same');
            spectator.controller.match('/dA/live-inode/fileAsset/file.vtl')[0].flush('same');

            expect(result).toEqual([]);
        });

        it('treats a working-only file (no live version) as an all-added diff', () => {
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 1).subscribe((r) => (result = r));

            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush(versionsResponse([{ inode: 'working-inode', working: true }]));

            // Only the working text is fetched; live falls back to empty.
            spectator
                .expectOne('/dA/working-inode/fileAsset/file.vtl', HttpMethod.GET)
                .flush('a\nb');

            expect(result).toEqual([
                expect.objectContaining({ working: 'a\nb', live: '', added: 2, removed: 0 })
            ]);
        });

        it('filters versions to the requested languageId', () => {
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 2).subscribe((r) => (result = r));

            // Only a lang-1 working version exists; none for lang 2 → no working → dropped.
            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush(
                    versionsResponse([{ inode: 'working-inode', working: true, languageId: 1 }])
                );

            expect(result).toEqual([]);
        });

        it('skips a file whose versions request errors, without aborting', () => {
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 1).subscribe((r) => (result = r));

            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush('boom', { status: 500, statusText: 'Server Error' });

            expect(result).toEqual([]);
        });

        it('drops a file whose working text fails to load, rather than showing a deletion', () => {
            // A 502 on the working version used to map to `''`, which diffs as every line
            // removed — a whole-file deletion the agent never made, in the one panel whose
            // job is to be the trustworthy account of what changed before publish.
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 1).subscribe((r) => (result = r));

            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush(
                    versionsResponse([
                        { inode: 'working-inode', working: true },
                        { inode: 'live-inode', live: true }
                    ])
                );
            spectator.controller
                .match('/dA/working-inode/fileAsset/file.vtl')[0]
                .flush('nope', { status: 502, statusText: 'Bad Gateway' });
            spectator.controller
                .match('/dA/live-inode/fileAsset/file.vtl')[0]
                .flush('line one\nline two');

            expect(result).toEqual([]);
        });

        it('drops a file whose live text fails to load', () => {
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 1).subscribe((r) => (result = r));

            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush(
                    versionsResponse([
                        { inode: 'working-inode', working: true },
                        { inode: 'live-inode', live: true }
                    ])
                );
            spectator.controller.match('/dA/working-inode/fileAsset/file.vtl')[0].flush('new');
            spectator.controller
                .match('/dA/live-inode/fileAsset/file.vtl')[0]
                .flush('nope', { status: 500, statusText: 'Server Error' });

            expect(result).toEqual([]);
        });

        it('prefers assetVersion over fileAssetVersion, matching getFileVersion', () => {
            // The local copy had these the other way round while its docblock claimed to
            // mirror the util, so a contentlet carrying both made the diff viewer show a
            // different version of the file than every other admin surface.
            let result = null as unknown;
            spectator.service.getDiffFiles(files, 1).subscribe((r) => (result = r));

            spectator
                .expectOne(
                    '/api/v1/content/versions?identifier=vtl-1&groupByLang=1',
                    HttpMethod.GET
                )
                .flush({
                    entity: {
                        versions: {
                            'en-us': [
                                {
                                    inode: 'working-inode',
                                    working: true,
                                    live: false,
                                    languageId: 1,
                                    assetVersion: '/dA/canonical/fileAsset/file.vtl',
                                    fileAssetVersion: '/dA/other/fileAsset/file.vtl'
                                }
                            ]
                        }
                    }
                });

            spectator.expectOne('/dA/canonical/fileAsset/file.vtl', HttpMethod.GET).flush('a');
            spectator.controller.verify();
            expect(result).toEqual([expect.objectContaining({ working: 'a', live: '' })]);
        });

        it('returns an empty array for an empty file list without any HTTP', () => {
            let result = null as unknown;
            spectator.service.getDiffFiles([], 1).subscribe((r) => (result = r));
            expect(result).toEqual([]);
            spectator.controller.verify();
        });
    });
});
