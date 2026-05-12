import { patchState, signalState } from '@ngrx/signals';
import { EMPTY, Observable, fromEvent, of } from 'rxjs';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass, NgStyle } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    DestroyRef,
    ElementRef,
    OnDestroy,
    ViewChild,
    WritableSignal,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { PopoverModule } from 'primeng/popover';
import { ProgressBarModule } from 'primeng/progressbar';
import { TabsModule } from 'primeng/tabs';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, filter, map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotCMSContentlet,
    DotCMSTempFile,
    DotLanguage,
    DotTreeNode,
    FeaturedFlags,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotEditContentDialogComponent, EditContentDialogData } from '@dotcms/edit-content';
import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { DotCMSPage, DotCMSURLContentMap, DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { StyleEditorFormSchema, __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { DotCopyContentModalService, DotMessagePipe } from '@dotcms/ui';
import { WINDOW, isEqual } from '@dotcms/utils';
import { getContentletsInContainer } from '@dotcms/uve/internal';

import { DotUveContentletQuickEditComponent } from './components/dot-uve-contentlet-quick-edit/dot-uve-contentlet-quick-edit.component';
import { DotUveContentletToolsComponent } from './components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component';
import { DotUveDeviceControlsComponent } from './components/dot-uve-device-controls/dot-uve-device-controls.component';
import { DotUveIframeComponent } from './components/dot-uve-iframe/dot-uve-iframe.component';
import { DotUveIframeResizeHandlesComponent } from './components/dot-uve-iframe-resize-handles/dot-uve-iframe-resize-handles.component';
import { DotUveIframeSizeInputComponent } from './components/dot-uve-iframe-size-input/dot-uve-iframe-size-input.component';
import { DotUveLockOverlayComponent } from './components/dot-uve-lock-overlay/dot-uve-lock-overlay.component';
import { DotUvePageVersionNotFoundComponent } from './components/dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { DotPaletteListStore } from './components/dot-uve-palette/components/dot-uve-palette-list/store/store';
import { DotUveStyleEditorEmptyStateComponent } from './components/dot-uve-palette/components/dot-uve-style-editor-empty-state/dot-uve-style-editor-empty-state.component';
import { DotUveStyleEditorFormComponent } from './components/dot-uve-palette/components/dot-uve-style-editor-form/dot-uve-style-editor-form.component';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DeviceSelectorChange } from './components/dot-uve-toolbar/components/dot-uve-device-selector/dot-uve-device-selector.models';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { DotUveZoomControlsComponent } from './components/dot-uve-zoom-controls/dot-uve-zoom-controls.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import { EmaDragItem } from './components/ema-page-dropzone/types';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiService } from '../services/dot-page-api/dot-page-api.service';
import { DotUveActionsHandlerService } from '../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveDragDropService } from '../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { UveIframeMessengerService } from '../services/iframe-messenger/uve-iframe-messenger.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import {
    CONTAINER_INSERT_ERROR,
    EDITOR_STATE,
    NG_CUSTOM_EVENTS,
    UVE_STATUS
} from '../shared/enums';
import {
    ActionPayload,
    ClientData,
    DeletePayload,
    DialogAction,
    InsertPayloadFromDelete,
    PageContainer,
    PositionPayload,
    PostMessage,
    VTLFile
} from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { IframeAccessMode, PageType } from '../store/models';
import {
    TEMPORAL_DRAG_ITEM,
    areContainersEquals,
    createFullURL,
    deleteContentletFromContainer,
    getTargetUrl,
    insertContentletInContainer,
    isSamePageNavigation,
    measureCanvasAvailableSize,
    shouldNavigate
} from '../utils';

// Message keys constants
const MESSAGE_KEY = {
    DUPLICATE_CONTENT: {
        TITLE: 'editpage.content.add.already.title',
        MESSAGE: 'editpage.content.add.already.message'
    },
    CONTAINER_LIMIT: {
        TITLE: 'editpage.content.container.limit.title',
        MESSAGE: 'editpage.content.container.limit.message'
    }
} as const;

@Component({
    selector: 'dot-edit-ema-editor',
    templateUrl: './edit-ema-editor.component.html',
    styleUrls: ['./edit-ema-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        NgClass,
        NgStyle,
        FormsModule,
        ReactiveFormsModule,
        DotEmaDialogComponent,
        ConfirmDialogModule,
        EmaPageDropzoneComponent,
        ProgressBarModule,
        DotResultsSeoToolComponent,
        DotUveToolbarComponent,
        DotBlockEditorSidebarComponent,
        DotUvePageVersionNotFoundComponent,
        DotUveContentletToolsComponent,
        DotUveContentletQuickEditComponent,
        DotUveStyleEditorEmptyStateComponent,
        DotUveLockOverlayComponent,
        DotUvePaletteComponent,
        DotUveStyleEditorFormComponent,
        DotUveIframeComponent,
        DotUveIframeResizeHandlesComponent,
        DotUveIframeSizeInputComponent,
        ButtonModule,
        ToolbarModule,
        TabsModule,
        InputGroupModule,
        InputGroupAddonModule,
        DotUveZoomControlsComponent,
        ClipboardModule,
        PopoverModule,
        TooltipModule,
        DotMessagePipe,
        DotUveDeviceControlsComponent
    ],
    providers: [
        DotPaletteListStore,
        DotCopyContentService,
        DotCopyContentModalService,
        DotHttpErrorManagerService,
        DotContentletService,
        DotTempFileUploadService,
        DotUveActionsHandlerService,
        DotUveDragDropService
    ]
})
export class EditEmaEditorComponent implements OnDestroy, AfterViewInit {
    @ViewChild('dialog') dialog: DotEmaDialogComponent;
    @ViewChild('iframe') iframeComponent!: DotUveIframeComponent;
    @ViewChild('blockSidebar') blockSidebar: DotBlockEditorSidebarComponent;
    @ViewChild('customDragImage') customDragImage: ElementRef<HTMLDivElement>;
    @ViewChild('zoomContainer') zoomContainer!: ElementRef<HTMLDivElement>;
    @ViewChild('canvasViewport') canvasViewport!: ElementRef<HTMLDivElement>;
    @ViewChild('editorContent') editorContent!: ElementRef<HTMLDivElement>;

    get iframe(): ElementRef<HTMLIFrameElement> | undefined {
        return this.iframeComponent?.iframe;
    }

    protected readonly uveStore = inject(UVEStore);

