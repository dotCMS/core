import { Observable, Subject, fromEvent, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    DestroyRef,
    ElementRef,
    HostListener,
    OnDestroy,
    OnInit,
    Signal,
    ViewChild,
    WritableSignal,
    computed,
    inject,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressBarModule } from 'primeng/progressbar';

import { takeUntil, catchError, filter, map, switchMap, tap, take } from 'rxjs/operators';

import { CUSTOMER_ACTIONS } from '@dotcms/client';
import {
    DotMessageService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotContentletService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotCMSTempFile,
    DotExperimentStatus,
    DotTreeNode,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import {
    SafeUrlPipe,
    DotSpinnerModule,
    DotMessagePipe,
    DotCopyContentModalService
} from '@dotcms/ui';

import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EditEmaToolbarComponent } from './components/edit-ema-toolbar/edit-ema-toolbar.component';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import {
    EmaDragItem,
    ClientContentletArea,
    Container,
    UpdatedContentlet,
    InlineEditingContentletDataset
} from './components/ema-page-dropzone/types';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DotPageApiParams } from '../services/dot-page-api.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import { DEFAULT_PERSONA, IFRAME_SCROLL_ZONE, WINDOW } from '../shared/consts';
import { EDITOR_MODE, EDITOR_STATE, NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import {
    ActionPayload,
    PositionPayload,
    ClientData,
    SetUrlPayload,
    VTLFile,
    ContentletDragPayload,
    DeletePayload,
    InsertPayloadFromDelete,
    DragDataset,
    DragDatasetItem,
    ContentTypeDragPayload,
    PostMessagePayload,
    ReorderPayload
} from '../shared/models';
import {
    SDK_EDITOR_SCRIPT_SOURCE,
    areContainersEquals,
    deleteContentletFromContainer,
    insertContentletInContainer
} from '../utils';

@Component({
    selector: 'dot-edit-ema-editor',
    standalone: true,
    templateUrl: './edit-ema-editor.component.html',
    styleUrls: ['./edit-ema-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        CommonModule,
        FormsModule,
        SafeUrlPipe,
        DotSpinnerModule,
        DotEmaDialogComponent,
        ConfirmDialogModule,
        EditEmaToolbarComponent,
        DotMessagePipe,
        EmaPageDropzoneComponent,
        EditEmaPaletteComponent,
        EmaContentletToolsComponent,
        DotEmaBookmarksComponent,
        ProgressBarModule,
        DotResultsSeoToolComponent
    ],
    providers: [
        DotCopyContentModalService,
        DotCopyContentService,
        DotHttpErrorManagerService,
        DotContentletService,
        DotTempFileUploadService
    ]
})
export class EditEmaEditorComponent implements OnInit, OnDestroy {
    @ViewChild('dialog') dialog: DotEmaDialogComponent;
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    private readonly router = inject(Router);
    private readonly activatedRouter = inject(ActivatedRoute);
    private readonly store = inject(EditEmaStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly messageService = inject(MessageService);
    private readonly window = inject(WINDOW);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly dotCopyContentModalService = inject(DotCopyContentModalService);
    private readonly dotCopyContentService = inject(DotCopyContentService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotSeoMetaTagsService = inject(DotSeoMetaTagsService);
    private readonly dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);
    private readonly dotContentletService = inject(DotContentletService);
    private readonly tempFileUploadService = inject(DotTempFileUploadService);
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly inlineEditingService = inject(InlineEditService);
    private readonly destroyRef = inject(DestroyRef);

    readonly editorState$ = this.store.editorState$;
    readonly dragState$ = this.store.dragState$;
    readonly destroy$ = new Subject<boolean>();
    protected ogTagsResults$: Observable<SeoMetaTagsResult[]>;

    readonly pageData = toSignal(this.store.pageData$);

    readonly ogTags: WritableSignal<SeoMetaTags> = signal(undefined);

    readonly clientData: WritableSignal<ClientData> = signal(undefined);

    readonly actionPayload: Signal<ActionPayload> = computed(() => {
        const clientData = this.clientData();
        const { containers, languageId, id, personaTag } = this.pageData();
        const { contentletsId } = containers.find((container) =>
            areContainersEquals(container, clientData.container)
        ) ?? { contentletsId: [] };

        const container = clientData.container
            ? {
                  ...clientData.container,
                  contentletsId
              }
            : null;

        return {
            ...clientData,
            language_id: languageId.toString(),
            pageId: id,
            pageContainers: containers,
            personaTag,
            container
        } as ActionPayload;
    });

    readonly currentTreeNode: Signal<DotTreeNode> = computed(() => {
        const { contentlet, container } = this.actionPayload();
        const { identifier: contentId } = contentlet;
        const { variantId, uuid: relationType, contentletsId, identifier: containerId } = container;
        const { personalization, id: pageId } = untracked(() => this.pageData());
        const treeOrder = contentletsId.findIndex((id) => id === contentId).toString();

        return {
            contentId,
            containerId,
            relationType,
            variantId,
            personalization,
            treeOrder,
            pageId
        };
    });

    readonly host = '*';
    readonly editorState = EDITOR_STATE;
    readonly editorMode = EDITOR_MODE;
    readonly experimentStatus = DotExperimentStatus;

    get queryParams(): DotPageApiParams {
        return this.activatedRouter.snapshot.queryParams as DotPageApiParams;
    }

    isVTLPage = toSignal(this.store.clientHost$.pipe(map((clientHost) => !clientHost)));
    $isInlineEditing = toSignal(
        this.store.editorMode$.pipe(map((mode) => mode === EDITOR_MODE.INLINE_EDITING))
    );

    ngOnInit(): void {
        this.handleReloadContent();
        this.handleDragEvents();

        fromEvent(this.window, 'message')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: MessageEvent) => {
                this.handlePostMessage(event)?.();
            });

        // In VTL Page if user click in a link in the page, we need to update the URL in the editor
        this.store.pageRendered$
            .pipe(
                takeUntil(this.destroy$),
                filter(() => this.isVTLPage())
            )
            .subscribe(() => {
                requestAnimationFrame(() => {
                    const win = this.iframe.nativeElement.contentWindow;

                    fromEvent(win, 'click').subscribe((e: MouseEvent) => {
                        this.handleInternalNav(e);
                    });
                });
            });

        this.store.vtlIframePage$
            .pipe(
                takeUntil(this.destroy$),
                filter(({ isEnterprise }) => this.isVTLPage() && isEnterprise)
            )
            .subscribe(({ mode }) => {
                requestAnimationFrame(() => {
                    const win = this.iframe.nativeElement.contentWindow;

                    if (
                        mode === EDITOR_MODE.EDIT ||
                        mode === EDITOR_MODE.EDIT_VARIANT ||
                        mode === EDITOR_MODE.INLINE_EDITING
                    ) {
                        this.inlineEditingService.injectInlineEdit(this.iframe);
                        fromEvent(win, 'click').subscribe((e: MouseEvent) => {
                            this.handleInlineEditing(e);
                        });
                    } else {
                        this.inlineEditingService.removeInlineEdit(this.iframe);
                    }
                });
            });
    }

    /**
     * Handles internal navigation by preventing the default behavior of the click event,
     * updating the query parameters, and opening external links in a new tab.
     *
     * @param e - The MouseEvent object representing the click event.
     */
    handleInternalNav(e: MouseEvent) {
        const href =
            (e.target as HTMLAnchorElement)?.href ||
            (e.target as HTMLElement)?.closest('a')?.getAttribute('href');

        if (href) {
            e.preventDefault();
            const url = new URL(href);

            // Check if the URL is not external
            if (url.hostname === window.location.hostname) {
                this.updateQueryParams({
                    url: url.pathname
                });

                return;
            }

            // Open external links in a new tab
            this.window.open(href, '_blank');
        }
    }

    /**
     * Handles the inline editing functionality triggered by a mouse event.
     * @param e - The mouse event that triggered the inline editing.
     */
    handleInlineEditing(e: MouseEvent) {
        const target = e.target as HTMLElement;
        const element: HTMLElement = target.dataset?.mode ? target : target.closest('[data-mode]');

        if (!element?.dataset.mode) {
            return;
        }

        this.inlineEditingService.handleInlineEdit({
            ...element.dataset
        } as unknown as InlineEditingContentletDataset);
    }

    handleDragEvents() {
        this.store.isUserDragging$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
            this.iframe.nativeElement.contentWindow?.postMessage(
                NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS,
                this.host
            );
        });

        fromEvent(this.window, 'dragstart')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: DragEvent) => {
                const dataset = (event.target as HTMLDivElement).dataset as unknown as DragDataset;

                const parsedItem = JSON.parse(dataset.item) as DragDatasetItem;

                const { contentType, contentlet, container, move } = parsedItem;

                if (dataset.type === 'content-type') {
                    this.store.setDragItem({
                        baseType: contentType.baseType,
                        contentType: contentType.variable,
                        draggedPayload: {
                            item: {
                                variable: contentType.variable,
                                name: contentType.name
                            },
                            type: dataset.type,
                            move
                        } as ContentTypeDragPayload
                    });
                } else {
                    this.store.setDragItem({
                        baseType: contentlet.baseType,
                        contentType: contentlet.contentType,
                        draggedPayload: {
                            item: {
                                contentlet,
                                container
                            },
                            type: dataset.type,
                            move
                        } as ContentletDragPayload
                    });
                }
            });

        fromEvent(this.window, 'dragenter')
            .pipe(
                takeUntil(this.destroy$),
                // For some reason the fromElement is not in the DragEvent type
                filter((event: DragEvent & { fromElement: HTMLElement }) => !event.fromElement), // I just want to trigger this when we are dragging from the outside
                switchMap((event) =>
                    this.dragState$.pipe(
                        take(1),
                        map(({ dragItem, editorState }) => ({
                            event,
                            dragItem,
                            editorState
                        }))
                    )
                )
            )
            .subscribe(
                ({
                    dragItem,
                    event,
                    editorState
                }: {
                    dragItem: EmaDragItem;
                    event: DragEvent;
                    editorState: EDITOR_STATE;
                }) => {
                    event.preventDefault();
                    // Set the temp item to be dragged, which is the outsider file if there is not a drag item
                    if (!dragItem) {
                        this.store.setDragItem({
                            baseType: 'dotAsset',
                            contentType: 'dotAsset',
                            draggedPayload: {
                                type: 'temp'
                            }
                        });
                    } else if (editorState === EDITOR_STATE.OUT_OF_BOUNDS) {
                        this.store.updateEditorState(EDITOR_STATE.DRAGGING);
                    }

                    this.iframe.nativeElement.contentWindow?.postMessage(
                        NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS,
                        this.host
                    );
                }
            );

        fromEvent(this.window, 'dragend')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: DragEvent) => {
                if (event.dataTransfer.dropEffect === 'none') {
                    this.store.updateEditorState(EDITOR_STATE.IDLE);
                }
            });

        fromEvent(this.window, 'dragover')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: DragEvent) => {
                event.preventDefault(); // Prevent file opening
                const iframeRect = this.iframe.nativeElement.getBoundingClientRect();

                const isInsideIframe =
                    event.clientX > iframeRect.left && event.clientX < iframeRect.right;

                if (!isInsideIframe) {
                    this.store.updateEditorState(EDITOR_STATE.DRAGGING);

                    return;
                }

                let direction;

                if (
                    event.clientY > iframeRect.top &&
                    event.clientY < iframeRect.top + IFRAME_SCROLL_ZONE
                ) {
                    direction = 'up';
                }

                if (
                    event.clientY > iframeRect.bottom - IFRAME_SCROLL_ZONE &&
                    event.clientY <= iframeRect.bottom
                ) {
                    direction = 'down';
                }

                if (!direction) {
                    this.store.updateEditorState(EDITOR_STATE.DRAGGING);

                    return;
                }

                this.store.setScrollingState();

                this.iframe.nativeElement.contentWindow?.postMessage(
                    { name: NOTIFY_CUSTOMER.EMA_SCROLL_INSIDE_IFRAME, direction },
                    this.host
                );
            });

        fromEvent(this.window, 'drop')
            .pipe(
                takeUntil(this.destroy$),
                switchMap((event) =>
                    this.dragState$.pipe(
                        take(1),
                        map(({ dragItem }) => ({
                            event,
                            dragItem
                        }))
                    )
                )
            )
            .subscribe(({ event, dragItem }: { event: DragEvent; dragItem: EmaDragItem }) => {
                event.preventDefault();
                const target = event.target as HTMLDivElement;

                const { position, payload, dropzone } = target.dataset;

                // If we drop in a container that is not a dropzone, we just reset the editor state
                if (dropzone !== 'true') {
                    this.store.updateEditorState(EDITOR_STATE.IDLE);

                    return;
                }

                const data: ClientData = JSON.parse(payload);

                const file = event.dataTransfer?.files[0]; // We are sure that is comes but in the tests we don't have DragEvent class

                if (file) {
                    // I need to publish the temp file to use it.
                    this.handleFileUpload({
                        file,
                        data,
                        position,
                        dragItem
                    });
                } else {
                    const positionPayload = <PositionPayload>{
                        position,
                        ...data
                    };

                    this.placeItem(positionPayload, dragItem);
                }
            });

        fromEvent(this.window, 'dragleave')
            .pipe(
                takeUntil(this.destroy$),
                filter((event: DragEvent) => !event.relatedTarget) // Just reset when is out of the window
            )
            .subscribe(() => {
                // I need to do this to hide the dropzone but maintain the current dragItem
                this.store.updateEditorState(EDITOR_STATE.OUT_OF_BOUNDS); // The user is dragging outside the window, we set this to know that user can potentially drop a file outside the window
            });
    }

    /**
     * Handle the reset of the editor when the user drops a file outside of the browser
     *
     * @param {(MouseEvent)} event
     * @memberof EditEmaEditorComponent
     */
    @HostListener('mouseover', ['$event'])
    resetEditorWhenOutOfBounds(event: MouseEvent) {
        event.preventDefault();

        this.dragState$
            .pipe(
                take(1),
                filter(
                    ({ dragItem, editorState }) =>
                        !!dragItem && editorState === EDITOR_STATE.OUT_OF_BOUNDS // If the user dropped outside of the window and we still have a dragItem we need to clean the editor
                )
            )
            .subscribe(() => {
                this.store.updateEditorState(EDITOR_STATE.IDLE);
            });
    }

    /**
     * Handles the reload of content in the editor.
     * If the editor state is LOADED and the content is not VTL, it reloads the iframe.
     * If the content is VTL, it loads the VTL iframe content.
     * @memberof EditEmaEditorComponent
     */
    handleReloadContent() {
        this.store.contentState$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ shouldReload, code, isVTL }) => {
                // If we are idle then we are not dragging

                this.resetDragProperties();

                if (!shouldReload) {
                    /** We have some EDITOR_STATE values that we don't want to reload the content
                     *  Only when we should realod the content we do it
                     */
                    return;
                }

                if (isVTL) {
                    this.setIframeContent(code);
                } else {
                    this.reloadIframeContent();
                }

                this.store.setShouldReload(false);
            });
    }

    /**
     * Handle the iframe page load
     *
     * @param {string} clientHost
     * @memberof EditEmaEditorComponent
     */
    onIframePageLoad(editorMode: EDITOR_MODE) {
        this.store.updateEditorState(EDITOR_STATE.IDLE);

        //The iframe is loaded after copy contentlet to inline editing.
        if (editorMode === EDITOR_MODE.INLINE_EDITING) {
            this.inlineEditingService.initEditor();
        }
    }

    /**
     * Add the editor page script to VTL pages
     *
     * @param {string} rendered
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    addEditorPageScript(rendered = ''): string {
        const scriptString = `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`;
        const bodyExists = rendered.includes('</body>');

        /*
         * For advance template case. It might not include `body` tag.
         */
        if (!bodyExists) {
            return rendered + scriptString;
        }

        const updatedRendered = rendered.replace('</body>', scriptString + '</body>');

        return updatedRendered;
    }

    /**
     * Add custom styles to the rendered content
     *
     * @param {string} rendered
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    addCustomStyles(rendered = ''): string {
        const styles = `<style>

        [data-dot-object="container"]:empty {
            width: 100%;
            background-color: #ECF0FD;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #030E32;
            height: 10rem;
        }

        [data-dot-object="contentlet"].empty-contentlet {
            min-height: 4rem;
            width: 100%;
        }

        [data-dot-object="container"]:empty::after {
            content: '${this.dotMessageService.get('editpage.container.is.empty')}';
        }
        </style>
        `;

        const headExists = rendered.includes('</head>');

        /*
         * For advance template case. It might not include `head` tag.
         */
        if (!headExists) {
            return rendered + styles;
        }

        return rendered.replace('</head>', styles + '</head>');
    }

    /**
     * Inject the editor page script and styles to the VTL content
     *
     * @private
     * @param {string} rendered
     * @return {*}  {string}
     * @memberof EditEmaEditorComponent
     */
    private inyectCodeToVTL(rendered: string): string {
        const fileWithScript = this.addEditorPageScript(rendered);
        const fileWithStylesAndScript = this.addCustomStyles(fileWithScript);

        return fileWithStylesAndScript;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle the custom event
     *
     * @param {{ event: CustomEvent; payload: ActionPayload }} { event, payload }
     * @memberof EditEmaEditorComponent
     */
    onCustomEvent({ event, payload }: { event: CustomEvent; payload: ActionPayload }) {
        this.handleNgEvent({ event, payload })?.();
    }

    /**
     * Handle the language selection
     *
     * @param {number} language_id
     * @memberof DotEmaComponent
     */
    onLanguageSelected(language_id: number) {
        this.updateQueryParams({
            language_id
        });
    }

    /**
     * When the user drop a palette item in the dropzone
     *
     * @param {PositionPayload} positionPayload
     * @return {*}  {void}
     * @memberof EditEmaEditorComponent
     */
    placeItem(positionPayload: PositionPayload, dragItem: EmaDragItem): void {
        let payload = this.getPageSavePayload(positionPayload);

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

            this.store.savePage({
                pageContainers,
                pageId: payload.pageId,
                params: this.queryParams
            });

            return;
        } else if (dragItem.draggedPayload.type === 'content-type') {
            this.store.updateEditorState(EDITOR_STATE.IDLE); // In case the user cancels the creation of the contentlet, we already have the editor in idle state

            this.dialog.createContentletFromPalette({ ...dragItem.draggedPayload.item, payload });
        } else if (dragItem.draggedPayload.type === 'temp') {
            const { pageContainers, didInsert } = insertContentletInContainer({
                ...payload,
                newContentletId: payload.newContentlet.identifier
            });

            if (!didInsert) {
                this.handleDuplicatedContentlet();

                return;
            }

            this.store.savePage({
                pageContainers,
                pageId: payload.pageId,
                params: this.queryParams
            });
        }
    }
    /**
     * Delete contentlet
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    deleteContentlet(payload: ActionPayload) {
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
                this.store.savePage({
                    pageContainers,
                    pageId: payload.pageId,
                    params: this.queryParams,
                    whenSaved: () => {
                        this.dialog.resetDialog();
                    }
                }); // Save when selected
            }
        });
    }

    /**
     *
     * Sets the content of the iframe with the provided code.
     * @param code - The code to be added to the iframe.
     * @memberof EditEmaEditorComponent
     */
    setIframeContent(code: string) {
        requestAnimationFrame(() => {
            const doc = this.iframe?.nativeElement.contentDocument;

            if (doc) {
                const newFile = this.inyectCodeToVTL(code);

                doc.open();
                doc.write(newFile);
                doc.close();

                this.ogTags.set(this.dotSeoMetaTagsUtilService.getMetaTags(doc));
                this.ogTagsResults$ = this.dotSeoMetaTagsService
                    .getMetaTagsResults(doc)
                    .pipe(take(1));
            }
        });
    }

    protected handleNgEvent({ event, payload }: { event: CustomEvent; payload: ActionPayload }) {
        const { detail } = event;

        return (<Record<NG_CUSTOM_EVENTS, () => void>>{
            [NG_CUSTOM_EVENTS.EDIT_CONTENTLET_LOADED]: () => {
                /* */
            },
            [NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT]: () => {
                const { pageContainers, didInsert } = insertContentletInContainer({
                    ...payload,
                    newContentletId: detail.data.identifier
                });

                if (!didInsert) {
                    this.handleDuplicatedContentlet();

                    return;
                }

                // Save when selected
                this.store.savePage({
                    pageContainers,
                    pageId: payload.pageId,
                    params: this.queryParams,
                    whenSaved: () => {
                        this.dialog.resetDialog();
                    }
                });
            },
            [NG_CUSTOM_EVENTS.SAVE_PAGE]: () => {
                const { shouldReloadPage, contentletIdentifier } = detail.payload;

                if (shouldReloadPage) {
                    this.reloadURLContentMapPage(contentletIdentifier);

                    return;
                }

                if (!payload) {
                    this.store.reload({
                        params: this.queryParams
                    });

                    return;
                }

                const { pageContainers, didInsert } = insertContentletInContainer({
                    ...payload,
                    newContentletId: contentletIdentifier
                });

                if (!didInsert) {
                    this.handleDuplicatedContentlet();

                    return;
                }

                this.store.savePage({
                    pageContainers,
                    pageId: payload.pageId,
                    params: this.queryParams,
                    whenSaved: () => {
                        this.dialog.resetDialog();
                    }
                });
            },
            [NG_CUSTOM_EVENTS.CREATE_CONTENTLET]: () => {
                this.dialog.createContentlet({
                    contentType: detail.data.contentType,
                    url: detail.data.url,
                    payload
                });
                this.cd.detectChanges();
            },
            [NG_CUSTOM_EVENTS.FORM_SELECTED]: () => {
                const identifier = detail.data.identifier;

                this.store.saveFormToPage({
                    payload,
                    formId: identifier,
                    params: this.queryParams,
                    whenSaved: () => {
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

                this.store.reload({
                    params: this.queryParams
                });
                this.dialog.resetDialog();
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
            }
        })[detail.name];
    }

    /**
     * Handle the post message event
     *
     * @private
     * @param {{ action: CUSTOMER_ACTIONS; payload: DotCMSContentlet }} data
     * @return {*}
     * @memberof DotEmaComponent
     */
    private handlePostMessage({
        origin: _origin = this.host,
        data
    }: {
        origin: string;
        data: {
            action: CUSTOMER_ACTIONS;
            payload: PostMessagePayload;
        };
    }): () => void {
        return (<Record<CUSTOMER_ACTIONS, () => void>>{
            [CUSTOMER_ACTIONS.NAVIGATION_UPDATE]: () => {
                const payload = <SetUrlPayload>data.payload;

                // When we set the url, we trigger in the shell component a load to get the new state of the page
                // This triggers a rerender that makes nextjs to send the set_url again
                // But this time the params are the same so the shell component wont trigger a load and there we know that the page is loaded
                const isSameUrl = this.queryParams.url === payload.url;

                if (isSameUrl) {
                    // TODO: HOW DO WE DO THIS NOW?
                    // this.personaSelector.fetchPersonas(); // We need to fetch the personas again because the page is loaded
                    this.store.updateEditorState(EDITOR_STATE.IDLE);
                } else {
                    this.updateQueryParams({
                        url: payload.url,
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    });
                }
            },
            [CUSTOMER_ACTIONS.SET_BOUNDS]: () => {
                this.store.setBounds(<Container[]>data.payload);
            },
            [CUSTOMER_ACTIONS.SET_CONTENTLET]: () => {
                const contentletArea = <ClientContentletArea>data.payload;

                const payload = this.getPageSavePayload(contentletArea.payload);

                this.store.setContentletArea({
                    ...contentletArea,
                    payload
                });
            },
            [CUSTOMER_ACTIONS.IFRAME_SCROLL]: () => {
                this.store.updateEditorScrollState();
            },
            [CUSTOMER_ACTIONS.IFRAME_SCROLL_END]: () => {
                this.store.updateEditorDragState();
            },
            [CUSTOMER_ACTIONS.INIT_INLINE_EDITING]: () => {
                // The iframe says that the editor is ready to start inline editing
                // The dataset of the inline-editing contentlet is ready inside the service.
                this.inlineEditingService.initEditor();
            },
            [CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING]: () => {
                // The iframe say the contentlet that try to be inline edited is in multiple pages
                // So the editor open the dialog to question if the edit is in ALL contentlets or only in this page.

                if (this.$isInlineEditing()) {
                    // If is already in inline editing, dont open the dialog.
                    return;
                }

                const payload = <{ dataset: InlineEditingContentletDataset }>data.payload;

                this.dotCopyContentModalService
                    .open()
                    .pipe(
                        switchMap(({ shouldCopy }) => {
                            if (!shouldCopy) {
                                return of(null);
                            }

                            return this.handleCopyContent();
                        })
                    )
                    .subscribe((res: DotCMSContentlet | null) => {
                        const updatedDataset = {
                            inode: res?.inode || payload.dataset.inode,
                            fieldName: payload.dataset.fieldName,
                            mode: payload.dataset.mode,
                            language: payload.dataset.language
                        };

                        this.inlineEditingService.setTargetInlineMCEDataset(updatedDataset);
                        this.store.setEditorMode(EDITOR_MODE.INLINE_EDITING);
                        if (res) {
                            this.store.reload({
                                params: this.queryParams
                            });

                            return;
                        }

                        this.inlineEditingService.initEditor();
                    });
            },
            [CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING]: () => {
                const payload = <UpdatedContentlet>data.payload;

                if (!payload) {
                    const mode =
                        this.queryParams.variantName &&
                        this.queryParams.variantName !== DEFAULT_VARIANT_ID
                            ? EDITOR_MODE.EDIT_VARIANT
                            : EDITOR_MODE.EDIT;

                    this.store.setEditorMode(mode);

                    return;
                }

                this.store.saveFromInlineEditedContentlet({
                    contentlet: {
                        inode: payload.dataset['inode'],
                        [payload.dataset.fieldName]: payload.innerHTML
                    },
                    params: this.queryParams
                });
            },
            [CUSTOMER_ACTIONS.REORDER_MENU]: () => {
                const { reorderUrl } = <ReorderPayload>data.payload;

                this.dialog.openDialogOnUrl(
                    reorderUrl,
                    this.dotMessageService.get('editpage.content.contentlet.menu.reorder.title')
                );
            },
            [CUSTOMER_ACTIONS.GET_PAGE_DATA]: () => {
                this.reloadIframeContent();
            },
            [CUSTOMER_ACTIONS.NOOP]: () => {
                /* Do Nothing because is not the origin we are expecting */
            }
        })[data.action];
    }

    /**
     * Notify the user to reload the iframe
     *
     * @private
     * @memberof DotEmaComponent
     */
    reloadIframeContent() {
        this.iframe?.nativeElement?.contentWindow?.postMessage(
            { name: NOTIFY_CUSTOMER.SET_PAGE_DATA, payload: this.store.state().editor },
            this.host
        );
    }

    /**
     * Update the query params
     *
     * @private
     * @param {Params} params
     * @memberof EditEmaEditorComponent
     */
    private updateQueryParams(params: Params) {
        this.store.updateEditorState(EDITOR_STATE.LOADING);
        this.router.navigate([], {
            queryParams: params,
            queryParamsHandling: 'merge'
        });
    }

    private handleDuplicatedContentlet() {
        this.messageService.add({
            severity: 'info',
            summary: this.dotMessageService.get('editpage.content.add.already.title'),
            detail: this.dotMessageService.get('editpage.content.add.already.message'),
            life: 2000
        });

        this.store.updateEditorState(EDITOR_STATE.IDLE);
        this.dialog.resetDialog();
    }

    /**
     * Get the page save payload
     *
     * @private
     * @param {PositionPayload} positionPayload
     * @return {*}  {ActionPayload}
     * @memberof EditEmaEditorComponent
     */
    private getPageSavePayload(positionPayload: PositionPayload): ActionPayload {
        this.clientData.set(positionPayload);

        return this.actionPayload();
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
        const { contentlet } = payload;
        const { onNumberOfPages = '1', title } = contentlet;

        if (Number(onNumberOfPages) <= 1) {
            this.dialog.editContentlet(contentlet);

            return;
        }

        this.dotCopyContentModalService
            .open()
            .pipe(
                switchMap(({ shouldCopy }) => {
                    if (!shouldCopy) {
                        return of(contentlet);
                    }

                    this.dialog.showLoadingIframe(title);

                    return this.handleCopyContent();
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
    protected editContentMap(contentlet: DotCMSContentlet): void {
        this.dialog.editUrlContentMapContentlet(contentlet);
    }

    /**
     * Handle copy content
     *
     * @private
     * @return {*}
     * @memberof DotEmaDialogComponent
     */
    private handleCopyContent(): Observable<DotCMSContentlet> {
        return this.dotCopyContentService.copyInPage(this.currentTreeNode()).pipe(
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
     * Reset the drag properties
     *
     * @private
     * @memberof EditEmaEditorComponent
     */
    protected resetDragProperties() {
        this.store.resetDragProperties();
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
        this.store.updateEditorState(EDITOR_STATE.LOADING);

        this.dotContentletService
            .getContentletByInode(inodeOrIdentifier)
            .pipe(
                catchError((error) => this.handlerError(error)),
                filter((contentlet) => !!contentlet)
            )
            .subscribe(({ URL_MAP_FOR_CONTENT }) => {
                if (URL_MAP_FOR_CONTENT != this.queryParams.url) {
                    this.store.updateEditorState(EDITOR_STATE.IDLE);
                    // If the URL is different, we need to navigate to the new URL
                    this.updateQueryParams({ url: URL_MAP_FOR_CONTENT });

                    return;
                }

                // If the URL is the same, we need to fetch the new page data
                this.store.reload({
                    params: this.queryParams
                });
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
        this.store.updateEditorState(EDITOR_STATE.ERROR);

        return this.dotHttpErrorManagerService.handle(error).pipe(map(() => null));
    }

    /**
     * Reloads the component from the dialog.
     */
    reloadFromDialog() {
        this.store.reload({ params: this.queryParams });
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
        if (!/image.*/.exec(file.type)) {
            this.messageService.add({
                severity: 'error',
                summary: this.dotMessageService.get('file-upload'),
                detail: this.dotMessageService.get('editpage.file.upload.not.image'),
                life: 3000
            });

            this.store.updateEditorState(EDITOR_STATE.IDLE);

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
                // If there is no contentlet then the file was not uploaded
                if (!contentlet) {
                    this.store.updateEditorState(EDITOR_STATE.IDLE);

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
}
