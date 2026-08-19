import { createHttpFactory, HttpMethod, mockProvider, SpectatorHttp } from '@openng/spectator/jest';

import { HttpErrorResponse, HttpRequest } from '@angular/common/http';

import {
    DotPageBrowserContentlet,
    DotPageBrowserPage,
    DotPageBrowserState,
    DotPageLockInfo
} from './dot-pages-browser.models';
import { DotPagesBrowserService } from './dot-pages-browser.service';

import { DotFolderService } from '../dot-folder/dot-folder.service';

const PAGE_SEARCH_URL = '/api/v1/page/search';
const ES_SEARCH_URL = '/api/es/search';

const HOSTNAME = 'demo.dotcms.com';
/** Identifiers are UUIDs in dotCMS; the query guard depends on that shape. */
const PAGE_ID = '2e2e5f6a-1e17-4b21-9c1a-7d3f5b90ac41';

const contentlet = (
    overrides: Partial<DotPageBrowserContentlet> = {}
): DotPageBrowserContentlet => ({
    identifier: PAGE_ID,
    inode: 'page-inode-1',
    title: 'About Us',
    url: '/index',
    path: '/about-us/index',
    host: 'host-1',
    hostName: HOSTNAME,
    template: 'template-1',
    modDate: '2026-01-01 00:00:00.0',
    languageId: 1,
    ...overrides
});

