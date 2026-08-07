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

import {
    DotContentDriveService,
    DotFolderService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotContentDriveSearchResponse,
    DotSite,
    TreeNodeItem
} from '@dotcms/dotcms-models';

import { DEFAULT_ASSET_PICKER_PAGINATION } from './constants';
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

/** What AssetPicker 6/7 will hand the store for a File field: locale only, no type restriction. */
const FILE_FIELD_CONFIG: DotAssetPickerConfig = {
    site: SITE,
    languageId: '1'
};

/** What AssetPicker 6/7 will hand the store for an Image field: locale + base types + silent mime. */
const IMAGE_FIELD_CONFIG: DotAssetPickerConfig = {
    site: SITE,
    languageId: '1',
    baseTypes: [DotCMSBaseTypesContentTypes.DOTASSET, DotCMSBaseTypesContentTypes.FILEASSET],
    mimeTypes: ['image/*']
};

describe('DotAssetPickerStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAssetPickerStore>>;
    let store: InstanceType<typeof DotAssetPickerStore>;
    let contentDriveService: SpyObject<DotContentDriveService>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;

    // Deliberately NO provideRouter / RouterTestingModule here — see the "no router" describe below.
    const createService = createServiceFactory({
        service: DotAssetPickerStore,
        providers: [
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(EMPTY_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                searchFolders: jest.fn().mockReturnValue(of({ folders: [], pagination: {} }))
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn().mockReturnValue(of({ redirected: false, status: 500 }))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        contentDriveService = spectator.inject(DotContentDriveService, true);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService, true);
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

        it('should not restrict base types', () => {
            expect(store.$request().baseTypes).toBeUndefined();
        });

        it('should not send a mimetype restriction', () => {
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

        it('should report through the shared HTTP error manager', () => {
            expect(httpErrorManager.handle).toHaveBeenCalled();
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

        it('should send the total count to the paginator', () => {
            contentDriveService.search.mockReturnValue(of({ ...EMPTY_RESPONSE, contentCount: 42 }));
            store.initPicker(FILE_FIELD_CONFIG);
            spectator.flushEffects();

            expect(store.totalItems()).toBe(42);
        });
    });

    describe('folder tree', () => {
        let folderService: SpyObject<DotFolderService>;

        beforeEach(() => {
            folderService = spectator.inject(DotFolderService, true);
        });

        it('should load the tree when the picker is configured', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            expect(folderService.searchFolders).toHaveBeenCalled();
        });

        it('should not load the tree before the picker is configured', () => {
            expect(folderService.searchFolders).not.toHaveBeenCalled();
        });

        it('should not load the tree for SYSTEM_HOST', () => {
            store.initPicker({
                site: { ...SITE, identifier: 'SYSTEM_HOST', hostname: 'SYSTEM_HOST' }
            });

            expect(folderService.searchFolders).not.toHaveBeenCalled();
        });

        it('should prepend a site-scoped root node to the tree', () => {
            store.initPicker(FILE_FIELD_CONFIG);

            expect(store.folders()[0]).toEqual(
                expect.objectContaining({
                    key: 'ALL_FOLDER',
                    data: expect.objectContaining({
                        id: SITE.identifier,
                        hostname: SITE.hostname
                    })
                })
            );
        });

        it('should replace the folders array on update so change detection sees it', () => {
            store.initPicker(FILE_FIELD_CONFIG);
            const next = [{ key: 'a', label: 'a' }] as TreeNodeItem[];

            store.updateFolders(next);

            expect(store.folders()).toEqual(next);
            expect(store.folders()).not.toBe(next);
        });

        it('should track the selected node', () => {
            const node = { key: 'docs', label: '/docs/' } as TreeNodeItem;

            store.setSelectedNode(node);

            expect(store.selectedNode()).toBe(node);
        });
    });

    describe('search term', () => {
        it('should drop the folder scope so results are site-wide', () => {
            store.initPicker({ ...FILE_FIELD_CONFIG, path: '/docs/' });

            store.setSearch('logo');

            expect(store.path()).toBeUndefined();
            expect(store.$request().filters?.text).toBe('logo');
        });

        it('should remove the filter entirely when the term is cleared', () => {
            store.initPicker(FILE_FIELD_CONFIG);
            store.setSearch('logo');

            store.setSearch('');

            expect(store.filters().title).toBeUndefined();
            expect(store.$request().filters?.text).toBe('');
        });
    });

    describe('selection', () => {
        it('should hold a single asset', () => {
            store.setSelectedAsset({ inode: 'a' });
            store.setSelectedAsset({ inode: 'b' });

            expect(store.selectedAsset()).toEqual({ inode: 'b' });
        });

        it('should clear the selection', () => {
            store.setSelectedAsset({ inode: 'a' });
            store.clearSelection();

            expect(store.selectedAsset()).toBeNull();
        });
    });
});
