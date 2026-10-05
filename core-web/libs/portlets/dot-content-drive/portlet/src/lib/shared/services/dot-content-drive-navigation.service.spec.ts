import {
    createServiceFactory,
    SpectatorService,
    mockProvider,
    SpyObject
} from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, Mocked, describe, expect, it, vi } from 'vitest';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { Router } from '@angular/router';

import {
    DotContentSearchService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeContentType } from '@dotcms/utils-testing';

import { DotContentDriveNavigationService } from './dot-content-drive-navigation.service';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { SYSTEM_HOST } from '../constants';

/**
 * Builds a mock store exposing what the nav service reads: the active language filter and the
 * environment default, used to pick a version's language. It carries no flags: Content Drive no
 * longer reads the side-panel flag (#37759, FR-006).
 */
const mockStore = () =>
    mockProvider(DotContentDriveStore, {
        getFilterValue: vi.fn().mockReturnValue(undefined),
        defaultLanguageId: vi.fn().mockReturnValue(undefined)
    });

describe('DotContentDriveNavigationService', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: Mocked<Router>;
    let contentTypeService: Mocked<DotContentTypeService>;
    let dotRouterService: Mocked<DotRouterService>;
    let location: SpyObject<Location>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;
    let contentSearch: Mocked<DotContentSearchService>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, {
                navigate: vi.fn()
            }),
            mockProvider(DotContentTypeService, {
                getContentType: vi.fn()
            }),
            mockProvider(DotRouterService, {
                goToEditPage: vi.fn()
            }),
            mockProvider(Location, {
                path: vi.fn()
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotContentSearchService, {
                get: vi.fn()
            }),
            mockStore()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        router = spectator.inject(Router);
        contentTypeService = spectator.inject(DotContentTypeService);
        dotRouterService = spectator.inject(DotRouterService);
        location = spectator.inject(Location);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
        contentSearch = spectator.inject(DotContentSearchService);
        store = spectator.inject(DotContentDriveStore, true);
        // The store mock's functions are shared across tests and `clearAllMocks` keeps their
        // return values, so start every test with no language known.
        store.getFilterValue.mockReturnValue(undefined);
        store.defaultLanguageId.mockReturnValue(undefined);
    });

    afterEach(() => {
        vi.clearAllMocks();
        location.path.mockReset();
    });

    describe('editContent', () => {
        it('should navigate to page editor when baseType is htmlpageasset', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/test-page',
                languageId: 1
            });

            service.editContent(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/test-page',
                language_id: 1
            });
        });

        it('should use url property when urlMap is not available for Pages contentlet', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                url: '/test-page-url',
                languageId: 2
            });

            service.editContent(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/test-page-url',
                language_id: 2
            });
        });

        it('should open the new editor side panel when feature flag is enabled', () => {
            const mockContentlet = createFakeContentlet({
                contentType: 'blog',
                inode: 'test-inode-123',
                identifier: 'test-identifier-123',
                title: 'My Blog Post'
            });

            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                contentletInode: 'test-inode-123',
                identifier: 'test-identifier-123',
                // Recorded so the shareable URL can name the exact version, not just the content.
                languageId: 1,
                title: 'My Blog Post'
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        /**
         * A type that has not opted into the new editor opens the legacy editor in its own Content
         * Drive panel (#37759, FR-001). It never leaves for the Content Search route, so a user without
         * Content Search in their menu can still edit it (FR-004).
         */
        it.each([
            ['turned off', { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }],
            ['missing', {}],
            ['absent', undefined]
        ])('opens the legacy panel when the new-editor setting is %s', (_label, metadata) => {
            // A Content Drive URL with folder params, which the old hand-off carried as CD_ params.
            location.path.mockReturnValue('/content-drive?folderId=123&path=/images');
            contentTypeService.getContentType.mockReturnValue(
                of(createFakeContentType({ id: 'news', name: 'News', metadata }))
            );

            service.editContent(
                createFakeContentlet({
                    contentType: 'news',
                    inode: 'test-inode-456',
                    identifier: 'news-id',
                    languageId: 2,
                    title: 'Breaking'
                })
            );

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('news');
            expect(service.$legacyPanelRequest()).toEqual({
                mode: 'edit',
                inode: 'test-inode-456',
                identifier: 'news-id',
                languageId: 2,
                title: 'Breaking',
                portletId: 'content-drive'
            });
            expect(service.$editPanelRequest()).toBeNull();
            expect(service.$panelLocation()).toEqual({
                kind: 'edit',
                editContent: 'news-id',
                editContentLang: 2
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should surface the error and not navigate when getContentType fails', () => {
            const error = new HttpErrorResponse({ status: 500 });
            const mockContentlet = createFakeContentlet({
                contentType: 'blog',
                inode: 'test-inode-123'
            });

            contentTypeService.getContentType.mockReturnValue(throwError(() => error));

            service.editContent(mockContentlet);

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(router.navigate).not.toHaveBeenCalled();
        });
    });

    describe('createContent', () => {
        it('should open the new editor side panel when feature flag is enabled and no folder given', () => {
            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('blog');

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
            expect(service.$editPanelRequest()).toEqual({
                mode: 'new',
                contentTypeId: 'blog',
                folderPath: undefined,
                title: 'Blog'
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should forward folderPath to the new editor side panel so it is created in the current folder', () => {
            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('blog', { folderPath: 'demo.dotcms.com/about-us/' });

            expect(service.$editPanelRequest()).toEqual({
                mode: 'new',
                contentTypeId: 'blog',
                folderPath: 'demo.dotcms.com/about-us/',
                title: 'Blog'
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        /**
         * A type that has not opted into the new editor opens the legacy create form in its own
         * Content Drive panel (#37759, FR-002), in the folder being browsed, never the Content Search
         * route and never with the old CD_ params.
         */
        it.each([
            ['turned off', { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }],
            ['missing', {}]
        ])(
            'opens the legacy create panel when the new-editor setting is %s',
            (_label, metadata) => {
                location.path.mockReturnValue('/content-drive?path=/foo&filters=bar');
                store.defaultLanguageId.mockReturnValue(1);
                contentTypeService.getContentType.mockReturnValue(
                    of(
                        createFakeContentType({
                            id: 'news',
                            variable: 'news',
                            name: 'News',
                            metadata
                        })
                    )
                );

                service.createContent('news', {
                    folderPath: 'demo.dotcms.com/foo/',
                    folderInode: 'inode-1'
                });

                expect(service.$legacyPanelRequest()).toEqual({
                    mode: 'new',
                    contentTypeVariable: 'news',
                    folderInode: 'inode-1',
                    languageId: 1,
                    title: 'News',
                    portletId: 'content-drive'
                });
                expect(service.$editPanelRequest()).toBeNull();
                expect(service.$panelLocation()).toEqual({ kind: 'create', createContent: 'news' });
                expect(router.navigate).not.toHaveBeenCalled();
            }
        );

        /**
         * Every create starts in the language the list is filtered by, so the new content shows in
         * the list once saved; with no language filter, in Content Drive's default (FR-003).
         */
        describe('starting language', () => {
            const typeWith = (newEditor: boolean) =>
                createFakeContentType({
                    id: 'blog',
                    variable: 'blog',
                    name: 'Blog',
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: newEditor }
                });

            it.each([
                ['the legacy editor', false],
                ['the new editor', true]
            ])('starts in the first filtered language in %s', (_label, newEditor) => {
                store.getFilterValue.mockReturnValue(['3', '4']);
                store.defaultLanguageId.mockReturnValue(1);
                contentTypeService.getContentType.mockReturnValue(of(typeWith(newEditor)));

                service.createContent('blog');

                const request = service.$legacyPanelRequest() ?? service.$editPanelRequest();
                expect(request?.languageId).toBe(3);
            });

            it.each([
                ['the legacy editor', false],
                ['the new editor', true]
            ])(
                'starts in the default language when nothing is filtered, in %s',
                (_label, newEditor) => {
                    store.getFilterValue.mockReturnValue(undefined);
                    store.defaultLanguageId.mockReturnValue(2);
                    contentTypeService.getContentType.mockReturnValue(of(typeWith(newEditor)));

                    service.createContent('blog');

                    const request = service.$legacyPanelRequest() ?? service.$editPanelRequest();
                    expect(request?.languageId).toBe(2);
                }
            );

            it('ignores the language of the current URL, which names the open edit, not the list', () => {
                location.path.mockReturnValue('/content-drive?editContent=id-1&editContentLang=5');
                store.getFilterValue.mockReturnValue(undefined);
                store.defaultLanguageId.mockReturnValue(2);
                contentTypeService.getContentType.mockReturnValue(of(typeWith(false)));

                service.createContent('blog');

                expect(service.$legacyPanelRequest()?.languageId).toBe(2);
            });
        });

        it('should surface the error and not navigate when getContentType fails', () => {
            const error = new HttpErrorResponse({ status: 500 });

            location.path.mockReturnValue('/content-drive?path=/foo');
            contentTypeService.getContentType.mockReturnValue(throwError(() => error));

            service.createContent('blog');

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(router.navigate).not.toHaveBeenCalled();
        });
    });

    describe('editPage', () => {
        it('should navigate to edit page with urlMap when available', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/about-us',
                url: '/fallback-url',
                languageId: 1
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/about-us',
                language_id: 1
            });
        });

        it('should navigate to edit page with url when urlMap is not available', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                url: '/contact',
                languageId: 2
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/contact',
                language_id: 2
            });
        });

        it('should prefer urlMap over url when both are available', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/primary-url',
                url: '/secondary-url',
                languageId: 3
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/primary-url',
                language_id: 3
            });
        });

        it('should handle empty urlMap and fallback to url', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '',
                url: '/home',
                languageId: 1
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/home',
                language_id: 1
            });
        });

        it('should handle undefined urlMap and use url', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: undefined,
                url: '/services',
                languageId: 4
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/services',
                language_id: 4
            });
        });

        it('should pass correct language_id parameter', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/blog-post',
                languageId: 5
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/blog-post',
                language_id: 5
            });
        });
    });

    /**
     * One source signal says what was asked to open; the shell reads two derived views of it (one per
     * panel) plus `$panelLocation`, which says what the URL must show. Keeping the location separate is
     * what lets it follow the panel later (first save, language switch) without remounting it (#37759).
     */
    describe('panel request and URL location', () => {
        const NEW_EDITOR_TYPE = createFakeContentType({
            id: 'blog',
            variable: 'blog',
            name: 'Blog',
            metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
        });

        it('starts with nothing open and no location', () => {
            expect(service.$editPanelRequest()).toBeNull();
            expect(service.$legacyPanelRequest()).toBeNull();
            expect(service.$panelLocation()).toBeNull();
        });

        it('exposes a new-editor edit through $editPanelRequest only, and names it in the location', () => {
            contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_TYPE));

            service.editContent(
                createFakeContentlet({
                    contentType: 'blog',
                    inode: 'inode-1',
                    identifier: 'id-1',
                    languageId: 2
                })
            );

            expect(service.$editPanelRequest()).toEqual(
                expect.objectContaining({ mode: 'edit', contentletInode: 'inode-1' })
            );
            expect(service.$legacyPanelRequest()).toBeNull();
            expect(service.$panelLocation()).toEqual({
                kind: 'edit',
                editContent: 'id-1',
                editContentLang: 2
            });
        });

        it('names a create by its content type variable in the location', () => {
            contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_TYPE));

            service.createContent('blog');

            expect(service.$panelLocation()).toEqual({ kind: 'create', createContent: 'blog' });
        });

        it('names a deep-linked edit by identifier and the resolved language', () => {
            contentSearch.get.mockReturnValue(
                of({
                    jsonObjectView: {
                        contentlets: [
                            createFakeContentlet({
                                inode: 'es-inode',
                                identifier: 'id-1',
                                languageId: 3
                            })
                        ]
                    }
                })
            );

            contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_TYPE));

            service.openEditByIdentifier('id-1', 3);

            expect(service.$panelLocation()).toEqual({
                kind: 'edit',
                editContent: 'id-1',
                editContentLang: 3
            });
        });

        /**
         * "Switch to the old editor" in the new-editor panel turns the new editor off for the type,
         * then reopens the same content, in the same language, in the legacy panel (#37759,
         * FR-028). The type was just switched, so it is not looked up again.
         */
        it('reopens the same content in the legacy panel on a switch to the old editor', () => {
            contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_TYPE));
            service.editContent(
                createFakeContentlet({
                    contentType: 'blog',
                    inode: 'inode-1',
                    identifier: 'id-1',
                    languageId: 2,
                    title: 'My Blog Post'
                })
            );
            const locationBefore = service.$panelLocation();
            contentTypeService.getContentType.mockClear();

            service.switchToLegacyEditor(
                createFakeContentlet({
                    contentType: 'blog',
                    inode: 'inode-1',
                    identifier: 'id-1',
                    languageId: 2,
                    title: 'My Blog Post'
                })
            );

            expect(service.$legacyPanelRequest()).toEqual({
                mode: 'edit',
                inode: 'inode-1',
                identifier: 'id-1',
                languageId: 2,
                title: 'My Blog Post',
                portletId: 'content-drive'
            });
            expect(service.$editPanelRequest()).toBeNull();
            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
            // Same content, same language: the URL already says so.
            expect(service.$panelLocation()).toEqual(locationBefore);
        });

        /**
         * The location follows the panel after it opened, without touching the request: replacing
         * the request would remount the panel and reload the editor under the author (#37759,
         * FR-020, FR-025).
         */
        describe('following the open panel', () => {
            const openCreate = () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_TYPE));
                service.createContent('blog');
            };

            const openEdit = () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_TYPE));
                service.editContent(
                    createFakeContentlet({
                        contentType: 'blog',
                        inode: 'inode-1',
                        identifier: 'id-1',
                        languageId: 1
                    })
                );
            };

            it('names the saved content once a create is saved for the first time', () => {
                openCreate();
                const request = service.$editPanelRequest();

                service.panelSaved({ identifier: 'id-9', languageId: 2 });

                expect(service.$panelLocation()).toEqual({
                    kind: 'edit',
                    editContent: 'id-9',
                    editContentLang: 2
                });
                expect(service.$editPanelRequest()).toBe(request);
            });

            it('leaves an edit where it is on a later save', () => {
                openEdit();

                service.panelSaved({ identifier: 'id-1', languageId: 1 });

                expect(service.$panelLocation()).toEqual({
                    kind: 'edit',
                    editContent: 'id-1',
                    editContentLang: 1
                });
            });

            it('follows a language switch in the open edit', () => {
                openEdit();
                const request = service.$editPanelRequest();

                service.panelLanguageChanged(3);

                expect(service.$panelLocation()).toEqual({
                    kind: 'edit',
                    editContent: 'id-1',
                    editContentLang: 3
                });
                expect(service.$editPanelRequest()).toBe(request);
            });

            it('ignores a language switch while creating, which has no content to name yet', () => {
                openCreate();

                service.panelLanguageChanged(3);

                expect(service.$panelLocation()).toEqual({ kind: 'create', createContent: 'blog' });
            });

            it('does nothing with no panel open', () => {
                service.panelSaved({ identifier: 'id-9', languageId: 2 });
                service.panelLanguageChanged(3);

                expect(service.$panelLocation()).toBeNull();
            });
        });

        it('clears both the request and the location on close', () => {
            contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_TYPE));
            service.createContent('blog');

            service.closeEditPanel();

            expect(service.$editPanelRequest()).toBeNull();
            expect(service.$legacyPanelRequest()).toBeNull();
            expect(service.$panelLocation()).toBeNull();
        });
    });

    describe('openEditByIdentifier', () => {
        const typeWith = (newEditor: boolean) =>
            createFakeContentType({
                id: 'banner',
                variable: 'Banner',
                name: 'Banner',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: newEditor }
            });

        // The link names content, not an editor: the content type picks it (#37759, FR-021). The
        // tests that follow are about resolving the version, so they default to a new-editor type.
        beforeEach(() => {
            contentTypeService.getContentType.mockReturnValue(of(typeWith(true)));
        });

        it('should resolve the identifier to its working inode and open the edit panel', () => {
            const resolved = createFakeContentlet({
                inode: 'working-inode-1',
                identifier: 'shared-identifier',
                title: 'Shared Content'
            });
            contentSearch.get.mockReturnValue(of({ jsonObjectView: { contentlets: [resolved] } }));

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith(
                expect.objectContaining({ query: '+identifier:shared-identifier +working:true' })
            );
            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                contentletInode: 'working-inode-1',
                identifier: 'shared-identifier',
                languageId: 1,
                title: 'Shared Content'
            });
        });

        /** Answers each `contentSearch.get` call in order, so a fallback lookup can be simulated. */
        const answerWith = (...batches: DotCMSContentlet[][]) => {
            let call = 0;
            contentSearch.get.mockImplementation(() =>
                of({ jsonObjectView: { contentlets: batches[call++] ?? [] } })
            );
        };

        it('should look up the exact version the URL asked for', () => {
            // One identifier has one inode PER LANGUAGE, so the identifier alone does not name a
            // version. Pinning the query is exact and has no result-window ceiling.
            answerWith([createFakeContentlet({ inode: 'es-inode', languageId: 2 })]);

            service.openEditByIdentifier('shared-identifier', 2);

            expect(contentSearch.get).toHaveBeenCalledWith({
                query: '+identifier:shared-identifier +working:true +languageId:2',
                limit: 1
            });
            expect(service.$editPanelRequest()).toEqual(
                expect.objectContaining({ contentletInode: 'es-inode', languageId: 2 })
            );
        });

        it('should prefer the URL language over the active filter', () => {
            store.getFilterValue.mockReturnValue(['1']);
            answerWith([createFakeContentlet({ inode: 'es-inode', languageId: 2 })]);

            service.openEditByIdentifier('shared-identifier', 2);

            expect(contentSearch.get).toHaveBeenCalledWith(
                expect.objectContaining({
                    query: '+identifier:shared-identifier +working:true +languageId:2'
                })
            );
        });

        it('should fall back to the drive language, then the environment default', () => {
            store.getFilterValue.mockReturnValue(['3']);
            answerWith([createFakeContentlet({ inode: 'i' })]);

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith(
                expect.objectContaining({
                    query: '+identifier:shared-identifier +working:true +languageId:3'
                })
            );

            contentSearch.get.mockClear();
            store.getFilterValue.mockReturnValue(undefined);
            store.defaultLanguageId.mockReturnValue(4);
            answerWith([createFakeContentlet({ inode: 'i' })]);

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith(
                expect.objectContaining({
                    query: '+identifier:shared-identifier +working:true +languageId:4'
                })
            );
        });

        it('should still open another language when the preferred one has no version', () => {
            // A pinned query returns nothing for a language the content was never translated into, and
            // "nothing" must not be read as "do not open" — a shared link to English-only content would
            // silently do nothing on a Spanish-default environment.
            answerWith([], [createFakeContentlet({ inode: 'en-inode', languageId: 1 })]);

            service.openEditByIdentifier('shared-identifier', 2);

            expect(contentSearch.get).toHaveBeenLastCalledWith({
                query: '+identifier:shared-identifier +working:true',
                limit: 1
            });
            expect(service.$editPanelRequest()).toEqual(
                expect.objectContaining({ contentletInode: 'en-inode' })
            );
        });

        it('should not run a second lookup when no language is known at all', () => {
            store.getFilterValue.mockReturnValue(undefined);
            store.defaultLanguageId.mockReturnValue(undefined);
            answerWith([createFakeContentlet({ inode: 'i' })]);

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledTimes(1);
            expect(contentSearch.get).toHaveBeenCalledWith({
                query: '+identifier:shared-identifier +working:true',
                limit: 1
            });
        });

        it('should not open the panel when the identifier resolves to nothing', () => {
            contentSearch.get.mockReturnValue(of({ jsonObjectView: { contentlets: [] } }));

            service.openEditByIdentifier('missing-identifier');

            expect(service.$editPanelRequest()).toBeNull();
        });

        it('should surface the error and not open the panel when the search fails', () => {
            const error = new HttpErrorResponse({ status: 500 });
            contentSearch.get.mockReturnValue(throwError(() => error));

            service.openEditByIdentifier('shared-identifier');

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(service.$editPanelRequest()).toBeNull();
        });

        /**
         * Every screen outside Content Drive reaches legacy-editor content through this link (Part
         * 1's redirect of `c/content/<inode>`), so it must open the editor the content type chose,
         * never the new editor it opted out of (#37759, FR-021, SC-007).
         */
        describe('picking the editor', () => {
            const BANNER = createFakeContentlet({
                contentType: 'Banner',
                inode: 'banner-inode',
                identifier: 'banner-id',
                languageId: 2,
                title: 'Spring sale'
            });

            beforeEach(() => {
                contentSearch.get.mockReturnValue(
                    of({ jsonObjectView: { contentlets: [BANNER] } })
                );
            });

            it("looks up the resolved content's type", () => {
                service.openEditByIdentifier('banner-id', 2);

                expect(contentTypeService.getContentType).toHaveBeenCalledWith('Banner');
            });

            it('opens legacy-editor content in the legacy panel, never the new editor', () => {
                contentTypeService.getContentType.mockReturnValue(of(typeWith(false)));

                service.openEditByIdentifier('banner-id', 2);

                expect(service.$legacyPanelRequest()).toEqual({
                    mode: 'edit',
                    inode: 'banner-inode',
                    identifier: 'banner-id',
                    languageId: 2,
                    title: 'Spring sale',
                    portletId: 'content-drive'
                });
                expect(service.$editPanelRequest()).toBeNull();
                expect(service.$panelLocation()).toEqual({
                    kind: 'edit',
                    editContent: 'banner-id',
                    editContentLang: 2
                });
            });

            it('opens new-editor content in the new-editor panel, as before', () => {
                service.openEditByIdentifier('banner-id', 2);

                expect(service.$editPanelRequest()).toEqual(
                    expect.objectContaining({ mode: 'edit', contentletInode: 'banner-inode' })
                );
                expect(service.$legacyPanelRequest()).toBeNull();
            });

            it('keeps the language fallback for legacy-editor content', () => {
                contentTypeService.getContentType.mockReturnValue(of(typeWith(false)));
                answerWith(
                    [],
                    [
                        createFakeContentlet({
                            contentType: 'Banner',
                            inode: 'en-inode',
                            languageId: 1
                        })
                    ]
                );

                service.openEditByIdentifier('banner-id', 2);

                expect(service.$legacyPanelRequest()).toEqual(
                    expect.objectContaining({ inode: 'en-inode', languageId: 1 })
                );
            });

            it('shows the standard error and opens nothing when the type lookup fails', () => {
                const error = new HttpErrorResponse({ status: 404 });
                contentTypeService.getContentType.mockReturnValue(throwError(() => error));

                service.openEditByIdentifier('banner-id', 2);

                expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
                expect(service.$legacyPanelRequest()).toBeNull();
                expect(service.$editPanelRequest()).toBeNull();
                expect(service.$panelLocation()).toBeNull();
            });
        });
    });
});

