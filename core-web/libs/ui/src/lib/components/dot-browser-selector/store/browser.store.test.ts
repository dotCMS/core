import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import {
    createServiceFactory,
    SpectatorService,
    mockProvider,
    SpyObject
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import {
    ComponentStatus,
    ContentByFolderParams,
    DOT_FOLDER_TREE_PAGE_SIZE,
    LOAD_MORE_NODE_TYPE,
    TreeNodeItem,
    TreeNodeSelectItem
} from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeEvent } from '@dotcms/utils-testing';

import { DotBrowserSelectorStore, SITE_PAGE_LIMIT, SYSTEM_HOST_ID } from './browser.store';

import { DotBrowsingService } from '../../../services/dot-browsing/dot-browsing.service';

const sitesPage = (sites: TreeNodeItem[], totalEntries = sites.length) =>
    of({
        sites,
        pagination: {
            currentPage: 1,
            perPage: SITE_PAGE_LIMIT,
            totalEntries
        }
    });

const TREE_SELECT_SITES_MOCK: TreeNodeItem[] = [
    {
        key: 'demo.dotcms.com',
        label: 'demo.dotcms.com',
        data: {
            id: 'demo.dotcms.com',
            hostname: 'demo.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-globe',
        collapsedIcon: 'pi pi-globe'
    },
    {
        key: 'nico.dotcms.com',
        label: 'nico.dotcms.com',
        data: {
            id: 'nico.dotcms.com',
            hostname: 'nico.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-globe',
        collapsedIcon: 'pi pi-globe'
    },
    {
        key: 'System Host',
        label: 'System Host',
        data: {
            id: 'System Host',
            hostname: 'System Host',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-globe',
        collapsedIcon: 'pi pi-globe'
    }
];

const TREE_SELECT_MOCK: TreeNodeItem[] = [
    {
        key: 'demo.dotcms.com',
        label: 'demo.dotcms.com',
        data: {
            id: 'demo.dotcms.com',
            hostname: 'demo.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-globe',
        collapsedIcon: 'pi pi-globe',
        children: [
            {
                key: 'demo.dotcms.comlevel1',
                label: 'demo.dotcms.com/level1/',
                data: {
                    id: 'demo.dotcms.comlevel1',
                    hostname: 'demo.dotcms.com',
                    path: '/level1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        key: 'demo.dotcms.comlevel1child1',
                        label: 'demo.dotcms.com/level1/child1/',
                        data: {
                            id: 'demo.dotcms.comlevel1child1',
                            hostname: 'demo.dotcms.com',
                            path: '/level1/child1/',
                            type: 'folder'
                        },
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder'
                    }
                ]
            },
            {
                key: 'demo.dotcms.comlevel2',
                label: 'demo.dotcms.com/level2/',
                data: {
                    id: 'demo.dotcms.comlevel2',
                    hostname: 'demo.dotcms.com',
                    path: '/level2/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder'
            }
        ]
    },
    {
        key: 'nico.dotcms.com',
        label: 'nico.dotcms.com',
        data: {
            id: 'nico.dotcms.com',
            hostname: 'nico.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-globe',
        collapsedIcon: 'pi pi-globe'
    }
];

describe('DotBrowserSelectorStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotBrowserSelectorStore>>;
    let store: InstanceType<typeof DotBrowserSelectorStore>;
    let dotBrowsingService: SpyObject<DotBrowsingService>;
    let dotUploadFileService: SpyObject<DotUploadFileService>;

    const createService = createServiceFactory({
        service: DotBrowserSelectorStore,
        providers: [
            mockProvider(DotBrowsingService, {
                getSitesPage: jest.fn().mockReturnValue(sitesPage(TREE_SELECT_SITES_MOCK)),
                getContentByFolder: jest.fn().mockReturnValue(of([])),
                searchFolders: jest.fn().mockReturnValue(
                    of({
                        folders: [],
                        pagination: {
                            currentPage: 1,
                            perPage: DOT_FOLDER_TREE_PAGE_SIZE,
                            totalEntries: 0
                        }
                    })
                )
            }),
            mockProvider(DotUploadFileService, {
                uploadDotAsset: jest.fn().mockReturnValue(of({}))
            })
        ]
    });

    beforeEach(fakeAsync(() => {
        spectator = createService();
        store = spectator.service;
        dotBrowsingService = spectator.inject(DotBrowsingService);
        dotUploadFileService = spectator.inject(DotUploadFileService);

        // Restore default implementations (prior tests may have overridden return values).
        dotBrowsingService.getSitesPage.mockReturnValue(sitesPage(TREE_SELECT_SITES_MOCK));
        dotBrowsingService.searchFolders.mockReturnValue(
            of({
                folders: [],
                pagination: {
                    currentPage: 1,
                    perPage: DOT_FOLDER_TREE_PAGE_SIZE,
                    totalEntries: 0
                }
            })
        );
        dotBrowsingService.getContentByFolder.mockReturnValue(of([]));
        store.loadFolders();
        tick(50);
    }));

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('Initial state', () => {
        it('should have initial state values after onInit', () => {
            // After onInit (which runs in beforeEach), folders should be loaded
            expect(store.folders().data).toEqual(TREE_SELECT_SITES_MOCK);
            expect(store.folders().status).toBe(ComponentStatus.LOADED);
            expect(store.content().data).toEqual([]);
            expect(store.content().status).toBe(ComponentStatus.INIT);
            expect(store.content().error).toBeNull();
            expect(store.selectedContent()).toBeNull();
            expect(store.searchQuery()).toBe('');
            expect(store.viewMode()).toBe('list');
        });
    });

    describe('Computed properties', () => {
        it('should compute foldersIsLoading as true when folders status is LOADING', () => {
            patchState(unprotected(store), {
                folders: { ...store.folders(), status: ComponentStatus.LOADING }
            });
            expect(store.foldersIsLoading()).toBe(true);
        });

        it('should compute foldersIsLoading as false when folders status is not LOADING', () => {
            patchState(unprotected(store), {
                folders: { ...store.folders(), status: ComponentStatus.LOADED }
            });
            expect(store.foldersIsLoading()).toBe(false);
        });

        it('should compute foldersIsLoading as false when folders status is ERROR', () => {
            patchState(unprotected(store), {
                folders: { ...store.folders(), status: ComponentStatus.ERROR }
            });
            expect(store.foldersIsLoading()).toBe(false);
        });

        it('should compute contentIsLoading as true when content status is LOADING', () => {
            patchState(unprotected(store), {
                content: { ...store.content(), status: ComponentStatus.LOADING }
            });
            expect(store.contentIsLoading()).toBe(true);
        });

        it('should compute contentIsLoading as false when content status is LOADED', () => {
            const mockContentlets = [createFakeContentlet()];
            patchState(unprotected(store), {
                content: { data: mockContentlets, status: ComponentStatus.LOADED, error: null }
            });
            expect(store.contentIsLoading()).toBe(false);
        });

        it('should compute contentIsLoading as false when content status is ERROR', () => {
            patchState(unprotected(store), {
                content: { data: [], status: ComponentStatus.ERROR, error: 'Error message' }
            });
            expect(store.contentIsLoading()).toBe(false);
        });
    });

    describe('Method: setSelectedContent', () => {
        it('should set selected content', () => {
            const mockContentlet = createFakeContentlet({ title: 'Test Content' });
            store.setSelectedContent(mockContentlet);
            expect(store.selectedContent()).toEqual(mockContentlet);
        });

        it('should update selected content', () => {
            const mockContentlet1 = createFakeContentlet({ title: 'Content 1' });
            const mockContentlet2 = createFakeContentlet({ title: 'Content 2' });

            store.setSelectedContent(mockContentlet1);
            expect(store.selectedContent()).toEqual(mockContentlet1);

            store.setSelectedContent(mockContentlet2);
            expect(store.selectedContent()).toEqual(mockContentlet2);
        });
    });

    describe('Method: loadFolders', () => {
        it('should set folders status to LOADING and then to LOADED with data', fakeAsync(() => {
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getSitesPage.mockReturnValue(
                sitesPage(TREE_SELECT_SITES_MOCK).pipe(delay(1))
            );

            store.loadFolders();
            expect(store.folders().status).toBe(ComponentStatus.LOADING);

            tick(50);

            expect(store.folders().status).toBe(ComponentStatus.LOADED);
            expect(store.folders().data).toEqual(TREE_SELECT_SITES_MOCK);
            expect(dotBrowsingService.getSitesPage).toHaveBeenCalledWith({
                perPage: SITE_PAGE_LIMIT,
                filter: '*',
                page: 1
            });
        }));

        it('should append a load-more sentinel when more sites remain', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockClear();
            dotBrowsingService.searchFolders.mockClear();
            dotBrowsingService.getContentByFolder.mockClear();
            dotBrowsingService.getSitesPage.mockReturnValue(
                sitesPage(TREE_SELECT_SITES_MOCK, SITE_PAGE_LIMIT + 10)
            );

            store.loadFolders();
            tick(50);

            const data = store.folders().data;
            const loadMoreNode = data[data.length - 1];

            expect(data).toHaveLength(TREE_SELECT_SITES_MOCK.length + 1);
            expect(loadMoreNode.type).toBe(LOAD_MORE_NODE_TYPE);
            expect(loadMoreNode.data?.type).toBe(LOAD_MORE_NODE_TYPE);
            expect(loadMoreNode.data).toEqual(
                expect.objectContaining({
                    nextPage: 2
                })
            );
        }));

        it('should set folders status to ERROR on service error', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockReturnValue(throwError(() => new Error('error')));

            store.loadFolders();
            tick(50);

            expect(store.folders().status).toBe(ComponentStatus.ERROR);
            expect(store.folders().data).toEqual([]);

            // Reset to default so subsequent tests that rely on onInit → loadFolders are not affected
            dotBrowsingService.getSitesPage.mockReturnValue(sitesPage(TREE_SELECT_SITES_MOCK));
        }));
    });

    describe('Method: loadContent', () => {
        it('should load content for a selected node', fakeAsync(() => {
            const mockContentlets = [
                createFakeContentlet({ title: 'Content 1' }),
                createFakeContentlet({ title: 'Content 2' })
            ];
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getContentByFolder.mockReturnValue(
                of(mockContentlets).pipe(delay(1))
            );

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            };

            store.loadContent(params);
            expect(store.content().status).toBe(ComponentStatus.LOADING);

            tick(50);

            expect(store.content().status).toBe(ComponentStatus.LOADED);
            expect(store.content().data).toEqual(mockContentlets);
            expect(store.content().error).toBeNull();
            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            });
        }));

        it('should preserve existing content data when setting status to LOADING', fakeAsync(() => {
            const existingContent = [createFakeContentlet({ title: 'Existing' })];
            patchState(unprotected(store), {
                content: { data: existingContent, status: ComponentStatus.LOADED, error: null }
            });

            const mockContentlets = [createFakeContentlet({ title: 'New' })];
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getContentByFolder.mockReturnValue(
                of(mockContentlets).pipe(delay(1))
            );

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            };

            store.loadContent(params);
            // During loading, the data should still be preserved from previous state
            expect(store.content().status).toBe(ComponentStatus.LOADING);
            tick(50);
        }));

        it('should load content using SYSTEM_HOST_ID when provided', fakeAsync(() => {
            const mockContentlets = [createFakeContentlet()];
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getContentByFolder.mockReturnValue(
                of(mockContentlets).pipe(delay(1))
            );

            const params: ContentByFolderParams = {
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: []
            };

            store.loadContent(params);
            // Verify LOADING state before the observable completes
            expect(store.content().status).toBe(ComponentStatus.LOADING);

            tick(50);

            expect(store.content().status).toBe(ComponentStatus.LOADED);
            expect(store.content().data).toEqual(mockContentlets);
            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: []
            });
        }));

        it('should use mimeTypes filter when loading content', fakeAsync(() => {
            const mimeTypes = ['image/jpeg', 'image/png'];
            const mockContentlets = [createFakeContentlet()];
            dotBrowsingService.getContentByFolder.mockReturnValue(of(mockContentlets));

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes
            };

            store.loadContent(params);
            tick(50);

            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: 'demo.dotcms.com',
                mimeTypes
            });
        }));

        it('should handle service error when loading content', fakeAsync(() => {
            dotBrowsingService.getContentByFolder.mockReturnValue(
                throwError(() => new Error('Service error'))
            );

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            };

            store.loadContent(params);
            tick(50);

            expect(store.content().status).toBe(ComponentStatus.ERROR);
            expect(store.content().error).toBe(
                'dot.file.field.dialog.select.existing.file.table.error.content'
            );
            expect(store.content().data).toEqual([]);
        }));
    });

    describe('Method: loadChildren', () => {
        it('should load children for a site node via paginated searchFolders', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockClear();
            dotBrowsingService.searchFolders.mockClear();
            dotBrowsingService.getContentByFolder.mockClear();

            const folders = TREE_SELECT_MOCK[0].children ?? [];
            dotBrowsingService.searchFolders.mockReturnValue(
                of({
                    folders,
                    pagination: {
                        currentPage: 1,
                        perPage: DOT_FOLDER_TREE_PAGE_SIZE,
                        totalEntries: folders.length
                    }
                })
            );

            // Annotated because the literal's `children: undefined` would otherwise infer
            // exactly `undefined`; loadChildren populates it.
            const node: TreeNodeItem = {
                ...TREE_SELECT_MOCK[0],
                children: undefined,
                leaf: false
            };
            const mockItem: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node
            };

            store.loadChildren(mockItem);
            tick(50);

            expect(node.children).toEqual(folders);
            expect(node.loading).toBe(false);
            expect(node.leaf).toBe(false);
            expect(node.expanded).toBe(true);
            expect(dotBrowsingService.searchFolders).toHaveBeenCalledTimes(1);
            expect(dotBrowsingService.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: 'demo.dotcms.com',
                    path: '/',
                    recursive: false,
                    page: 1,
                    per_page: DOT_FOLDER_TREE_PAGE_SIZE
                },
                'demo.dotcms.com'
            );
        }));

        it('should append load-more when more child folders remain', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockClear();
            dotBrowsingService.searchFolders.mockClear();
            dotBrowsingService.getContentByFolder.mockClear();

            const folders = TREE_SELECT_MOCK[0].children ?? [];
            dotBrowsingService.searchFolders.mockReturnValue(
                of({
                    folders,
                    pagination: {
                        currentPage: 1,
                        perPage: DOT_FOLDER_TREE_PAGE_SIZE,
                        totalEntries: DOT_FOLDER_TREE_PAGE_SIZE + 5
                    }
                })
            );

            // Annotated because the literal's `children: undefined` would otherwise infer
            // exactly `undefined`; loadChildren populates it.
            const node: TreeNodeItem = {
                ...TREE_SELECT_MOCK[0],
                children: undefined,
                leaf: false
            };
            store.loadChildren({
                originalEvent: createFakeEvent('click'),
                node
            });
            tick(50);

            expect(node.children).toHaveLength(folders.length + 1);
            const children = node.children!;
            expect(children[children.length - 1].type).toBe(LOAD_MORE_NODE_TYPE);
        }));

        it('should handle error when loading children', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockClear();
            dotBrowsingService.searchFolders.mockClear();
            dotBrowsingService.getContentByFolder.mockClear();

            dotBrowsingService.searchFolders.mockReturnValue(throwError(() => new Error('error')));

            // Annotated because the literal's `children: undefined` would otherwise infer
            // exactly `undefined`; loadChildren populates it.
            const node: TreeNodeItem = {
                ...TREE_SELECT_MOCK[0],
                children: undefined,
                leaf: false
            };
            const mockItem: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node
            };

            store.loadChildren(mockItem);
            tick(50);

            expect(node.children).toBeUndefined();
            expect(node.loading).toBe(false);
        }));

        it('should resolve folder path and siteId for nested folders', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockClear();
            dotBrowsingService.searchFolders.mockClear();
            dotBrowsingService.getContentByFolder.mockClear();

            // Ensure site roots are present so findSiteIdByHostname works
            patchState(unprotected(store), {
                folders: {
                    data: TREE_SELECT_SITES_MOCK,
                    status: ComponentStatus.LOADED
                }
            });

            dotBrowsingService.searchFolders.mockReturnValue(
                of({
                    folders: [],
                    pagination: {
                        currentPage: 1,
                        perPage: DOT_FOLDER_TREE_PAGE_SIZE,
                        totalEntries: 0
                    }
                })
            );

            const childNode = TREE_SELECT_MOCK[0].children?.[0];
            if (!childNode) {
                throw new Error('Test setup error: child node not found');
            }

            const node: TreeNodeItem = {
                ...childNode,
                children: undefined,
                leaf: false
            };

            store.loadChildren({
                originalEvent: createFakeEvent('click'),
                node
            });
            tick(50);

            expect(dotBrowsingService.searchFolders).toHaveBeenCalledTimes(1);
            expect(dotBrowsingService.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: 'demo.dotcms.com',
                    path: '/level1/',
                    recursive: false,
                    page: 1,
                    per_page: DOT_FOLDER_TREE_PAGE_SIZE
                },
                'demo.dotcms.com'
            );
            expect(node.leaf).toBe(true);
        }));
    });

    describe('Method: loadMore', () => {
        it('should load the next page of sites', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockClear();
            dotBrowsingService.searchFolders.mockClear();
            dotBrowsingService.getContentByFolder.mockClear();

            const pageOne = TREE_SELECT_SITES_MOCK.slice(0, 2);
            const pageTwo = [TREE_SELECT_SITES_MOCK[2]];

            patchState(unprotected(store), {
                folders: {
                    data: [
                        ...pageOne,
                        {
                            key: 'load-more:sites',
                            type: LOAD_MORE_NODE_TYPE,
                            selectable: false,
                            leaf: true,
                            data: {
                                type: LOAD_MORE_NODE_TYPE,
                                id: 'load-more:sites',
                                nextPage: 2,
                                path: '',
                                hostname: ''
                            }
                        }
                    ],
                    status: ComponentStatus.LOADED
                }
            });

            dotBrowsingService.getSitesPage.mockReturnValue(
                of({
                    sites: pageTwo,
                    pagination: {
                        currentPage: 2,
                        perPage: SITE_PAGE_LIMIT,
                        totalEntries: 3
                    }
                })
            );

            const loadMoreNode = store.folders().data[store.folders().data.length - 1];
            store.loadMore(loadMoreNode);
            tick(50);

            expect(dotBrowsingService.getSitesPage).toHaveBeenCalledWith({
                filter: '*',
                perPage: SITE_PAGE_LIMIT,
                page: 2
            });
            expect(store.folders().data.map((node) => node.key)).toEqual(
                TREE_SELECT_SITES_MOCK.map((node) => node.key)
            );
        }));

        it('should append the next folder page under the parent site', fakeAsync(() => {
            dotBrowsingService.getSitesPage.mockClear();
            dotBrowsingService.searchFolders.mockClear();
            dotBrowsingService.getContentByFolder.mockClear();

            const site = {
                ...TREE_SELECT_MOCK[0],
                children: [
                    ...(TREE_SELECT_MOCK[0].children ?? []).slice(0, 1),
                    {
                        key: 'load-more:demo.dotcms.com',
                        type: LOAD_MORE_NODE_TYPE,
                        selectable: false,
                        leaf: true,
                        data: {
                            type: LOAD_MORE_NODE_TYPE,
                            id: 'load-more:demo.dotcms.com',
                            nextPage: 2,
                            path: '/',
                            hostname: 'demo.dotcms.com'
                        }
                    }
                ]
            };

            patchState(unprotected(store), {
                folders: {
                    data: [site, TREE_SELECT_SITES_MOCK[1]],
                    status: ComponentStatus.LOADED
                }
            });

            const nextFolders = (TREE_SELECT_MOCK[0].children ?? []).slice(1);
            dotBrowsingService.searchFolders.mockReturnValue(
                of({
                    folders: nextFolders,
                    pagination: {
                        currentPage: 2,
                        perPage: DOT_FOLDER_TREE_PAGE_SIZE,
                        totalEntries: 2
                    }
                })
            );

            const loadMoreNode = site.children?.[site.children.length - 1] as TreeNodeItem;
            store.loadMore(loadMoreNode);
            tick(50);

            expect(dotBrowsingService.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: 'demo.dotcms.com',
                    path: '/',
                    recursive: false,
                    page: 2,
                    per_page: DOT_FOLDER_TREE_PAGE_SIZE
                },
                'demo.dotcms.com'
            );

            const updatedSite = store.folders().data[0];
            expect(updatedSite.children?.map((child) => child.key)).toEqual(
                (TREE_SELECT_MOCK[0].children ?? []).map((child) => child.key)
            );
        }));
    });

    describe('onInit hook', () => {
        it('should call loadFolders on initialization', () => {
            // Store is created in beforeEach, which triggers onInit
            // onInit completes in beforeEach via fakeAsync/tick
            expect(dotBrowsingService.getSitesPage).toHaveBeenCalledWith({
                perPage: SITE_PAGE_LIMIT,
                filter: '*',
                page: 1
            });
            expect(store.folders().status).toBe(ComponentStatus.LOADED);
            expect(store.folders().data).toEqual(TREE_SELECT_SITES_MOCK);
        });
    });

    describe('Method: uploadFile', () => {
        const folderParams: ContentByFolderParams = {
            hostFolderId: 'demo.dotcms.com',
            mimeTypes: ['image/jpeg', 'image/png']
        };

        it('should set content status to LOADING when upload starts', fakeAsync(() => {
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });
            dotUploadFileService.uploadDotAsset.mockReturnValue(
                of(createFakeContentlet()).pipe(delay(1))
            );

            store.uploadFile({ file: mockFile, folderParams });
            expect(store.content().status).toBe(ComponentStatus.LOADING);

            tick(50);
        }));

        it('should call uploadDotAsset with the file and hostFolder', fakeAsync(() => {
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });
            dotUploadFileService.uploadDotAsset.mockReturnValue(of(createFakeContentlet()));

            store.uploadFile({ file: mockFile, folderParams });
            tick(50);

            expect(dotUploadFileService.uploadDotAsset).toHaveBeenCalledWith(mockFile, {
                hostFolder: folderParams.hostFolderId
            });
        }));

        it('should reload content after a successful upload', fakeAsync(() => {
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });
            const uploadedContentlet = createFakeContentlet({ title: 'Uploaded' });
            const refreshedContentlets = [uploadedContentlet];

            dotUploadFileService.uploadDotAsset.mockReturnValue(of(uploadedContentlet));
            dotBrowsingService.getContentByFolder.mockReturnValue(of(refreshedContentlets));

            store.uploadFile({ file: mockFile, folderParams });
            tick(50);

            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith(folderParams);
            expect(store.content().data).toEqual(refreshedContentlets);
            expect(store.content().status).toBe(ComponentStatus.LOADED);
        }));

        it('should auto-select the uploaded contentlet on success', fakeAsync(() => {
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });
            const uploadedContentlet = createFakeContentlet({ title: 'Uploaded' });

            dotUploadFileService.uploadDotAsset.mockReturnValue(of(uploadedContentlet));
            dotBrowsingService.getContentByFolder.mockReturnValue(of([uploadedContentlet]));

            store.uploadFile({ file: mockFile, folderParams });
            tick(50);

            expect(store.selectedContent()).toEqual(uploadedContentlet);
        }));

        it('should not auto-select anything when upload fails', fakeAsync(() => {
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });

            dotUploadFileService.uploadDotAsset.mockReturnValue(
                throwError({ status: 500, message: 'Server error' })
            );

            store.uploadFile({ file: mockFile, folderParams });
            tick(50);

            expect(store.selectedContent()).toBeNull();
        }));

        it('should preserve existing content and show generic error when upload fails', fakeAsync(() => {
            const existingContentlets = [createFakeContentlet({ title: 'Existing' })];
            patchState(unprotected(store), {
                content: {
                    data: existingContentlets,
                    status: ComponentStatus.LOADED,
                    error: null
                }
            });

            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });
            dotUploadFileService.uploadDotAsset.mockReturnValue(
                throwError({ status: 500, message: 'Server error' })
            );

            store.uploadFile({ file: mockFile, folderParams });
            tick(50);

            expect(store.content().status).toBe(ComponentStatus.LOADED);
            expect(store.content().error).toBe('dot.file.field.dialog.upload.file.error');
            expect(store.content().data).toEqual(existingContentlets);
        }));

        it('should preserve existing content and show permissions error on 403', fakeAsync(() => {
            const existingContentlets = [createFakeContentlet({ title: 'Existing' })];
            patchState(unprotected(store), {
                content: {
                    data: existingContentlets,
                    status: ComponentStatus.LOADED,
                    error: null
                }
            });

            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });
            dotUploadFileService.uploadDotAsset.mockReturnValue(
                throwError({ status: 403, message: 'Forbidden' })
            );

            store.uploadFile({ file: mockFile, folderParams });
            tick(50);

            expect(store.content().status).toBe(ComponentStatus.LOADED);
            expect(store.content().error).toBe(
                'dot.file.field.dialog.upload.file.error.permissions'
            );
            expect(store.content().data).toEqual(existingContentlets);
        }));
    });
});