describe('DotPagesBrowserService', () => {
    let spectator: SpectatorHttp<DotPagesBrowserService>;

    const createHttp = createHttpFactory({
        service: DotPagesBrowserService,
        providers: [mockProvider(DotFolderService)]
    });

    /** The one page-search request in flight, so its params can be asserted directly. */
    const expectPageSearch = () =>
        spectator.controller.expectOne(
            (request: HttpRequest<unknown>) =>
                request.method === HttpMethod.GET && request.url === PAGE_SEARCH_URL
        );

    const searchPagesReturning = (contentlets: DotPageBrowserContentlet[]) => {
        let pages: DotPageBrowserPage[] = [];
        spectator.service.searchPages({ hostname: HOSTNAME }).subscribe((result) => {
            pages = result;
        });
        expectPageSearch().flush({ entity: contentlets });

        return () => pages;
    };

    beforeEach(() => {
        spectator = createHttp();
    });

    afterEach(() => spectator.controller.verify());

    describe('searchPages', () => {
        it('should scope the search to the site through the //hostname prefix', () => {
            // The endpoint has no `hostId` parameter — the site is expressed in the path.
            spectator.service.searchPages({ hostname: HOSTNAME, path: '/about-us/' }).subscribe();

            const request = expectPageSearch();

            expect(request.request.params.get('path')).toBe(`//${HOSTNAME}/about-us/`);
            request.flush({ entity: [] });
        });

        it('should default to the site root, drafts included and every site searched', () => {
            spectator.service.searchPages().subscribe();

            const request = expectPageSearch();

            expect(request.request.params.get('path')).toBe('/');
            expect(request.request.params.get('live')).toBe('false');
            expect(request.request.params.get('onlyLiveSites')).toBe('false');
            request.flush({ entity: [] });
        });

        it('should pass the version filters through when asked for', () => {
            spectator.service.searchPages({ live: true, onlyLiveSites: true }).subscribe();

            const request = expectPageSearch();

            expect(request.request.params.get('live')).toBe('true');
            expect(request.request.params.get('onlyLiveSites')).toBe('true');
            request.flush({ entity: [] });
        });

        it('should prefix a path that does not start with a slash', () => {
            spectator.service.searchPages({ path: 'about-us/' }).subscribe();

            const request = expectPageSearch();

            expect(request.request.params.get('path')).toBe('/about-us/');
            request.flush({ entity: [] });
        });

        it('should return an empty list when the response carries no entity', () => {
            let pages: DotPageBrowserPage[] = [];
            spectator.service.searchPages().subscribe((result) => (pages = result));

            expectPageSearch().flush({});

            expect(pages).toEqual([]);
        });

        it('should fall back to the url when the page has no title', () => {
            const pages = searchPagesReturning([contentlet({ title: '', url: '/index' })]);

            expect(pages()[0].title).toBe('/index');
        });

        describe('query filter', () => {
            // `GET /api/v1/page/search` has no free-text parameter, so the term is applied to
            // the rows it returns rather than sent along.
            const CONTENTLETS = [
                contentlet({ identifier: 'page-1', title: 'About Us', url: '/about' }),
                contentlet({ identifier: 'page-2', title: 'Contact', url: '/contact-us' })
            ];

            const searchWith = (query: string): DotPageBrowserPage[] => {
                let pages: DotPageBrowserPage[] = [];
                spectator.service.searchPages({ query }).subscribe((result) => (pages = result));
                const request = expectPageSearch();

                expect(request.request.params.has('query')).toBe(false);
                request.flush({ entity: CONTENTLETS });

                return pages;
            };

            it('should match the title case-insensitively', () => {
                expect(searchWith('about us').map(({ identifier }) => identifier)).toEqual([
                    'page-1'
                ]);
            });

            it('should match the url as well as the title', () => {
                expect(searchWith('contact-us').map(({ identifier }) => identifier)).toEqual([
                    'page-2'
                ]);
            });

            it('should keep every row when the term is only whitespace', () => {
                expect(searchWith('   ')).toHaveLength(CONTENTLETS.length);
            });

            it('should return nothing when the term matches no row', () => {
                expect(searchWith('nothing-matches-this')).toEqual([]);
            });
        });

        describe('publication state', () => {
            it.each([
                [
                    'archived, whatever the other flags say',
                    { archived: true, live: true, working: true },
                    DotPageBrowserState.ARCHIVED
                ],
                [
                    'published with no pending changes',
                    { live: true, working: true },
                    DotPageBrowserState.PUBLISHED
                ],
                [
                    'live with newer working changes',
                    { live: true, working: false },
                    DotPageBrowserState.CHANGED
                ],
                [
                    'working with a live version behind it',
                    { live: false, hasLiveVersion: true },
                    DotPageBrowserState.CHANGED
                ],
                [
                    'never published',
                    { live: false, hasLiveVersion: false },
                    DotPageBrowserState.DRAFT
                ]
            ])('should read %s as %s', (_name, flags, expectedState) => {
                const pages = searchPagesReturning([contentlet(flags)]);

                expect(pages()[0].state).toBe(expectedState);
            });
        });
    });

    describe('getPageLockState', () => {
        const lockStateOf = (contentlets: DotPageBrowserContentlet[]) => {
            let lockInfo: DotPageLockInfo | null = null;
            spectator.service.getPageLockState(PAGE_ID).subscribe((result) => (lockInfo = result));
            spectator.expectOne(ES_SEARCH_URL, HttpMethod.POST).flush({ contentlets });

            return lockInfo as unknown as DotPageLockInfo;
        };

        it('should ask for the single page by identifier, not by a prefix match', () => {
            // Without `+identifier:` being an exact term the search would return every page
            // whose identifier merely starts with this one.
            spectator.service.getPageLockState(PAGE_ID).subscribe();

            const request = spectator.expectOne(ES_SEARCH_URL, HttpMethod.POST);
            const body = request.request.body as {
                query: { query_string: { query: string } };
                size: number;
            };

            expect(body.query.query_string.query).toBe(`+basetype:5 +identifier:${PAGE_ID}`);
            expect(body.size).toBe(1);
            request.flush({ contentlets: [] });
        });

        it('should report an unlocked page when nothing matches the identifier', () => {
            expect(lockStateOf([])).toEqual({ locked: false });
        });

        /**
         * The endpoint takes a Lucene string, so an identifier carrying operators would widen the
         * search and answer with another contentlet's lock state. Nothing outside the identifier
         * shape can name a page, so it never reaches the wire.
         */
        it('should not query at all for a value that is not shaped like an identifier', () => {
            let lockInfo: DotPageLockInfo | null = null;

            spectator.service
                .getPageLockState(`${PAGE_ID} OR +contentType:Host`)
                .subscribe((result) => (lockInfo = result));

            spectator.controller.expectNone(ES_SEARCH_URL);
            expect(lockInfo).toEqual({ locked: false });
        });

        it('should report an unlocked page when the lock holder is absent', () => {
            expect(lockStateOf([contentlet({ locked: false })])).toEqual({ locked: false });
        });

        it('should read the plain user id pages are serialized with', () => {
            expect(
                lockStateOf([
                    contentlet({ locked: true, lockedBy: 'dotcms.org.1', lockedByName: 'Admin' })
                ])
            ).toEqual({ locked: true, lockedBy: 'dotcms.org.1', lockedByName: 'Admin' });
        });

        it('should read the object shape the default contentlet strategy writes', () => {
            expect(
                lockStateOf([
                    contentlet({
                        locked: true,
                        lockedBy: { userId: 'dotcms.org.1', firstName: 'Admin' }
                    })
                ])
            ).toEqual({ locked: true, lockedBy: 'dotcms.org.1' });
        });

        it('should not claim a lock on an empty user id', () => {
            expect(lockStateOf([contentlet({ locked: true, lockedBy: '' })])).toEqual({
                locked: false
            });
        });
    });

    /**
     * Nothing here catches: a picker that cannot load its list has to say so, and only the caller
     * knows whether that is an empty state, a toast or a retry. An error swallowed into an empty
     * list would read as "no pages here", which is a different fact.
     */
    describe('backend failures', () => {
        const SERVER_ERROR = { status: 500, statusText: 'Server Error' };

        it('should surface a failed page search', () => {
            let emitted = false;
            let status: number | undefined;
            spectator.service.searchPages({ hostname: HOSTNAME }).subscribe({
                next: () => (emitted = true),
                error: (error: HttpErrorResponse) => (status = error.status)
            });

            expectPageSearch().flush('Boom', SERVER_ERROR);

            expect(status).toBe(500);
            expect(emitted).toBe(false);
        });

        it('should surface a failed lock lookup', () => {
            let emitted = false;
            let status: number | undefined;
            spectator.service.getPageLockState(PAGE_ID).subscribe({
                next: () => (emitted = true),
                error: (error: HttpErrorResponse) => (status = error.status)
            });

            spectator.expectOne(ES_SEARCH_URL, HttpMethod.POST).flush('Boom', SERVER_ERROR);

            expect(status).toBe(500);
            expect(emitted).toBe(false);
        });
    });
});