/**
 * Content Drive no longer reads the side-panel flag (#37759, FR-006): both editors always open in a
 * panel, and the content type's editor setting alone decides which one. Run against a store with the
 * flag off and against one that carries no flags at all, so neither a stale flag nor its absence can
 * change the routing.
 */
describe.each([
    [
        'the side-panel flag off',
        { flags: signal({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: false }) }
    ],
    ['no flags at all', {}]
])('DotContentDriveNavigationService with %s', (_label, storeOverrides) => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: Mocked<Router>;
    let contentTypeService: Mocked<DotContentTypeService>;

    const typeWith = (newEditor: boolean) =>
        createFakeContentType({
            id: 'blog',
            variable: 'blog',
            name: 'Blog',
            metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: newEditor }
        });

    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, { navigate: vi.fn() }),
            mockProvider(DotContentTypeService, { getContentType: vi.fn() }),
            mockProvider(DotRouterService, { goToEditPage: vi.fn() }),
            mockProvider(Location, { path: vi.fn() }),
            mockProvider(DotHttpErrorManagerService, {
                handle: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotContentSearchService, { get: vi.fn() }),
            mockProvider(DotContentDriveStore, {
                getFilterValue: vi.fn().mockReturnValue(undefined),
                defaultLanguageId: vi.fn().mockReturnValue(undefined),
                ...storeOverrides
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        router = spectator.inject(Router);
        contentTypeService = spectator.inject(DotContentTypeService);
    });

    it('opens new-editor content in the new-editor panel, never the full-screen editor', () => {
        contentTypeService.getContentType.mockReturnValue(of(typeWith(true)));

        service.editContent(createFakeContentlet({ contentType: 'blog', inode: 'inode-x' }));

        expect(service.$editPanelRequest()).toEqual(
            expect.objectContaining({ mode: 'edit', contentletInode: 'inode-x' })
        );
        expect(router.navigate).not.toHaveBeenCalled();
    });

    it('opens legacy-editor content in the legacy panel', () => {
        contentTypeService.getContentType.mockReturnValue(of(typeWith(false)));

        service.editContent(createFakeContentlet({ contentType: 'blog', inode: 'inode-x' }));

        expect(service.$legacyPanelRequest()).toEqual(
            expect.objectContaining({ mode: 'edit', inode: 'inode-x' })
        );
        expect(router.navigate).not.toHaveBeenCalled();
    });

    it('opens a deep-linked new-editor content in the panel, never the full-screen editor', () => {
        contentTypeService.getContentType.mockReturnValue(of(typeWith(true)));
        const contentSearch = spectator.inject(DotContentSearchService);
        (contentSearch.get as Mock).mockReturnValue(
            of({ jsonObjectView: { contentlets: [createFakeContentlet({ inode: 'inode-y' })] } })
        );

        service.openEditByIdentifier('shared-identifier');

        expect(router.navigate).not.toHaveBeenCalled();
    });
});

