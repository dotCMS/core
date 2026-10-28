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
    DotActionUrlService,
    DotContentSearchService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import { EDIT_CONTENT_NAVIGATION_OVERRIDE } from '@dotcms/edit-content';
import { createFakeContentlet, createFakeContentType } from '@dotcms/utils-testing';

import {
    DotContentDriveNavigationService,
    provideContentDriveNavigationOverride
} from './dot-content-drive-navigation.service';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { SYSTEM_HOST } from '../constants';

/**
 * The environment default language. Held apart from the store mock because the store types it as
 * a `Signal`, which a test cannot call `mockReturnValue` on.
 */
const defaultLanguageId = vi.fn<() => number | undefined>();

/**
 * Builds a mock store exposing what the nav service reads: the active language filter and the
 * environment default, used to pick a version's language, and the folder being browsed. It carries
 * no flags: Content Drive reads no side-panel flag.
 */
const mockStore = () =>
    mockProvider(DotContentDriveStore, {
        getFilterValue: vi.fn().mockReturnValue(undefined),
        defaultLanguageId,
        // The folder a create lands in: a site root, outside System Host.
        $systemHostSelected: signal(false),
        currentSite: vi.fn().mockReturnValue({ hostname: 'demo.dotcms.com' }),
        path: vi.fn().mockReturnValue(''),
        selectedNode: vi.fn().mockReturnValue(undefined)
    });

/** What `/api/v1/portlet/_actionurl/<type>` answers: the legacy create screen for a type. */
const CREATE_URL = '/c/portal/layout?p_p_id=content&_content_cmd=new&selectedStructure=news';

/** A content type that opted into the new editor. */
const NEW_EDITOR_TYPE = createFakeContentType({
    id: 'blog',
    variable: 'blog',
    name: 'Blog',
    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
});

/** A content type on the legacy editor. */
const LEGACY_TYPE = createFakeContentType({
    id: 'news',
    variable: 'news',
    name: 'News',
    metadata: {}
});

/**
 * Answers a content type lookup the way the server would: `news` is on the legacy editor, every
 * other type opted into the new one.
 *
 * Content Drive no longer chooses the editor (#38303): it hands one request to the opener, which
 * looks the type up. The lookup is answered here only so code that still asks reaches the request
 * it builds; the tests below assert that it is never asked.
 */
const answerTypeLookup = (variable: string) =>
    of(variable === LEGACY_TYPE.variable ? LEGACY_TYPE : NEW_EDITOR_TYPE);

