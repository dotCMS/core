import { Dispatcher } from '@ngrx/signals/events';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { Observable, of } from 'rxjs';
import { Mock, MockInstance, vi } from 'vitest';

import { Component, computed, input, output, reflectComponentType, signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';

import {
    DotCopyContentService,
    DotMessageService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotTreeNode } from '@dotcms/dotcms-models';
import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { WINDOW } from '@dotcms/utils';
import {
    MockDotMessageService,
    createFakeContentlet,
    createFakeEvent,
    mockDotCMSTempFile
} from '@dotcms/utils-testing';

import { DotUveCanvasComponent } from './dot-uve-canvas.component';

import { DotPageApiParams } from '../../../services/dot-page-api/dot-page-api.service';
import {
    ActionsHandlerDependencies,
    DotUveActionsHandlerService
} from '../../../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveDragDropService } from '../../../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { InlineEditService } from '../../../services/inline-edit/inline-edit.service';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../../../shared/consts';
import { CONTAINER_INSERT_ERROR, EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import {
    ActionPayload,
    ClientData,
    ContainerPayload,
    ContentletPayload,
    PageContainer,
    PositionPayload,
    SelectedContentlet
} from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { EditorInsertResult, ReloadEditorContent } from '../../../store/features/editor/models';
import { PageAssetSource, PageSnapshot } from '../../../store/features/page/withPage';
import { WorkflowLockOptions } from '../../../store/features/workflow/withWorkflow';
import { IframeAccessMode, PageType } from '../../../store/models';
import { TEMPORAL_DRAG_ITEM } from '../../../utils';
import { DotUveCanvasSizingDirective } from '../../directives/dot-uve-canvas-sizing/dot-uve-canvas-sizing.directive';
import { uveContentEditorEvents } from '../dot-uve-content-editor/dot-uve-content-editor.events';
import { DotUveContentletToolsComponent } from '../dot-uve-contentlet-tools/dot-uve-contentlet-tools.component';
import { DotUveIframeComponent } from '../dot-uve-iframe/dot-uve-iframe.component';
import { DotUveIframeResizeHandlesComponent } from '../dot-uve-iframe-resize-handles/dot-uve-iframe-resize-handles.component';
import { DotUveLockOverlayComponent } from '../dot-uve-lock-overlay/dot-uve-lock-overlay.component';
import { DotUvePageVersionNotFoundComponent } from '../dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { EmaPageDropzoneComponent } from '../ema-page-dropzone/ema-page-dropzone.component';
import { Container, ContentletArea, EmaDragItem } from '../ema-page-dropzone/types';

/** The contentlet tools have their own spec; here they only need to show and to ask for a delete. */
@Component({ selector: 'dot-uve-contentlet-tools', template: '' })
class DotUveContentletToolsStubComponent {
    readonly contentletArea = input<ContentletArea | null>(null);
    readonly allowContentDelete = input(true);
    readonly showStyleEditorOption = input(false);
    readonly deleteContent = output<ActionPayload>();
}

/** The dropzone has its own spec; here it only needs to show and to receive the drop targets. */
@Component({ selector: 'dot-ema-page-dropzone', template: '' })
class EmaPageDropzoneStubComponent {
    readonly containers = input<Container[]>([]);
    readonly dragItem = input<EmaDragItem | null>(null);
    readonly zoomLevel = input(1);
}

/** The lock overlay has its own spec; here it only needs to show. */
@Component({ selector: 'dot-uve-lock-overlay', template: '' })
class DotUveLockOverlayStubComponent {}

/** The page-not-found state has its own spec; here it only needs to show. */
@Component({ selector: 'dot-uve-page-version-not-found', template: '' })
class DotUvePageVersionNotFoundStubComponent {}

/** The resize handles have their own spec; here they only need to be placed. */
@Component({ selector: 'dot-uve-iframe-resize-handles', template: '' })
class DotUveIframeResizeHandlesStubComponent {}

const MESSAGES = {
    'editpage.content.contentlet.remove.confirmation_message.header': 'Deleting Content',
    'editpage.content.contentlet.remove.confirmation_message.message':
        'Are you sure you want to remove this content?',
    'dot.common.dialog.accept': 'Accept',
    'dot.common.dialog.reject': 'Reject',
    'editpage.content.add.already.title': 'Content already added',
    'editpage.content.add.already.message': 'This content is already added to this container',
    'editpage.content.container.limit.title': 'Container full',
    'editpage.content.container.limit.message': 'This container holds up to {0} contentlets',
    'file-upload': 'File upload',
    'editpage.file.upload.not.image': 'The file is not an image',
    'upload-image': 'Upload image',
    'editpage.file.uploading': 'Uploading {0}',
    'editpage.file.upload.error': 'The image could not be uploaded',
    'Workflow-Action': 'Workflow Action',
    'editpage.file.publishing': 'Publishing {0}',
    'uve.contentlet.no.edit.permission': 'You do not have permission to edit this content'
};

/** Where the editor loads the headless page in the iframe. */
const PAGE_URL = 'http://localhost:3000/index?language_id=1&mode=EDIT_MODE';

const PAGE_PARAMS: DotPageApiParams = {
    url: 'index',
    language_id: '1',
    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
};

/** A page nobody holds the lock of. */
const UNLOCKED: WorkflowLockOptions = {
    inode: 'page-inode',
    isLocked: false,
    shouldShowButton: true,
    lockedBy: '',
    canLock: true,
    isLockedByCurrentUser: false
};

const LOCKED_BY_ANOTHER_USER: WorkflowLockOptions = {
    ...UNLOCKED,
    isLocked: true,
    lockedBy: 'another-user'
};

const LOCKED_BY_THE_USER: WorkflowLockOptions = {
    ...UNLOCKED,
    isLocked: true,
    lockedBy: 'current-user',
    isLockedByCurrentUser: true
};

/** The page's containers: two contentlets in the first, one in the second. */
const PAGE_CONTAINERS: PageContainer[] = [
    { identifier: 'container-1', uuid: '1', contentletsId: ['contentlet-1', 'contentlet-2'] },
    { identifier: 'container-2', uuid: '1', contentletsId: ['contentlet-3'] }
];

const CONTAINER_1: ContainerPayload = {
    identifier: 'container-1',
    uuid: '1',
    acceptTypes: '*',
    maxContentlets: 25,
    contentletsId: ['contentlet-1', 'contentlet-2']
};

const CONTAINER_2: ContainerPayload = {
    identifier: 'container-2',
    uuid: '1',
    acceptTypes: '*',
    maxContentlets: 25,
    contentletsId: ['contentlet-3']
};

/** A contentlet on the page, as the page reports it. */
const contentletOnPage = (identifier: string): ContentletPayload => {
    const { inode, title, contentType, baseType } = createFakeContentlet({
        identifier,
        inode: `${identifier}-inode`,
        title: identifier,
        contentType: 'Banner'
    });

    return { identifier, inode, title, contentType, baseType };
};

/** Drop data for the place right after the third contentlet, in the second container. */
const AFTER_CONTENTLET_3: ClientData = {
    container: CONTAINER_2,
    contentlet: contentletOnPage('contentlet-3')
};

/** A contentlet dragged from the palette into the page. */
const PALETTE_CONTENTLET: EmaDragItem = {
    baseType: 'CONTENT',
    contentType: 'Banner',
    draggedPayload: {
        type: 'contentlet',
        move: false,
        item: { contentlet: contentletOnPage('palette-contentlet') }
    }
};

/** The first contentlet, dragged from its container to another place on the page. */
const MOVED_CONTENTLET: EmaDragItem = {
    baseType: 'CONTENT',
    contentType: 'Banner',
    draggedPayload: {
        type: 'contentlet',
        move: true,
        item: { container: CONTAINER_1, contentlet: contentletOnPage('contentlet-1') }
    }
};

/** A content type dragged from the palette into the page. */
const CONTENT_TYPE: EmaDragItem = {
    baseType: 'CONTENT',
    contentType: 'Banner',
    draggedPayload: {
        type: 'content-type',
        item: { variable: 'Banner', name: 'Banner' }
    }
};

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

/** The page as the store hands it out, with where it came from. */
const pageSnapshot = (source: PageAssetSource): PageSnapshot => ({
    ...MOCK_RESPONSE_HEADLESS,
    source,
    requestMetadata: { query: 'query', variables: {} },
    clientResponse: {
        pageAsset: MOCK_RESPONSE_HEADLESS,
        source,
        requestMetadata: { query: 'query', variables: {} }
    }
});

/**
 * The part of `UVEStore` the canvas, its iframe and its sizing directive read and write. Signals
 * so a test can change what the store says and see the canvas follow; methods as spies. The
 * reload-pending setter writes back, as the real store's does.
 */
function createUVEStoreMock() {
    const editorState = signal(EDITOR_STATE.IDLE);
    const pageReloadPending = signal(false);

    return {
        // What the canvas shows
        uveStatus: signal(UVE_STATUS.LOADED),
        viewMode: signal(UVE_MODE.EDIT),
        $iframeURL: signal<string | InstanceType<typeof String>>(PAGE_URL),
        $editorIsIframeLoading: signal(false),
        $lockFeatureEnabled: signal(false),
        $lockOptions: signal<WorkflowLockOptions | null>(UNLOCKED),
        editorCanEditContent: signal(true),
        editorState,
        $editorIsInDraggingState: computed(() => editorState() === EDITOR_STATE.DRAGGING),
        editorBounds: signal<Container[]>([]),
        editorDragItem: signal<EmaDragItem | null>(null),
        $viewZoomFactor: signal(1),
        $showContentletControls: signal(false),
        editorContentArea: signal<ContentletArea | null>(null),
        $allowContentDelete: signal(true),
        editorCanEditStyles: signal(false),
        $areaContentType: signal(''),
        editorSelected: signal<SelectedContentlet | null>(null),
        $viewCanvasOuterStyles: signal({ width: '1200px', height: '800px' }),
        $viewCanvasInnerStyles: signal({
            width: '1200px',
            height: '800px',
            transform: 'scale(1)',
            transformOrigin: 'top left'
        }),
        // Read by the sizing directive on the viewport, which has its own spec
        $viewIsResponsiveMode: signal(true),
        viewCanvasAvailableWidth: signal(1200),
        viewCanvasAvailableHeight: signal(800),
        viewIframeWidth: signal(1200),
        viewIframeHeight: signal(800),
        viewSetCanvasAvailableSize: vi.fn(),
        viewSetIframeSize: vi.fn(),
        // Read by the iframe, which has its own spec
        pageType: signal(PageType.HEADLESS),
        $pageRender: signal(''),
        editorEnableInlineEdit: signal(false),
        $isEmaLegacyScriptInjectionEnabled: signal(false),
        setSeoData: vi.fn(),
        // The page in the iframe
        $reloadEditorContent: signal<ReloadEditorContent>({
            code: undefined,
            pageType: PageType.HEADLESS,
            enableInlineEdit: false,
            pageAssetRef: {}
        }),
        isClientReady: signal(false),
        pageAsset: signal<PageSnapshot>(pageSnapshot('rest')),
        pageParams: signal<DotPageApiParams>(PAGE_PARAMS),
        pageReloadPending,
        setPageReloadPending: vi.fn((pending: boolean) => pageReloadPending.set(pending)),
        iframeAccessMode: signal(IframeAccessMode.LOCAL),
        setIsClientReady: vi.fn(),
        // What the canvas changes
        resetEditorProperties: vi.fn(),
        resetSelected: vi.fn(),
        /** Like the store's: the drop position, with the page it is on and its containers. */
        getPageSavePayload: vi.fn(
            (position: PositionPayload): ActionPayload => ({
                ...position,
                language_id: '1',
                pageId: 'page-identifier',
                pageContainers: PAGE_CONTAINERS.map((container) => ({
                    ...container,
                    contentletsId: [...container.contentletsId]
                }))
            })
        ),
        editorInsertContentlet: vi.fn(
            (_into: ActionPayload, _contentletId: string): EditorInsertResult => ({
                didInsert: true
            })
        ),
        editorSave: vi.fn(),
        pageLoad: vi.fn()
    };
}

/**
 * The admin window the canvas runs in. Its events are the test's real window, so a message or a
 * drop dispatched on the page reaches the canvas; its location and document are the test's, so a
 * link test controls the admin's origin and sees the tab a link opens.
 */
function createAdminWindowMock() {
    const openedLink = {
        href: '',
        target: '',
        rel: '',
        click: vi.fn(),
        remove: vi.fn()
    };

    return {
        openedLink,
        adminWindow: {
            location: { origin: 'http://localhost:3000', hostname: 'localhost' },
            open: vi.fn(),
            document: {
                createElement: vi.fn().mockReturnValue(openedLink),
                body: { appendChild: vi.fn() }
            },
            addEventListener: window.addEventListener.bind(window),
            removeEventListener: window.removeEventListener.bind(window)
        }
    };
}

describe('DotUveCanvasComponent', () => {
    let spectator: Spectator<DotUveCanvasComponent>;
    let store: ReturnType<typeof createUVEStoreMock>;
    let admin: ReturnType<typeof createAdminWindowMock>;
    let actionsHandler: { handleAction: Mock };
    let tempFileUploadService: { upload: Mock };
    let messageService: SpyObject<MessageService>;
    let confirmationService: SpyObject<ConfirmationService>;
    let iframeMessenger: SpyObject<UveIframeMessengerService>;
    let inlineEditService: SpyObject<InlineEditService>;
    let workflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;

    /** Elements a test put on the page, removed after it. */
    let attached: HTMLElement[];

    const createComponent = createComponentFactory({
        component: DotUveCanvasComponent,
        overrideComponents: [
            [
                DotUveCanvasComponent,
                {
                    remove: {
                        imports: [
                            DotUveContentletToolsComponent,
                            EmaPageDropzoneComponent,
                            DotUveLockOverlayComponent,
                            DotUvePageVersionNotFoundComponent,
                            DotUveIframeResizeHandlesComponent
                        ]
                    },
                    add: {
                        imports: [
                            DotUveContentletToolsStubComponent,
                            EmaPageDropzoneStubComponent,
                            DotUveLockOverlayStubComponent,
                            DotUvePageVersionNotFoundStubComponent,
                            DotUveIframeResizeHandlesStubComponent
                        ]
                    }
                }
            ]
        ],
        providers: [
            { provide: UVEStore, useFactory: () => store },
            { provide: WINDOW, useFactory: () => admin.adminWindow },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(MessageService),
            mockProvider(ConfirmationService),
            mockProvider(UveIframeMessengerService),
            mockProvider(InlineEditService),
            mockProvider(DotCopyContentService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotSeoMetaTagsService),
            mockProvider(DotSeoMetaTagsUtilService)
        ],
        // The canvas provides these itself, and these replace them. The drag-drop service is the
        // real one, so a drop on the page goes through it as it does in the editor.
        componentProviders: [
            DotUveDragDropService,
            { provide: DotUveActionsHandlerService, useFactory: () => actionsHandler },
            { provide: DotTempFileUploadService, useFactory: () => tempFileUploadService }
        ]
    });

    /** The iframe the page renders in. */
    const pageFrame = (): HTMLIFrameElement =>
        spectator.query(byTestId('iframe')) as HTMLIFrameElement;

    /** A message the page in the iframe posts to the editor. */
    const postFromPage = (data: unknown, source: Window | null = pageFrame().contentWindow) => {
        window.dispatchEvent(new MessageEvent('message', { data, source }));
    };

    /** Puts an element on the page for the length of the test. */
    const attach = <T extends HTMLElement>(element: T): T => {
        document.body.appendChild(element);
        attached.push(element);

        return element;
    };

    /** A drop target, as the dropzone renders it: where the drop goes, and the container it is in. */
    const dropTarget = (data: ClientData, position?: 'before' | 'after'): HTMLElement => {
        const target = document.createElement('div');
        target.dataset['dropzone'] = 'true';
        target.dataset['payload'] = JSON.stringify(data);

        if (position) {
            target.dataset['position'] = position;
        }

        return attach(target);
    };

    /** Drops on an element of the page, with the files the user dragged in. */
    const dropOn = (target: HTMLElement, files: File[] = []): Event => {
        const event = createFakeEvent('drop');
        Object.defineProperty(event, 'dataTransfer', { value: { files } });
        target.dispatchEvent(event);

        return event;
    };

    beforeEach(() => {
        store = createUVEStoreMock();
        admin = createAdminWindowMock();
        attached = [];
        actionsHandler = { handleAction: vi.fn() };
        tempFileUploadService = { upload: vi.fn().mockReturnValue(of([mockDotCMSTempFile])) };

        spectator = createComponent();

        messageService = spectator.inject(MessageService);
        confirmationService = spectator.inject(ConfirmationService);
        iframeMessenger = spectator.inject(UveIframeMessengerService);
        inlineEditService = spectator.inject(InlineEditService);
        workflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
    });

    afterEach(() => {
        attached.forEach((element) => element.remove());
        vi.restoreAllMocks();
    });

    it('should take no inputs and send no outputs', () => {
        const mirror = reflectComponentType(DotUveCanvasComponent);

        expect(mirror?.inputs).toEqual([]);
        expect(mirror?.outputs).toEqual([]);
    });

    describe('page', () => {
        it('should load the page URL in the iframe', () => {
            expect(pageFrame().getAttribute('src')).toBe(PAGE_URL);
        });

        it('should load no URL for a traditional page, whose content the iframe writes itself', () => {
            store.$iframeURL.set(new String(''));
            spectator.detectChanges();

            expect(pageFrame().getAttribute('src')).toBe('');
        });

        it('should dim the page while it loads', () => {
            store.$editorIsIframeLoading.set(true);
            spectator.detectChanges();

            expect(pageFrame().style.opacity).toBe('0.5');
        });

        it('should show the page fully once it is loaded', () => {
            expect(pageFrame().style.opacity).toBe('1');
        });

        it('should let the pointer reach the page when nothing is dragged', () => {
            expect(pageFrame().style.pointerEvents).toBe('auto');
        });

        it.each([EDITOR_STATE.DRAGGING, EDITOR_STATE.SCROLL_DRAG])(
            'should keep the pointer off the page while the editor is %s, so the dropzone reads the drag',
            (state) => {
                store.editorState.set(state);
                spectator.detectChanges();

                expect(pageFrame().style.pointerEvents).toBe('none');
            }
        );
    });

    describe('progress bar', () => {
        it('should show while the page in the iframe loads', () => {
            store.$editorIsIframeLoading.set(true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('progress-bar'))).toBeTruthy();
        });

        it('should not show once the page is ready', () => {
            expect(spectator.query(byTestId('progress-bar'))).toBeNull();
        });
    });

    describe('page not found', () => {
        it('should show when the page has no version to show', () => {
            store.uveStatus.set(UVE_STATUS.ERROR);
            spectator.detectChanges();

            expect(spectator.query(byTestId('page-version-not-found'))).toBeTruthy();
        });

        it('should not show while the page loads or once it is loaded', () => {
            expect(spectator.query(byTestId('page-version-not-found'))).toBeNull();

            store.uveStatus.set(UVE_STATUS.LOADING);
            spectator.detectChanges();

            expect(spectator.query(byTestId('page-version-not-found'))).toBeNull();
        });
    });

    describe('sizing', () => {
        it('should size the canvas from its viewport', () => {
            const sizing = spectator.debugElement.query(By.directive(DotUveCanvasSizingDirective));

            expect(sizing).not.toBeNull();
            expect(sizing.nativeElement).toBe(spectator.query(byTestId('canvas-viewport')));
        });

        it('should place the resize handles', () => {
            expect(spectator.query(byTestId('iframe-resize-handles'))).toBeTruthy();
        });
    });

    describe('contentlet tools', () => {
        const HOVERED: ContentletArea = {
            x: 100,
            y: 100,
            width: 500,
            height: 300,
            payload: {
                ...AFTER_CONTENTLET_3,
                language_id: '1',
                pageId: 'page-identifier',
                pageContainers: PAGE_CONTAINERS
            }
        };

        it('should show over the hovered contentlet when the store shows the controls', () => {
            store.editorContentArea.set(HOVERED);
            store.$allowContentDelete.set(false);
            store.editorCanEditStyles.set(true);
            store.$showContentletControls.set(true);
            spectator.detectChanges();

            const tools = spectator.query(DotUveContentletToolsStubComponent);

            expect(tools?.contentletArea()).toEqual(HOVERED);
            expect(tools?.allowContentDelete()).toBe(false);
            expect(tools?.showStyleEditorOption()).toBe(true);
        });

        it('should not show when the store hides the controls', () => {
            store.editorContentArea.set(HOVERED);
            spectator.detectChanges();

            expect(spectator.query(byTestId('contentlet-tools'))).toBeNull();
        });
    });

    describe('lock overlay', () => {
        const lockOverlay = () => spectator.query(byTestId('lock-overlay'));

        const showPage = (lockOptions: WorkflowLockOptions | null, lockFeatureEnabled: boolean) => {
            store.$lockOptions.set(lockOptions);
            store.$lockFeatureEnabled.set(lockFeatureEnabled);
            spectator.detectChanges();
        };

        it.each([true, false])(
            'should not show outside edit mode, even on a page another user locked (lock required: %s)',
            (lockFeatureEnabled) => {
                store.viewMode.set(UVE_MODE.PREVIEW);
                showPage(LOCKED_BY_ANOTHER_USER, lockFeatureEnabled);

                expect(lockOverlay()).toBeNull();
            }
        );

        describe('when the user must lock a page to edit it', () => {
            it('should show on a page nobody locked', () => {
                showPage(UNLOCKED, true);

                expect(lockOverlay()).toBeTruthy();
            });

            it('should show on a page another user locked', () => {
                showPage(LOCKED_BY_ANOTHER_USER, true);

                expect(lockOverlay()).toBeTruthy();
            });

            it('should not show on a page the user locked', () => {
                showPage(LOCKED_BY_THE_USER, true);

                expect(lockOverlay()).toBeNull();
            });
        });

        describe('when the user may edit a page nobody locked', () => {
            it('should not show on a page nobody locked', () => {
                showPage(UNLOCKED, false);

                expect(lockOverlay()).toBeNull();
            });

            it('should show on a page another user locked', () => {
                showPage(LOCKED_BY_ANOTHER_USER, false);

                expect(lockOverlay()).toBeTruthy();
            });

            it('should not show on a page the user locked and can unlock', () => {
                showPage(LOCKED_BY_THE_USER, false);

                expect(lockOverlay()).toBeNull();
            });

            it('should show on a locked page the user has no permission to lock', () => {
                showPage({ ...LOCKED_BY_ANOTHER_USER, canLock: false }, false);

                expect(lockOverlay()).toBeTruthy();
            });

            it('should show on a page the user locked but has no permission to lock any more', () => {
                showPage({ ...LOCKED_BY_THE_USER, canLock: false }, false);

                expect(lockOverlay()).toBeTruthy();
            });

            it('should not show when no page is loaded', () => {
                showPage(null, false);

                expect(lockOverlay()).toBeNull();
            });
        });
    });

    describe('dropzone', () => {
        const BOUNDS: Container[] = [
            { x: 0, y: 0, width: 800, height: 400, contentlets: [], payload: AFTER_CONTENTLET_3 }
        ];

        const startDragging = () => {
            store.editorBounds.set(BOUNDS);
            store.editorDragItem.set(PALETTE_CONTENTLET);
            store.editorState.set(EDITOR_STATE.DRAGGING);
        };

        it('should show the drop targets of the page while the user drags', () => {
            startDragging();
            store.$viewZoomFactor.set(1.5);
            spectator.detectChanges();

            const dropzone = spectator.query(EmaPageDropzoneStubComponent);

            expect(dropzone?.containers()).toEqual(BOUNDS);
            expect(dropzone?.dragItem()).toEqual(PALETTE_CONTENTLET);
            expect(dropzone?.zoomLevel()).toBe(1.5);
        });

        it('should not show when nothing is dragged', () => {
            store.editorBounds.set(BOUNDS);
            spectator.detectChanges();

            expect(spectator.query(byTestId('dropzone'))).toBeNull();
        });

        it('should not show while a drag scrolls the page', () => {
            startDragging();
            store.editorState.set(EDITOR_STATE.SCROLL_DRAG);
            spectator.detectChanges();

            expect(spectator.query(byTestId('dropzone'))).toBeNull();
        });

        it('should not show when the user cannot edit the page', () => {
            startDragging();
            store.editorCanEditContent.set(false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('dropzone'))).toBeNull();
        });

        it('should not show under the lock overlay', () => {
            startDragging();
            store.$lockOptions.set(LOCKED_BY_ANOTHER_USER);
            spectator.detectChanges();

            expect(spectator.query(byTestId('lock-overlay'))).toBeTruthy();
            expect(spectator.query(byTestId('dropzone'))).toBeNull();
        });
    });

    describe('drag image', () => {
        it('should name the content type of the contentlet being dragged', () => {
            store.$areaContentType.set('Banner');
            spectator.detectChanges();

            expect(spectator.query(byTestId('drag-image'))).toHaveText('Banner');
        });
    });

    describe('messages from the page', () => {
        it.each([
            DotCMSUVEAction.SET_CONTENTLET,
            DotCMSUVEAction.SET_SELECTED_CONTENTLET,
            DotCMSUVEAction.SET_BOUNDS,
            DotCMSUVEAction.IFRAME_SCROLL,
            DotCMSUVEAction.CLIENT_READY,
            DotCMSUVEAction.EDIT_CONTENTLET
        ])('should hand %s from the page to the actions handler', (action) => {
            const message = { action, payload: { from: 'the page' } };

            postFromPage(message);

            expect(actionsHandler.handleAction).toHaveBeenCalledWith(message, expect.any(Object));
        });

        it("should give the actions handler the canvas's store, inline editor, page window and host", () => {
            postFromPage({ action: DotCMSUVEAction.SET_BOUNDS, payload: [] });

            expect(actionsHandler.handleAction).toHaveBeenCalledWith(expect.any(Object), {
                uveStore: store,
                inlineEditingService: inlineEditService,
                contentWindow: pageFrame().contentWindow,
                host: '*',
                onCopyContent: expect.any(Function)
            });
        });

        it('should copy a contentlet through the copy service when the actions handler asks to', () => {
            const treeNode: DotTreeNode = {
                containerId: 'container-1',
                contentId: 'contentlet-1',
                pageId: 'page-identifier',
                relationType: '1',
                treeOrder: '0'
            };
            const copied: Observable<DotCMSContentlet> = of(createFakeContentlet());
            spectator.inject(DotCopyContentService).copyInPage.mockReturnValue(copied);

            postFromPage({ action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING, payload: {} });
            const [, deps] = actionsHandler.handleAction.mock.lastCall as [
                unknown,
                ActionsHandlerDependencies
            ];

            expect(deps.onCopyContent(treeNode)).toBe(copied);
            expect(spectator.inject(DotCopyContentService).copyInPage).toHaveBeenCalledWith(
                treeNode
            );
        });

        it("should accept a message from the editor's own window, the page's parent", () => {
            postFromPage({ action: DotCMSUVEAction.SET_BOUNDS, payload: [] }, window);

            expect(actionsHandler.handleAction).toHaveBeenCalled();
        });

        it('should ignore a message from another frame, such as a thumbnail iframe', () => {
            const thumbnail = attach(document.createElement('iframe'));

            postFromPage(
                { action: DotCMSUVEAction.SET_BOUNDS, payload: [] },
                thumbnail.contentWindow
            );

            expect(actionsHandler.handleAction).not.toHaveBeenCalled();
        });

        it('should ignore a message with no source window', () => {
            postFromPage({ action: DotCMSUVEAction.SET_BOUNDS, payload: [] }, null);

            expect(actionsHandler.handleAction).not.toHaveBeenCalled();
        });

        it.each([{ name: 'no action' }, 'a string', null])(
            'should ignore a message that is not a UVE action: %s',
            (data) => {
                postFromPage(data);

                expect(actionsHandler.handleAction).not.toHaveBeenCalled();
            }
        );

        it('should ignore the page height when the iframe is locally accessible', () => {
            store.iframeAccessMode.set(IframeAccessMode.LOCAL);

            postFromPage({ action: DotCMSUVEAction.IFRAME_HEIGHT, payload: { height: 321 } });

            expect(actionsHandler.handleAction).not.toHaveBeenCalled();
        });

        it('should hand the page height to the actions handler when the iframe is cross-origin', () => {
            const message = { action: DotCMSUVEAction.IFRAME_HEIGHT, payload: { height: 321 } };
            store.iframeAccessMode.set(IframeAccessMode.CROSS_ORIGIN);

            postFromPage(message);

            expect(actionsHandler.handleAction).toHaveBeenCalledWith(message, expect.any(Object));
        });

        it('should stop listening once the canvas is gone', () => {
            const pageWindow = pageFrame().contentWindow;
            spectator.fixture.destroy();

            postFromPage({ action: DotCMSUVEAction.SET_BOUNDS, payload: [] }, pageWindow);

            expect(actionsHandler.handleAction).not.toHaveBeenCalled();
        });
    });

    describe('when the page content reloads', () => {
        let postMessage: MockInstance<Window['postMessage']>;

        /** What the store re-emits each time the page in the iframe needs its content again. */
        const reloadContent = (pageType: PageType) => {
            store.$reloadEditorContent.set({
                code: undefined,
                pageType,
                enableInlineEdit: false,
                pageAssetRef: {}
            });
            spectator.detectChanges();
        };

        /** The messages of one kind the canvas posted to the page. */
        const posted = (name: __DOTCMS_UVE_EVENT__) =>
            postMessage.mock.calls.filter(([message]) => message?.name === name);

        beforeEach(() => {
            const pageWindow = pageFrame().contentWindow;

            if (!pageWindow) {
                throw new Error('Expected the iframe to have a window');
            }

            postMessage = vi.spyOn(pageWindow, 'postMessage').mockImplementation(() => undefined);
            store.isClientReady.set(true);
            store.resetEditorProperties.mockClear();
        });

        it('should clear the drag and hover state', () => {
            reloadContent(PageType.TRADITIONAL);

            expect(store.resetEditorProperties).toHaveBeenCalled();
        });

        it('should never message a traditional page, which the iframe rewrites itself', () => {
            store.pageType.set(PageType.TRADITIONAL);
            store.pageAsset.set(pageSnapshot('graphql'));

            reloadContent(PageType.TRADITIONAL);

            expect(postMessage).not.toHaveBeenCalled();
        });

        it('should not message a headless page whose client is not ready yet', () => {
            store.isClientReady.set(false);

            reloadContent(PageType.HEADLESS);

            expect(postMessage).not.toHaveBeenCalled();
        });

        it('should send a headless page its own GraphQL response once the stored page comes from it', () => {
            const page = pageSnapshot('graphql');
            store.pageAsset.set(page);

            reloadContent(PageType.HEADLESS);

            expect(postMessage).toHaveBeenCalledWith(
                { name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA, payload: page?.clientResponse },
                '*'
            );
            expect(posted(__DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE)).toHaveLength(0);
        });

        it('should ask a headless page to reload itself while the stored page is not from its GraphQL query (#37097)', () => {
            store.pageAsset.set(pageSnapshot('rest'));

            reloadContent(PageType.HEADLESS);

            expect(postMessage).toHaveBeenCalledWith(
                { name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE },
                '*'
            );
            expect(store.setPageReloadPending).toHaveBeenCalledWith(true);
            expect(posted(__DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA)).toHaveLength(0);
        });

        // Every CLIENT_READY the page re-announces makes the store emit again. While the GraphQL
        // query stays unresolved, asking again each time would reload the page in a loop.
        it('should ask only once while the GraphQL query stays unresolved, not in a loop', () => {
            store.pageAsset.set(pageSnapshot('rest'));

            for (let i = 0; i < 10; i++) {
                reloadContent(PageType.HEADLESS);
            }

            expect(posted(__DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE)).toHaveLength(1);
            expect(posted(__DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA)).toHaveLength(0);
        });

        // The store clears the pending reload when the query resolves or a new page loads.
        it('should ask again once the store clears the pending reload', () => {
            store.pageAsset.set(pageSnapshot('rest'));
            reloadContent(PageType.HEADLESS);

            store.pageReloadPending.set(false);
            reloadContent(PageType.HEADLESS);

            expect(posted(__DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE)).toHaveLength(2);
        });
    });

    describe('selection', () => {
        const SELECTED: SelectedContentlet = {
            bounds: { x: 0, y: 0, width: 100, height: 100 },
            payload: {
                ...AFTER_CONTENTLET_3,
                language_id: '1',
                pageId: 'page-identifier',
                pageContainers: PAGE_CONTAINERS
            }
        };

        // The page keeps track of the last contentlet clicked; without this a click on the same
        // contentlet would pass through to the page and the toolbar would not come back.
        it('should tell the page when the selection is cleared', () => {
            store.editorSelected.set(SELECTED);
            spectator.detectChanges();

            store.editorSelected.set(null);
            spectator.detectChanges();

            expect(iframeMessenger.selectionCleared).toHaveBeenCalledTimes(1);
        });

        it('should not tell the page when nothing was selected', () => {
            store.editorSelected.set(null);
            spectator.detectChanges();

            expect(iframeMessenger.selectionCleared).not.toHaveBeenCalled();
        });

        it('should not tell the page when another contentlet is selected', () => {
            store.editorSelected.set(SELECTED);
            spectator.detectChanges();

            store.editorSelected.set({
                ...SELECTED,
                bounds: { x: 10, y: 10, width: 50, height: 50 }
            });
            spectator.detectChanges();

            expect(iframeMessenger.selectionCleared).not.toHaveBeenCalled();
        });
    });

    describe('drag bounds', () => {
        it('should ask the page for fresh bounds as soon as a drag starts', () => {
            store.editorState.set(EDITOR_STATE.DRAGGING);
            spectator.detectChanges();

            expect(iframeMessenger.flushBounds).toHaveBeenCalled();
        });

        it('should not ask while nothing is dragged', () => {
            store.editorState.set(EDITOR_STATE.SCROLLING);
            spectator.detectChanges();

            expect(iframeMessenger.flushBounds).not.toHaveBeenCalled();
        });
    });

    describe('iframe load', () => {
        const loadPage = () => {
            pageFrame().dispatchEvent(new Event('load'));
        };

        it("should hand the page's window to the iframe messenger", () => {
            loadPage();

            expect(iframeMessenger.setIframeWindow).toHaveBeenLastCalledWith(
                pageFrame().contentWindow
            );
        });

        it('should start the inline editor again when the page loads during an inline edit', () => {
            store.editorState.set(EDITOR_STATE.INLINE_EDITING);

            loadPage();

            expect(inlineEditService.initEditor).toHaveBeenCalled();
        });

        it('should not start the inline editor when no inline edit is running', () => {
            loadPage();

            expect(inlineEditService.initEditor).not.toHaveBeenCalled();
        });
    });

    describe('when the canvas goes away', () => {
        it("should mark a traditional page's client as ready", () => {
            store.pageType.set(PageType.TRADITIONAL);

            spectator.fixture.destroy();

            expect(store.setIsClientReady).toHaveBeenCalledWith(true);
        });

        it("should leave a headless page's client as it is", () => {
            spectator.fixture.destroy();

            expect(store.setIsClientReady).not.toHaveBeenCalled();
        });
    });

    describe('drops', () => {
        beforeEach(() => {
            store.resetEditorProperties.mockClear();
        });

        it('should keep the browser from opening what was dropped', () => {
            const event = dropOn(attach(document.createElement('div')));

            expect(event.defaultPrevented).toBe(true);
        });

        it('should reset the editor when the drop misses the drop targets', () => {
            store.editorDragItem.set(PALETTE_CONTENTLET);

            dropOn(attach(document.createElement('div')));

            expect(store.resetEditorProperties).toHaveBeenCalled();
            expect(store.editorInsertContentlet).not.toHaveBeenCalled();
        });

        it('should reset the editor when the drag leaves the window', () => {
            window.dispatchEvent(createFakeEvent('dragleave'));

            expect(store.resetEditorProperties).toHaveBeenCalled();
        });

        it('should reset the editor when the drag ends', () => {
            window.dispatchEvent(createFakeEvent('dragend'));

            expect(store.resetEditorProperties).toHaveBeenCalled();
        });

        it.each([
            { case: 'nothing is dragged', dragItem: null },
            { case: 'something unknown is dragged in', dragItem: TEMPORAL_DRAG_ITEM }
        ])('should reset the editor and insert nothing when $case', ({ dragItem }) => {
            store.editorDragItem.set(dragItem);

            dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

            expect(store.resetEditorProperties).toHaveBeenCalled();
            expect(store.editorInsertContentlet).not.toHaveBeenCalled();
        });

        describe('a contentlet from the palette', () => {
            beforeEach(() => {
                store.editorDragItem.set(PALETTE_CONTENTLET);
            });

            it('should insert it at the drop position', () => {
                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

                expect(store.getPageSavePayload).toHaveBeenCalledWith({
                    position: 'after',
                    ...AFTER_CONTENTLET_3
                });
                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    store.getPageSavePayload.mock.results[0].value,
                    'palette-contentlet'
                );
            });

            it('should insert it into an empty container', () => {
                dropOn(dropTarget({ container: CONTAINER_2 }));

                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    expect.objectContaining({ container: CONTAINER_2 }),
                    'palette-contentlet'
                );
            });

            it('should tell the user when the container is full', () => {
                store.editorInsertContentlet.mockReturnValue(CONTAINER_FULL);

                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'warn',
                    summary: 'Container full',
                    detail: 'This container holds up to 1 contentlets',
                    life: 3000
                });
            });

            it('should tell the user when the container already holds it', () => {
                store.editorInsertContentlet.mockReturnValue(ALREADY_IN_CONTAINER);

                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'info',
                    summary: 'Content already added',
                    detail: 'This content is already added to this container',
                    life: 2000
                });
            });

            it('should tell the user nothing when it goes in', () => {
                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

                expect(messageService.add).not.toHaveBeenCalled();
            });
        });

        describe('a contentlet moved on the page', () => {
            beforeEach(() => {
                store.editorDragItem.set(MOVED_CONTENTLET);
            });

            it('should take it out of its container and insert it at the drop position', () => {
                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    expect.objectContaining({
                        pageContainers: [
                            expect.objectContaining({
                                identifier: 'container-1',
                                contentletsId: ['contentlet-2']
                            }),
                            expect.objectContaining({
                                identifier: 'container-2',
                                contentletsId: ['contentlet-3']
                            })
                        ],
                        container: expect.objectContaining({
                            identifier: 'container-2',
                            uuid: '1'
                        }),
                        contentlet: expect.objectContaining({ identifier: 'contentlet-3' }),
                        position: 'after'
                    }),
                    'contentlet-1'
                );
            });
        });

        describe('a content type from the palette', () => {
            beforeEach(() => {
                store.editorDragItem.set(CONTENT_TYPE);
            });

            it('should ask the content editor to create a contentlet of that content type for the drop position', () => {
                const dispatch = vi.spyOn(spectator.inject(Dispatcher), 'dispatch');

                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

                expect(dispatch).toHaveBeenCalledWith(
                    uveContentEditorEvents.uveCreateContentRequested({
                        contentType: { variable: 'Banner', name: 'Banner' },
                        into: store.getPageSavePayload.mock.results[0].value
                    }),
                    { scope: 'self' }
                );
            });

            // In case the user cancels the create, the editor is already idle.
            it('should reset the editor right away', () => {
                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'));

                expect(store.resetEditorProperties).toHaveBeenCalled();
                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
            });
        });

        describe('an image file', () => {
            const IMAGE = new File(['image'], 'photo.png', { type: 'image/png' });

            const UPLOADED = createFakeContentlet({
                identifier: 'uploaded-image',
                inode: 'uploaded-image-inode',
                title: 'photo.png',
                contentType: 'dotAsset',
                baseType: 'DOTASSET'
            });

            beforeEach(() => {
                // A file dragged in from the computer is not a palette item.
                store.editorDragItem.set(TEMPORAL_DRAG_ITEM);
                workflowActionsFireService.publishContentletAndWaitForIndex.mockReturnValue(
                    of(UPLOADED)
                );
            });

            it('should upload and publish it, then insert it at the drop position', () => {
                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'), [IMAGE]);

                expect(tempFileUploadService.upload).toHaveBeenCalledWith(IMAGE);
                expect(
                    workflowActionsFireService.publishContentletAndWaitForIndex
                ).toHaveBeenCalledWith('dotAsset', { asset: mockDotCMSTempFile.id });
                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    expect.objectContaining({
                        ...AFTER_CONTENTLET_3,
                        position: 'after',
                        newContentlet: {
                            identifier: 'uploaded-image',
                            inode: 'uploaded-image-inode',
                            title: 'photo.png',
                            contentType: 'dotAsset',
                            baseType: 'DOTASSET'
                        }
                    }),
                    'uploaded-image'
                );
            });

            it('should tell the user while it uploads and publishes', () => {
                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'), [IMAGE]);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'info',
                    summary: 'Upload image',
                    detail: 'Uploading photo.png',
                    life: 3000
                });
                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'info',
                    summary: 'Workflow Action',
                    detail: 'Publishing photo.png',
                    life: 3000
                });
            });

            it('should tell the user when the container is full', () => {
                store.editorInsertContentlet.mockReturnValue(CONTAINER_FULL);

                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'), [IMAGE]);

                expect(messageService.add).toHaveBeenCalledWith(
                    expect.objectContaining({ severity: 'warn', summary: 'Container full' })
                );
            });

            it('should refuse a file that is not an image', () => {
                const pdf = new File(['pdf'], 'report.pdf', { type: 'application/pdf' });

                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'), [pdf]);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'File upload',
                    detail: 'The file is not an image',
                    life: 3000
                });
                expect(tempFileUploadService.upload).not.toHaveBeenCalled();
                expect(store.resetEditorProperties).toHaveBeenCalled();
                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
            });

            // The upload service reports the HTTP error itself and answers with the status code.
            it('should tell the user and insert nothing when the upload fails', () => {
                tempFileUploadService.upload.mockReturnValue(of('500'));

                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'), [IMAGE]);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'Upload image',
                    detail: 'The image could not be uploaded',
                    life: 3000
                });
                expect(
                    workflowActionsFireService.publishContentletAndWaitForIndex
                ).not.toHaveBeenCalled();
                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
            });

            it('should tell the user and insert nothing when the upload is not an image', () => {
                tempFileUploadService.upload.mockReturnValue(
                    of([{ ...mockDotCMSTempFile, image: false }])
                );

                dropOn(dropTarget(AFTER_CONTENTLET_3, 'after'), [IMAGE]);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'Upload image',
                    detail: 'The image could not be uploaded',
                    life: 3000
                });
                expect(
                    workflowActionsFireService.publishContentletAndWaitForIndex
                ).not.toHaveBeenCalled();
                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
                expect(store.resetEditorProperties).toHaveBeenCalled();
            });
        });
    });

    describe('delete', () => {
        /** The second contentlet of the first container, as the contentlet tools hand it over. */
        const CONTENTLET_2: ActionPayload = {
            language_id: '1',
            pageId: 'page-identifier',
            container: CONTAINER_1,
            contentlet: contentletOnPage('contentlet-2'),
            pageContainers: PAGE_CONTAINERS,
            position: 'after'
        };

        /** The user asks the contentlet tools to remove a contentlet. */
        const deleteFromTools = (payload: ActionPayload) => {
            store.$showContentletControls.set(true);
            spectator.detectChanges();
            spectator.triggerEventHandler(
                DotUveContentletToolsStubComponent,
                'deleteContent',
                payload
            );
        };

        /** The confirmation the canvas asked for. */
        const confirmation = (): Confirmation => confirmationService.confirm.mock.calls[0][0];

        it('should ask for confirmation before removing a contentlet', () => {
            deleteFromTools(CONTENTLET_2);

            expect(confirmationService.confirm).toHaveBeenCalledWith(
                expect.objectContaining({
                    header: 'Deleting Content',
                    message: 'Are you sure you want to remove this content?',
                    acceptLabel: 'Accept',
                    rejectLabel: 'Reject'
                })
            );
            expect(store.editorSave).not.toHaveBeenCalled();
        });

        it('should save the page without the contentlet once the user accepts', () => {
            deleteFromTools(CONTENTLET_2);

            confirmation().accept?.();

            expect(store.editorSave).toHaveBeenCalledWith([
                { identifier: 'container-1', uuid: '1', contentletsId: ['contentlet-1'] },
                { identifier: 'container-2', uuid: '1', contentletsId: ['contentlet-3'] }
            ]);
        });

        it('should clear the selection when the removed contentlet is the selected one', () => {
            store.editorSelected.set({
                bounds: { x: 0, y: 0, width: 100, height: 100 },
                payload: CONTENTLET_2
            });
            deleteFromTools(CONTENTLET_2);

            confirmation().accept?.();

            expect(store.resetSelected).toHaveBeenCalledTimes(1);
        });

        it('should keep the selection when another contentlet is removed', () => {
            store.editorSelected.set({
                bounds: { x: 0, y: 0, width: 100, height: 100 },
                payload: { ...CONTENTLET_2, contentlet: contentletOnPage('contentlet-1') }
            });
            deleteFromTools(CONTENTLET_2);

            confirmation().accept?.();

            expect(store.resetSelected).not.toHaveBeenCalled();
        });
    });

    describe('links in the page', () => {
        /** The user clicks a link in the page; the iframe hands the click over. */
        const clickLink = (event: MouseEvent) => {
            spectator.triggerEventHandler(DotUveIframeComponent, 'internalNav', event);
        };

        const createMockEvent = (href: string, isInlineEditing = false): MouseEvent => {
            const mockAnchor = {
                href,
                getAttribute: vi.fn().mockReturnValue(href),
                closest: vi.fn().mockReturnValue({ href, getAttribute: () => href })
            };

            const mockEvent = {
                target: mockAnchor,
                preventDefault: vi.fn()
            } as unknown as MouseEvent;

            store.editorState.set(
                isInlineEditing ? EDITOR_STATE.INLINE_EDITING : EDITOR_STATE.IDLE
            );

            return mockEvent;
        };

        it('should not do anything if href is empty', () => {
            const mockEvent = {
                target: { href: '', closest: () => null, querySelectorAll: () => [] },
                preventDefault: vi.fn()
            } as unknown as MouseEvent;

            clickLink(mockEvent);

            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).not.toHaveBeenCalled();
        });

        it('should not do anything if isInlineEditing is true', () => {
            const mockEvent = createMockEvent('/test-page', true);

            clickLink(mockEvent);

            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).not.toHaveBeenCalled();
        });

        it('should not do anything for a javascript:-href anchor', () => {
            // A `javascript:…` href never navigates: it's a page's own click handler invoked
            // via the href itself, not a link. `anchor.href` for such an anchor resolves to the
            // literal string back (hostname ""), which wrongly tripped the external-link branch
            // and opened it in a new tab before it bailed early alongside the hash-only case.
            const mockEvent = createMockEvent('javascript:void(0)');

            clickLink(mockEvent);

            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).not.toHaveBeenCalled();
            expect(admin.adminWindow.open).not.toHaveBeenCalled();
            expect(admin.openedLink.click).not.toHaveBeenCalled();
        });

        it('should open external URL in new tab', () => {
            const externalUrl = 'https://external-site.com/page';
            const mockEvent = createMockEvent(externalUrl);

            clickLink(mockEvent);

            expect(admin.openedLink.href).toBe(externalUrl);
            expect(admin.openedLink.target).toBe('_blank');
            expect(admin.openedLink.click).toHaveBeenCalled();
            expect(store.pageLoad).not.toHaveBeenCalled();
        });

        // Without it the anchor also navigates the iframe to the external site, which refuses to
        // be framed and blanks the canvas.
        it('should keep the iframe on the page when opening an external URL', () => {
            const mockEvent = createMockEvent('https://external-site.com/page');

            clickLink(mockEvent);

            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        describe('absolute URLs to the edited site', () => {
            beforeEach(() => {
                const pageAsset = store.pageAsset();

                store.pageAsset.set({
                    ...pageAsset,
                    site: {
                        ...pageAsset?.site,
                        hostname: 'www.site.com',
                        aliases: 'site.com\nalias.site.com'
                    }
                } as PageSnapshot);
            });

            it('should load a link to the site hostname inside the editor', () => {
                const mockEvent = createMockEvent(
                    'https://www.site.com/news?anno_pubblicazione=2025'
                );

                clickLink(mockEvent);

                expect(admin.openedLink.click).not.toHaveBeenCalled();
                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/news',
                    anno_pubblicazione: '2025'
                });
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            it('should load a link to a site alias inside the editor', () => {
                const mockEvent = createMockEvent('https://alias.site.com/news');

                clickLink(mockEvent);

                expect(admin.openedLink.click).not.toHaveBeenCalled();
                expect(store.pageLoad).toHaveBeenCalledWith({ url: '/news' });
            });

            it('should still open a different site in a new tab', () => {
                const mockEvent = createMockEvent('https://other-site.com/news');

                clickLink(mockEvent);

                expect(admin.openedLink.click).toHaveBeenCalled();
                expect(store.pageLoad).not.toHaveBeenCalled();
            });
        });

        it('should load page asset with pathname only for internal URL without query params', () => {
            const mockEvent = createMockEvent('http://localhost:3000/test-page');

            clickLink(mockEvent);

            expect(store.pageLoad).toHaveBeenCalledWith({
                url: '/test-page'
            });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should resolve a descendant anchor when the click lands on a wrapper with no ancestor anchor', () => {
            // Mirrors a real pattern (confirmed live): an anchor styled `pointer-events: none`
            // via a stylesheet rule, wrapped in a container a page's own script makes clickable
            // instead. The click's target is the wrapper — `closest('a')` finds nothing — so the
            // href must come from the single descendant anchor instead. Needs real DOM elements,
            // not plain mocks: resolving this case checks computed style.
            const wrapper = document.createElement('div');
            const anchor = document.createElement('a');
            anchor.href = 'http://localhost:3000/test-page';
            anchor.style.pointerEvents = 'none';
            wrapper.appendChild(anchor);

            const mockEvent = {
                target: wrapper,
                preventDefault: vi.fn()
            } as unknown as MouseEvent;

            clickLink(mockEvent);

            expect(store.pageLoad).toHaveBeenCalledWith({ url: '/test-page' });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should not resolve a link when the wrapper contains more than one descendant anchor', () => {
            const wrapper = document.createElement('div');
            const anchor1 = document.createElement('a');
            anchor1.href = 'http://localhost:3000/page-1';
            anchor1.style.pointerEvents = 'none';
            const anchor2 = document.createElement('a');
            anchor2.href = 'http://localhost:3000/page-2';
            anchor2.style.pointerEvents = 'none';
            wrapper.append(anchor1, anchor2);

            const mockEvent = {
                target: wrapper,
                preventDefault: vi.fn()
            } as unknown as MouseEvent;

            clickLink(mockEvent);

            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).not.toHaveBeenCalled();
        });

        it('should not resolve a link when the single descendant anchor is a normal, clickable link', () => {
            // Regression test (review feedback on #37962): a click resolves to `target` instead
            // of the anchor only when pointer-events: none prevented hit-testing from ever
            // reaching it. Without that check, clicking padding or whitespace inside any
            // container that happens to contain exactly one ordinary link would wrongly
            // navigate there.
            const wrapper = document.createElement('div');
            const anchor = document.createElement('a');
            anchor.href = 'http://localhost:3000/unrelated-page';
            wrapper.appendChild(anchor);

            const mockEvent = {
                target: wrapper,
                preventDefault: vi.fn()
            } as unknown as MouseEvent;

            clickLink(mockEvent);

            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).not.toHaveBeenCalled();
        });

        it('should open a same-host PDF link in a new tab instead of loading it as a page', () => {
            const pdfUrl = 'http://localhost:3000/application/files/report.pdf';
            const mockEvent = createMockEvent(pdfUrl);

            clickLink(mockEvent);

            expect(admin.openedLink.href).toBe(pdfUrl);
            expect(admin.openedLink.target).toBe('_blank');
            expect(admin.openedLink.click).toHaveBeenCalled();
            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should open a /dA/ asset link in a new tab instead of loading it as a page', () => {
            const assetUrl = 'http://localhost:3000/dA/abc123/asset/report.pdf';
            const mockEvent = createMockEvent(assetUrl);

            clickLink(mockEvent);

            expect(admin.openedLink.href).toBe(assetUrl);
            expect(admin.openedLink.click).toHaveBeenCalled();
            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should sever the opener without a windowFeatures popup request', () => {
            // A windowFeatures string makes Firefox treat the call as a popup, which the iframe
            // sandbox rejects with "The operation is insecure".
            const mockEvent = createMockEvent('http://localhost:3000/dA/abc123/asset/photo.jpg');

            clickLink(mockEvent);

            expect(admin.openedLink.rel).toBe('noopener');
            expect(admin.adminWindow.open).not.toHaveBeenCalled();
        });

        it('should resolve a relative asset href against the site origin, not the admin path', () => {
            // The click lands on a child of the anchor, so `target.href` is undefined and the
            // raw (relative) href attribute is what reaches the handler.
            const mockEvent = {
                target: {
                    closest: vi.fn().mockReturnValue({
                        getAttribute: () => 'files/report.pdf'
                    })
                },
                preventDefault: vi.fn()
            } as unknown as MouseEvent;

            clickLink(mockEvent);

            expect(admin.openedLink.href).toBe('http://localhost:3000/files/report.pdf');
            expect(admin.openedLink.click).toHaveBeenCalled();
            expect(store.pageLoad).not.toHaveBeenCalled();
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should keep the iframe on the page when opening a new tab throws', () => {
            // The click must still be cancelled if the open fails for any reason, or the anchor
            // navigates the iframe to the asset, and the throw must not escape into the RxJS
            // subscriber driving this.
            admin.openedLink.click.mockImplementation(() => {
                throw new DOMException('The operation is insecure.');
            });

            const mockEvent = createMockEvent('http://localhost:3000/dA/abc123/asset/photo.jpg');

            expect(() => clickLink(mockEvent)).not.toThrow();

            expect(mockEvent.preventDefault).toHaveBeenCalled();
            expect(store.pageLoad).not.toHaveBeenCalled();
            // click() is what throws, so cleanup has to be unconditional or every refused open
            // strands an anchor in the admin document.
            expect(admin.openedLink.remove).toHaveBeenCalled();
        });

        it('should still load a page when the URL uses the page extension', () => {
            const mockEvent = createMockEvent('http://localhost:3000/test-page/index.html');

            clickLink(mockEvent);

            expect(admin.adminWindow.open).not.toHaveBeenCalled();
            expect(store.pageLoad).toHaveBeenCalledWith({
                url: '/test-page/index.html'
            });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should extract and pass query parameters from URL', () => {
            const mockEvent = createMockEvent(
                'http://localhost:3000/test-page?param1=value1&param2=value2'
            );

            clickLink(mockEvent);

            expect(store.pageLoad).toHaveBeenCalledWith({
                url: '/test-page',
                param1: 'value1',
                param2: 'value2'
            });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should handle URL encoded query parameters', () => {
            const mockEvent = createMockEvent(
                'http://localhost:3000/test-page?path=%2Fhome%2Fuser&name=John%20Doe'
            );

            clickLink(mockEvent);

            expect(store.pageLoad).toHaveBeenCalledWith({
                url: '/test-page',
                path: '/home/user',
                name: 'John Doe'
            });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should handle complex query parameters', () => {
            const mockEvent = createMockEvent(
                'http://localhost:3000/test-page?language_id=2&mode=EDIT&persona=test-persona&custom=value'
            );

            clickLink(mockEvent);

            expect(store.pageLoad).toHaveBeenCalledWith({
                url: '/test-page',
                language_id: '2',
                mode: 'EDIT',
                persona: 'test-persona',
                custom: 'value'
            });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should handle relative URLs correctly', () => {
            const mockEvent = createMockEvent('relative-page?param=value');

            clickLink(mockEvent);

            expect(store.pageLoad).toHaveBeenCalledWith({
                url: '/relative-page',
                param: 'value'
            });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should fallback to closest anchor href when target href is not available', () => {
            const mockEvent = {
                target: {
                    href: null,
                    closest: vi.fn().mockReturnValue({
                        href: 'http://localhost:3000/fallback-page?test=123',
                        getAttribute: vi
                            .fn()
                            .mockReturnValue('http://localhost:3000/fallback-page?test=123')
                    })
                },
                preventDefault: vi.fn()
            } as unknown as MouseEvent;

            clickLink(mockEvent);

            expect(store.pageLoad).toHaveBeenCalledWith({
                url: '/fallback-page',
                test: '123'
            });
            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        describe('same-page navigation (same pathname)', () => {
            const samePathPageParams = (): DotPageApiParams => ({
                url: '/current-page',
                language_id: '1',
                [PERSONA_KEY]: DEFAULT_PERSONA.identifier
            });

            beforeEach(() => {
                store.pageParams.set(samePathPageParams());
            });

            // Cancelled so the link can't load outside the editor; the iframe is scrolled
            // instead of reloaded.
            it('should cancel hash-only navigation on same page without reloading', () => {
                const mockEvent = createMockEvent('http://localhost:3000/current-page#sectionA');

                clickLink(mockEvent);

                expect(store.pageLoad).not.toHaveBeenCalled();
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            it('should not trigger pageLoad for hash-only with complex id', () => {
                const mockEvent = createMockEvent(
                    'http://localhost:3000/current-page#section-123_complex'
                );

                clickLink(mockEvent);

                expect(store.pageLoad).not.toHaveBeenCalled();
            });

            // Traditional pages render in an iframe UVE writes itself, so a browser-handled
            // query change leaves it blank and never reaches the Page API (#36999). It has to
            // go through pageLoad.
            it('should trigger pageLoad with the new query for query-only navigation on same page', () => {
                const mockEvent = createMockEvent('http://localhost:3000/current-page?tab=2');

                clickLink(mockEvent);

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/current-page',
                    tab: '2'
                });
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            it('should pass every query param for multiple query params on same page', () => {
                const mockEvent = createMockEvent(
                    'http://localhost:3000/current-page?filter=value&sort=date'
                );

                clickLink(mockEvent);

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/current-page',
                    filter: 'value',
                    sort: 'date'
                });
            });

            it('should trigger pageLoad when both hash and query are present on same path', () => {
                const mockEvent = createMockEvent(
                    'http://localhost:3000/current-page?tab=2#section'
                );

                clickLink(mockEvent);

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/current-page',
                    tab: '2'
                });
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            it('should load a new language for a same-path language_id link', () => {
                const mockEvent = createMockEvent(
                    'http://localhost:3000/current-page?language_id=3'
                );

                clickLink(mockEvent);

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/current-page',
                    language_id: '3'
                });
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            describe('when the current page already carries a page query param', () => {
                beforeEach(() => {
                    store.pageParams.set({
                        ...samePathPageParams(),
                        mode: UVE_MODE.EDIT,
                        device: 'mobile',
                        anno_pubblicazione: '2025'
                    });
                });

                it('should clear it when the link no longer sets it', () => {
                    const mockEvent = createMockEvent('http://localhost:3000/current-page');

                    clickLink(mockEvent);

                    // Strict: toHaveBeenCalledWith would treat an undefined key as absent.
                    expect(store.pageLoad.mock.lastCall?.[0]).toStrictEqual({
                        url: '/current-page',
                        anno_pubblicazione: undefined
                    });
                });

                // `/folder/` and `/folder/index` are one page under two paths, so the path
                // comparison sees a different page. The filter must still be cleared on the
                // first click (QA follow-up on #36999).
                it('should clear it when the link reaches the same page by its index path', () => {
                    store.pageParams.set({
                        ...samePathPageParams(),
                        url: '/folder/',
                        anno_pubblicazione: '2026'
                    });
                    const mockEvent = createMockEvent('http://localhost:3000/folder/index');

                    clickLink(mockEvent);

                    // Strict: toHaveBeenCalledWith would treat an undefined key as absent.
                    expect(store.pageLoad.mock.lastCall?.[0]).toStrictEqual({
                        url: '/folder/index',
                        anno_pubblicazione: undefined
                    });
                });

                it('should not carry it to a different page', () => {
                    const mockEvent = createMockEvent('http://localhost:3000/other-page');

                    clickLink(mockEvent);

                    // Strict: toHaveBeenCalledWith would treat an undefined key as absent.
                    expect(store.pageLoad.mock.lastCall?.[0]).toStrictEqual({
                        url: '/other-page',
                        anno_pubblicazione: undefined
                    });
                });

                it('should replace it with the value a different page link sets', () => {
                    const mockEvent = createMockEvent(
                        'http://localhost:3000/other-page?anno_pubblicazione=2024'
                    );

                    clickLink(mockEvent);

                    expect(store.pageLoad).toHaveBeenCalledWith({
                        url: '/other-page',
                        anno_pubblicazione: '2024'
                    });
                });

                it('should replace it when the link sets a new value', () => {
                    const mockEvent = createMockEvent(
                        'http://localhost:3000/current-page?anno_pubblicazione=2024'
                    );

                    clickLink(mockEvent);

                    expect(store.pageLoad).toHaveBeenCalledWith({
                        url: '/current-page',
                        anno_pubblicazione: '2024'
                    });
                });

                it('should still scroll a hash-only link without reloading', () => {
                    const mockEvent = createMockEvent('http://localhost:3000/current-page#section');

                    clickLink(mockEvent);

                    expect(store.pageLoad).not.toHaveBeenCalled();
                });
            });

            it('should trigger pageLoad when re-clicking an exact duplicate of the current URL (no hash, no query)', () => {
                // Confirmed live (#37327): for a traditional page, the iframe's real location is
                // always the synthetic `about:srcdoc`. If this early-returns without calling
                // preventDefault() for an exact duplicate link (no hash/query to react to), the
                // anchor's own default action navigates the iframe to a live fetch of the real
                // URL, permanently blanking the edit canvas with no console error.
                const mockEvent = createMockEvent('http://localhost:3000/current-page');

                clickLink(mockEvent);

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/current-page'
                });
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            it('should trigger pageLoad when navigating to different page with hash', () => {
                const mockEvent = createMockEvent('http://localhost:3000/other-page#section');

                clickLink(mockEvent);

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/other-page'
                });
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            it('should trigger pageLoad when navigating to different page with query', () => {
                const mockEvent = createMockEvent('http://localhost:3000/other-page?tab=1');

                clickLink(mockEvent);

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/other-page',
                    tab: '1'
                });
                expect(mockEvent.preventDefault).toHaveBeenCalled();
            });

            it('should handle root path hash navigation', () => {
                store.pageParams.set({
                    url: '/',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                });

                const mockEvent = createMockEvent('http://localhost:3000/#top');

                clickLink(mockEvent);

                expect(store.pageLoad).not.toHaveBeenCalled();
            });

            // Traditional VTL pages render via doc.write into an iframe whose src is empty, so
            // the document URL stays at about:blank. The anchor's IDL .href then resolves
            // "#section" to "about:blank#section" — hostname "" — which used to fall into the
            // external-link branch and trigger window.open.
            it('should not open a new tab for hash-only links when iframe document is about:blank', () => {
                const mockEvent = {
                    target: {
                        href: 'about:blank#page-section',
                        getAttribute: vi.fn().mockReturnValue('#page-section'),
                        closest: vi.fn().mockReturnValue({
                            href: 'about:blank#page-section',
                            getAttribute: () => '#page-section'
                        })
                    },
                    preventDefault: vi.fn()
                } as unknown as MouseEvent;

                clickLink(mockEvent);

                expect(admin.adminWindow.open).not.toHaveBeenCalled();
                expect(admin.openedLink.click).not.toHaveBeenCalled();
                expect(store.pageLoad).not.toHaveBeenCalled();
                expect(mockEvent.preventDefault).not.toHaveBeenCalled();
            });
        });
    });

    describe('inline editing (#37376)', () => {
        /** The user clicks in the page; the iframe hands the click over. */
        const clickInPage = (target: HTMLElement) => {
            spectator.triggerEventHandler(DotUveIframeComponent, 'inlineEditing', {
                target
            } as unknown as MouseEvent);
        };

        /**
         * Build a `[data-mode]` field inside a contentlet wrapper, mirroring what the container
         * renderer emits.
         */
        const buildInlineTarget = (canEdit?: string): HTMLElement => {
            const wrapper = document.createElement('div');
            wrapper.dataset['dotObject'] = 'contentlet';
            wrapper.dataset['dotInode'] = 'inode-123';
            if (canEdit !== undefined) {
                wrapper.dataset['dotCanEdit'] = canEdit;
            }

            const field = document.createElement('div');
            field.dataset['mode'] = 'minimal';
            field.dataset['inode'] = 'inode-123';
            field.dataset['fieldName'] = 'body';
            field.dataset['language'] = '1';
            wrapper.appendChild(field);
            attach(wrapper);

            return field;
        };

        it('should start inline editing on the field the user clicks', () => {
            clickInPage(buildInlineTarget('true'));

            expect(inlineEditService.handleInlineEdit).toHaveBeenCalledWith({
                mode: 'minimal',
                inode: 'inode-123',
                fieldName: 'body',
                language: '1'
            });
        });

        it('should start inline editing from a click inside the field', () => {
            const bold = document.createElement('strong');
            buildInlineTarget('true').appendChild(bold);

            clickInPage(bold);

            expect(inlineEditService.handleInlineEdit).toHaveBeenCalledWith(
                expect.objectContaining({ inode: 'inode-123', fieldName: 'body' })
            );
        });

        it('should not start inline editing for a click outside an inline-editable field', () => {
            clickInPage(attach(document.createElement('p')));

            expect(inlineEditService.handleInlineEdit).not.toHaveBeenCalled();
            expect(messageService.add).not.toHaveBeenCalled();
        });

        it('should not start inline editing on a contentlet the user cannot edit', () => {
            clickInPage(buildInlineTarget('false'));

            expect(inlineEditService.handleInlineEdit).not.toHaveBeenCalled();
        });

        it('should tell the user why with a toast instead of failing silently', () => {
            // The field has no control to grey out and no hover target for a tooltip, so a
            // silent no-op reads as a broken editor.
            clickInPage(buildInlineTarget('false'));

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    detail: 'You do not have permission to edit this content'
                })
            );
        });

        it('should start inline editing when the permission attribute is absent', () => {
            // Headless pages never emit it; fail open.
            clickInPage(buildInlineTarget(undefined));

            expect(inlineEditService.handleInlineEdit).toHaveBeenCalled();
        });
    });
});
