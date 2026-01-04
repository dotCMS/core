import { EMPTY, Observable, of } from 'rxjs';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass, NgStyle } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    AfterViewInit,
    ViewChild,
    WritableSignal,
    effect,
    inject,
    signal,
    untracked,
    computed
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ProgressBarModule } from 'primeng/progressbar';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, filter, map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSClazzes,
    DotCMSTempFile,
    DotLanguage,
    DotTreeNode,
    SeoMetaTags
} from '@dotcms/dotcms-models';
import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import {
    DotCMSInlineEditingPayload,
    DotCMSInlineEditingType,
    DotCMSPage,
    DotCMSURLContentMap,
    DotCMSUVEAction,
    UVE_MODE
} from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { DotCopyContentModalService, DotMessagePipe } from '@dotcms/ui';
import { WINDOW, isEqual } from '@dotcms/utils';

import { DotUveContentletQuickEditComponent } from './components/dot-uve-contentlet-quick-edit/dot-uve-contentlet-quick-edit.component';
import { DotUveContentletToolsComponent } from './components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component';
import { DotUveIframeComponent } from './components/dot-uve-iframe/dot-uve-iframe.component';
import { DotUveLockOverlayComponent } from './components/dot-uve-lock-overlay/dot-uve-lock-overlay.component';
import { DotUvePageVersionNotFoundComponent } from './components/dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { DotPaletteListStore } from './components/dot-uve-palette/components/dot-uve-palette-list/store/store';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { DotUveZoomControlsComponent } from './components/dot-uve-zoom-controls/dot-uve-zoom-controls.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import { EmaDragItem } from './components/ema-page-dropzone/types';
import { parseFieldValues, getQuickEditFields } from './utils';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DotUveActionsHandlerService } from '../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveBridgeService } from '../services/dot-uve-bridge/dot-uve-bridge.service';
import { DotUveDragDropService } from '../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { DotUveZoomService } from '../services/dot-uve-zoom/dot-uve-zoom.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, PALETTE_CLASSES, UVE_STATUS } from '../shared/enums';
import {
    ActionPayload,
    ClientData,
    ContentletPayload,
    DeletePayload,
    DialogAction,
    InsertPayloadFromDelete,
    PositionPayload,
    PostMessage,
    VTLFile
} from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { PageType } from '../store/models';
import {
    TEMPORAL_DRAG_ITEM,
    createFullURL,
    deleteContentletFromContainer,
    getEditorStates,
    getTargetUrl,
    getWrapperMeasures,
    insertContentletInContainer,
    shouldNavigate
} from '../utils';

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
        DotUveLockOverlayComponent,
        DotUvePaletteComponent,
        DotUveIframeComponent,
        ButtonModule,
        ToolbarModule,
        InputGroupModule,
        InputGroupAddonModule,
        DotUveZoomControlsComponent,
        ClipboardModule,
        OverlayPanelModule,
        TooltipModule,
        DotMessagePipe
    ],
    providers: [
        DotPaletteListStore,
        DotCopyContentModalService,
        DotCopyContentService,
        DotHttpErrorManagerService,
        DotContentletService,
        DotTempFileUploadService,
        DotUveZoomService,
        DotUveBridgeService,
        DotUveActionsHandlerService,
        DotUveDragDropService
    ]
})
export class EditEmaEditorComponent implements OnInit, OnDestroy, AfterViewInit {
    @ViewChild('dialog') dialog: DotEmaDialogComponent;
    @ViewChild('iframe') iframeComponent!: DotUveIframeComponent;
    @ViewChild('blockSidebar') blockSidebar: DotBlockEditorSidebarComponent;
    @ViewChild('customDragImage') customDragImage: ElementRef<HTMLDivElement>;
    @ViewChild('zoomContainer') zoomContainer!: ElementRef<HTMLDivElement>;
    @ViewChild('editorContent') editorContent!: ElementRef<HTMLDivElement>;

    get iframe(): ElementRef<HTMLIFrameElement> | undefined {
        return this.iframeComponent?.iframe;
    }

    protected readonly uveStore = inject(UVEStore);
    protected readonly dotPaletteListStore = inject(DotPaletteListStore);

