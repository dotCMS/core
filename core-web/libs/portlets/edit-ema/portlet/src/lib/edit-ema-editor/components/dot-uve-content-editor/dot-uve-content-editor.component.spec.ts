import { Dispatcher } from '@ngrx/signals/events';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { EMPTY, Observable, Subject, of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, Signal, input, output, signal } from '@angular/core';
import { Router } from '@angular/router';

import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';

import {
    DotContentletEditUrlService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import { HttpCode } from '@dotcms/dotcms-js';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotContentletCanLock,
    DotTreeNode
} from '@dotcms/dotcms-models';
import {
    DotEditContentOpenOptions,
    DotEditContentOpenerService,
    DotEditContentRequest,
    DotLegacyScreenEvent,
    DotLegacyScreenRequest
} from '@dotcms/edit-content';
import { DotCMSInlineEditingPayload, DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';
import {
    DOT_VELOCITY_LANGUAGE_ID,
    DotCopyContentModalService,
    DotSidePanelConfig,
    DotSidePanelRef,
    DotSidePanelService
} from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import {
    MockDotMessageService,
    createFakeContentlet,
    createFakeLanguage
} from '@dotcms/utils-testing';

import { DotUveContentEditorComponent } from './dot-uve-content-editor.component';
import {
    DotUveAddContentRequest,
    DotUveCreateContentRequest,
    DotUveEditContentRequest,
    DotUveTranslatePageRequest,
    uveContentEditorEvents
} from './dot-uve-content-editor.events';

import { DotBlockEditorSidebarComponent } from '../../../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { EmaFormSelectorComponent } from '../../../components/ema-form-selector/ema-form-selector.component';
import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { CONTAINER_INSERT_ERROR, NG_CUSTOM_EVENTS, UVE_STATUS } from '../../../shared/enums';
import { EDIT_ACTION_PAYLOAD_MOCK, MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { ActionPayload, ReorderMenuPayload, VTLFile } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { EditorInsertResult, ReloadEditorContent } from '../../../store/features/editor/models';
import { PageType, TranslateProps } from '../../../store/models';
import { createAddContentletUrl, createTranslatePageUrl } from '../../../utils';

/**
 * The side panels open through a mocked `DotSidePanelService`, so their content is never rendered.
 * The real loaders start an `import()` nothing here waits for, which can still be loading when the
 * file ends and then fails the run; these hand over a stand-in instead.
 */
vi.mock('@dotcms/edit-content', async () => {
    class SidePanelContentStub {}

    return {
        ...(await vi.importActual<typeof import('@dotcms/edit-content')>('@dotcms/edit-content')),
        loadDotEditContentSidePanel: () => Promise.resolve(SidePanelContentStub),
        loadDotLegacyScreenSidePanel: () => Promise.resolve(SidePanelContentStub),
        loadDotSourceEditorSidePanel: () => Promise.resolve(SidePanelContentStub)
    };
});

/**
 * Stands in for the block editor sidebar, which loads a full Block Editor. It exposes what the
 * content editor calls and binds, so a test can tell whether it was asked to open and can save
 * for it.
 */
@Component({
    selector: 'dot-block-editor-sidebar',
    template: ''
})
class DotBlockEditorSidebarStubComponent {
    readonly variantName = input<string>();
    readonly onSaved = output<void>();
    readonly contentletEdit = output<{ inode: string; contentType: string }>();
    readonly open = vi.fn();
}

/** Content the content editor opened through the opener, as the fake opener records it. */
interface OpenedContent {
    request: DotEditContentRequest | Signal<DotEditContentRequest>;
    options: DotEditContentOpenOptions;
    ref: DotSidePanelRef;
}

/**
 * Stands in for the side-panel opener (#38303): records each open and hands back a real panel ref,
 * so `closed` and `isClosed` behave as the outlet's would. Like the real opener, it reports
 * `closed` from the ref.
 */
const createOpenerFake = () => {
    const opened: OpenedContent[] = [];

    return {
        opened,
        open: vi.fn(
            (
                request: DotEditContentRequest | Signal<DotEditContentRequest>,
                options: DotEditContentOpenOptions
            ) => {
                const ref = new DotSidePanelRef(() => undefined);
                vi.spyOn(ref, 'requestClose').mockImplementation(() => undefined);
                ref.closed.subscribe(() => options.closed?.());
                opened.push({ request, options, ref });

                return ref;
            }
        )
    };
};

/** The request an open names, whether the opener got it as a value or as a signal. */
const requestOf = ({ request }: OpenedContent): DotEditContentRequest =>
    typeof request === 'function' ? request() : request;

const CONTENTLET = createFakeContentlet({
    identifier: 'contentlet-identifier-123',
    inode: 'contentlet-inode-123',
    title: 'Hello World',
    contentType: 'test',
    languageId: 1
});

const MULTI_PAGE_CONTENTLET = createFakeContentlet({
    ...CONTENTLET,
    onNumberOfPages: 2
});

const COPIED_CONTENTLET = createFakeContentlet({
    identifier: 'copied-contentlet-identifier-456',
    inode: 'copied-contentlet-inode-456',
    title: 'Hello World',
    contentType: 'test',
    languageId: 1
});

/** The container the pencil was clicked in. */
const FORK_FROM = EDIT_ACTION_PAYLOAD_MOCK.container;

const TREE_NODE: DotTreeNode = {
    containerId: 'test',
    contentId: 'contentlet-identifier-123',
    pageId: 'test',
    relationType: 'test',
    treeOrder: '0'
};

const VTL_FILE: VTLFile = { inode: 'vtl-inode-123', name: 'my-template.vtl' };

const VELOCITY_FILE = createFakeContentlet({
    inode: 'vtl-inode-123',
    identifier: 'vtl-identifier-123',
    title: 'my-template.vtl',
    languageId: 2,
    baseType: 'FILEASSET',
    contentType: 'FileAsset',
    extension: 'vtl',
    fileName: 'my-template.vtl'
});

/** A file the source editor does not handle, so it goes to the content editor. */
const NON_SOURCE_FILE = createFakeContentlet({
    inode: 'vtl-inode-123',
    identifier: 'vtl-identifier-123',
    title: 'my-template.vtl',
    contentType: 'test',
    languageId: 1
});

const CAN_LOCK: DotContentletCanLock = {
    canLock: true,
    locked: false,
    id: '',
    inode: '',
    lockedBy: ''
};

/** The container position a palette drop or a container's "add" menu asks to insert into. */
const INTO: ActionPayload = EDIT_ACTION_PAYLOAD_MOCK;

/** The content type to create, as a palette drop names it. */
const CONTENT_TYPE_TO_CREATE: DotUveCreateContentRequest['contentType'] = {
    variable: 'test',
    name: 'Test'
};

/** A contentlet the editor reports it created. */
const CREATED_CONTENTLET = createFakeContentlet({
    identifier: 'created-contentlet-identifier-789',
    inode: 'created-contentlet-inode-789',
    contentType: 'test'
});

/** What the store answers when the container is full. */
const CONTAINER_FULL: EditorInsertResult = {
    didInsert: false,
    errorCode: CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED,
    maxContentlets: 1
};

/** What the store answers when the container already holds the contentlet. */
const ALREADY_IN_CONTAINER: EditorInsertResult = {
    didInsert: false,
    errorCode: CONTAINER_INSERT_ERROR.DUPLICATE_CONTENT
};

/** The toast for a full container, naming how many contentlets it holds. */
const CONTAINER_FULL_TOAST = {
    severity: 'warn',
    summary: 'Container full',
    detail: 'This container holds up to 1 contentlets',
    life: 3000
};

/** The toast for a contentlet the container already holds. */
const ALREADY_IN_CONTAINER_TOAST = {
    severity: 'info',
    summary: 'Content already added',
    detail: 'This content is already added to this container',
    life: 2000
};

const PAGE = MOCK_RESPONSE_HEADLESS.page;

/** The language the page is loaded in, and has a version in. */
const ENGLISH = createFakeLanguage({ id: 1, language: 'English', translated: true });

/** A language the page has no version in yet. */
const SPANISH = createFakeLanguage({ id: 2, language: 'Spanish', translated: false });

/** What the toolbar asks for once the user agreed to translate the page into Spanish. */
const TRANSLATE_TO_SPANISH: DotUveTranslatePageRequest = { page: PAGE, newLanguage: 2 };

/** The contentlet a URL-mapped page shows. */
const URL_MAPPED_CONTENTLET = createFakeContentlet({
    identifier: 'url-map-identifier',
    inode: 'url-map-inode',
    title: 'My blog post',
    contentType: 'Blog',
    languageId: 1,
    URL_MAP_FOR_CONTENT: '/blog/my-blog-post'
});

/** The same contentlet, as the page asset names it. */
const URL_CONTENT_MAP = URL_MAPPED_CONTENTLET as unknown as DotCMSURLContentMap;

/** What the SDK's reorderMenu() asks for. */
const REORDER_MENU: ReorderMenuPayload = { startLevel: 1, depth: 2 };

/** A Block Editor field the user clicked to edit inline. */
const BLOCK_EDITOR_FIELD: DotCMSInlineEditingPayload = {
    inode: 'block-contentlet-inode',
    content: { type: 'doc', content: [] },
    language: 1,
    fieldName: 'body',
    contentType: 'Blog'
};

/**
 * The slice of `UVEStore` the content editor reads. Signals so a test can switch the mode or the
 * page and see the template follow; methods as spies.
 */
function createStoreMock() {
    return {
        flags: signal<Record<string, boolean>>({}),
        editorCanEditContent: signal(true),
        viewMode: signal(UVE_MODE.EDIT),
        uveStatus: signal(UVE_STATUS.LOADED),
        pageAsset: signal<{
            urlContentMap?: DotCMSURLContentMap;
            site?: { identifier: string };
        } | null>({ site: { identifier: 'site-identifier' } }),
        pageParams: signal({ url: 'index' }),
        pageLanguageId: signal(1),
        pageVariantId: signal('DEFAULT'),
        pageTranslateProps: signal<TranslateProps>({ page: PAGE, currentLanguage: ENGLISH }),
        $reloadEditorContent: signal<ReloadEditorContent>({
            code: undefined,
            pageType: PageType.TRADITIONAL,
            enableInlineEdit: false,
            pageAssetRef: {}
        }),
        getCurrentTreeNode: vi.fn().mockReturnValue(TREE_NODE),
        editorInsertContentlet: vi.fn<() => EditorInsertResult>(() => ({ didInsert: true })),
        setUveStatus: vi.fn(),
        setEditorState: vi.fn(),
        editorSetContentEditorPlaced: vi.fn(),
        pageReload: vi.fn(),
        pageLoad: vi.fn()
    };
}

describe('DotUveContentEditorComponent', () => {
    let spectator: Spectator<DotUveContentEditorComponent>;
    let store: ReturnType<typeof createStoreMock>;
    let opener: ReturnType<typeof createOpenerFake>;
    let resolveEditor: Mock;
    let contentletService: SpyObject<DotContentletService>;
    let copyContentService: SpyObject<DotCopyContentService>;
    let copyContentModalService: SpyObject<DotCopyContentModalService>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;
    let pageApiService: SpyObject<DotPageApiService>;
    let messageService: SpyObject<MessageService>;
    let confirmationService: SpyObject<ConfirmationService>;
    let dotRouterService: SpyObject<DotRouterService>;
    let router: SpyObject<Router>;
    let iframeMessenger: SpyObject<UveIframeMessengerService>;

    /** The side panels the content editor opened through the panel service, recorded instead of rendered. */
    let opened: {
        component: unknown;
        config: DotSidePanelConfig;
        ref: { closed: Subject<void>; close: Mock; requestClose: Mock };
    }[];

    const createComponent = createComponentFactory({
        component: DotUveContentEditorComponent,
        overrideComponents: [
            [
                DotUveContentEditorComponent,
                {
                    remove: { imports: [DotBlockEditorSidebarComponent] },
                    add: { imports: [DotBlockEditorSidebarStubComponent] }
                }
            ]
        ],
        providers: [
            { provide: UVEStore, useFactory: () => store },
            { provide: DotEditContentOpenerService, useFactory: () => opener },
            {
                provide: DotContentletEditUrlService,
                useFactory: () => ({ resolveEditor: (variable: string) => resolveEditor(variable) })
            },
            mockProvider(DotContentletService),
            mockProvider(DotCopyContentService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSidePanelService),
            mockProvider(DotPageApiService),
            mockProvider(MessageService),
            mockProvider(ConfirmationService),
            mockProvider(DotRouterService),
            mockProvider(Router),
            mockProvider(UveIframeMessengerService),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'contenttypes.content.create.contenttype': 'Create {0}',
                    'editpage.content.add.already.title': 'Content already added',
                    'editpage.content.add.already.message':
                        'This content is already added to this container',
                    'editpage.content.container.limit.title': 'Container full',
                    'editpage.content.container.limit.message':
                        'This container holds up to {0} contentlets',
                    'editpage.language-change-missing-lang-populate.confirm.header':
                        'Create a new language version?',
                    'editpage.language-change-missing-lang-populate.confirm.message':
                        'This page has no {0} version yet. Do you want to create it?',
                    'editpage.content.contentlet.menu.reorder.title': 'Reorder Menu',
                    'message.menu.reordered': 'Menu reordered',
                    'error.menu.reorder.user_has_not_permission':
                        'You do not have permission to reorder this menu',
                    'edit.ema.page.dialog.header.search.content': 'Search Content',
                    'edit.ema.page.dialog.header.search.form': 'Search Form'
                })
            },
            { provide: WINDOW, useValue: window }
        ],
        componentProviders: [mockProvider(DotCopyContentModalService)]
    });

    /** Dispatches a request to the content editor, as any part of the editor does. */
    const dispatch = (event: Parameters<Dispatcher['dispatch']>[0]) => {
        spectator.inject(Dispatcher).dispatch(event);
        spectator.detectChanges();
    };

    const editContent = (request: DotUveEditContentRequest) =>
        dispatch(uveContentEditorEvents.uveEditContentRequested(request));

    const editSource = (file: VTLFile) =>
        dispatch(uveContentEditorEvents.uveEditSourceRequested(file));

    const createContent = (request: DotUveCreateContentRequest) =>
        dispatch(uveContentEditorEvents.uveCreateContentRequested(request));

    const addContent = (request: DotUveAddContentRequest) =>
        dispatch(uveContentEditorEvents.uveAddContentRequested(request));

    const translatePage = (request: DotUveTranslatePageRequest) =>
        dispatch(uveContentEditorEvents.uveTranslatePageRequested(request));

    const editUrlContentMap = (contentlet: DotCMSURLContentMap) =>
        dispatch(uveContentEditorEvents.uveEditUrlContentMapRequested(contentlet));

    const reorderMenu = (request: ReorderMenuPayload) =>
        dispatch(uveContentEditorEvents.uveReorderMenuRequested(request));

    const editBlockEditorField = (field: DotCMSInlineEditingPayload) =>
        dispatch(uveContentEditorEvents.uveEditBlockEditorFieldRequested(field));

    const blockSidebar = () => spectator.query(DotBlockEditorSidebarStubComponent);

    /** The Edit Content panel the content editor opened last through the opener. */
    const editPanel = (): OpenedContent => {
        const panel = opener.opened.at(-1);

        expect(panel, 'the content editor opens content through the opener').toBeDefined();

        return panel as OpenedContent;
    };

    /** The legacy-screen panel the content editor opened last. */
    const legacyScreen = () => {
        const panel = opened.filter(({ config }) => !!config.outputs?.['legacyEvent']).at(-1);

        expect(panel, 'the content editor opens a legacy-screen panel').toBeDefined();

        return {
            ...panel,
            request: (panel?.config.inputs?.['request'] as () => DotLegacyScreenRequest)(),
            emit: (event: DotLegacyScreenEvent) => {
                (panel?.config.outputs?.['legacyEvent'] as (e: DotLegacyScreenEvent) => void)(
                    event
                );
                spectator.detectChanges();
            }
        };
    };

    /** The form selector panel the content editor opened last. */
    const formSelector = () => {
        const panel = opened
            .filter(({ component }) => component === EmaFormSelectorComponent)
            .at(-1);

        expect(panel, 'the content editor opens the form selector').toBeDefined();

        return {
            ...panel,
            select: (formId: string) =>
                (panel?.config.outputs?.['selected'] as (id: string) => void)(formId)
        };
    };

    /** A save in the open Edit Content panel. */
    const savePanel = (contentlet: DotCMSContentlet) => {
        editPanel().options.saved?.(contentlet);
        spectator.detectChanges();
    };

    /** The open Edit Content panel closes itself. */
    const closePanel = () => {
        editPanel().ref.close();
        spectator.detectChanges();
    };

    /** The page context every request the page editor opens carries. */
    const PAGE_CONTEXT = { siteId: 'site-identifier', portletId: 'edit-page' };

    beforeEach(() => {
        store = createStoreMock();
        opener = createOpenerFake();
        resolveEditor = vi.fn().mockReturnValue(of({ editor: 'legacy', contentType: null }));
        opened = [];

        spectator = createComponent();

        contentletService = spectator.inject(DotContentletService);
        copyContentService = spectator.inject(DotCopyContentService);
        copyContentModalService = spectator.inject(DotCopyContentModalService, true);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
        pageApiService = spectator.inject(DotPageApiService);
        messageService = spectator.inject(MessageService);
        confirmationService = spectator.inject(ConfirmationService);
        dotRouterService = spectator.inject(DotRouterService);
        router = spectator.inject(Router);
        iframeMessenger = spectator.inject(UveIframeMessengerService);

        contentletService.getContentletByInode.mockReturnValue(of(VELOCITY_FILE));
        contentletService.canLock.mockReturnValue(of(CAN_LOCK));
        copyContentService.copyInPage.mockReturnValue(of(COPIED_CONTENTLET));
        copyContentModalService.open.mockReturnValue(of({ shouldCopy: false }));
        spectator.inject(DotSidePanelService).open.mockImplementation((component, config) => {
            const closed = new Subject<void>();
            const ref = {
                closed,
                close: vi.fn(() => {
                    closed.next();
                    closed.complete();
                }),
                requestClose: vi.fn()
            };
            opened.push({ component, config, ref });

            return ref as unknown as ReturnType<DotSidePanelService['open']>;
        });
    });

    // The shell sends the page's Properties to a placed content editor, and opens them itself
    // otherwise.
    describe('placement', () => {
        it('should tell the store it is placed', () => {
            expect(store.editorSetContentEditorPlaced).toHaveBeenCalledWith(true);
        });

        it('should tell the store it is no longer placed once it is destroyed', () => {
            spectator.fixture.destroy();

            expect(store.editorSetContentEditorPlaced).toHaveBeenLastCalledWith(false);
        });
    });

    /**
     * Every edit opens one request in the Edit Content side panel, through the opener, which
     * picks the editor from the content type (#38303, FR-001 to FR-003).
     */
    describe('uveEditContentRequested', () => {
        it('should open an edit request for the contentlet, completed with the page it is opened from', () => {
            editContent({ contentlet: CONTENTLET });

            expect(opener.opened).toHaveLength(1);
            expect(requestOf(editPanel())).toEqual({
                mode: 'edit',
                inode: 'contentlet-inode-123',
                identifier: 'contentlet-identifier-123',
                contentType: 'test',
                languageId: 1,
                title: 'Hello World',
                ...PAGE_CONTEXT
            });
        });

        it('should open in the page language when the contentlet names none', () => {
            store.pageLanguageId.set(3);

            editContent({
                contentlet: {
                    identifier: 'page-identifier',
                    inode: 'page-inode',
                    title: 'A page',
                    contentType: 'htmlpageasset'
                }
            });

            expect(requestOf(editPanel())).toEqual(expect.objectContaining({ languageId: 3 }));
        });

        // An experiment's variant is the content's working copy on that variant (FR-022).
        it('should name the experiment variant the page is on, but not the default one', () => {
            store.pageVariantId.set('variant-a');

            editContent({ contentlet: CONTENTLET });

            expect(requestOf(editPanel())).toEqual(
                expect.objectContaining({ variantName: 'variant-a' })
            );
        });

        it('should ignore a request that arrives while the panel is open', () => {
            editContent({ contentlet: CONTENTLET });

            editContent({ contentlet: COPIED_CONTENTLET });

            expect(opener.opened).toHaveLength(1);
            expect(requestOf(editPanel()).inode).toBe('contentlet-inode-123');
        });

        it('should open the panel again after it closed itself', () => {
            editContent({ contentlet: CONTENTLET });
            closePanel();

            editContent({ contentlet: CONTENTLET });

            expect(opener.opened).toHaveLength(2);
        });

        describe('copy prompt for a contentlet on more than one page', () => {
            it('should ask first, and open nothing until the user answers', () => {
                copyContentModalService.open.mockReturnValue(
                    new Subject<{ shouldCopy: boolean }>()
                );

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(copyContentModalService.open).toHaveBeenCalled();
                expect(opener.opened).toHaveLength(0);
            });

            it('should ask when the number of pages comes from the page markup as text', () => {
                copyContentModalService.open.mockReturnValue(
                    new Subject<{ shouldCopy: boolean }>()
                );

                editContent({
                    contentlet: createFakeContentlet({ ...CONTENTLET, onNumberOfPages: '2' }),
                    forkFrom: FORK_FROM
                });

                expect(copyContentModalService.open).toHaveBeenCalled();
            });

            it('should not ask for a contentlet on a single page', () => {
                editContent({
                    contentlet: createFakeContentlet({ ...CONTENTLET, onNumberOfPages: 1 }),
                    forkFrom: FORK_FROM
                });

                expect(copyContentModalService.open).not.toHaveBeenCalled();
                expect(opener.opened).toHaveLength(1);
            });

            it('should not ask when the request does not say which container to fork from', () => {
                // Quick edit's full editor and page Properties have always opened directly.
                editContent({ contentlet: MULTI_PAGE_CONTENTLET });

                expect(copyContentModalService.open).not.toHaveBeenCalled();
                expect(requestOf(editPanel()).inode).toBe('contentlet-inode-123');
            });

            it('should open the original when the user edits all pages', () => {
                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(copyContentService.copyInPage).not.toHaveBeenCalled();
                expect(store.pageReload).not.toHaveBeenCalled();
                expect(requestOf(editPanel()).inode).toBe('contentlet-inode-123');
            });

            it('should copy from the container it was picked in when the user edits this page only', () => {
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(store.getCurrentTreeNode).toHaveBeenCalledWith(
                    FORK_FROM,
                    MULTI_PAGE_CONTENTLET
                );
                expect(copyContentService.copyInPage).toHaveBeenCalledWith(TREE_NODE);
            });

            it('should reload the page and open the copy when the user edits this page only', () => {
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(store.pageReload).toHaveBeenCalled();
                expect(requestOf(editPanel())).toEqual(
                    expect.objectContaining({
                        mode: 'edit',
                        inode: 'copied-contentlet-inode-456',
                        identifier: 'copied-contentlet-identifier-456'
                    })
                );
            });

            it('should open nothing when the user dismisses the prompt', () => {
                copyContentModalService.open.mockReturnValue(EMPTY);

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(copyContentService.copyInPage).not.toHaveBeenCalled();
                expect(opener.opened).toHaveLength(0);
            });

            it('should report a failed copy and open nothing', () => {
                const error = new Error('copy failed');
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));
                copyContentService.copyInPage.mockReturnValue(throwError(() => error));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
                expect(store.pageReload).not.toHaveBeenCalled();
                expect(opener.opened).toHaveLength(0);
            });

            it('should keep handling requests after one failed', () => {
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));
                copyContentService.copyInPage.mockReturnValue(
                    throwError(() => new Error('copy failed'))
                );
                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                editContent({ contentlet: CONTENTLET });

                expect(requestOf(editPanel()).inode).toBe('contentlet-inode-123');
            });
        });

        /**
         * The edit panel's events (#38303, FR-007 to FR-021). The page refreshes on every save
         * while the panel stays open.
         */
        describe('panel events', () => {
            beforeEach(() => editContent({ contentlet: CONTENTLET }));

            it('should reload the page when the edit is saved', () => {
                savePanel(CONTENTLET);

                expect(store.pageReload).toHaveBeenCalledTimes(1);
                expect(iframeMessenger.reloadPage).not.toHaveBeenCalled();
            });

            it('should load the page in the language the editor switched to', () => {
                editPanel().options.languageChanged?.(2);

                expect(store.pageLoad).toHaveBeenCalledWith({ language_id: '2' });
            });

            it('should reload the page when the content is deleted', () => {
                editPanel().options.deleted?.();

                expect(store.pageReload).toHaveBeenCalled();
            });

            // The legacy editor's workflow actions report nothing the page can use (R4).
            it('should reload the page when the legacy editor closes', () => {
                editPanel().options.editorResolved?.('legacy');

                closePanel();

                expect(store.pageReload).toHaveBeenCalledTimes(1);
            });

            it('should not reload the page when the new editor closes, since it reported every save', () => {
                editPanel().options.editorResolved?.('new');

                closePanel();

                expect(store.pageReload).not.toHaveBeenCalled();
            });

            it('should reload the page when the panel closes before its editor was known', () => {
                closePanel();

                expect(store.pageReload).toHaveBeenCalledTimes(1);
            });

            it('should load the page the legacy editor asks for, and not reload it when the panel closes right after', () => {
                editPanel().options.editorResolved?.('legacy');

                editPanel().options.pageEditorRequested?.({ url: '/new-page', languageId: 2 });
                spectator.detectChanges();

                expect(store.pageLoad).toHaveBeenCalledWith({ url: '/new-page', language_id: '2' });
                expect(store.pageReload).not.toHaveBeenCalled();
            });
        });

        // A headless page renders its own content, so an edit the SDK asked for also asks the
        // page in the iframe to reload.
        describe('an edit the SDK asked for', () => {
            it('should ask the page in the iframe to reload, and reload the page, when it is saved', () => {
                editContent({ contentlet: CONTENTLET, fromSdk: true });

                savePanel(CONTENTLET);

                expect(iframeMessenger.reloadPage).toHaveBeenCalledTimes(1);
                expect(store.pageReload).toHaveBeenCalledTimes(1);
            });

            it('should forget it once the panel closed, so the next edit does not reload the iframe', () => {
                editContent({ contentlet: CONTENTLET, fromSdk: true });
                closePanel();
                editContent({ contentlet: CONTENTLET });

                savePanel(CONTENTLET);

                expect(iframeMessenger.reloadPage).not.toHaveBeenCalled();
            });
        });
    });

    describe('uveEditSourceRequested', () => {
        it('should open the file in the content editor by its inode alone when the file lookup fails', () => {
            contentletService.getContentletByInode.mockReturnValue(
                throwError(() => new Error('network error'))
            );

            editSource(VTL_FILE);

            expect(opened).toHaveLength(0);
            expect(requestOf(editPanel())).toEqual({
                mode: 'edit',
                inode: 'vtl-inode-123',
                title: 'my-template.vtl',
                languageId: 1,
                ...PAGE_CONTEXT
            });
        });

        it('should open the file in the content editor when the source editor does not handle it', () => {
            contentletService.getContentletByInode.mockReturnValue(of(NON_SOURCE_FILE));

            editSource(VTL_FILE);

            expect(requestOf(editPanel())).toEqual(
                expect.objectContaining({
                    mode: 'edit',
                    inode: 'vtl-inode-123',
                    identifier: 'vtl-identifier-123',
                    contentType: 'test'
                })
            );
        });

        it('should not ask to copy a file that is on more than one page', () => {
            contentletService.getContentletByInode.mockReturnValue(
                of(createFakeContentlet({ ...NON_SOURCE_FILE, onNumberOfPages: 2 }))
            );

            editSource(VTL_FILE);

            expect(copyContentModalService.open).not.toHaveBeenCalled();
            expect(opener.opened).toHaveLength(1);
        });

        describe('Edit Code panel', () => {
            it("should open a Velocity file's source instead of the content editor", () => {
                editSource(VTL_FILE);

                const [{ config }] = opened;
                expect((config.inputs?.['request'] as () => unknown)()).toEqual({
                    inode: 'vtl-inode-123',
                    title: 'my-template.vtl',
                    language: DOT_VELOCITY_LANGUAGE_ID
                });
                expect((config.heading as () => string)()).toBe('my-template.vtl');
                expect(opener.opened).toHaveLength(0);
            });

            it.each([
                [
                    'the user cannot edit it',
                    of<DotContentletCanLock>({
                        canLock: false,
                        locked: true,
                        id: '',
                        inode: '',
                        lockedBy: 'someone'
                    })
                ],
                ['the lock check fails', throwError(() => new Error('network'))]
            ])('should open the content editor instead when %s', (_label, lockAnswer) => {
                contentletService.canLock.mockReturnValue(
                    lockAnswer as Observable<DotContentletCanLock>
                );

                editSource(VTL_FILE);

                expect(opened).toHaveLength(0);
                expect(requestOf(editPanel())).toEqual(
                    expect.objectContaining({ inode: 'vtl-inode-123', contentType: 'FileAsset' })
                );
            });

            it('should reload the page when the source is saved', () => {
                editSource(VTL_FILE);

                (opened[0].config.outputs?.['saved'] as () => void)();

                expect(store.pageReload).toHaveBeenCalled();
            });

            it('should open the file again after the panel closed itself', () => {
                editSource(VTL_FILE);

                opened[0].ref.close();
                editSource(VTL_FILE);

                // The close cleared the request, so the same file opens a new panel.
                expect(opened).toHaveLength(2);
            });
        });
    });

    describe('uveCreateContentRequested', () => {
        it("should open a create of the content type in the page's language, titled with its name", () => {
            store.pageLanguageId.set(2);

            createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

            expect(requestOf(editPanel())).toEqual({
                mode: 'new',
                contentType: 'test',
                languageId: 2,
                title: 'Create Test',
                ...PAGE_CONTEXT
            });
        });

        // An experiment's variant is the content's working copy on that variant (FR-022).
        it('should name the variant on a create from a variant page', () => {
            store.pageVariantId.set('variant-a');

            createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

            expect(requestOf(editPanel())).toEqual(
                expect.objectContaining({ mode: 'new', variantName: 'variant-a' })
            );
        });

        describe('with a container position', () => {
            beforeEach(() => createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO }));

            it('should insert what the first save created into the requested position', () => {
                savePanel(CREATED_CONTENTLET);

                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    INTO,
                    'created-contentlet-identifier-789'
                );
                expect(store.pageReload).not.toHaveBeenCalled();
            });

            it('should only reload the page on a later save of the same panel', () => {
                savePanel(CREATED_CONTENTLET);

                savePanel(CREATED_CONTENTLET);

                expect(store.editorInsertContentlet).toHaveBeenCalledTimes(1);
                expect(store.pageReload).toHaveBeenCalledTimes(1);
            });

            it('should tell the user why when the container refuses what was saved', () => {
                store.editorInsertContentlet.mockReturnValue(CONTAINER_FULL);

                savePanel(CREATED_CONTENTLET);

                expect(messageService.add).toHaveBeenCalledWith(CONTAINER_FULL_TOAST);
            });

            // ff2be6f7aa: marked placed only once a container takes it.
            it('should try the placement again on the next save when the container refused it', () => {
                store.editorInsertContentlet.mockReturnValueOnce(ALREADY_IN_CONTAINER);
                savePanel(CREATED_CONTENTLET);

                savePanel(CREATED_CONTENTLET);

                expect(messageService.add).toHaveBeenCalledWith(ALREADY_IN_CONTAINER_TOAST);
                expect(store.editorInsertContentlet).toHaveBeenCalledTimes(2);
                expect(store.pageReload).not.toHaveBeenCalled();
            });
        });

        describe('without a container position', () => {
            beforeEach(() => createContent({ contentType: CONTENT_TYPE_TO_CREATE }));

            it('should reload the page instead of inserting when it is saved', () => {
                savePanel(CREATED_CONTENTLET);

                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
                expect(store.pageReload).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('uveAddContentRequested', () => {
        it('should open the content selector in a legacy-screen panel, filtered by what the container accepts', () => {
            addContent({ type: 'content', into: INTO });

            expect(legacyScreen().request).toEqual({
                url: createAddContentletUrl(
                    INTO.container.identifier,
                    INTO.container.acceptTypes ?? '*',
                    INTO.language_id,
                    'DEFAULT'
                ),
                title: 'Search Content'
            });
        });

        it('should accept every type in a container that names none', () => {
            // The server can leave it out, whatever the type says.
            const into: ActionPayload = {
                ...INTO,
                container: { ...INTO.container, acceptTypes: undefined as unknown as string }
            };

            addContent({ type: 'content', into });

            expect(legacyScreen().request.url).toBe(
                createAddContentletUrl(INTO.container.identifier, '*', INTO.language_id, 'DEFAULT')
            );
        });

        it('should open the content selector filtered to widgets for a widget', () => {
            addContent({ type: 'widget', into: INTO });

            expect(legacyScreen().request.url).toBe(
                createAddContentletUrl(
                    INTO.container.identifier,
                    DotCMSBaseTypesContentTypes.WIDGET,
                    INTO.language_id,
                    'DEFAULT'
                )
            );
        });

        describe('what the content selector reports', () => {
            beforeEach(() => addContent({ type: 'content', into: INTO }));

            it('should insert the picked contentlet into the position and close the selector', () => {
                const screen = legacyScreen();

                screen.emit({
                    name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                    data: { identifier: 'picked-identifier' }
                });

                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    INTO,
                    'picked-identifier'
                );
                expect(screen.ref?.close).toHaveBeenCalled();
            });

            it('should tell the user why, and keep the selector open, when the container refuses the contentlet', () => {
                store.editorInsertContentlet.mockReturnValue(ALREADY_IN_CONTAINER);
                const screen = legacyScreen();

                screen.emit({
                    name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                    data: { identifier: 'picked-identifier' }
                });

                expect(messageService.add).toHaveBeenCalledWith(ALREADY_IN_CONTAINER_TOAST);
                expect(screen.ref?.close).not.toHaveBeenCalled();
            });

            it('should close the selector and open a create that places its contentlet in the position on "create new"', () => {
                const screen = legacyScreen();

                screen.emit({
                    name: NG_CUSTOM_EVENTS.CREATE_CONTENTLET,
                    data: { contentType: 'Blog' }
                });

                expect(screen.ref?.close).toHaveBeenCalled();
                expect(requestOf(editPanel())).toEqual({
                    mode: 'new',
                    contentType: 'Blog',
                    languageId: 1,
                    ...PAGE_CONTEXT
                });

                savePanel(CREATED_CONTENTLET);

                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    INTO,
                    'created-contentlet-identifier-789'
                );
            });
        });

        describe('a form', () => {
            beforeEach(() => {
                pageApiService.getFormIndetifier.mockReturnValue(of('form-in-container-id'));
                addContent({ type: 'form', into: INTO });
            });

            it('should open the form selector as side panel content', () => {
                expect(formSelector().config?.heading).toBe('Search Form');
                expect(opener.opened).toHaveLength(0);
            });

            it('should insert the form under the identifier its container gives it, showing the page as loading, and close the selector', () => {
                const selector = formSelector();

                selector.select('form-id');

                expect(pageApiService.getFormIndetifier).toHaveBeenCalledWith(
                    INTO.container.identifier,
                    'form-id'
                );
                expect(store.setUveStatus).toHaveBeenCalledWith(UVE_STATUS.LOADING);
                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    INTO,
                    'form-in-container-id'
                );
                expect(selector.ref?.close).toHaveBeenCalled();
            });

            it('should tell the user why, stop loading and keep the selector open when the container refuses the form', () => {
                store.editorInsertContentlet.mockReturnValue(CONTAINER_FULL);
                const selector = formSelector();

                selector.select('form-id');

                expect(messageService.add).toHaveBeenCalledWith(CONTAINER_FULL_TOAST);
                expect(store.setUveStatus).toHaveBeenLastCalledWith(UVE_STATUS.LOADED);
                expect(selector.ref?.close).not.toHaveBeenCalled();
            });

            it('should report a form its container cannot resolve and keep the selector open', () => {
                const error = new HttpErrorResponse({ status: HttpCode.SERVER_ERROR });
                pageApiService.getFormIndetifier.mockReturnValue(throwError(() => error));
                const selector = formSelector();

                selector.select('form-id');

                expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
                expect(selector.ref?.close).not.toHaveBeenCalled();
            });
        });
    });

    /** A translation opens in the editor the page's type uses (#38303, T178). */
    describe('uveTranslatePageRequested', () => {
        describe('a page type on the legacy editor', () => {
            beforeEach(() => translatePage(TRANSLATE_TO_SPANISH));

            it("should open the legacy editor's translate screen through the opener, with the editor already chosen", () => {
                const panel = editPanel();

                expect(resolveEditor).toHaveBeenCalledWith(PAGE.contentType);
                expect(requestOf(panel)).toEqual(
                    expect.objectContaining({
                        mode: 'new',
                        contentType: PAGE.contentType,
                        languageId: 2,
                        ...PAGE_CONTEXT
                    })
                );
                expect(panel.options.createUrl).toBe(
                    createTranslatePageUrl(PAGE, 2, 'DEFAULT', 'site-identifier')
                );
                expect(panel.options.resolved).toEqual({ editor: 'legacy', contentType: null });
            });

            it('should load the saved translation, and reload once the panel closes', () => {
                editPanel().options.saved?.(
                    createFakeContentlet({ languageId: 2, url: '/es/index' })
                );

                expect(store.pageLoad).toHaveBeenCalledWith({ language_id: '2', url: '/es/index' });

                editPanel().ref.close();

                expect(store.pageReload).toHaveBeenCalledTimes(1);
            });

            it('should not reload when the panel closes with nothing saved', () => {
                editPanel().ref.close();

                expect(store.pageReload).not.toHaveBeenCalled();
            });

            it('should close the panel and load the page the editor asks for, without reloading it', () => {
                editPanel().options.saved?.(createFakeContentlet({ languageId: 2 }));

                editPanel().options.pageEditorRequested?.({ url: '/es/index', languageId: 2 });

                expect(store.pageLoad).toHaveBeenLastCalledWith({
                    url: '/es/index',
                    language_id: '2'
                });
                expect(store.pageReload).not.toHaveBeenCalled();
            });
        });

        it('should open the page in the new editor, in the new language, for a page type on it', () => {
            resolveEditor.mockReturnValue(of({ editor: 'new', contentType: null }));

            translatePage(TRANSLATE_TO_SPANISH);

            const panel = editPanel();
            expect(requestOf(panel)).toEqual(
                expect.objectContaining({
                    mode: 'edit',
                    inode: PAGE.inode,
                    identifier: PAGE.identifier,
                    languageId: 2,
                    ...PAGE_CONTEXT
                })
            );
            expect(panel.options.createUrl).toBeUndefined();
            expect(panel.options.resolved).toEqual({ editor: 'new', contentType: null });
        });

        describe('a page type on the new editor', () => {
            beforeEach(() => {
                resolveEditor.mockReturnValue(of({ editor: 'new', contentType: null }));
                translatePage(TRANSLATE_TO_SPANISH);
            });

            it('should load the saved translation at its URL and in its language', () => {
                editPanel().options.saved?.(
                    createFakeContentlet({ languageId: 2, url: '/translated-page' })
                );

                expect(store.pageLoad).toHaveBeenCalledWith({
                    language_id: '2',
                    url: '/translated-page'
                });
            });

            it('should reload the page when the panel closes after a save', () => {
                editPanel().options.saved?.(
                    createFakeContentlet({ languageId: 2, url: '/translated-page' })
                );

                editPanel().ref.close();

                expect(store.pageReload).toHaveBeenCalledTimes(1);
            });

            it('should leave the page alone when the panel closes without a save', () => {
                editPanel().ref.close();

                expect(store.pageReload).not.toHaveBeenCalled();
            });
        });
    });

    // A page can also load in a language it has no version in, from a link or a typed URL. The
    // content editor then offers to create that version, in the shell's confirm dialog.
    describe('a page loaded in a language it has no version in', () => {
        /** Loads the page in Spanish, as far as the content editor can tell. */
        const loadPageInSpanish = () => {
            store.pageTranslateProps.set({ page: PAGE, currentLanguage: SPANISH });
            spectator.detectChanges();
        };

        /** The prompt the content editor showed. */
        const translationPrompt = () =>
            confirmationService.confirm.mock.calls[0][0] as Confirmation;

        /** Records the URL the user was on before the editor, as `DotRouterService` does. */
        const setPreviousUrl = (previousUrl: string, isPublicUrl = false) => {
            Object.defineProperty(dotRouterService, 'previousUrl', {
                get: () => previousUrl,
                configurable: true
            });
            dotRouterService.isPublicUrl.mockReturnValue(isPublicUrl);
        };

        it("should ask whether to create the page's version in that language, in the shell's confirm dialog", () => {
            loadPageInSpanish();

            expect(confirmationService.confirm).toHaveBeenCalledWith(
                expect.objectContaining({
                    header: 'Create a new language version?',
                    message: 'This page has no Spanish version yet. Do you want to create it?',
                    acceptIcon: 'hidden',
                    rejectIcon: 'hidden',
                    key: 'shell-confirm-dialog'
                })
            );
        });

        it('should not ask while the page is still loading', () => {
            // The language may still be the previous page's.
            store.uveStatus.set(UVE_STATUS.LOADING);

            loadPageInSpanish();

            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should ask once the page finished loading', () => {
            store.uveStatus.set(UVE_STATUS.LOADING);
            loadPageInSpanish();

            store.uveStatus.set(UVE_STATUS.LOADED);
            spectator.detectChanges();

            expect(confirmationService.confirm).toHaveBeenCalledTimes(1);
        });

        it('should not ask when the page has a version in its language', () => {
            store.pageTranslateProps.set({
                page: PAGE,
                currentLanguage: createFakeLanguage({ id: 3, language: 'French', translated: true })
            });
            spectator.detectChanges();

            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should open the new language version when the user accepts', () => {
            loadPageInSpanish();

            translationPrompt().accept?.();

            expect(resolveEditor).toHaveBeenCalledWith(PAGE.contentType);
            expect(requestOf(editPanel())).toEqual(expect.objectContaining({ languageId: 2 }));
        });

        // Reloading the same URL would only show the prompt again (#36661), so declining takes
        // the user out of the editor, to a page they can use.
        describe('when the user declines', () => {
            it('should go back to the dotCMS URL the user was on before the editor', () => {
                setPreviousUrl('/pages/import');
                loadPageInSpanish();

                translationPrompt().reject?.();

                expect(router.navigateByUrl).toHaveBeenCalledWith('/pages/import');
                expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
                expect(store.pageLoad).not.toHaveBeenCalled();
                expect(store.pageReload).not.toHaveBeenCalled();
            });

            it('should go to the Pages portlet when there is no previous URL', () => {
                // The user opened the editor directly.
                setPreviousUrl('');
                loadPageInSpanish();

                translationPrompt().reject?.();

                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/pages');
                expect(router.navigateByUrl).not.toHaveBeenCalled();
            });

            it('should go to the Pages portlet when the previous URL is another edit-page route', () => {
                // It could resolve to the same page in the same language, and ask again.
                setPreviousUrl('/edit-page/content?url=/index&language_id=2');
                loadPageInSpanish();

                translationPrompt().reject?.();

                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/pages');
                expect(router.navigateByUrl).not.toHaveBeenCalled();
            });

            it('should go to the Pages portlet when the previous URL is a public route', () => {
                setPreviousUrl('/public/login', true);
                loadPageInSpanish();

                translationPrompt().reject?.();

                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/pages');
                expect(router.navigateByUrl).not.toHaveBeenCalled();
            });
        });
    });

    /** The URL-mapped content's edit follows the content to its new URL (#38303, FR-021). */
    describe('uveEditUrlContentMapRequested', () => {
        beforeEach(() => editUrlContentMap(URL_CONTENT_MAP));

        it("should open an edit request for the URL-mapped page's contentlet", () => {
            expect(requestOf(editPanel())).toEqual({
                mode: 'edit',
                inode: 'url-map-inode',
                identifier: 'url-map-identifier',
                contentType: 'Blog',
                languageId: 1,
                title: 'My blog post',
                ...PAGE_CONTEXT
            });
        });

        it("should load the contentlet's new URL when the save moved it", () => {
            savePanel({ ...URL_MAPPED_CONTENTLET, URL_MAP_FOR_CONTENT: '/blog/my-renamed-post' });

            expect(store.pageLoad).toHaveBeenCalledWith({ url: '/blog/my-renamed-post' });
            expect(store.pageReload).not.toHaveBeenCalled();
        });

        it('should reload the page when the save kept its URL', () => {
            store.pageParams.set({ url: '/blog/my-blog-post' });

            savePanel(URL_MAPPED_CONTENTLET);

            expect(store.pageReload).toHaveBeenCalled();
            expect(store.pageLoad).not.toHaveBeenCalled();
        });

        it('should keep the author on the page when another edit of a URL-mapped contentlet moves its URL', () => {
            closePanel();
            store.pageReload.mockClear();
            editContent({ contentlet: URL_MAPPED_CONTENTLET });

            savePanel({ ...URL_MAPPED_CONTENTLET, URL_MAP_FOR_CONTENT: '/blog/my-renamed-post' });

            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(store.pageReload).toHaveBeenCalledTimes(1);
        });
    });

    describe('uveReorderMenuRequested', () => {
        beforeEach(() => {
            store.pageParams.set({ url: '/about-us/index' });
            reorderMenu(REORDER_MENU);
        });

        it("should open the menu reorder screen for the page's path and site in a legacy-screen panel", () => {
            const { url, title } = legacyScreen().request;
            const { pathname, searchParams } = new URL(url);

            expect(pathname).toBe('/c/portal/layout');
            expect(searchParams.get('_site_browser_struts_action')).toBe('/ext/folders/order_menu');
            expect(searchParams.get('startLevel')).toBe('1');
            expect(searchParams.get('depth')).toBe('2');
            expect(searchParams.get('pagePath')).toBe('/about-us/index');
            expect(searchParams.get('hostId')).toBe('site-identifier');
            expect(title).toBe('Reorder Menu');
        });

        it('should tell the user, reload the page, close the panel and reload the page in the iframe when the new order is saved', () => {
            const screen = legacyScreen();

            screen.emit({ name: NG_CUSTOM_EVENTS.SAVE_MENU_ORDER });

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'Reorder Menu',
                detail: 'Menu reordered',
                life: 2000
            });
            expect(store.pageReload).toHaveBeenCalled();
            expect(screen.ref?.close).toHaveBeenCalled();
            // The page in the iframe renders the menu itself, so it reloads too.
            expect(iframeMessenger.reloadPage).toHaveBeenCalled();
        });

        it('should tell the user they cannot reorder the menu, and keep the panel open, when the order cannot be saved', () => {
            const screen = legacyScreen();

            screen.emit({ name: NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER });

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'Reorder Menu',
                detail: 'You do not have permission to reorder this menu',
                life: 2000
            });
            expect(store.pageReload).not.toHaveBeenCalled();
            expect(screen.ref?.close).not.toHaveBeenCalled();
        });

        it('should close the panel when the user cancels', () => {
            const screen = legacyScreen();

            screen.emit({ name: NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER });

            expect(screen.ref?.close).toHaveBeenCalled();
            expect(store.pageReload).not.toHaveBeenCalled();
        });
    });

    describe('uveEditBlockEditorFieldRequested', () => {
        it('should open the block editor sidebar on the field', () => {
            editBlockEditorField(BLOCK_EDITOR_FIELD);

            expect(blockSidebar()?.open).toHaveBeenCalledWith(BLOCK_EDITOR_FIELD);
        });

        it("should save the field in the page's variant", () => {
            store.pageVariantId.set('variant-b');
            spectator.detectChanges();

            expect(blockSidebar()?.variantName()).toBe('variant-b');
        });

        it('should reload the page when the block editor sidebar saves', () => {
            editBlockEditorField(BLOCK_EDITOR_FIELD);

            spectator.triggerEventHandler(DotBlockEditorSidebarStubComponent, 'onSaved', undefined);

            expect(store.pageReload).toHaveBeenCalled();
        });

        // A contentlet in the Story Block opens in the side panel, above the drawer (#38303).
        it('should open a contentlet the Story Block asks to edit in the side panel', () => {
            spectator.triggerEventHandler(DotBlockEditorSidebarStubComponent, 'contentletEdit', {
                inode: 'story-contentlet-inode',
                contentType: 'Banner'
            });
            spectator.detectChanges();

            expect(requestOf(editPanel())).toEqual({
                mode: 'edit',
                inode: 'story-contentlet-inode',
                contentType: 'Banner',
                languageId: 1,
                ...PAGE_CONTEXT
            });
        });

        it('should not render the block editor sidebar, so a request opens nothing, when the user cannot edit the page content', () => {
            store.editorCanEditContent.set(false);
            spectator.detectChanges();

            editBlockEditorField(BLOCK_EDITOR_FIELD);

            expect(spectator.query(byTestId('uve-content-editor-block-sidebar'))).toBeNull();
        });
    });
});
