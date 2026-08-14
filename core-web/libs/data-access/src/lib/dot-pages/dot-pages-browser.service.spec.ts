import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse, HttpRequest } from '@angular/common/http';

import { DotPagination, FolderSearchView } from '@dotcms/dotcms-models';

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

const SITE_ID = 'site-1';
const HOSTNAME = 'demo.dotcms.com';
const PAGE_ID = 'page-1';

/** `GET /api/v1/folder/search` reports the *parent* path and the folder name separately. */
const folderView = (overrides: Partial<FolderSearchView> = {}): FolderSearchView => ({
    id: 'folder-1',
    inode: 'folder-inode-1',
    name: 'about-us',
    path: '/',
    addChildrenAllowed: true,
    hasChildren: false,
    ...overrides
});

const pagination = (overrides: Partial<DotPagination> = {}): DotPagination => ({
    currentPage: 1,
    perPage: 40,
    totalEntries: 1,
    ...overrides
});

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
    let folderService: SpyObject<DotFolderService>;

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
        folderService = spectator.inject(DotFolderService);
    });

    afterEach(() => spectator.controller.verify());

    describe('getFolderChildren', () => {
        /** `null` stands for a response that carries no pagination block at all. */
        const searchFoldersReturns = (
            folders: FolderSearchView[],
            paginationView: DotPagination | null = pagination()
        ) =>
            folderService.searchFolders.mockReturnValue(
                of({ folders, pagination: paginationView ?? undefined } as {
                    folders: FolderSearchView[];
                    pagination: DotPagination;
                })
            );

        it('should ask the folder search for the direct children of the given path', () => {
            searchFoldersReturns([]);

            spectator.service
                .getFolderChildren({
                    siteId: SITE_ID,
                    hostname: HOSTNAME,
                    path: '/about-us/',
                    page: 2,
                    perPage: 10
                })
                .subscribe();

            expect(folderService.searchFolders).toHaveBeenCalledWith({
                siteId: SITE_ID,
                path: '/about-us/',
                recursive: false,
                page: 2,
                per_page: 10
            });
        });

        it('should default to the site root and the shared pagination defaults', () => {
            searchFoldersReturns([]);

            spectator.service
                .getFolderChildren({ siteId: SITE_ID, hostname: HOSTNAME })
                .subscribe();

            expect(folderService.searchFolders).toHaveBeenCalledWith(
                expect.objectContaining({ path: '/', page: 1, per_page: 40 })
            );
        });

        it('should join the parent path and the name into a full folder path', () => {
            // The endpoint never returns the joined path, and consumers query pages by it.
            searchFoldersReturns([
                folderView({ name: 'about-us', path: '/' }),
                folderView({ id: 'folder-2', name: 'team', path: '/about-us/' })
            ]);

            let paths: string[] = [];
            spectator.service
                .getFolderChildren({ siteId: SITE_ID, hostname: HOSTNAME })
                .subscribe(({ folders }) => (paths = folders.map(({ path }) => path)));

            expect(paths).toEqual(['/about-us/', '/about-us/team/']);
        });

        it('should carry the hostname the folder search does not return', () => {
            searchFoldersReturns([folderView()]);

            let hostnames: string[] = [];
            spectator.service
                .getFolderChildren({ siteId: SITE_ID, hostname: HOSTNAME })
                .subscribe(({ folders }) => (hostnames = folders.map((folder) => folder.hostname)));

            expect(hostnames).toEqual([HOSTNAME]);
        });

        it('should report the total the caller needs to decide whether to page again', () => {
            searchFoldersReturns([folderView()], pagination({ totalEntries: 12 }));

            let children: { totalFolders: number; page: number; perPage: number } | null = null;
            spectator.service
                .getFolderChildren({ siteId: SITE_ID, hostname: HOSTNAME, page: 3, perPage: 5 })
                .subscribe((result) => (children = result));

            expect(children).toEqual(
                expect.objectContaining({ totalFolders: 12, page: 3, perPage: 5 })
            );
        });

        it('should fall back to the returned count when no pagination comes back', () => {
            searchFoldersReturns([folderView(), folderView({ id: 'folder-2' })], null);

            let totalFolders = 0;
            spectator.service
                .getFolderChildren({ siteId: SITE_ID, hostname: HOSTNAME })
                .subscribe((result) => (totalFolders = result.totalFolders));

            expect(totalFolders).toBe(2);
        });
    });

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

        it('should surface a failed folder search', () => {
            const error = new HttpErrorResponse({ status: 403, statusText: 'Forbidden' });
            folderService.searchFolders.mockReturnValue(throwError(() => error));

            let emitted = false;
            let status: number | undefined;
            spectator.service.getFolderChildren({ siteId: SITE_ID, hostname: HOSTNAME }).subscribe({
                next: () => (emitted = true),
                error: (failure: HttpErrorResponse) => (status = failure.status)
            });

            expect(status).toBe(403);
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
