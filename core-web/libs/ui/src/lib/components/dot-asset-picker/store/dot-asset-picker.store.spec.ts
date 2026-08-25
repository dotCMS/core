import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DotContentDriveService, DotFolderService, DotSiteService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotContentDriveSearchResponse,
    DotSite,
    TreeNodeItem
} from '@dotcms/dotcms-models';

import { ASSET_PICKER_ERROR_KEYS, DEFAULT_ASSET_PICKER_PAGINATION } from './constants';
import { DotAssetPickerStore } from './dot-asset-picker.store';
import { DotAssetPickerConfig } from './models';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

const EMPTY_RESPONSE: DotContentDriveSearchResponse = {
    folderCount: 0,
    contentCount: 0,
    list: [],
    hasMoreContent: false,
    hasMoreFolders: false,
    nextContentCursor: 0,
    nextFolderCursor: 0
};

/** A second site, to prove the picker is not pinned to the one that opened it. */
const OTHER_SITE: DotSite = {
    identifier: 'site-2',
    hostname: 'blog.dotcms.com',
    aliases: null,
    archived: false
};

/** One page of sites as `/api/v1/site` returns them (`name`, not `hostname`). */
const SITES_RESPONSE = {
    sites: [
        { identifier: SITE.identifier, hostname: SITE.hostname, aliases: null, archived: false },
        {
            identifier: OTHER_SITE.identifier,
            hostname: OTHER_SITE.hostname,
            aliases: null,
            archived: false
        }
    ],
    pagination: { currentPage: 1, perPage: 40, totalEntries: 2 }
};

const EMPTY_FOLDERS = { folders: [], pagination: { currentPage: 1, perPage: 40, totalEntries: 0 } };

/** The only two base types that carry an asset — the boundary both entry points impose. */
const ASSET_BASE_TYPES = [
    DotCMSBaseTypesContentTypes.DOTASSET,
    DotCMSBaseTypesContentTypes.FILEASSET
];

/** What the File field hands the store: a type boundary and a locale, nothing pre-selected. */
const FILE_FIELD_CONFIG: DotAssetPickerConfig = {
    site: SITE,
    languageId: '1',
    allowedBaseTypes: ASSET_BASE_TYPES
};

/** What the Image field hands the store: the same boundary, pre-selected, plus a silent mime. */
const IMAGE_FIELD_CONFIG: DotAssetPickerConfig = {
    site: SITE,
    languageId: '1',
    allowedBaseTypes: ASSET_BASE_TYPES,
    baseTypes: ASSET_BASE_TYPES,
    mimeTypes: ['image/*']
};