    /**
     * Right sidebar tab management using NgRx signalState (similar to palette component).
     * Tabs: 0 = Contentlet Quick Edit, 1 = Style Editor
     */
    readonly #rightSidebarTabState = signalState({
        currentTab: 0
    });

    readonly $editorPanelActiveTab = this.#rightSidebarTabState.currentTab;
    readonly $showStyleEditorTab = computed(() => this.uveStore.editorCanEditStyles());
    readonly $styleSchema = computed<StyleEditorFormSchema | undefined>(() => {
        return this.uveStore.$styleSchema();
    });
    readonly $styleSchemaContentTypeVar = computed(
        () => this.uveStore.editorSelected()?.payload?.contentlet?.contentType ?? ''
    );

    protected readonly $contentletEditData = computed(() => {
        const { container, contentlet: contentletPayload } =
            this.uveStore.editorSelected()?.payload ?? {};

        // Get the full contentlet from containers using the SDK utility.
        // It handles both uuid-${uuid} and uuid-dotParser_${uuid} key formats.
        let contentlet: DotCMSContentlet = contentletPayload as DotCMSContentlet;
        const pageAsset = this.uveStore.pageAsset();

        if (
            container?.identifier &&
            container?.uuid &&
            contentletPayload?.identifier &&
            pageAsset?.containers?.[container.identifier]
        ) {
            const contentlets = getContentletsInContainer(pageAsset, {
                identifier: container.identifier,
                uuid: container.uuid,
                historyUUIDs: []
            });
            const foundContentlet = contentlets.find(
                (c) => c.identifier === contentletPayload.identifier
            );

            if (foundContentlet) {
                contentlet = foundContentlet as DotCMSContentlet;
            }
        }

        return { container, contentlet };
    });
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly messageService = inject(MessageService);
    private readonly window = inject(WINDOW);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotCopyContentService = inject(DotCopyContentService);
    private readonly dotContentTypeService = inject(DotContentTypeService);
    private readonly dotCopyContentModalService = inject(DotCopyContentModalService);
    private readonly dotContentletService = inject(DotContentletService);
    private readonly dialogService = inject(DialogService);
    private readonly tempFileUploadService = inject(DotTempFileUploadService);
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly inlineEditingService = inject(InlineEditService);
    private readonly dotPageApiService = inject(DotPageApiService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly actionsHandler = inject(DotUveActionsHandlerService);
    private readonly globalStore = inject(GlobalStore);
    private readonly dragDropService = inject(DotUveDragDropService);
    private readonly iframeMessenger = inject(UveIframeMessengerService);

    readonly host = '*';
    readonly $ogTags: WritableSignal<SeoMetaTags> = signal(undefined);

    // Component builds its own editor props locally
    protected readonly $showDialogs = computed<boolean>(() => {
        const canEditPage = this.uveStore.editorCanEditContent();
        const isEditState = this.uveStore.viewMode() === UVE_MODE.EDIT;
        return canEditPage && isEditState;
    });

    protected readonly $showBlockEditorSidebar = computed<boolean>(() => {
        const canEditPage = this.uveStore.editorCanEditContent();
        const isEditState = this.uveStore.viewMode() === UVE_MODE.EDIT;
        return canEditPage && isEditState;
    });

    protected readonly $isIframeLoading = computed<boolean>(() => {
        const mode = this.uveStore.viewMode();
        const pageType = this.uveStore.pageType();
        const isClientReady = this.uveStore.isClientReady();
        const isEditMode = mode === UVE_MODE.EDIT;
        const isPageReady = pageType === PageType.TRADITIONAL || isClientReady || !isEditMode;

        return !isPageReady || this.uveStore.uveStatus() === UVE_STATUS.LOADING;
    });

    protected readonly $progressBar = computed<boolean>(() => {
        const mode = this.uveStore.viewMode();
        const pageType = this.uveStore.pageType();
        const isClientReady = this.uveStore.isClientReady();

        const isEditMode = mode === UVE_MODE.EDIT;
        const isPageReady = pageType === PageType.TRADITIONAL || isClientReady || !isEditMode;
        return !isPageReady || this.uveStore.uveStatus() === UVE_STATUS.LOADING;
    });

    protected readonly $dropzone = computed(() => {
        const canEditPage = this.uveStore.editorCanEditContent();
        const state = this.uveStore.editorState();
        const bounds = this.uveStore.editorBounds();
        const dragItem = this.uveStore.editorDragItem();

        const showDropzone = canEditPage && state === EDITOR_STATE.DRAGGING;

        return showDropzone
            ? {
                  bounds,
                  dragItem
              }
            : null;
    });

    protected readonly $seoResults = computed(() => {
        const socialMedia = this.uveStore.viewSocialMedia();
        const ogTags = this.uveStore.editorOgTags();
        const shouldShowSeoResults = socialMedia && ogTags;

        return shouldShowSeoResults
            ? {
                  ogTags,
                  socialMedia
              }
            : null;
    });

    readonly $mode = this.uveStore.viewMode;

    // Component-level computed to avoid cross-feature dependency
    readonly $editorContentStyles = computed<Record<string, string>>(() => {
        const socialMedia = this.uveStore.viewSocialMedia();
        return {
            display: socialMedia ? 'none' : 'grid'
        };
    });

    readonly ogTagsResults$ = toObservable<SeoMetaTagsResult[]>(this.uveStore.viewOgTagsResults);

    get $paletteOpen() {
        return this.uveStore.editorPaletteOpen();
    }
    get $editPanelOpen() {
        return this.uveStore.editorEditPanelOpen();
    }
    readonly $lockOptions = this.uveStore.$lockOptions;
    readonly $showLockOverlay = computed(() => {
        const lockOptions = this.$lockOptions();

        const lockFeatureEnabled = this.uveStore.$lockFeatureEnabled();
        const isLocked = lockOptions?.isLocked;
        const isLockedByCurrentUser = lockOptions?.isLockedByCurrentUser;
        const canLock = lockOptions?.canLock;

        // For feature flag, we force the user to lock pages to edit
        // So we show the lock overlay if the page is not locked
        if (lockFeatureEnabled) {
            return !isLocked;
        }

        // Without feature flag, we show the lock overlay if the page is locked
        // And is not locked by the current user or the user has no permission to lock/unlock
        if (isLocked && (!isLockedByCurrentUser || !canLock)) {
            return true;
        }

        return false;
    });

    readonly $showContentletControls = this.uveStore.$showContentletControls;
    get $contentArea() {
        return this.uveStore.editorContentArea();
    }
    readonly $allowContentDelete = this.uveStore.$allowContentDelete;
    readonly $isDragging = this.uveStore.$isDragging;

    readonly UVE_STATUS = UVE_STATUS;
    readonly UVE_MODE = UVE_MODE;
    readonly DotCMSClazzes = DotCMSClazzes;

    readonly $viewCanvasOuterStyles = this.uveStore.$viewCanvasOuterStyles;
    readonly $viewCanvasInnerStyles = this.uveStore.$viewCanvasInnerStyles;

    readonly $iframeSrc = computed((): string => {
        const url = this.uveStore.$iframeURL();
        return (typeof url === 'string' ? url : '') || '';
    });
    readonly $iframePointerEvents = computed((): string => {
        // Block iframe pointer events while a drag is in flight (with or
        // without iframe scroll) so the dropzone reads drag events instead
        // of the page underneath.
        const state = this.uveStore.editorState();
        const dragIsActive = state === EDITOR_STATE.DRAGGING || state === EDITOR_STATE.SCROLL_DRAG;
        return dragIsActive ? 'none' : 'auto';
    });
    readonly $iframeOpacity = computed((): number => {
        return this.$isIframeLoading() ? 0.5 : 1;
    });

    readonly $pageURL = computed((): string => {
        return this.$pageURLS()[0]?.value ?? '/';
    });

    get contentWindow(): Window | null {
        return this.iframeComponent?.contentWindow || null;
    }

    readonly $translatePageEffect = effect(() => {
        const { page, currentLanguage } = this.uveStore.pageTranslateProps();

        if (currentLanguage && !currentLanguage?.translated) {
            this.createNewTranslation(currentLanguage, page);
        }
    });

    readonly $handleReloadContentEffect = effect(() => {
        /**
         * We should not depend on this `$reloadEditorContent` computed to `resetEditorProperties` or `resetDialog`
         * This depends on the `code` with each the page renders code. This reset should be done in `widthLoad` signal feature but we can't do it yet
         */
        const { pageType } = this.uveStore.$reloadEditorContent();
        const isClientReady = untracked(() => this.uveStore.isClientReady());

        untracked(() => {
            this.uveStore.resetEditorProperties();
            this.dialog?.resetActionPayload();
        });

        if (pageType === PageType.TRADITIONAL || !isClientReady) {
            return;
        }

        this.reloadIframeContent();
    });

    /**
     * Notify the iframe whenever the editor clears its contentlet selection
     * (canvas resize, scroll, device switch, etc.). The SDK keeps a
     * `lastSelectedInode` tracker that gates click→passthrough behavior; if
     * the editor drops the selection without telling the SDK, a follow-up
     * click on the same contentlet would be silently treated as a passthrough
     * (page click) and the toolbar wouldn't reappear.
     */
    #lastSelectedAreaWasSet = false;
    readonly $notifySelectionClearedEffect = effect(() => {
        const hasSelection = !!this.uveStore.editorSelected();

        untracked(() => {
            if (this.#lastSelectedAreaWasSet && !hasSelection) {
                this.iframeMessenger.selectionCleared();
            }
            this.#lastSelectedAreaWasSet = hasSelection;
        });
    });

    readonly $handleIsDraggingEffect = effect(() => {
        const isDragging = this.uveStore.$editorIsInDraggingState();

        if (!isDragging) {
            return;
        }

        // Drag needs bounds NOW so the dropzone can compute targets
        // before the user moves another pixel — bypass the auto-bounds
        // debounce.
        this.iframeMessenger.flushBounds();
    });

    // Reflow re-anchoring (sidebar open/close, scroll-end, device switch,
    // zoom change, image/font load shifts, media-query reflows) flows
    // through the SDK's debounced ResizeObserver in onAutoBounds, which
    // emits SET_BOUNDS automatically when the iframe layout settles. The
    // editor only needs to flip editorState back to IDLE so the overlay
    // un-hides; SET_BOUNDS lands ~100ms later with fresh coords.

    // Device-switch / zoom-change recovery is handled by the SET_BOUNDS
    // handler in DotUveActionsHandlerService: when the SDK's auto-bounds
    // channel pushes fresh bounds after the layout settles, the handler
    // flips editorState back to IDLE. Flipping IDLE earlier (on rAF after
    // the trigger) used to race the bounds round-trip and caused the
    // selected overlay to flash at stale coordinates before snapping to
    // the new ones.

    readonly $responsiveModeSyncEffect = effect(() => {
        const isResponsive = this.uveStore.$viewIsResponsiveMode();
        if (!isResponsive) {
            return;
        }

        untracked(() => {
            if (this.uveStore.editorState() === EDITOR_STATE.RESIZING) {
                return;
            }
            const width = this.uveStore.viewCanvasAvailableWidth();
            const height = this.uveStore.viewCanvasAvailableHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            this.uveStore.viewSetIframeSize({ width, height });
        });
    });

    /**
     * Handle right sidebar tab changes
     */
    protected handleRightSidebarTabChange(index: string | number | undefined): void {
        if (index === undefined) {
            return;
        }

        const currentTab = Number(index);

        if (isNaN(currentTab)) {
            return;
        }

        patchState(this.#rightSidebarTabState, { currentTab });
    }

    ngAfterViewInit(): void {
        if (!this.iframe) {
            return;
        }

        fromEvent<MessageEvent>(this.window, 'message')
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((event) => this.#isMessageFromUvePreviewFrame(event))
            )
            .subscribe((event) => {
                const data = event.data;
                if (this.#isUvePostMessage(data)) {
                    this.handleUveMessage(data);
                }
            });

        this.setupDragDrop();
        this.#setupCanvasViewportObserver();
    }

    #canvasResizeObserver: ResizeObserver | null = null;

    #setupCanvasViewportObserver(): void {
        const el = this.canvasViewport?.nativeElement;
        if (!el) {
            return;
        }

        const apply = () => {
            const size = measureCanvasAvailableSize(el);
            if (!size) {
                return;
            }

            // Snapshot the previous canvas size *before* the store update so
            // we can tell whether the iframe was auto-filling it.
            const prevCanvasW = this.uveStore.viewCanvasAvailableWidth();
            const prevCanvasH = this.uveStore.viewCanvasAvailableHeight();
            // viewSetCanvasAvailableSize flips RESIZING internally when the
            // canvas actually changes size — overlays hide until SET_BOUNDS
            // settles them.
            this.uveStore.viewSetCanvasAvailableSize(size);

            if (!this.uveStore.$viewIsResponsiveMode()) {
                return;
            }

            const iframeWidth = this.uveStore.viewIframeWidth();
            const iframeHeight = this.uveStore.viewIframeHeight();

            // The iframe was auto-filling the canvas (its dimensions match the
            // previous canvas size), so keep it filling. If the user has
            // explicitly resized it, leave their size alone — but re-apply it
            // through viewSetIframeSize so its built-in canvas-clamp can
            // shrink it when the canvas shrinks (e.g. a side panel opens).
            const wasAutoFilling = iframeWidth === prevCanvasW && iframeHeight === prevCanvasH;

            if (wasAutoFilling) {
                this.uveStore.viewSetIframeSize(size);
            } else {
                this.uveStore.viewSetIframeSize({
                    width: iframeWidth,
                    height: iframeHeight
                });
            }
        };

        // Initial sync — runs synchronously in ngAfterViewInit before first paint
        apply();

        this.#canvasResizeObserver = new ResizeObserver(() => apply());
        this.#canvasResizeObserver.observe(el);

        this.destroyRef.onDestroy(() => {
            this.#canvasResizeObserver?.disconnect();
            this.#canvasResizeObserver = null;
        });
    }

    private setupDragDrop(): void {
        if (!this.iframe) {
            return;
        }

        this.dragDropService.setupDragEvents(
            this.uveStore,
            this.iframe,
            this.customDragImage,
            this.contentWindow,
            this.host,
            {
                onDrop: (event) => this.handleDrop(event),
                onDragEnter: () => {
                    // Handled in dragDropService
                },
                onDragOver: () => {
                    // Handled in dragDropService
                },
                onDragLeave: () => {
                    this.uveStore.resetEditorProperties();
                },
                onDragEnd: () => {
                    this.uveStore.resetEditorProperties();
                },
                onDragStart: () => {
                    // Handled in dragDropService
                }
            }
        );
    }

    private handleUveMessage(message: PostMessage): void {
        if (
            !this.#isUvePostMessage(message) ||
            (message.action === DotCMSUVEAction.IFRAME_HEIGHT &&
                this.uveStore.iframeAccessMode() === IframeAccessMode.LOCAL)
        ) {
            return;
        }

        this.actionsHandler.handleAction(message, {
            uveStore: this.uveStore,
            dialog: this.dialog,
            blockSidebar: this.blockSidebar,
            inlineEditingService: this.inlineEditingService,
            contentWindow: this.contentWindow,
            host: this.host,
            onCopyContent: (currentTreeNode) => this.handleCopyContent(currentTreeNode),
            onSectionOffset: (payload) => this.handleSectionOffset(payload)
        });
    }

    /**
     * Scroll the iframe to the given y offset (sent by the SDK from inside the
     * iframe, e.g. when a section node is selected in the palette).
     *
     * Pre-PR-35539 this scrolled the canvas viewport, but the canvas viewport
     * is now overflow:hidden and the iframe scrolls internally. offsetTop is
     * already in the iframe's CSS coordinate space, so no zoom math is needed.
     */
    protected handleSectionOffset({ offsetTop }: { offsetTop: number }): void {
        this.iframeComponent?.contentWindow?.scrollTo({
            top: Math.max(0, offsetTop),
            left: 0,
            behavior: 'smooth'
        });
    }

    private handleDrop(event: DragEvent): void {
        event.preventDefault();
        const target = event.target as HTMLDivElement;
        const { position, payload, dropzone } = target.dataset;

        if (dropzone !== 'true') {
            this.uveStore.resetEditorProperties();
            return;
        }

        const data: ClientData = JSON.parse(payload || '{}');
        const file = event.dataTransfer?.files[0];
        const dragItem = this.uveStore.editorDragItem();

        if (file) {
            this.handleFileUpload({
                file,
                data,
                position,
                dragItem
            });
            return;
        }

        if (!isEqual(dragItem, TEMPORAL_DRAG_ITEM) && dragItem) {
            const positionPayload = <PositionPayload>{
                position,
                ...data
            };

            this.placeItem(positionPayload, dragItem);
            return;
        }

        this.uveStore.resetEditorProperties();
    }

    /**
     * Handles internal navigation by preventing the default behavior of the click event,
     * updating the query parameters, and opening external links in a new tab.
     *
     * @param e - The MouseEvent object representing the click event.
     */
    handleInternalNav(e: MouseEvent) {
        const target = e.target as HTMLAnchorElement;
        const anchor = target.closest('a');
        const rawHref = anchor?.getAttribute('href') ?? '';

        // Same-page anchors (#section) are browser-handled scrolls. Bail
        // before any URL parsing — for traditional VTL pages the iframe
        // sits at about:blank, so target.href resolves to "about:blank#section",
        // whose hostname is "" and would incorrectly trip the external-link
        // branch below.
        if (rawHref.startsWith('#')) {
            return;
        }

        const href = target.href || rawHref;
        const isInlineEditing = this.uveStore.editorState() === EDITOR_STATE.INLINE_EDITING;

        // If the link is not valid or we are in inline editing mode, we do nothing
        if (!href || isInlineEditing) {
            return;
        }

        const url = new URL(href, this.window.location.origin);
        // Get the query parameters from the URL
        const urlQueryParams = Object.fromEntries(url.searchParams.entries());

        if (url.hostname !== this.window.location.hostname) {
            this.window.open(href, '_blank');

            return;
        }

        // Same pathname (any hash/query): let the browser handle it (anchors, query-driven UI)
        if (isSamePageNavigation(href, this.uveStore.pageParams()?.url)) {
            return;
        }

        this.uveStore.pageLoad({ url: url.pathname, ...urlQueryParams });
        e.preventDefault();
    }

    /**
     * Handles the inline editing functionality triggered by a mouse event.
     * @param e - The mouse event that triggered the inline editing.
     */
    handleInlineEditing(e: MouseEvent): void {
        const target = e.target as HTMLElement;
        const element: HTMLElement = target.dataset?.mode ? target : target.closest('[data-mode]');

        if (!element?.dataset.mode) {
            return;
        }

        this.inlineEditingService.handleInlineEdit({
            ...element.dataset
        } as unknown as { language: string; mode: string; inode: string; fieldName: string });
    }

    onIframePageLoad(): void {
        this.iframeMessenger.setIframeWindow(this.iframe?.nativeElement.contentWindow ?? null);

        if (this.uveStore.editorState() === EDITOR_STATE.INLINE_EDITING) {
            this.inlineEditingService.initEditor();
        }
    }

    ngOnDestroy(): void {
        if (this.uveStore.pageType() === PageType.TRADITIONAL) {
            this.uveStore.setIsClientReady(true);
        }
    }

    /**
     * Handle the custom event
     *
     * @param {DialogAction}
     * @memberof EditEmaEditorComponent
     */
    onCustomEvent(dialogAction: DialogAction) {
        this.handleNgEvent(dialogAction)?.();
    }

    /**
     * Check if the active contentlet still exists in the page containers
     * and reset it if it was removed
     *
     * @private
     * @param {PageContainer[]} pageContainers
     * @memberof EditEmaEditorComponent
     */
    #checkAndResetActiveContentlet(pageContainers: PageContainer[]): void {
        const selected = this.uveStore.editorSelected();
        const payload = selected?.payload;

        if (!payload?.contentlet?.identifier) {
            return;
        }

        const activeContentletId = payload.contentlet.identifier;
        const stillExists = pageContainers.some((container) => {
            // For now, if is not the same container, we deactivate the active contentlet
            // This can be improved in the future to check if the contentlet was moved, but we need to implement optimistic updates in UVE first
            // Because moving contentlets change the container structure, and we need to have a rollback mechanism in case the update fails
            const isSameContainer = areContainersEquals(container, payload.container);
            return container.contentletsId?.includes(activeContentletId) && isSameContainer;
        });

        if (!stillExists) {
            this.uveStore.resetSelected();
        }
    }

    /**
     * When the user drop a palette item in the dropzone
     *
     * @param {PositionPayload} positionPayload
     * @return {*}  {void}
     * @memberof EditEmaEditorComponent
     */
    placeItem(positionPayload: PositionPayload, dragItem: EmaDragItem): void {
        let payload = this.uveStore.getPageSavePayload(positionPayload);

        const destinationContainer = payload.container;
        const pivotContentlet = payload.contentlet;
        const positionToInsert = positionPayload.position;

        if (dragItem.draggedPayload.type === 'contentlet') {
            const draggedPayload = dragItem.draggedPayload;
            const originContainer = draggedPayload.item.container;
            const contentletToMove = draggedPayload.item.contentlet;

            if (draggedPayload.move) {
                const deletePayload = this.createDeletePayload({
                    payload,
                    originContainer,
                    contentletToMove
                });

                const { pageContainers, contentletsId } =
                    deleteContentletFromContainer(deletePayload); // Delete from the original position

                // Update the payload to handle the data to insert the contentlet in the new position
                payload = this.createInsertPayloadFromDelete({
                    payload,
                    pageContainers,
                    contentletsId,
                    destinationContainer,
                    pivotContentlet,
                    positionToInsert
                });
            }

            const { pageContainers, didInsert, errorCode } = insertContentletInContainer({
                ...payload,
                newContentletId: draggedPayload.item.contentlet.identifier
            });

            if (!didInsert) {
                if (errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED) {
                    this.handleContainerLimitReached(payload.container.maxContentlets);
                } else {
                    this.handleDuplicatedContentlet();
                }

                return;
            }

            // Check if active contentlet was removed during move operation
            this.#checkAndResetActiveContentlet(pageContainers);

            this.uveStore.editorSave(pageContainers);

            return;
        } else if (dragItem.draggedPayload.type === 'content-type') {
            this.uveStore.resetEditorProperties(); // In case the user cancels the creation of the contentlet, we already have the editor in idle state

            const item = dragItem.draggedPayload.item;
            const languageId = this.uveStore.pageLanguageId();

            this.#openNewContentDialogOrFallback(
                item.variable,
                (contentType) =>
                    this.#openNewEditContentDialogForPaletteDrop(
                        payload,
                        item.variable,
                        contentType?.name ?? item.name
                    ),
                () => {
                    this.dialog.createContentletFromPalette({
                        ...item,
                        actionPayload: payload,
                        language_id: languageId
                    });
                }
            );
        } else if (dragItem.draggedPayload.type === 'temp') {
            const { pageContainers, didInsert, errorCode } = insertContentletInContainer({
                ...payload,
                newContentletId: payload.newContentlet.identifier
            });

            if (!didInsert) {
                if (errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED) {
                    this.handleContainerLimitReached(payload.container.maxContentlets);
                } else {
                    this.handleDuplicatedContentlet();
                }

                return;
            }

            // Check if active contentlet was removed during temp insert operation
            this.#checkAndResetActiveContentlet(pageContainers);

            this.uveStore.editorSave(pageContainers);
        }
    }
    /**
     * Delete contentlet
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    deleteContent(payload: ActionPayload) {
        const { pageContainers } = deleteContentletFromContainer(payload);

        this.confirmationService.confirm({
            header: this.dotMessageService.get(
                'editpage.content.contentlet.remove.confirmation_message.header'
            ),
            message: this.dotMessageService.get(
                'editpage.content.contentlet.remove.confirmation_message.message'
            ),
            acceptLabel: this.dotMessageService.get('dot.common.dialog.accept'),
            rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
            accept: () => {
                // Check if active contentlet was removed during delete operation
                this.#checkAndResetActiveContentlet(pageContainers);

                this.uveStore.editorSave(pageContainers);
            }
        });
    }

    handleInternalNavFromIframe(e: MouseEvent): void {
        this.handleInternalNav(e);
    }

    handleInlineEditingFromIframe(e: MouseEvent): void {
        this.handleInlineEditing(e);
    }

    protected handleNgEvent({ event, actionPayload, clientAction }: DialogAction) {
        const { detail } = event;

        return (<Record<NG_CUSTOM_EVENTS, () => void>>{
            [NG_CUSTOM_EVENTS.EDIT_CONTENTLET_LOADED]: () => {
                /* */
            },
            [NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT]: () => {
                const { pageContainers, didInsert, errorCode } = insertContentletInContainer({
                    ...actionPayload,
                    newContentletId: detail.data.identifier
                });

                if (!didInsert) {
                    if (errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED) {
                        this.handleContainerLimitReached(actionPayload.container.maxContentlets);
                    } else {
                        this.handleDuplicatedContentlet();
                    }

                    return;
                }

                this.uveStore.editorSave(pageContainers);
                this.dialog.resetDialog();
            },
            [NG_CUSTOM_EVENTS.SAVE_PAGE]: () => {
                const { shouldReloadPage, contentletIdentifier } = detail.payload ?? {};

                if (shouldReloadPage) {
                    this.reloadURLContentMapPage(contentletIdentifier);

                    return;
                }

                if (!actionPayload) {
                    this.uveStore.pageReload();

                    return;
                }

                if (clientAction === DotCMSUVEAction.EDIT_CONTENTLET) {
                    this.sendMessageToIframe(
                        {
                            name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
                        },
                        this.host
                    );
                }

                const { pageContainers, didInsert, errorCode } = insertContentletInContainer({
                    ...actionPayload,
                    newContentletId: contentletIdentifier
                });

                if (!didInsert) {
                    if (errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED) {
                        this.handleContainerLimitReached(actionPayload.container.maxContentlets);
                    } else {
                        this.handleDuplicatedContentlet();
                    }

                    return;
                }

                this.uveStore.editorSave(pageContainers);
            },
            /**
             * Handles the create contentlet event emitted from within the JSP/iframe
             * when a content type is selected (e.g. via the + button in a container).
             *
             * Opens the contentlet creation dialog with the pre-resolved action URL and
             * inserts the new contentlet into the target container on save.
             *
             * @see {NG_CUSTOM_EVENTS.CREATE_CONTENTLET}
             * @memberof EditEmaEditorComponent
             */
            [NG_CUSTOM_EVENTS.CREATE_CONTENTLET]: () => {
                this.#openNewContentDialogOrFallback(
                    detail.data.contentType,
                    (contentType) =>
                        this.#openNewEditContentDialogForPaletteDrop(
                            actionPayload,
                            detail.data.contentType,
                            contentType?.name ?? detail.data.contentType
                        ),
                    () => {
                        this.dialog.createContentlet({
                            contentType: detail.data.contentType,
                            url: detail.data.url,
                            actionPayload
                        });
                        this.cd.detectChanges();
                    }
                );
            },
            [NG_CUSTOM_EVENTS.FORM_SELECTED]: () => {
                const formId = detail.data.identifier;

                this.dotPageApiService
                    .getFormIndetifier(actionPayload.container.identifier, formId)
                    .pipe(
                        tap(() => {
                            this.uveStore.setUveStatus(UVE_STATUS.LOADING);
                        }),
                        map((newFormId: string) => {
                            return {
                                ...actionPayload,
                                newContentletId: newFormId
                            };
                        }),
                        catchError(() => EMPTY),
                        take(1)
                    )
                    .subscribe((response) => {
                        const { pageContainers, didInsert, errorCode } =
                            insertContentletInContainer(response);

                        if (!didInsert) {
                            if (errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED) {
                                this.handleContainerLimitReached(response.container.maxContentlets);
                            } else {
                                this.handleDuplicatedContentlet();
                            }
                            this.uveStore.setUveStatus(UVE_STATUS.LOADED);
                        } else {
                            this.uveStore.editorSave(pageContainers);
                            this.dialog.resetDialog();
                        }
                    });
            },
            [NG_CUSTOM_EVENTS.SAVE_MENU_ORDER]: () => {
                this.messageService.add({
                    severity: 'success',
                    summary: this.dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.dotMessageService.get('message.menu.reordered'),
                    life: 2000
                });

                this.uveStore.pageReload();
                this.dialog.resetDialog();

                // This is a temporary solution to "reload" the content by reloading the window
                // we should change this with a new SDK reload strategy
                this.sendMessageToIframe(
                    {
                        name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
                    },
                    this.host
                );
            },
            [NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER]: () => {
                this.messageService.add({
                    severity: 'error',
                    summary: this.dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.dotMessageService.get(
                        'error.menu.reorder.user_has_not_permission'
                    ),
                    life: 2000
                });
            },
            [NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER]: () => {
                this.dialog.resetDialog();
                this.cd.detectChanges();
            },
            [NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED]: () => {
                const htmlPageReferer = event.detail.payload?.htmlPageReferer;
                const url = new URL(htmlPageReferer, this.window.location.origin); // Add base for relative URLs
                const targetUrl = getTargetUrl(
                    url.pathname,
                    this.uveStore.pageAsset()?.urlContentMap
                );
                const language_id = url.searchParams.get('com.dotmarketing.htmlpage.language');

                if (shouldNavigate(targetUrl, this.uveStore.pageParams().url)) {
                    // Navigate to the new URL if it's different from the current one
                    this.uveStore.pageLoad({ url: targetUrl, language_id });

                    return;
                }

                this.uveStore.pageLoad({
                    language_id
                });
            }
        })[detail.name];
    }

    /**
     * Notify the user to reload the iframe
     *
     * @private
     * @memberof DotEmaComponent
     */
    reloadIframeContent(): void {
        this.sendMessageToIframe(
            {
                name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA,
                payload: this.#clientPayload()
            },
            this.host
        );
    }

    private sendMessageToIframe(message: unknown, host = '*'): void {
        this.iframe?.nativeElement.contentWindow?.postMessage(message, host);
    }

    /**
     * Only handle postMessage events sent from the UVE preview iframe (or a nested frame
     * inside it). Ignores same-window messages (e.g. dot-html-to-image thumbnail iframes).
     */
    #isMessageFromUvePreviewFrame(event: MessageEvent): boolean {
        const previewWindow = this.iframe?.nativeElement?.contentWindow;
        const source = event.source as Window;
        if (!previewWindow || !source) {
            return false;
        }
        return source === previewWindow || source === previewWindow.parent;
    }

    #isUvePostMessage(data: unknown): data is PostMessage {
        return !!data && typeof data === 'object' && 'action' in data;
    }

    private handleDuplicatedContentlet() {
        this.messageService.add({
            severity: 'info',
            summary: this.dotMessageService.get(MESSAGE_KEY.DUPLICATE_CONTENT.TITLE),
            detail: this.dotMessageService.get(MESSAGE_KEY.DUPLICATE_CONTENT.MESSAGE),
            life: 2000
        });

        this.uveStore.resetEditorProperties();

        this.dialog.resetDialog();
    }

    private handleContainerLimitReached(maxContentlets: number) {
        this.messageService.add({
            severity: 'warn',
            summary: this.dotMessageService.get(MESSAGE_KEY.CONTAINER_LIMIT.TITLE),
            detail: this.dotMessageService.get(
                MESSAGE_KEY.CONTAINER_LIMIT.MESSAGE,
                maxContentlets.toString()
            ),
            life: 3000
        });

        this.uveStore.resetEditorProperties();

        this.dialog.resetDialog();
    }

    /**
     * Handle edit contentlet
     *
     * @protected
     * @param {ActionPayload} payload
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    protected handleOpenQuickEdit(): void {
        this.uveStore.setEditPanelOpen(true);
        patchState(this.#rightSidebarTabState, { currentTab: 0 });
    }

    protected handleOpenFullEditor(): void {
        // Use $contentletEditData (not editorSelected directly) so the dialog
        // always receives the freshest contentlet from the page asset. After
        // a dialog save + pageReload(), the page asset is updated with a new
        // inode, but editorSelected().payload still holds the stale one.
        // $contentletEditData looks up the contentlet by identifier from the
        // updated page asset, so it reflects the post-save version.
        const { contentlet } = this.$contentletEditData();

        if (!contentlet?.inode) {
            return;
        }

        const contentTypeVariable = contentlet.contentType;
        if (!contentTypeVariable) {
            this.dialog?.editContentlet(contentlet);
            return;
        }

        this.#openNewContentDialogOrFallback(
            contentTypeVariable,
            () => this.#openNewEditContentDialog(contentlet),
            () => this.dialog?.editContentlet(contentlet)
        );
    }

    /**
     * Opens the Angular-based edit content dialog (same shell as relationship field "create").
     */
    #openNewEditContentDialog(contentlet: DotCMSContentlet): void {
        const dialogData: EditContentDialogData = {
            mode: 'edit',
            contentletInode: contentlet.inode,
            onContentSaved: () => {
                this.uveStore.pageReload();
            }
        };

        this.#openDotEditContentShell(contentlet.title ?? '', dialogData);
    }

    /**
     * Fetches the content type and opens the new Angular-based editor if the feature flag is enabled,
     * otherwise calls the legacy fallback.
     */
    #openNewContentDialogOrFallback(
        contentTypeVariable: string,
        onNewEditor: (contentType: DotCMSContentType | null) => void,
        legacyFallback: () => void
    ): void {
        this.dotContentTypeService
            .getContentType(contentTypeVariable)
            .pipe(
                take(1),
                takeUntilDestroyed(this.destroyRef),
                catchError(() => of(null))
            )
            .subscribe((contentType) => {
                if (
                    contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] ===
                    true
                ) {
                    onNewEditor(contentType);
                } else {
                    legacyFallback();
                }
            });
    }

    /**
     * Create flow when a content type is dropped from the palette and the type uses the new editor.
     */
    #openNewEditContentDialogForPaletteDrop(
        actionPayload: ActionPayload,
        contentTypeVariable: string,
        contentTypeName: string
    ): void {
        this.dialog.resetDialog();

        const dialogData: EditContentDialogData = {
            mode: 'new',
            contentTypeId: contentTypeVariable,
            onContentSaved: (contentlet) => {
                if (!contentlet?.identifier) {
                    return;
                }

                const { pageContainers, didInsert, errorCode } = insertContentletInContainer({
                    ...actionPayload,
                    newContentletId: contentlet.identifier
                });

                if (!didInsert) {
                    if (errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED) {
                        this.handleContainerLimitReached(actionPayload.container.maxContentlets);
                    } else {
                        this.handleDuplicatedContentlet();
                    }

                    return;
                }

                this.#checkAndResetActiveContentlet(pageContainers);
                this.uveStore.editorSave(pageContainers);
            }
        };

        this.#openDotEditContentShell(
            this.dotMessageService.get('contenttypes.content.create.contenttype', contentTypeName),
            dialogData
        );
    }

    /**
     * Opens the DotEditContentDialogComponent shell with the given header and dialog data.
     */
    #openDotEditContentShell(header: string, dialogData: EditContentDialogData): void {
        this.dialogService.open(DotEditContentDialogComponent, {
            appendTo: 'body',
            baseZIndex: 10000,
            closable: true,
            closeOnEscape: true,
            draggable: false,
            keepInViewport: true,
            modal: true,
            resizable: true,
            position: 'center',
            width: '95%',
            height: '95%',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-create-content',
            style: { 'max-width': '1400px', 'max-height': '900px' },
            data: dialogData,
            header
        });
    }

    /**
     * Pencil button on the hover toolbar: open the full content editor
     * dialog. If the contentlet appears on more than one page, prompt
     * the user first ("edit on all pages" vs "this page only"). On
     * "this page only", fork the contentlet via copyInPage so the
     * other pages are unaffected, then open the dialog with the new copy.
     *
     * Reads the contentlet from the event payload, NOT from
     * editorSelected — pencil is intentionally stateless
     * with respect to editor selection. Selection (border) and active
     * (side panel) are owned by other actions.
     */
    protected handleEditWithCopyDecision(payload: ActionPayload): void {
        const contentlet = payload?.contentlet;
        if (!contentlet?.inode) {
            return;
        }

        const onMultiplePages = Number(contentlet.onNumberOfPages ?? 1) > 1;
        if (!onMultiplePages) {
            const contentTypeVariable = contentlet.contentType;
            if (!contentTypeVariable) {
                this.dialog?.editContentlet(contentlet as unknown as DotCMSContentlet);
                return;
            }

            this.#openNewContentDialogOrFallback(
                contentTypeVariable,
                () => this.#openNewEditContentDialog(contentlet as unknown as DotCMSContentlet),
                () => this.dialog?.editContentlet(contentlet as unknown as DotCMSContentlet)
            );
            return;
        }

        const treeNode = this.uveStore.getCurrentTreeNode(payload.container, contentlet);

        this.dotCopyContentModalService
            .open()
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                switchMap(({ shouldCopy }) =>
                    shouldCopy ? this.dotCopyContentService.copyInPage(treeNode) : of(null)
                )
            )
            .subscribe({
                next: (copied) => {
                    // shouldCopy === false → edit the original (affects all pages).
                    // shouldCopy === true  → copyInPage forked it; edit the new copy.
                    const target = copied ?? (contentlet as unknown as DotCMSContentlet);
                    if (copied) {
                        this.uveStore.pageReload();
                    }

                    const contentTypeVariable = target.contentType;
                    if (!contentTypeVariable) {
                        this.dialog?.editContentlet(target);
                        return;
                    }

                    this.#openNewContentDialogOrFallback(
                        contentTypeVariable,
                        () => this.#openNewEditContentDialog(target),
                        () => this.dialog?.editContentlet(target)
                    );
                },
                error: (error: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(error);
                }
            });
    }

    private handleCopyContent(treeNode: DotTreeNode): Observable<DotCMSContentlet> {
        return this.dotCopyContentService.copyInPage(treeNode);
    }

    /**
     * Handles the edit of a VTL file.
     *
     * @param {VTLFile} vtlFile - The VTL file to be edited.
     * @memberof EditEmaEditorComponent
     */
    handleEditVTL(vtlFile: VTLFile) {
        this.dialog.editVTLContentlet(vtlFile);
    }

    /**
     * Handle edit content map
     *
     * @protected
     * @param {DotCMSContentlet} contentlet
     * @memberof EditEmaEditorComponent
     */
    protected editContentMap(contentlet: DotCMSURLContentMap): void {
        this.dialog.editUrlContentMapContentlet(contentlet);
    }

    /**
     * Create the payload to delete a contentlet
     *
     * @private
     * @param {DeletePayload} {
     *         payload,
     *         originContainer,
     *         contentletToMove
     *     }
     * @return {*}  {ActionPayload}
     * @memberof EditEmaEditorComponent
     */
    private createDeletePayload({
        payload,
        originContainer,
        contentletToMove
    }: DeletePayload): ActionPayload {
        return {
            ...payload,
            container: {
                ...originContainer // The container where the contentlet was before
            },
            contentlet: {
                ...contentletToMove // The contentlet that was dragged
            }
        };
    }

    /**
     * Reload the URL content map page
     *
     * @private
     * @param {string} inodeOrIdentifier
     * @memberof EditEmaEditorComponent
     */
    private reloadURLContentMapPage(inodeOrIdentifier: string): void {
        // Set loading state to prevent the user to interact with the iframe
        this.uveStore.setUveStatus(UVE_STATUS.LOADING);

        this.dotContentletService
            .getContentletByInode(inodeOrIdentifier)
            .pipe(
                catchError((error) => this.handlerError(error)),
                filter((contentlet) => !!contentlet)
            )
            .subscribe(({ URL_MAP_FOR_CONTENT }) => {
                if (URL_MAP_FOR_CONTENT != this.uveStore.pageParams().url) {
                    // If the URL is different, we need to navigate to the new URL
                    this.uveStore.pageLoad({ url: URL_MAP_FOR_CONTENT });

                    return;
                }

                // If the URL is the same, we need to fetch the new page data
                this.uveStore.pageReload();
            });
    }

    /**
     * Create the payload to insert a contentlet after deleting it
     *
     * @private
     * @param {InsertPayloadFromDelete} {
     *         payload,
     *         pageContainers,
     *         contentletsId,
     *         destinationContainer,
     *         pivotContentlet,
     *         positionToInsert
     *     }
     * @return {*}  {ActionPayload}
     * @memberof EditEmaEditorComponent
     */
    private createInsertPayloadFromDelete({
        payload,
        pageContainers,
        contentletsId,
        destinationContainer,
        pivotContentlet,
        positionToInsert
    }: InsertPayloadFromDelete): ActionPayload {
        return {
            ...payload,
            pageContainers,
            container: {
                ...payload.container,
                ...destinationContainer,
                contentletsId // Contentlets id after deleting the contentlet
            },
            contentlet: pivotContentlet,
            position: positionToInsert
        };
    }

    /**
     * Handle the error
     *
     * @private
     * @param {HttpErrorResponse} error
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    private handlerError(error: HttpErrorResponse) {
        // CHECK IF HAVE TO SET THE UVE TO ERROR
        this.uveStore.setEditorState(EDITOR_STATE.ERROR);

        return this.dotHttpErrorManagerService.handle(error).pipe(map(() => null));
    }

    /**
     * Reloads the component from the dialog/sidebar.
     */
    reloadPage() {
        this.uveStore.pageReload();
    }

    /**
     * Handle the file upload
     *
     * @private
     * @param {{
     *         data: ClientData;
     *         position?: string;
     *         file: File;
     *         dragItem: EmaDragItem;
     *     }} {
     *         data,
     *         position,
     *         file,
     *         dragItem
     *     }
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    handleFileUpload({
        data,
        position,
        file,
        dragItem
    }: {
        data: ClientData;
        position?: string;
        file: File;
        dragItem: EmaDragItem;
    }): void {
        this.uveStore.resetEditorProperties();

        if (!/image.*/.exec(file.type)) {
            this.messageService.add({
                severity: 'error',
                summary: this.dotMessageService.get('file-upload'),
                detail: this.dotMessageService.get('editpage.file.upload.not.image'),
                life: 3000
            });

            return;
        }

        this.tempFileUploadService
            .upload(file)
            .pipe(
                tap(() => {
                    this.messageService.add({
                        severity: 'info',
                        summary: this.dotMessageService.get('upload-image'),
                        detail: this.dotMessageService.get('editpage.file.uploading', file.name),
                        life: 3000
                    });
                }),
                switchMap(([{ id, image }]: DotCMSTempFile[]) => {
                    if (!image) {
                        this.messageService.add({
                            severity: 'error',
                            summary: this.dotMessageService.get('upload-image'),
                            detail: this.dotMessageService.get('editpage.file.upload.error'),
                            life: 3000
                        });

                        return of(undefined);
                    }

                    return this.dotWorkflowActionsFireService
                        .publishContentletAndWaitForIndex<DotCMSContentlet>('dotAsset', {
                            asset: id
                        })
                        .pipe(
                            tap(() => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get('Workflow-Action'),
                                    detail: this.dotMessageService.get(
                                        'editpage.file.publishing',
                                        file.name
                                    ),
                                    life: 3000
                                });
                            })
                        );
                })
            )
            .subscribe((contentlet) => {
                if (!contentlet) {
                    this.uveStore.resetEditorProperties();

                    return;
                }

                const payload = {
                    ...data,
                    position,
                    newContentlet: {
                        identifier: contentlet.identifier,
                        inode: contentlet.inode,
                        title: contentlet.title,
                        contentType: contentlet.contentType,
                        baseType: contentlet.baseType
                    }
                } as ActionPayload;

                this.placeItem(payload, dragItem);
            });
    }

    private createNewTranslation(language: DotLanguage, page: DotCMSPage): void {
        this.confirmationService.confirm({
            header: this.dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.header'
            ),
            message: this.dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.message',
                language.language
            ),
            rejectIcon: 'hidden',
            acceptIcon: 'hidden',
            key: 'shell-confirm-dialog',
            accept: () => {
                this.translatePage({ page, newLanguage: language.id });
            },
            reject: () => {
                // If is rejected, bring back the current language on selector
                this.#goBackToCurrentLanguage();
            }
        });
    }

    translatePage(event: { page: DotCMSPage; newLanguage: number }) {
        this.dialog.translatePage(event);
    }

    /**
     * Use the Page Language to navigate back to the current language
     *
     * @memberof DotEmaShellComponent
     */
    #goBackToCurrentLanguage(): void {
        const currentLanguageId = this.uveStore.pageLanguage()?.id?.toString() ?? '1';
        this.uveStore.pageLoad({ language_id: currentLanguageId });
    }

    #clientPayload() {
        const clientResponse = this.uveStore.pageAsset()?.clientResponse;

        if (clientResponse) {
            return clientResponse;
        }

        return {
            // Removed pageAPIResponse spread
            params: this.uveStore.pageParams()
        };
    }

    protected handleSelectContent(_contentletActionPayload: ActionPayload): void {
        // The hover toolbar's `promoteHoverToSelected` (called inline in
        // the (click) before this output fires) has already pinned the
        // contentlet as `editorSelected`. We just need to open the
        // style editor — the side panel binds to editorSelected().payload
        // for its data.
        this.#openStyleEditor();
    }

    protected togglePalette(): void {
        this.uveStore.setPaletteOpen(!this.$paletteOpen);
    }

    protected toggleEditPanel(): void {
        this.uveStore.setEditPanelOpen(!this.$editPanelOpen);
    }

    readonly $deviceSelectorState = computed(() => ({
        device: this.uveStore.viewDevice(),
        socialMedia: this.uveStore.viewSocialMedia(),
        orientation: this.uveStore.viewDeviceOrientation()
    }));

    handleDeviceSelectorChange(change: DeviceSelectorChange): void {
        switch (change.type) {
            case 'device':
                this.uveStore.viewSetDevice(change.device);
                break;
            case 'socialMedia':
                this.uveStore.viewSetSEO(change.socialMedia);
                break;
            case 'orientation':
                this.uveStore.viewSetOrientation(change.orientation);
                break;
        }
    }

    readonly $pageURLS = computed<{ label: string; value: string }[]>(() => {
        const params = this.uveStore.pageParams();
        const siteId = this.uveStore.pageAsset()?.site?.identifier;
        const host = params?.clientHost || this.window.location.origin;
        const path = params?.url?.replace(/\/index(\.html)?$/, '') || '/';

        const currentSiteHostname = this.globalStore.siteDetails()?.hostname;

        const urls: { label: string; value: string }[] = [
            {
                label: 'uve.toolbar.page.live.url',
                value: new URL(path, host).toString()
            },
            {
                label: 'uve.toolbar.page.current.view.url',
                value: createFullURL(params, siteId)
            }
        ];

        if (currentSiteHostname) {
            urls.push({
                label: 'uve.toolbar.page.current.site.url',
                value: new URL(path, `https://${currentSiteHostname}`).toString()
            });
        }

        return urls;
    });

    protected triggerCopyToast(): void {
        this.messageService.add({
            severity: 'success',
            summary: this.dotMessageService.get('Copied'),
            life: 3000
        });
    }

    #openStyleEditor(): void {
        this.uveStore.setEditPanelOpen(true);
        patchState(this.#rightSidebarTabState, { currentTab: 1 });
    }

    protected handleAddContent(event: {
        type: 'content' | 'form' | 'widget';
        payload: ActionPayload;
    }): void {
        switch (event.type) {
            case 'content':
                this.dialog.addContentlet(event.payload);
                break;
            case 'form':
                this.dialog.addForm(event.payload);
                break;
            case 'widget':
                this.dialog.addWidget(event.payload);
                break;
        }
    }

    /**
     * Handles palette node selection by asking the iframe SDK for the section's offsetTop,
     * then scrolling the editor-content container once the reply arrives.
     *
     * @param event Event containing selector and type of the selected node
     */
    protected handlePaletteNodeSelect(event: { selector: string; type: string }): void {
        // Extract 1-based section index from '#section-N' or '#dot-section-N'
        const match = event.selector.match(/#(?:dot-)?section-(\d+)/);

        if (!match) {
            return;
        }

        this.sendMessageToIframe(
            {
                name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_TO_SECTION,
                sectionIndex: parseInt(match[1], 10)
            },
            this.host
        );
    }
}