/**
 * Where a create from Content Drive puts the new content: the folder being browsed. Shared by the
 * create action and by `createContent` links, so both land in the same place (#37759, FR-003).
 */
describe('DotContentDriveNavigationService.currentFolder', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    const systemHostSelected = signal(false);
    const currentSite = vi.fn();
    const path = vi.fn();
    const selectedNode = vi.fn();

    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, { navigate: vi.fn() }),
            mockProvider(DotContentTypeService, { getContentType: vi.fn() }),
            mockProvider(DotRouterService, { goToEditPage: vi.fn() }),
            mockProvider(Location, { path: vi.fn() }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotContentSearchService, { get: vi.fn() }),
            mockProvider(DotContentDriveStore, {
                getFilterValue: vi.fn().mockReturnValue(undefined),
                defaultLanguageId: vi.fn().mockReturnValue(undefined),
                $systemHostSelected: systemHostSelected,
                currentSite,
                path,
                selectedNode
            })
        ]
    });

    beforeEach(() => {
        systemHostSelected.set(false);
        currentSite.mockReturnValue({ hostname: 'demo.dotcms.com', identifier: 'site-1' });
        path.mockReturnValue('/about-us/');
        selectedNode.mockReturnValue({ data: { type: 'folder', inode: 'inode-1' } });
        spectator = createService();
    });

    it('names the browsed folder: its host path for the new editor, its inode for the legacy one', () => {
        expect(spectator.service.currentFolder()).toEqual({
            folderPath: 'demo.dotcms.com/about-us/',
            folderInode: 'inode-1'
        });
    });

    it('falls back to the site when browsing the root', () => {
        path.mockReturnValue(undefined);
        selectedNode.mockReturnValue({ data: { inode: '' } });

        expect(spectator.service.currentFolder()).toEqual({
            folderPath: 'demo.dotcms.com',
            folderInode: undefined
        });
    });

    it('uses the site node inode at the site root', () => {
        path.mockReturnValue('');
        selectedNode.mockReturnValue({ data: { type: 'site', inode: 'site-inode' } });

        expect(spectator.service.currentFolder()).toEqual({
            folderPath: 'demo.dotcms.com',
            folderInode: 'site-inode'
        });
    });

    it('names System Host by its identifier, never pasted onto the site hostname', () => {
        systemHostSelected.set(true);

        expect(spectator.service.currentFolder()).toEqual({ folderInode: SYSTEM_HOST.identifier });
    });
});