describe('DotAssetPickerStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAssetPickerStore>>;
    let store: InstanceType<typeof DotAssetPickerStore>;
    let contentDriveService: SpyObject<DotContentDriveService>;
    let folderService: SpyObject<DotFolderService>;
    let siteService: SpyObject<DotSiteService>;

    // Deliberately NO provideRouter / RouterTestingModule here — see the "no router" describe below.
    const createService = createServiceFactory({
        service: DotAssetPickerStore,
        providers: [
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(EMPTY_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                searchFolders: jest.fn().mockReturnValue(of(EMPTY_FOLDERS))
            }),
            mockProvider(DotSiteService, {
                getSites: jest.fn().mockReturnValue(of(SITES_RESPONSE))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        contentDriveService = spectator.inject(DotContentDriveService, true);
        folderService = spectator.inject(DotFolderService, true);
        siteService = spectator.inject(DotSiteService, true);

        // `mockProvider` builds one mock per file and `clearAllMocks` only clears calls, not
        // implementations — so re-seed the defaults here or a test that overrides a return value
        // leaks into every test after it.
        contentDriveService.search.mockReturnValue(of(EMPTY_RESPONSE));
        folderService.searchFolders.mockReturnValue(of(EMPTY_FOLDERS));
        siteService.getSites.mockReturnValue(of(SITES_RESPONSE));
    });

    afterEach(() => jest.clearAllMocks());

    describe('File field entry point', () => {
        beforeEach(() => {
            store.initPicker(FILE_FIELD_CONFIG);
        });

        it('should send the pre-selected locale', () => {
            expect(store.$request().language).toEqual(['1']);
        });

        it('should not restrict content types', () => {
            expect(store.$request().contentTypes).toBeUndefined();
        });

        it('should restrict to the asset base types even with nothing pre-selected', () => {
            // Regression: a File field starts with no base-type chip, and "no chip" used to mean
            // "no restriction" — so the list offered Pages and every other content type. A File
            // field holds files (images, PDFs, videos), never content.
            expect(store.$request().baseTypes).toEqual(ASSET_BASE_TYPES);
        });

        it('should not send a mimetype restriction', () => {
            // Any kind of file is fair game here — that is what separates it from an Image field.
            expect(store.$request().mimeTypes).toBeUndefined();
        });

        it('should address the site root when no path is configured', () => {
            expect(store.$request().assetPath).toBe('//dotcms.com/');
        });

        it('should address the configured folder when one is given', () => {
            store.initPicker({ ...FILE_FIELD_CONFIG, path: '/images/' });

            expect(store.$request().assetPath).toBe('//dotcms.com/images/');
        });
    });

    describe('entry-point boundary', () => {
        // `allowedBaseTypes` is what the field can EVER hold; `baseTypes` is only what starts
        // selected. Conflating them is what let a File field list Pages.
        it('should keep restricting after the editor clears every filter', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            store.clearFilters();

            expect(store.$request().baseTypes).toEqual(ASSET_BASE_TYPES);
        });

        it('should keep restricting after the editor removes the base-type filter', () => {
            store.initPicker(IMAGE_FIELD_CONFIG);

            store.removeFilter('baseType');

            expect(store.$request().baseTypes).toEqual(ASSET_BASE_TYPES);
        });

        it('should let a narrower selection win over the boundary', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            store.patchFilters({ baseType: [DotCMSBaseTypesContentTypes.DOTASSET] });

            expect(store.$request().baseTypes).toEqual([DotCMSBaseTypesContentTypes.DOTASSET]);
        });

        it('should stay unrestricted when the host configures no boundary', () => {
            // Content Drive-style usage: the picker itself imposes nothing.
            store.initPicker({ site: SITE });

            expect(store.$request().baseTypes).toBeUndefined();
        });
    });

    describe('Image field entry point', () => {
        beforeEach(() => {
            store.initPicker(IMAGE_FIELD_CONFIG);
        });

        it('should send the pre-selected locale', () => {
            expect(store.$request().language).toEqual(['1']);
        });

        it('should restrict to the dotAsset and File Asset base types', () => {
            expect(store.$request().baseTypes).toEqual(['DOTASSET', 'FILEASSET']);
        });

        it('should send the mimetype restriction', () => {
            expect(store.$request().mimeTypes).toEqual(['image/*']);
        });

        it('should keep the mimetype restriction out of the user-facing filters', () => {
            // FR-004: nothing downstream can render it as a chip or offer to clear it.
            expect(Object.values(store.filters())).not.toContainEqual(['image/*']);
            expect(JSON.stringify(store.filters())).not.toContain('image/*');
        });

        it('should keep applying the mimetype restriction after the user clears every filter', () => {
            store.clearFilters();

            expect(store.$request().mimeTypes).toEqual(['image/*']);
            expect(store.$request().language).toBeUndefined();
        });
    });

    describe('showFolders invariant', () => {
        it('should be false with no filters applied', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            expect(store.$request().showFolders).toBe(false);
        });

        it('should be false with every filter applied', () => {
            store.initPicker(IMAGE_FIELD_CONFIG);
            store.patchFilters({
                title: 'logo',
                contentType: ['fileAsset'],
                baseType: ['FILEASSET'],
                languageId: ['1', '2']
            });

            expect(store.$request().showFolders).toBe(false);
        });

        it('should be false after filters are cleared', () => {
            store.initPicker(FILE_FIELD_CONFIG);
            store.patchFilters({ title: 'logo' });
            store.clearFilters();

            expect(store.$request().showFolders).toBe(false);
        });

        it('should never advance the folder cursor', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            expect(store.$request().folderCursor).toBe(0);
        });
    });

    describe('no router dependency', () => {
        it('should construct with no router providers configured', () => {
            // If any part of the store injects ActivatedRoute or Location, createService() throws.
            expect(() => createService()).not.toThrow();
        });

        it('should not read a path from anywhere but its own configuration', () => {
            store.initPicker({ ...FILE_FIELD_CONFIG, path: '/docs/' });

            expect(store.path()).toBe('/docs/');
        });
    });

    describe('search lifecycle', () => {
        it('should not search before it is configured', () => {
            spectator.flushEffects();

            expect(contentDriveService.search).not.toHaveBeenCalled();
        });

        it('should not search when the configured site is SYSTEM_HOST', () => {
            store.initPicker({
                site: { ...SITE, identifier: 'SYSTEM_HOST', hostname: 'SYSTEM_HOST' }
            });
            spectator.flushEffects();

            expect(contentDriveService.search).not.toHaveBeenCalled();
        });

        it('should search through the drive endpoint once configured', () => {
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            expect(contentDriveService.search).toHaveBeenCalledWith(
                expect.objectContaining({ showFolders: false, assetPath: '//dotcms.com/' })
            );
        });

        it('should store the returned items and mark itself loaded', () => {
            contentDriveService.search.mockReturnValue(
                of({ ...EMPTY_RESPONSE, list: [{ inode: 'a' }], contentCount: 1 })
            );
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            expect(store.items()).toEqual([{ inode: 'a' }]);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should reset to page one when a filter changes', () => {
            store.initPicker(FILE_FIELD_CONFIG);
            patchState(unprotected(store), {
                pagination: { ...DEFAULT_ASSET_PICKER_PAGINATION, page: 3 }
            });

            store.patchFilters({ title: 'logo' });

            expect(store.pagination().page).toBe(1);
        });
    });

    describe('error handling', () => {
        beforeEach(() => {
            contentDriveService.search.mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();
        });

        it('should record the failure for the host to report', () => {
            // Not `DotHttpErrorManagerService`: it transitively needs `Router`, which the legacy
            // Dojo host has none of. The store says what failed; the picker component toasts it.
            expect(store.requestError()).toEqual({
                messageKey: ASSET_PICKER_ERROR_KEYS.assets
            });
        });

        it('should not leave itself loading', () => {
            expect(store.status()).toBe(ComponentStatus.ERROR);
        });

        it('should still accept a retry after failing', () => {
            contentDriveService.search.mockReturnValue(of(EMPTY_RESPONSE));

            store.patchFilters({ title: 'retry' });
            spectator.flushEffects();

            expect(store.status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('cursor paging', () => {
        it('should not re-fire the search when the response writes the next cursor back', () => {
            // The response updates `pages`, which `$request` reads. If that read were tracked, the
            // request would recompute, refire the search, and loop forever.
            contentDriveService.search.mockReturnValue(
                of({ ...EMPTY_RESPONSE, nextContentCursor: 20, hasMoreContent: true })
            );

            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();
            spectator.flushEffects();

            expect(contentDriveService.search).toHaveBeenCalledTimes(1);
        });

        it('should resume page two from the cursor the previous page returned', () => {
            contentDriveService.search.mockReturnValue(
                of({ ...EMPTY_RESPONSE, nextContentCursor: 20, hasMoreContent: true })
            );
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            store.setPagination({ ...DEFAULT_ASSET_PICKER_PAGINATION, page: 2 });

            expect(store.$request().contentCursor).toBe(20);
        });

        it('should discard cursor bookmarks when the page size changes', () => {
            contentDriveService.search.mockReturnValue(
                of({ ...EMPTY_RESPONSE, nextContentCursor: 20, hasMoreContent: true })
            );
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            store.setPagination({ limit: 50, page: 2 });

            expect(store.pagination().page).toBe(1);
            expect(store.$request().contentCursor).toBe(0);
        });

        it('should keep the count the endpoint actually returned', () => {
            contentDriveService.search.mockReturnValue(of({ ...EMPTY_RESPONSE, contentCount: 42 }));
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            expect(store.totalItems()).toBe(42);
        });
    });

    describe('paginator row count', () => {
        const page = (list: unknown[], hasMoreContent: boolean) => ({
            ...EMPTY_RESPONSE,
            list,
            contentCount: list.length,
            hasMoreContent,
            nextContentCursor: list.length
        });

        it('should claim a page beyond the current one while there is more content', () => {
            // `contentCount` is the size of THIS page, so a full page looks like the last one and
            // PrimeNG disables "next". Claiming one page beyond is what keeps the arrow clickable.
            contentDriveService.search.mockReturnValue(of(page(Array(20).fill({}), true)));
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            expect(store.totalItems()).toBe(20);
            expect(store.$totalRecords()).toBe(40);
        });

        it('should report the exact total once the last page is on screen', () => {
            contentDriveService.search.mockReturnValue(of(page(Array(7).fill({}), false)));
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            expect(store.$totalRecords()).toBe(7);
        });

        it('should count the pages already behind it on a later page', () => {
            contentDriveService.search.mockReturnValue(of(page(Array(20).fill({}), true)));
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            // Page 2 comes back short and with nothing after it: 20 behind + 5 on screen.
            contentDriveService.search.mockReturnValue(of(page(Array(5).fill({}), false)));
            store.setPagination({ ...DEFAULT_ASSET_PICKER_PAGINATION, page: 2 });
            spectator.flushEffects();

            expect(store.$totalRecords()).toBe(25);
        });

        it('should report nothing before the first response lands', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            expect(store.$totalRecords()).toBe(0);
        });
    });

    describe('folder tree', () => {
        it('should load the tree when the picker is configured', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            expect(folderService.searchFolders).toHaveBeenCalled();
        });

        it('should not load the tree before the picker is configured', () => {
            expect(folderService.searchFolders).not.toHaveBeenCalled();
        });

        it('should root the tree at the browsed site and nothing else', () => {
            // Sites used to be the roots — that is how the editor changed site. The site is now
            // chosen explicitly in the sidebar's selector, so the tree shows one site's folders.
            store.initPicker(FILE_FIELD_CONFIG);

            expect(store.folders()).toHaveLength(1);
            expect(store.folders()[0].data).toEqual(
                expect.objectContaining({ type: 'site', id: SITE.identifier })
            );
        });

        it('should expand the site it opens on', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            expect(store.folders()[0].expanded).toBe(true);
        });

        it('should open on the remembered site rather than the one being edited', () => {
            store.initPicker({ ...FILE_FIELD_CONFIG, browseSite: OTHER_SITE });

            expect(store.browsingSite()).toEqual(OTHER_SITE);
            expect(store.folders()[0].data?.id).toBe(OTHER_SITE.identifier);
            expect(store.folders()[0].expanded).toBe(true);
        });

        it('should track the selected node', () => {
            const node = { key: 'docs', label: '/docs/' } as TreeNodeItem;

            store.setSelectedNode(node);

            expect(store.selectedNode()).toBe(node);
        });

        describe('when the folders request fails', () => {
            beforeEach(() => {
                folderService.searchFolders.mockReturnValue(
                    throwError(() => new HttpErrorResponse({ status: 500 }))
                );
                store.initPicker(FILE_FIELD_CONFIG);
            });

            it('should record the failure for the host to report', () => {
                expect(store.requestError()).toEqual({
                    messageKey: ASSET_PICKER_ERROR_KEYS.folders
                });
            });

            it('should stay in error instead of looking like an empty tree', () => {
                // The success path used to run for the error fallback too and patch LOADED back
                // over ERROR, so a failed load was indistinguishable from a site with no folders.
                expect(store.foldersStatus()).toBe(ComponentStatus.ERROR);
            });

            it('should still accept a retry after failing', () => {
                folderService.searchFolders.mockReturnValue(of(EMPTY_FOLDERS));

                store.initPicker(FILE_FIELD_CONFIG);

                expect(store.foldersStatus()).toBe(ComponentStatus.LOADED);
            });
        });

        describe('expanding a node', () => {
            /**
             * A collapsed folder hanging off the single `All` root — what expansion acts on now
             * that a second site root no longer exists to expand.
             */
            const COLLAPSED_FOLDER = {
                key: 'folder-docs',
                label: `${SITE.hostname}/docs/`,
                leaf: false,
                data: {
                    type: 'folder' as const,
                    id: 'folder-docs',
                    hostname: SITE.hostname,
                    path: '/docs/'
                }
            } as TreeNodeItem;

            /** The node as the tree is currently rendering it — not the reference we passed in. */
            const renderedFolder = () =>
                store.folders()[0].children?.find((node) => node.key === COLLAPSED_FOLDER.key);

            const expandFolder = () => store.expandNode(renderedFolder() as TreeNodeItem);

            beforeEach(() => {
                store.initPicker(FILE_FIELD_CONFIG);
                store.folders()[0].children = [structuredClone(COLLAPSED_FOLDER)];
                folderService.searchFolders.mockReturnValue(
                    of({
                        folders: [
                            {
                                id: 'folder-1',
                                name: 'images',
                                path: '/',
                                hasChildren: false
                            }
                        ],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                    })
                );
            });

            it('should stop showing the spinner once the request resolves', () => {
                // Regression: the pre-request state refresh deep-cloned the tree, so the response
                // cleared `loading` on a node that was no longer the one being rendered — the
                // spinner span forever even though the call had returned 200.
                expandFolder();

                expect(renderedFolder()?.loading).toBeFalsy();
            });

            it('should show the loaded children under the node', () => {
                expandFolder();

                expect(renderedFolder()?.expanded).toBe(true);
                expect(renderedFolder()?.children).toHaveLength(1);
            });

            it('should publish the node as a new object so the OnPush tree re-renders it', () => {
                // `p-tree` and its `p-treeNode`s are OnPush and their `*ngFor` tracks by object
                // identity: a node mutated in place keeps its identity, so the row is never
                // re-rendered and the spinner only clears when something else triggers change
                // detection. Publishing a fresh object is what makes the update visible.
                const before = store.folders()[1];

                store.expandNode(before);

                expect(renderedFolder()).not.toBe(before);
            });

            it('should keep the highlight on the selected node across a load', () => {
                // The selection is compared by reference, so re-pointing it at the new object graph
                // is what stops the highlight from vanishing when a branch loads.
                store.selectNode(store.folders()[0]);
                const selectedKey = store.selectedNode()?.key;

                expandFolder();

                expect(store.selectedNode()?.key).toBe(selectedKey);
                expect(store.selectedNode()).toBe(
                    store.folders().find((node) => node.key === selectedKey)
                );
            });

            it('should clear the spinner when the request fails', () => {
                folderService.searchFolders.mockReturnValue(
                    throwError(() => new HttpErrorResponse({ status: 500 }))
                );

                expandFolder();

                expect(renderedFolder()?.loading).toBeFalsy();
                expect(store.requestError()).toEqual({
                    messageKey: ASSET_PICKER_ERROR_KEYS.folders
                });
            });

            it('should not re-fetch a node that already has children', () => {
                expandFolder();
                folderService.searchFolders.mockClear();

                store.expandNode(renderedFolder() as TreeNodeItem);

                expect(folderService.searchFolders).not.toHaveBeenCalled();
            });
        });

        describe('scoping to a node', () => {
            // The tree is no longer how the editor changes site — the selector is (US1). What it
            // still does is scope the list within the site it is rooted at.
            beforeEach(() => store.initPicker(FILE_FIELD_CONFIG));

            it('should address the whole site when the root is picked', () => {
                store.selectNode(store.folders()[0]);

                expect(store.$request().assetPath).toBe(`//${SITE.hostname}/`);
            });

            it('should scope the search to a folder of that site', () => {
                const folder = {
                    key: 'f1',
                    label: `${SITE.hostname}/images/`,
                    data: {
                        type: 'folder' as const,
                        id: 'folder-1',
                        hostname: SITE.hostname,
                        path: '/images/'
                    }
                } as TreeNodeItem;
                store.folders()[0].children = [folder];

                store.selectNode(folder);

                expect(store.$request().assetPath).toBe(`//${SITE.hostname}/images/`);
            });

            it('should send the user back to page one', () => {
                store.setPagination({ limit: 20, page: 3 });

                store.selectNode(store.folders()[0]);

                expect(store.pagination().page).toBe(1);
            });

            it('should ignore a click on a "Load more" sentinel', () => {
                const before = store.$request().assetPath;

                store.selectNode({
                    key: 'load-more:sites',
                    data: { type: 'load-more', id: 'load-more:sites' }
                } as TreeNodeItem);

                expect(store.$request().assetPath).toBe(before);
            });
        });

        describe('folder search (US2)', () => {
            // `/folder/search` returns each match with its **parent** path; the browsing service
            // appends the name to build the node's own path. So `thumbnails` arrives as
            // `path: '/images/'` and becomes `/images/thumbnails/` — which is what makes a flat
            // result list addressable without a tree to walk up.
            const results = {
                folders: [
                    { id: 'f1', name: 'images', path: '/', hasChildren: false },
                    { id: 'f2', name: 'thumbnails', path: '/images/', hasChildren: false }
                ],
                pagination: { currentPage: 1, perPage: 40, totalEntries: 2 }
            };

            beforeEach(() => store.initPicker(FILE_FIELD_CONFIG));

            it('should search folder names recursively inside the site being browsed', () => {
                store.setFolderSearch('ima');

                expect(folderService.searchFolders).toHaveBeenCalledWith(
                    expect.objectContaining({
                        siteId: SITE.identifier,
                        path: '/',
                        recursive: true,
                        name: 'ima',
                        page: 1
                    })
                );
            });

            it('should treat a one-character term as no search', () => {
                // `/folder/search` rejects a `name` shorter than two characters, so a single letter
                // is treated as "no search" rather than sent and failed.
                folderService.searchFolders.mockClear();

                store.setFolderSearch('i');

                expect(folderService.searchFolders).not.toHaveBeenCalledWith(
                    expect.objectContaining({ recursive: true })
                );
                expect(store.searchResults()).toBeNull();
            });

            it('should not leak into the asset search', () => {
                store.setFolderSearch('images');

                expect(store.$request().filters?.text).toBe('');
            });

            it('should leave the tree highlight where it was', () => {
                // Searching changes what the sidebar *shows*; it must not move where the asset list
                // and any upload are pointed.
                const before = store.selectedNode();

                store.setFolderSearch('images');

                expect(store.selectedNode()).not.toBeUndefined();
                expect(store.selectedNode()?.key).toBe(before?.key);
            });

            it('should publish the matches as a flat result list', () => {
                folderService.searchFolders.mockReturnValue(of(results));

                store.setFolderSearch('ima');

                expect(store.searchResults()).toHaveLength(2);
                expect(store.isSearchingFolders()).toBe(true);
            });

            it('should distinguish "searched, nothing matched" from "not searching"', () => {
                // `null` and `[]` must not collapse, or the empty state fires before the editor has
                // typed anything.
                expect(store.searchResults()).toBeNull();

                folderService.searchFolders.mockReturnValue(of(EMPTY_FOLDERS));
                store.setFolderSearch('zzz');

                expect(store.searchResults()).toEqual([]);
                expect(store.showResultsEmptyState()).toBe(true);
            });

            it('should record that more matches exist than the one page shown', () => {
                folderService.searchFolders.mockReturnValue(
                    of({ ...results, pagination: { currentPage: 1, perPage: 2, totalEntries: 40 } })
                );

                store.setFolderSearch('a');
                store.setFolderSearch('ab');

                expect(store.searchHasMore()).toBe(true);
                expect(store.showRefineHint()).toBe(true);
            });

            it('should surface a failed search as an error, not as an empty result', () => {
                folderService.searchFolders.mockReturnValue(
                    throwError(() => new HttpErrorResponse({ status: 500 }))
                );

                store.setFolderSearch('ima');

                expect(store.searchStatus()).toBe(ComponentStatus.ERROR);
                expect(store.showResultsEmptyState()).toBe(false);
            });

            describe('acting on a result', () => {
                beforeEach(() => {
                    folderService.searchFolders.mockReturnValue(of(results));
                    store.setFolderSearch('ima');
                });

                it('should scope the asset list to the chosen folder', () => {
                    store.selectSearchResult(store.searchResults()![1]);

                    expect(store.$request().assetPath).toBe(
                        `//${SITE.hostname}/images/thumbnails/`
                    );
                });

                it('should keep the result list open so another can be picked without retyping', () => {
                    store.selectSearchResult(store.searchResults()![0]);

                    expect(store.searchResults()).toHaveLength(2);
                    expect(store.folderSearch()).toBe('ima');
                    expect(store.isSearchingFolders()).toBe(true);
                });

                it('should mark the chosen row as selected', () => {
                    const target = store.searchResults()![1];

                    store.selectSearchResult(target);

                    expect(store.selectedResultKey()).toBe(target.key);
                });

                it('should return to the tree when the term is cleared', () => {
                    store.setFolderSearch('');

                    expect(store.searchResults()).toBeNull();
                    expect(store.isSearchingFolders()).toBe(false);
                    expect(store.folders()).toHaveLength(1);
                });
            });
        });

        describe('reopening on a remembered location (US4)', () => {
            const REMEMBERED = {
                identifier: OTHER_SITE.identifier,
                hostname: OTHER_SITE.hostname
            };

            it('should open on the remembered site when it still resolves', () => {
                store.initPicker({ ...FILE_FIELD_CONFIG, browseSite: REMEMBERED });

                expect(store.browsingSite()).toEqual(REMEMBERED);
                expect(store.folders()[0].data?.id).toBe(REMEMBERED.identifier);
            });

            describe('when the remembered site is gone', () => {
                beforeEach(() => {
                    // A site the editor can no longer browse — deleted, archived, or permissions
                    // revoked — fails the folder load rather than returning an empty tree.
                    let firstCall = true;
                    folderService.searchFolders.mockImplementation(() => {
                        if (firstCall) {
                            firstCall = false;

                            return throwError(() => new HttpErrorResponse({ status: 404 }));
                        }

                        return of(EMPTY_FOLDERS);
                    });

                    // With a remembered *folder* too, so "drop the folder" is a real assertion
                    // rather than one that passes because there was never a path.
                    store.initPicker({
                        ...FILE_FIELD_CONFIG,
                        browseSite: REMEMBERED,
                        path: '/images/'
                    });
                });

                it('should fall back to the site the editor is actually on', () => {
                    expect(store.browsingSite()).toEqual({
                        identifier: SITE.identifier,
                        hostname: SITE.hostname
                    });
                    expect(store.folders()[0].data?.id).toBe(SITE.identifier);
                });

                it('should drop the remembered folder along with the site it belonged to', () => {
                    // `/images/` on a dead site says nothing about where to land on the live one.
                    expect(store.path()).toBeUndefined();
                });

                it('should not surface an error for something the editor did not do', () => {
                    // Reopening somewhere sensible is the correct outcome, not a failure to report.
                    expect(store.requestError()).toBeNull();
                    expect(store.foldersStatus()).toBe(ComponentStatus.LOADED);
                });
            });

            it('should still report a failure when the entry site itself cannot load', () => {
                // No remembered site to fall back from — this one is a real error, and retrying the
                // same site forever would hide it.
                folderService.searchFolders.mockReturnValue(
                    throwError(() => new HttpErrorResponse({ status: 500 }))
                );

                store.initPicker(FILE_FIELD_CONFIG);

                expect(store.foldersStatus()).toBe(ComponentStatus.ERROR);
                expect(store.requestError()).toEqual({
                    messageKey: ASSET_PICKER_ERROR_KEYS.folders
                });
            });

            it('should try the fallback only once, even if it fails too', () => {
                folderService.searchFolders.mockReturnValue(
                    throwError(() => new HttpErrorResponse({ status: 500 }))
                );

                store.initPicker({ ...FILE_FIELD_CONFIG, browseSite: REMEMBERED });

                // Remembered attempt, then one fallback. Never a loop.
                expect(folderService.searchFolders).toHaveBeenCalledTimes(2);
                expect(store.foldersStatus()).toBe(ComponentStatus.ERROR);
            });
        });

        describe('per-level paging (US3)', () => {
            // Removing the sites level must not have touched folder-level paging: it is the same
            // `loadMore` method, and the sites branch used to sit in front of this one.
            beforeEach(() => store.initPicker(FILE_FIELD_CONFIG));

            it('should append the next page to the folder level that asked for it', () => {
                const parent = {
                    key: 'folder-docs',
                    label: `${SITE.hostname}/docs/`,
                    expanded: true,
                    children: [
                        {
                            key: 'folder-a',
                            label: `${SITE.hostname}/docs/a/`,
                            data: {
                                type: 'folder' as const,
                                id: 'a',
                                hostname: SITE.hostname,
                                path: '/docs/a/'
                            }
                        }
                    ],
                    data: {
                        type: 'folder' as const,
                        id: 'folder-docs',
                        hostname: SITE.hostname,
                        path: '/docs/'
                    }
                } as unknown as TreeNodeItem;
                store.folders()[0].children = [parent];

                folderService.searchFolders.mockReturnValue(
                    of({
                        folders: [{ id: 'b', name: 'b', path: '/docs/', hasChildren: false }],
                        pagination: { currentPage: 2, perPage: 40, totalEntries: 2 }
                    })
                );

                store.loadMore({
                    key: 'load-more:folder-docs',
                    label: '',
                    data: {
                        type: 'load-more',
                        id: 'load-more:folder-docs',
                        nextPage: 2,
                        path: '/docs/',
                        hostname: SITE.hostname
                    }
                } as unknown as TreeNodeItem);

                expect(folderService.searchFolders).toHaveBeenLastCalledWith(
                    expect.objectContaining({
                        siteId: SITE.identifier,
                        path: '/docs/',
                        recursive: false,
                        page: 2
                    })
                );
            });
        });

        describe('changing the browsed site (US1)', () => {
            const OTHER = { identifier: OTHER_SITE.identifier, hostname: OTHER_SITE.hostname };

            beforeEach(() => store.initPicker({ ...FILE_FIELD_CONFIG, path: '/docs/' }));

            it('should re-root the tree at the new site', () => {
                store.setBrowsingSite(OTHER);

                expect(store.browsingSite()).toEqual(OTHER);
                expect(store.folders()).toHaveLength(1);
                expect(store.folders()[0].data?.id).toBe(OTHER.identifier);
            });

            it('should re-scope the asset list to the new site root', () => {
                store.setBrowsingSite(OTHER);

                expect(store.$request().assetPath).toBe(`//${OTHER.hostname}/`);
            });

            it('should drop the folder the old site was scoped to', () => {
                // `/docs/` exists on the site we came from. Carrying it over would either 404 or,
                // worse, silently land on a same-named folder that is not the one the editor meant.
                store.setBrowsingSite(OTHER);

                expect(store.path()).toBeUndefined();
            });

            it('should clear an active folder term', () => {
                // A term is only meaningful against the site it was typed for.
                store.setFolderSearch('images');

                store.setBrowsingSite(OTHER);

                expect(store.folderSearch()).toBe('');
                expect(store.searchResults()).toBeNull();
            });

            it('should send the user back to page one', () => {
                store.setPagination({ limit: 20, page: 4 });

                store.setBrowsingSite(OTHER);

                expect(store.pagination().page).toBe(1);
            });

            it('should do nothing when the same site is re-selected', () => {
                folderService.searchFolders.mockClear();

                store.setBrowsingSite({
                    identifier: SITE.identifier,
                    hostname: SITE.hostname
                });

                expect(folderService.searchFolders).not.toHaveBeenCalled();
            });
        });

        describe('load more', () => {
            it('should ignore a sites-level sentinel — there is no sites level left to page', () => {
                store.initPicker(FILE_FIELD_CONFIG);
                folderService.searchFolders.mockClear();
                siteService.getSites.mockClear();

                // The shape the old `isSitesLevel` branch keyed on: no hostname, empty parent path.
                store.loadMore({
                    key: 'load-more:sites',
                    label: '',
                    data: {
                        type: 'load-more',
                        id: 'load-more:sites',
                        nextPage: 2,
                        hostname: '',
                        path: ''
                    }
                } as unknown as TreeNodeItem);

                expect(siteService.getSites).not.toHaveBeenCalled();
                expect(folderService.searchFolders).not.toHaveBeenCalled();
            });
        });
    });
});