describe('DotContentDriveNavigationService', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: Mocked<Router>;
    let contentTypeService: Mocked<DotContentTypeService>;
    let actionUrlService: Mocked<DotActionUrlService>;
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
            provideContentDriveNavigationOverride(),
            mockProvider(DotActionUrlService, {
                getCreateContentletUrl: vi.fn().mockReturnValue(of(CREATE_URL))
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
        actionUrlService = spectator.inject(DotActionUrlService);
        dotRouterService = spectator.inject(DotRouterService);
        location = spectator.inject(Location);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
        contentSearch = spectator.inject(DotContentSearchService);
        store = spectator.inject(DotContentDriveStore);
        // The store mock's functions are shared across tests and `clearAllMocks` keeps their
        // return values, so start every test with no language known.
        store.getFilterValue.mockReturnValue(undefined);
        defaultLanguageId.mockReturnValue(undefined);
        // Same for the folder being browsed: every test starts at the site root.
        store.path.mockReturnValue('');
        store.selectedNode.mockReturnValue(undefined);
        contentTypeService.getContentType.mockImplementation(answerTypeLookup);
        (actionUrlService.getCreateContentletUrl as Mock).mockReturnValue(of(CREATE_URL));
    });

    afterEach(() => {
        vi.clearAllMocks();
        location.path.mockReset();
    });

    describe('editPageProperties', () => {
        it("should open the page's content form in the side panel, not the page editor", () => {
            const page = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                contentType: 'htmlpageasset',
                inode: 'page-inode',
                identifier: 'page-identifier',
                languageId: 1,
                title: 'A Page',
                url: '/a-page'
            });

            service.editPageProperties(page);

            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                inode: 'page-inode',
                identifier: 'page-identifier',
                contentType: 'htmlpageasset',
                languageId: 1,
                title: 'A Page',
                portletId: 'content-drive'
            });
            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
            expect(dotRouterService.goToEditPage).not.toHaveBeenCalled();
        });
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

        /**
         * One request names the content and never the editor: the opener picks the editor from the
         * content type (#38303, FR-001, FR-003). So content of either type produces the same shape,
         * and Content Drive never leaves for another route.
         */
        it.each([
            ['a type that opted into the new editor', NEW_EDITOR_TYPE.variable],
            ['a type on the legacy editor', LEGACY_TYPE.variable]
        ])('should open one edit request for content of %s', (_label, contentType) => {
            service.editContent(
                createFakeContentlet({
                    contentType,
                    inode: 'inode-1',
                    identifier: 'id-1',
                    languageId: 2,
                    title: 'Breaking'
                })
            );

            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                inode: 'inode-1',
                identifier: 'id-1',
                contentType,
                languageId: 2,
                title: 'Breaking',
                portletId: 'content-drive'
            });
            expect(service.$panelLocation()).toEqual({
                kind: 'edit',
                editContent: 'id-1',
                editContentLang: 2
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should leave the editor choice to the opener, without looking the type up', () => {
            service.editContent(
                createFakeContentlet({ contentType: LEGACY_TYPE.variable, inode: 'inode-1' })
            );

            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
        });
    });

    describe('createContent', () => {
        /** Browsing a folder below the site root, so a create names it both ways. */
        const browseFolder = () => {
            store.path.mockReturnValue('/foo/');
            store.selectedNode.mockReturnValue({
                data: {
                    type: 'folder',
                    id: 'folder-1',
                    inode: 'inode-1',
                    path: '/foo/',
                    hostname: 'demo.dotcms.com'
                }
            });
        };

        /**
         * One create request for either editor, in the folder being browsed: the folder path the
         * new editor pre-fills and the folder inode the legacy create screen pre-selects (#38303).
         * No title: the service no longer looks the type up, and the opener heads a create with the
         * type's name.
         */
        it.each([
            ['a type that opted into the new editor', NEW_EDITOR_TYPE.variable],
            ['a type on the legacy editor', LEGACY_TYPE.variable]
        ])(
            'should open one create request for %s, in the folder being browsed',
            (_label, contentType) => {
                location.path.mockReturnValue('/content-drive?path=/foo&filters=bar');
                defaultLanguageId.mockReturnValue(1);
                browseFolder();

                service.createContent(contentType);

                expect(service.$editPanelRequest()).toEqual({
                    mode: 'new',
                    contentType,
                    folderPath: 'demo.dotcms.com/foo/',
                    folderInode: 'inode-1',
                    languageId: 1,
                    portletId: 'content-drive'
                });
                expect(service.$panelLocation()).toEqual({
                    kind: 'create',
                    createContent: contentType
                });
                expect(router.navigate).not.toHaveBeenCalled();
            }
        );

        it('should create at the site root with the site path and no folder inode', () => {
            service.createContent(NEW_EDITOR_TYPE.variable);

            expect(service.$editPanelRequest()).toEqual({
                mode: 'new',
                contentType: NEW_EDITOR_TYPE.variable,
                folderPath: 'demo.dotcms.com',
                portletId: 'content-drive'
            });
        });

        /**
         * The opener resolves the editor and, for a legacy create, the create screen, so a refused
         * create shows the standard error and opens no editor there (#38303). The service asks the
         * server for neither.
         */
        it('should leave the editor choice and the create screen to the opener', () => {
            service.createContent(LEGACY_TYPE.variable);

            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
            expect(actionUrlService.getCreateContentletUrl).not.toHaveBeenCalled();
        });

        /**
         * Every create starts in the language the list is filtered by, so the new content shows in
         * the list once saved; with no language filter, in Content Drive's default (FR-003). The
         * same for either editor, since the request does not name one.
         */
        describe('starting language', () => {
            it.each([
                ['a new-editor type', NEW_EDITOR_TYPE.variable],
                ['a legacy-editor type', LEGACY_TYPE.variable]
            ])('should start in the first filtered language for %s', (_label, contentType) => {
                store.getFilterValue.mockReturnValue(['3', '4']);
                defaultLanguageId.mockReturnValue(1);

                service.createContent(contentType);

                expect(service.$editPanelRequest()?.languageId).toBe(3);
            });

            it.each([
                ['a new-editor type', NEW_EDITOR_TYPE.variable],
                ['a legacy-editor type', LEGACY_TYPE.variable]
            ])(
                'should start in the default language when nothing is filtered, for %s',
                (_label, contentType) => {
                    store.getFilterValue.mockReturnValue(undefined);
                    defaultLanguageId.mockReturnValue(2);

                    service.createContent(contentType);

                    expect(service.$editPanelRequest()?.languageId).toBe(2);
                }
            );

            it('should ignore the language of the current URL, which names the open edit, not the list', () => {
                location.path.mockReturnValue('/content-drive?editContent=id-1&editContentLang=5');
                store.getFilterValue.mockReturnValue(undefined);
                defaultLanguageId.mockReturnValue(2);

                service.createContent(LEGACY_TYPE.variable);

                expect(service.$editPanelRequest()?.languageId).toBe(2);
            });
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
     * One signal says what was asked to open, whichever editor will show it, plus `$panelLocation`,
     * which says what the URL must show. Keeping the location separate is what lets it follow the
     * panel later (first save, language switch) without remounting it (#37759, #38303).
     */
    describe('panel request and URL location', () => {
        it('starts with nothing open and no location', () => {
            expect(service.$editPanelRequest()).toBeNull();
            expect(service.$panelLocation()).toBeNull();
        });

        it('exposes every request through one signal, with no second one for the legacy editor', () => {
            expect(service).not.toHaveProperty('$legacyPanelRequest');
        });

        it('exposes an edit through $editPanelRequest and names it in the location', () => {
            service.editContent(
                createFakeContentlet({
                    contentType: 'blog',
                    inode: 'inode-1',
                    identifier: 'id-1',
                    languageId: 2
                })
            );

            expect(service.$editPanelRequest()).toEqual(
                expect.objectContaining({ mode: 'edit', inode: 'inode-1' })
            );
            expect(service.$panelLocation()).toEqual({
                kind: 'edit',
                editContent: 'id-1',
                editContentLang: 2
            });
        });

        it('names a create by its content type variable in the location', () => {
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

            service.openEditByIdentifier('id-1', 3);

            expect(service.$panelLocation()).toEqual({
                kind: 'edit',
                editContent: 'id-1',
                editContentLang: 3
            });
        });

        /**
         * The location follows the panel after it opened, without touching the request: replacing
         * the request would remount the panel and reload the editor under the author (#37759,
         * FR-020, FR-025).
         */
        describe('following the open panel', () => {
            const openCreate = () => {
                service.createContent('blog');
            };

            const openEdit = () => {
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

            // A new translation saved in the new editor: the saved content names the language
            // (#37759, FR-020, T124).
            it('follows the language a save names in an open edit', () => {
                openEdit();
                const request = service.$editPanelRequest();

                service.panelSaved({ identifier: 'id-1', languageId: 4 });

                expect(service.$panelLocation()).toEqual({
                    kind: 'edit',
                    editContent: 'id-1',
                    editContentLang: 4
                });
                expect(service.$editPanelRequest()).toBe(request);
            });

            // A save of related content reached in place names another content: its language is
            // not the language of the content the URL names (#37759, FR-020, T131).
            it('ignores the language of a save that names another content', () => {
                openEdit();

                service.panelSaved({ identifier: 'id-9', languageId: 4 });

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

        /**
         * Content Drive takes over how its new-editor panel leaves for the legacy editor or after
         * a load error (#37759, FR-028, FR-029). It answers only for the panel it opened, named by
         * what that editor was opened with, so a nested editor (a related content opened from a
         * relationship field) declines and navigates as on main.
         *
         * Switching reopens the content as a fresh request, the same shape as any other: the request
         * never names an editor, so the opener resolves the type again and finds the legacy editor
         * it was just set back to (#38303).
         */
        describe('as the editor navigation override', () => {
            const BLOG_POST = createFakeContentlet({
                contentType: 'blog',
                inode: 'inode-1',
                identifier: 'id-1',
                languageId: 2,
                title: 'My Blog Post'
            });

            const openEdit = () => {
                service.editContent(BLOG_POST);
                contentTypeService.getContentType.mockClear();
            };

            it('is what the shell provides for the editor', () => {
                expect(spectator.inject(EDIT_CONTENT_NAVIGATION_OVERRIDE)).toBe(service);
            });

            it('reopens its edit, in the same language, as a fresh request', () => {
                openEdit();
                const before = service.$editPanelRequest();
                const locationBefore = service.$panelLocation();

                const handled = service.switchToLegacyEditor(
                    { inode: 'inode-1' },
                    BLOG_POST,
                    'blog'
                );

                expect(handled).toBe(true);
                expect(service.$editPanelRequest()).toEqual({
                    mode: 'edit',
                    inode: 'inode-1',
                    identifier: 'id-1',
                    contentType: 'blog',
                    languageId: 2,
                    title: 'My Blog Post',
                    portletId: 'content-drive'
                });
                // A new request, so the panel reopens rather than keeping the new editor.
                expect(service.$editPanelRequest()).not.toBe(before);
                // The type was just switched; the service does not look it up.
                expect(contentTypeService.getContentType).not.toHaveBeenCalled();
                // Same content, same language: the URL already says so.
                expect(service.$panelLocation()).toEqual(locationBefore);
            });

            // Inside the panel, related content opens in place, so the panel opened for one content
            // can show another. The URL names what reopens (#37759, FR-020, T131).
            it('names the switched content when the panel moved to related content', () => {
                openEdit();

                const handled = service.switchToLegacyEditor(
                    { inode: 'inode-1' },
                    createFakeContentlet({
                        inode: 'inode-9',
                        identifier: 'id-9',
                        languageId: 3,
                        title: 'Related'
                    }),
                    'news'
                );

                expect(handled).toBe(true);
                expect(service.$editPanelRequest()).toEqual(
                    expect.objectContaining({
                        mode: 'edit',
                        inode: 'inode-9',
                        identifier: 'id-9',
                        contentType: 'news'
                    })
                );
                expect(service.$panelLocation()).toEqual({
                    kind: 'edit',
                    editContent: 'id-9',
                    editContentLang: 3
                });
            });

            it('reopens its create as a fresh create of the same type, in the same language', () => {
                defaultLanguageId.mockReturnValue(2);
                service.createContent('blog');
                const before = service.$editPanelRequest();

                const handled = service.switchToLegacyEditor(
                    { contentTypeId: 'blog' },
                    null,
                    'blog'
                );

                expect(handled).toBe(true);
                expect(service.$editPanelRequest()).toEqual(
                    expect.objectContaining({
                        mode: 'new',
                        contentType: 'blog',
                        languageId: 2,
                        portletId: 'content-drive'
                    })
                );
                expect(service.$editPanelRequest()).not.toBe(before);
                // The opener resolves the legacy create screen, not the service.
                expect(actionUrlService.getCreateContentletUrl).not.toHaveBeenCalled();
                expect(service.$panelLocation()).toEqual({ kind: 'create', createContent: 'blog' });
            });

            it('declines a switch from another editor and leaves its panel alone', () => {
                openEdit();
                const request = service.$editPanelRequest();

                const handled = service.switchToLegacyEditor(
                    { inode: 'related-inode' },
                    createFakeContentlet({ inode: 'related-inode' }),
                    'news'
                );

                expect(handled).toBe(false);
                expect(service.$editPanelRequest()).toBe(request);
            });

            it('declines a switch when no panel is open', () => {
                expect(service.switchToLegacyEditor({ inode: 'inode-1' }, BLOG_POST, 'blog')).toBe(
                    false
                );
            });

            it('closes its panel when its content fails to load', () => {
                openEdit();

                const handled = service.leaveOnLoadError({ inode: 'inode-1' });

                expect(handled).toBe(true);
                expect(service.$editPanelRequest()).toBeNull();
                expect(service.$panelLocation()).toBeNull();
            });

            it("declines another editor's load error and keeps its panel open", () => {
                openEdit();
                const request = service.$editPanelRequest();

                expect(service.leaveOnLoadError({ inode: 'related-inode' })).toBe(false);
                expect(service.$editPanelRequest()).toBe(request);
            });
        });

        it('clears the request and the location on close', () => {
            service.createContent('blog');

            service.closeEditPanel();

            expect(service.$editPanelRequest()).toBeNull();
            expect(service.$panelLocation()).toBeNull();
        });
    });

    describe('openEditByIdentifier', () => {
        it('should resolve the identifier to its working inode and open one edit request', () => {
            const resolved = createFakeContentlet({
                contentType: 'blog',
                inode: 'working-inode-1',
                identifier: 'shared-identifier',
                languageId: 1,
                title: 'Shared Content'
            });
            contentSearch.get.mockReturnValue(of({ jsonObjectView: { contentlets: [resolved] } }));

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith(
                expect.objectContaining({ query: '+identifier:shared-identifier +working:true' })
            );
            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                inode: 'working-inode-1',
                identifier: 'shared-identifier',
                contentType: 'blog',
                languageId: 1,
                title: 'Shared Content',
                portletId: 'content-drive'
            });
            expect(router.navigate).not.toHaveBeenCalled();
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
                expect.objectContaining({ inode: 'es-inode', languageId: 2 })
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
            defaultLanguageId.mockReturnValue(4);
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
                expect.objectContaining({ inode: 'en-inode' })
            );
        });

        it('should not run a second lookup when no language is known at all', () => {
            store.getFilterValue.mockReturnValue(undefined);
            defaultLanguageId.mockReturnValue(undefined);
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
         * Every screen outside Content Drive reaches content through this link, so it opens the same
         * request whatever editor the content's type uses: the opener picks the editor, and never
         * shows the new editor for a type that opted out of it (#37759, FR-021; #38303).
         */
        describe('leaving the editor to the opener', () => {
            /** The resolved content, of the given type. */
            const banner = (contentType: string) =>
                createFakeContentlet({
                    contentType,
                    inode: 'banner-inode',
                    identifier: 'banner-id',
                    languageId: 2,
                    title: 'Spring sale'
                });

            it.each([
                ['a type that opted into the new editor', NEW_EDITOR_TYPE.variable],
                ['a type on the legacy editor', LEGACY_TYPE.variable]
            ])(
                'should open the same edit request for content of %s, without looking its type up',
                (_label, contentType) => {
                    contentSearch.get.mockReturnValue(
                        of({ jsonObjectView: { contentlets: [banner(contentType)] } })
                    );

                    service.openEditByIdentifier('banner-id', 2);

                    expect(service.$editPanelRequest()).toEqual({
                        mode: 'edit',
                        inode: 'banner-inode',
                        identifier: 'banner-id',
                        contentType,
                        languageId: 2,
                        title: 'Spring sale',
                        portletId: 'content-drive'
                    });
                    expect(service.$panelLocation()).toEqual({
                        kind: 'edit',
                        editContent: 'banner-id',
                        editContentLang: 2
                    });
                    expect(contentTypeService.getContentType).not.toHaveBeenCalled();
                    expect(router.navigate).not.toHaveBeenCalled();
                }
            );

            it('should keep the language fallback for content of any type', () => {
                answerWith(
                    [],
                    [
                        createFakeContentlet({
                            contentType: LEGACY_TYPE.variable,
                            inode: 'en-inode',
                            languageId: 1
                        })
                    ]
                );

                service.openEditByIdentifier('banner-id', 2);

                expect(service.$editPanelRequest()).toEqual(
                    expect.objectContaining({ inode: 'en-inode', languageId: 1 })
                );
            });
        });
    });
});

/**
 * Where a create from Content Drive puts the new content: the folder being browsed. Shared by the
 * create action and by `createContent` links, so both land in the same place (#37759, FR-003). The
 * one request names the folder both ways, for whichever editor the opener picks (#38303).
 */
describe('DotContentDriveNavigationService, where a create lands', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    const getContentType = vi.fn();
    const systemHostSelected = signal(false);
    const currentSite = vi.fn();
    const path = vi.fn();
    const selectedNode = vi.fn();

    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, { navigate: vi.fn() }),
            mockProvider(DotContentTypeService, { getContentType }),
            mockProvider(DotRouterService, { goToEditPage: vi.fn() }),
            mockProvider(Location, { path: vi.fn() }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotActionUrlService, {
                getCreateContentletUrl: vi.fn().mockReturnValue(of(CREATE_URL))
            }),
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
        // See `answerTypeLookup`: answered, never expected to be asked.
        getContentType.mockImplementation(answerTypeLookup);
        spectator = createService();
    });

    /** Where a create from here puts the new content, read back from the one request it opens. */
    const createdIn = () => {
        spectator.service.createContent('blog');
        const request = spectator.service.$editPanelRequest();

        return { folderPath: request?.folderPath, folderInode: request?.folderInode };
    };

    it('names the browsed folder by its host path and by its inode', () => {
        expect(createdIn()).toEqual({
            folderPath: 'demo.dotcms.com/about-us/',
            folderInode: 'inode-1'
        });
    });

    it('falls back to the site when browsing the root', () => {
        path.mockReturnValue(undefined);
        selectedNode.mockReturnValue({ data: { inode: '' } });

        expect(createdIn()).toEqual({
            folderPath: 'demo.dotcms.com',
            folderInode: undefined
        });
    });

    it('uses the site node inode at the site root', () => {
        path.mockReturnValue('');
        selectedNode.mockReturnValue({ data: { type: 'site', inode: 'site-inode' } });

        expect(createdIn()).toEqual({
            folderPath: 'demo.dotcms.com',
            folderInode: 'site-inode'
        });
    });

    it('names System Host by its identifier, never pasted onto the site hostname', () => {
        systemHostSelected.set(true);

        expect(createdIn()).toEqual({ folderInode: SYSTEM_HOST.identifier });
    });
});