    protected readonly $contenttypes = this.dotPaletteListStore.contenttypes;

    protected readonly $contentletEditData = computed(() => {
        const { container, contentlet: contentletPayload } = this.uveStore.editor().selectedContentlet ?? {};
        // Removed pageAPIResponse - use normalized accessors

        const contentType = this.$contenttypes().find(
            (ct) => ct.variable === contentletPayload?.contentType
        );

        const fields = contentType?.layout ? getQuickEditFields(contentType.layout) : [];

        // Parse values for each field
        const fieldsWithOptions = fields.map((field) => ({
            ...field,
            options: parseFieldValues(field.values)
        }));

        // Get the full contentlet from containers using container identifier and uuid
        let contentlet: DotCMSContentlet = contentletPayload as DotCMSContentlet;
        const containers = this.uveStore.containers();
        if (container?.identifier && container?.uuid && contentletPayload?.identifier && containers) {
            const containerData = containers[container.identifier];
            const contentletUuid = `uuid-${container.uuid}`;
            const contentlets = containerData?.contentlets?.[contentletUuid] || [];
            const foundContentlet = contentlets.find(
                (c) => c.identifier === contentletPayload.identifier
            );
            if (foundContentlet) {
                contentlet = foundContentlet as DotCMSContentlet;
            }
        }

        return { container, contentlet, fields: fieldsWithOptions };
    });
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly messageService = inject(MessageService);
    private readonly window = inject(WINDOW);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly dotCopyContentModalService = inject(DotCopyContentModalService);
    private readonly dotCopyContentService = inject(DotCopyContentService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotContentletService = inject(DotContentletService);
    private readonly tempFileUploadService = inject(DotTempFileUploadService);
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly inlineEditingService = inject(InlineEditService);
    private readonly dotPageApiService = inject(DotPageApiService);
    readonly zoomService = inject(DotUveZoomService);
    private readonly bridgeService = inject(DotUveBridgeService);
    private readonly actionsHandler = inject(DotUveActionsHandlerService);
    private readonly dragDropService = inject(DotUveDragDropService);
    readonly #dotAlertConfirmService = inject(DotAlertConfirmService);
    #iframeResizeObserver: ResizeObserver | null = null;

    readonly #isSubmitting = signal<boolean>(false);
    readonly $isSubmitting = computed(() => this.#isSubmitting());

    readonly host = '*';
    readonly $ogTags: WritableSignal<SeoMetaTags> = signal(undefined);

    // Component builds its own editor props locally (Phase 2.2: Move view models from store to components)
    protected readonly $showDialogs = computed<boolean>(() => {
        const canEditPage = this.uveStore.$canEditPageContent();
        const isEditState = this.uveStore.view().isEditState;
        return canEditPage && isEditState;
    });

    protected readonly $showBlockEditorSidebar = computed<boolean>(() => {
        const canEditPage = this.uveStore.$canEditPageContent();
        const isEditState = this.uveStore.view().isEditState;
        const isEnterprise = this.uveStore.isEnterprise();
        return canEditPage && isEditState && isEnterprise;
    });

    protected readonly $iframeProps = computed(() => {
        // Use it to create dependencies to the pageAPIResponse
        const mode = this.uveStore.$mode();
        const pageType = this.uveStore.pageType();
        const isClientReady = this.uveStore.isClientReady();
        const editor = this.uveStore.editor();
        const toolbar = this.uveStore.view();
        const state = editor.state;
        const device = toolbar.device;

        const isEditMode = mode === UVE_MODE.EDIT;
        const isPageReady = pageType === PageType.TRADITIONAL || isClientReady || !isEditMode;
        const isLoading = !isPageReady || this.uveStore.status() === UVE_STATUS.LOADING;
        const { dragIsActive } = getEditorStates(state);
        const iframeOpacity = isLoading || !isPageReady ? '0.5' : '1';
        const wrapper = getWrapperMeasures(device, toolbar.orientation);

        return {
            opacity: iframeOpacity,
            pointerEvents: dragIsActive ? 'none' : 'auto',
            wrapper: device ? wrapper : null
        };
    });

    protected readonly $progressBar = computed<boolean>(() => {
        const mode = this.uveStore.$mode();
        const pageType = this.uveStore.pageType();
        const isClientReady = this.uveStore.isClientReady();

        const isEditMode = mode === UVE_MODE.EDIT;
        const isPageReady = pageType === PageType.TRADITIONAL || isClientReady || !isEditMode;
        return !isPageReady || this.uveStore.status() === UVE_STATUS.LOADING;
    });

    protected readonly $dropzone = computed(() => {
        const canEditPage = this.uveStore.$canEditPageContent();
        const editor = this.uveStore.editor();
        const state = editor.state;
        const bounds = editor.bounds;
        const dragItem = editor.dragItem;

        const showDropzone = canEditPage && state === EDITOR_STATE.DRAGGING;

        return showDropzone
            ? {
                  bounds,
                  dragItem
              }
            : null;
    });

    protected readonly $seoResults = computed(() => {
        const toolbar = this.uveStore.view();
        const editor = this.uveStore.editor();
        const socialMedia = toolbar.socialMedia;
        const ogTags = editor.ogTags;
        const shouldShowSeoResults = socialMedia && ogTags;

        return shouldShowSeoResults
            ? {
                  ogTags,
                  socialMedia
              }
            : null;
    });

    readonly $mode = this.uveStore.$mode;

    // Phase 4.3: Component-level computed (was in withEditor with cross-feature dependency)
    readonly $editorContentStyles = computed<Record<string, string>>(() => {
        const socialMedia = this.uveStore.view().socialMedia;
        return {
            display: socialMedia ? 'none' : 'block'
        };
    });

    // toObservable requires a Signal, so computed() is necessary here
    readonly ogTagsResults$ = toObservable(computed(() => this.uveStore.view().ogTagsResults));

    get $paletteOpen() {
        return this.uveStore.editor().panels.palette.open;
    }
    get $rightSidebarOpen() {
        return this.uveStore.editor().panels.rightSidebar.open;
    }
    readonly $toggleLockOptions = this.uveStore.$toggleLockOptions;
    readonly $showContentletControls = this.uveStore.$showContentletControls;
    get $contentArea() {
        return this.uveStore.editor().contentArea;
    }
    readonly $allowContentDelete = this.uveStore.$allowContentDelete;
    readonly $isDragging = this.uveStore.$isDragging;

    readonly UVE_STATUS = UVE_STATUS;
    readonly UVE_MODE = UVE_MODE;
    readonly DotCMSClazzes = DotCMSClazzes;

    readonly $paletteClass = computed(() => {
        return this.$paletteOpen ? PALETTE_CLASSES.OPEN : PALETTE_CLASSES.CLOSED;
    });

    readonly $canvasOuterStyles = this.zoomService.$canvasOuterStyles;
    readonly $canvasInnerStyles = this.zoomService.$canvasInnerStyles;

    readonly $iframeWrapperStyles = computed((): Record<string, string> => {
        const wrapper = this.$iframeProps().wrapper;
        if (!wrapper) {
            return {};
        }
        return {
            width: wrapper.width,
            minWidth: wrapper.width,
            maxWidth: wrapper.width
        };
    });

    readonly $iframeSrc = computed((): string => {
        const url = this.uveStore.$iframeURL();
        return (typeof url === 'string' ? url : '') || '';
    });
    readonly $iframePointerEvents = computed((): string => {
        const events = this.$iframeProps().pointerEvents;
        return (typeof events === 'string' ? events : '') || '';
    });
    readonly $iframeOpacity = computed((): number => {
        const opacity = this.$iframeProps().opacity;
        return (typeof opacity === 'number' ? opacity : 1) || 1;
    });

    readonly $pageURL = computed((): string => {
        // Removed pageAPIResponse - use normalized accessors
        if (!this.uveStore.page()?.pageURI) {
            return '';
        }
        const site = this.uveStore.site();
        const page = this.uveStore.page();
        const hostname = site?.hostname || 'mysite.com';
        const protocol = page?.httpsRequired ? 'https' : 'http';
        const pageURI = page.pageURI;
        const url = pageURI.startsWith('/') ? pageURI : `/${pageURI}`;
        return `${protocol}://${hostname}${url}`;
    });

    get contentWindow(): Window | null {
        return this.iframeComponent?.contentWindow || null;
    }

    readonly $translatePageEffect = effect(() => {
        const { page, currentLanguage } = this.uveStore.$translateProps();

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

    readonly $handleIsDraggingEffect = effect(() => {
        const isDragging = this.uveStore.$editorIsInDraggingState();

        if (!isDragging) {
            return;
        }

        this.bridgeService.sendMessageToIframe(
            { name: __DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS },
            this.host
        );
    });


    /**
     * Save the contentlet form with the given form data
     *
     * @private
     * @param {Record<string, unknown>} formData - The form data to save
     * @memberof EditEmaEditorComponent
     */
    private saveContentletForm(formData: Record<string, unknown>): void {
        this.#isSubmitting.set(true);
        this.dotWorkflowActionsFireService.saveContentlet(formData as Record<string, string>).subscribe({
            next: () => {
                this.#isSubmitting.set(false);
                this.reloadPage();
            },
            error: () => {
                this.#isSubmitting.set(false);
            }
        });
    }

    protected onFormSubmit(formData: Record<string, unknown>): void {
        const { container, contentlet } = this.$contentletEditData();

        const onNumberOfPages = Number(contentlet.onNumberOfPages || '1');

        // If the contentlet is on only one page, we can save it directly
        if (onNumberOfPages <= 1) {
            this.saveContentletForm(formData);
            return;
        }

        const currentTreeNode = this.uveStore.getCurrentTreeNode(container, contentlet);

        this.dotCopyContentModalService
            .open()
            .pipe(
                switchMap(({ shouldCopy }) => {
                    if (!shouldCopy) {
                        return of(contentlet);
                    }

                    this.dialog.showLoadingIframe(contentlet.title);
                    return this.handleCopyContent(currentTreeNode);
                })
            )
            .subscribe((resultContentlet: DotCMSContentlet) => {
                // Only update selected contentlet if content was actually copied (new inode)
                if (resultContentlet.inode !== contentlet.inode) {
                    this.uveStore.setSelectedContentlet({
                        container,
                        contentlet: {
                            identifier: resultContentlet.identifier,
                            inode: resultContentlet.inode,
                            title: resultContentlet.title,
                            contentType: resultContentlet.contentType,
                            onNumberOfPages: 1 // Because we just copied the contentlet to the same page
                        } as ContentletPayload
                    } as Pick<ClientData, 'container' | 'contentlet'>);
                }

                // Update formData with the new inode if content was copied
                const updatedFormData = {
                    ...formData,
                    inode: resultContentlet.inode
                };
                this.saveContentletForm(updatedFormData);
            });

    }

    protected onCancel(): void {
        this.uveStore.setSelectedContentlet(undefined);
    }

    ngOnInit(): void {
        // Initialization happens in ngAfterViewInit when ViewChild references are available
        // This lifecycle hook satisfies OnInit interface requirement
        if (!this.uveStore) {
            // Early validation - will never execute in normal flow
            throw new Error('UVEStore not available');
        }
    }

    ngAfterViewInit(): void {
        if (!this.iframe) {
            return;
        }

        // Bridge service handles message events - needs iframe which is available now
        const messageStream = this.bridgeService.initialize(
            this.iframe,
            this.zoomService
        );

        messageStream.subscribe((event) => {
            this.bridgeService.handleMessage(
                event,
                (message) => this.handleUveMessage(message),
                () => this.#clampScrollWithinBounds()
            );
        });

        this.setupZoom();
        this.setupDragDrop();
    }

    handleSelectedContentlet(
        selectedContentlet: Pick<ClientData, 'container' | 'contentlet'>
    ): void {
        this.uveStore.setSelectedContentlet(selectedContentlet);
    }

    private setupZoom(): void {
        const zoomContainer = this.zoomContainer?.nativeElement;
        const editorContent = this.editorContent?.nativeElement;

        if (!zoomContainer || !editorContent) {
            return;
        }

        this.zoomService.setupZoomInteractions(zoomContainer, editorContent, () =>
            this.#clampScrollWithinBounds()
        );
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
        this.actionsHandler.handleAction(message, {
            uveStore: this.uveStore,
            dialog: this.dialog,
            blockSidebar: this.blockSidebar,
            inlineEditingService: this.inlineEditingService,
            dotPageApiService: this.dotPageApiService,
            contentWindow: this.contentWindow,
            host: this.host,
            onCopyContent: (currentTreeNode) => this.handleCopyContent(currentTreeNode)
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
        const dragItem = this.uveStore.editor().dragItem;

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
        const href = target.href || target.closest('a')?.getAttribute('href');
        const isInlineEditing = this.uveStore.editor().state === EDITOR_STATE.INLINE_EDITING;

        // If the link is not valid or we are in inline editing mode, we do nothing
        if (!href || isInlineEditing) {
            return;
        }

        const url = new URL(href, location.origin);
        // Get the query parameters from the URL
        const urlQueryParams = Object.fromEntries(url.searchParams.entries());

        if (url.hostname !== location.hostname) {
            this.window.open(href, '_blank');

            return;
        }

        this.uveStore.loadPageAsset({ url: url.pathname, ...urlQueryParams });
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
        if (this.uveStore.editor().state === EDITOR_STATE.INLINE_EDITING) {
            this.inlineEditingService.initEditor();
        }

        this.uveStore.setIsClientReady(true);
    }

    onIframeDocHeightChange(height: number): void {
        this.zoomService.setIframeDocHeight(height);
        this.#clampScrollWithinBounds();
    }

    ngOnDestroy(): void {
        this.#iframeResizeObserver?.disconnect();
        this.#iframeResizeObserver = null;
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

            const { pageContainers, didInsert } = insertContentletInContainer({
                ...payload,
                newContentletId: draggedPayload.item.contentlet.identifier
            });

            if (!didInsert) {
                this.handleDuplicatedContentlet();

                return;
            }

            this.uveStore.savePage(pageContainers);

            return;
        } else if (dragItem.draggedPayload.type === 'content-type') {
            this.uveStore.resetEditorProperties(); // In case the user cancels the creation of the contentlet, we already have the editor in idle state

            this.dialog.createContentletFromPalette({
                ...dragItem.draggedPayload.item,
                actionPayload: payload,
                language_id: this.uveStore.$languageId()
            });
        } else if (dragItem.draggedPayload.type === 'temp') {
            const { pageContainers, didInsert } = insertContentletInContainer({
                ...payload,
                newContentletId: payload.newContentlet.identifier
            });

            if (!didInsert) {
                this.handleDuplicatedContentlet();

                return;
            }

            this.uveStore.savePage(pageContainers);
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
                this.uveStore.savePage(pageContainers);
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
                const { pageContainers, didInsert } = insertContentletInContainer({
                    ...actionPayload,
                    newContentletId: detail.data.identifier
                });

                if (!didInsert) {
                    this.handleDuplicatedContentlet();

                    return;
                }

                this.uveStore.savePage(pageContainers);
                this.dialog.resetDialog();
            },
            [NG_CUSTOM_EVENTS.SAVE_PAGE]: () => {
                const { shouldReloadPage, contentletIdentifier } = detail.payload ?? {};

                if (shouldReloadPage) {
                    this.reloadURLContentMapPage(contentletIdentifier);

                    return;
                }

                if (!actionPayload) {
                    this.uveStore.reloadCurrentPage();

                    return;
                }

                if (clientAction === DotCMSUVEAction.EDIT_CONTENTLET) {
                    this.bridgeService.sendMessageToIframe(
                        {
                            name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
                        },
                        this.host
                    );
                }

                const { pageContainers, didInsert } = insertContentletInContainer({
                    ...actionPayload,
                    newContentletId: contentletIdentifier
                });

                if (!didInsert) {
                    this.handleDuplicatedContentlet();

                    return;
                }

                this.uveStore.savePage(pageContainers);
            },
            [NG_CUSTOM_EVENTS.CREATE_CONTENTLET]: () => {
                this.dialog.createContentlet({
                    contentType: detail.data.contentType,
                    url: detail.data.url,
                    actionPayload
                });
                this.cd.detectChanges();
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
                        const { pageContainers, didInsert } = insertContentletInContainer(response);

                        if (!didInsert) {
                            this.handleDuplicatedContentlet();
                            this.uveStore.setUveStatus(UVE_STATUS.LOADED);
                        } else {
                            this.uveStore.savePage(pageContainers);
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

                this.uveStore.reloadCurrentPage();
                this.dialog.resetDialog();

                // This is a temporary solution to "reload" the content by reloading the window
                // we should change this with a new SDK reload strategy
                this.bridgeService.sendMessageToIframe(
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
                const url = new URL(htmlPageReferer, window.location.origin); // Add base for relative URLs
                const targetUrl = getTargetUrl(
                    url.pathname,
                    this.uveStore.urlContentMap()
                );
                const language_id = url.searchParams.get('com.dotmarketing.htmlpage.language');

                if (shouldNavigate(targetUrl, this.uveStore.pageParams().url)) {
                    // Navigate to the new URL if it's different from the current one
                    this.uveStore.loadPageAsset({ url: targetUrl, language_id });

                    return;
                }

                this.uveStore.loadPageAsset({
                    language_id
                });
            }
        })[detail.name];
    }

    /**
     * Handle the post message event
     *
     * @private
     * @param {{ action: CLIENT_ACTIONS; payload: DotCMSContentlet }} data
     * @return {*}
     * @memberof DotEmaComponent
     */

    /**
     * Notify the user to reload the iframe
     *
     * @private
     * @memberof DotEmaComponent
     */
    reloadIframeContent(): void {
        this.bridgeService.sendMessageToIframe(
            {
                name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA,
                payload: this.#clientPayload()
            },
            this.host
        );
    }

    private handleDuplicatedContentlet() {
        this.messageService.add({
            severity: 'info',
            summary: this.dotMessageService.get('editpage.content.add.already.title'),
            detail: this.dotMessageService.get('editpage.content.add.already.message'),
            life: 2000
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
    protected handleEditContentlet(payload: ActionPayload) {
        const { contentlet, container } = payload;
        const { onNumberOfPages = '1', title } = contentlet;

        if (Number(onNumberOfPages) <= 1) {
            this.dialog?.editContentlet(contentlet);

            return;
        }

        const currentTreeNode = this.uveStore.getCurrentTreeNode(container, contentlet);

        this.dotCopyContentModalService
            .open()
            .pipe(
                switchMap(({ shouldCopy }) => {
                    if (!shouldCopy) {
                        return of(contentlet);
                    }

                    this.dialog.showLoadingIframe(title);

                    return this.handleCopyContent(currentTreeNode);
                })
            )
            .subscribe((contentlet) => {
                this.dialog.editContentlet(contentlet);
            });
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
     * Handle copy content
     *
     * @private
     * @return {*}
     * @memberof DotEmaDialogComponent
     */
    private handleCopyContent(currentTreeNode: DotTreeNode): Observable<DotCMSContentlet> {
        return this.dotCopyContentService.copyInPage(currentTreeNode).pipe(
            catchError((error) =>
                this.dotHttpErrorManagerService.handle(error).pipe(
                    tap(() => this.dialog.resetDialog()), // If there is an error, we set the status to idle
                    map(() => null)
                )
            ),
            filter((contentlet: DotCMSContentlet) => !!contentlet?.inode)
        );
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
                    this.uveStore.loadPageAsset({ url: URL_MAP_FOR_CONTENT });

                    return;
                }

                // If the URL is the same, we need to fetch the new page data
                this.uveStore.reloadCurrentPage();
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
        this.uveStore.reloadCurrentPage();
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

    /**
     * Handle the inline editing event
     *
     * @param {*} { type, data }
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    #handleInlineEditingEvent({
        type,
        data
    }: {
        type: DotCMSInlineEditingType;
        data?: DotCMSInlineEditingPayload;
    }) {
        if (!this.uveStore.isEnterprise()) {
            this.#dotAlertConfirmService.alert({
                header: this.dotMessageService.get('dot.common.license.enterprise.only.error'),
                message: this.dotMessageService.get('editpage.not.lincese.error')
            });

            return;
        }

        switch (type) {
            case 'BLOCK_EDITOR':
                this.blockSidebar?.open(data);
                break;

            case 'WYSIWYG':
                this.inlineEditingService.initEditor();
                this.uveStore.setEditorState(EDITOR_STATE.INLINE_EDITING);
                break;

            default:
                console.warn('Unknown block editor type', type);

                break;
        }
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
        this.uveStore.loadPageAsset({ language_id: '1' });
    }

    #clientPayload() {
        const graphqlResponse = this.uveStore.$customGraphqlResponse();

        if (graphqlResponse) {
            return graphqlResponse;
        }

        return {
            // Removed pageAPIResponse spread
            params: this.uveStore.pageParams()
        };
    }

    #resetContentletArea(): void {
        this.uveStore.resetContentletArea();
    }

    protected handleSelectContent(contentlet: ContentletPayload): void {
        this.uveStore.setActiveContentlet(contentlet);
    }

    protected togglePalette(): void {
        this.uveStore.setPaletteOpen(!this.$paletteOpen);
    }

    protected toggleRightSidebar(): void {
        this.uveStore.setRightSidebarOpen(!this.$rightSidebarOpen);
    }

    readonly $pageURLS = computed<{ label: string; value: string }[]>(() => {
        const params = this.uveStore.pageParams();
        const siteId = this.uveStore.site()?.identifier;
        const host = params?.clientHost || this.window.location.origin;
        const path = params?.url?.replace(/\/index(\.html)?$/, '') || '/';

        return [
            {
                label: 'uve.toolbar.page.live.url',
                value: new URL(path, host).toString()
            },
            {
                label: 'uve.toolbar.page.current.view.url',
                value: createFullURL(params, siteId)
            }
        ];
    });

    protected triggerCopyToast(): void {
        this.messageService.add({
            severity: 'success',
            summary: this.dotMessageService.get('Copied'),
            life: 3000
        });
    }

    #scrollToTopLeft(): void {
        const el = this.editorContent?.nativeElement;
        if (!el) {
            return;
        }

        requestAnimationFrame(() => {
            el.scrollLeft = 0;
            el.scrollTop = 0;
        });
    }

    #clampScrollWithinBounds(): void {
        const el = this.editorContent?.nativeElement;
        if (!el) {
            return;
        }

        requestAnimationFrame(() => {
            // Use real scroll bounds so gutters/padding inside the content are included.
            const maxLeft = Math.max(0, el.scrollWidth - el.clientWidth);
            const maxTop = Math.max(0, el.scrollHeight - el.clientHeight);

            el.scrollLeft = Math.min(Math.max(0, el.scrollLeft), maxLeft);
            el.scrollTop = Math.min(Math.max(0, el.scrollTop), maxTop);
        });
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
     * Handles palette node selection and scrolls the editor-content to the corresponding element.
     *
     * @param event Event containing selector and type of the selected node
     */
    protected handlePaletteNodeSelect(event: { selector: string; type: string }): void {
        const iframeElement = this.iframe?.nativeElement;
        const editorContentElement = this.editorContent?.nativeElement;

        if (!iframeElement || !editorContentElement) {
            return;
        }

        // Get the iframe document
        let iframeDoc: Document | null = null;
        try {
            iframeDoc = iframeElement.contentDocument;
        } catch {
            // Cross-origin iframe, cannot access document
            return;
        }

        if (!iframeDoc) {
            return;
        }

        // Find the element in the iframe
        const element = iframeDoc.querySelector(event.selector);
        if (!element) {
            return;
        }

        const htmlElement = element as HTMLElement;

        // Use getBoundingClientRect() which accounts for all transforms including zoom
        const elementRect = htmlElement.getBoundingClientRect();
        const zoomLevel = this.zoomService.$zoomLevel();

        // elementRect.top works correctly at 100% zoom (zoomLevel = 1)
        // For other zoom levels, convert from scaled to unscaled coordinates
        const scrollTop = elementRect.top * zoomLevel;

        // Scroll the editor-content smoothly
        editorContentElement.scrollTo({
            top: Math.max(0, scrollTop),
            left: 0,
            behavior: 'smooth'
        });
    }
}
