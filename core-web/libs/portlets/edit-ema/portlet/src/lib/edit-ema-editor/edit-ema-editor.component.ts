import { Observable, Subject, fromEvent, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
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
import { toSignal } from '@angular/core/rxjs-interop';
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
    DotContentletService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
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
import { DEFAULT_PERSONA, WINDOW } from '../shared/consts';
import { EDITOR_MODE, EDITOR_STATE, NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import {
    ActionPayload,
    PositionPayload,
    ClientData,
    SetUrlPayload,
    VTLFile,
    ReorderPayload,
    PostMessagePayload,
    ContentletDragPayload,
    DeletePayload,
    DraggedPalettePayload,
    InsertPayloadFromDelete
} from '../shared/models';
import {
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
        DotContentletService
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
    private readonly inlineEditingService = inject(InlineEditService);

    readonly editorState$ = this.store.editorState$;
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

        return {
            ...clientData,
            language_id: languageId.toString(),
            pageId: id,
            pageContainers: containers,
            personaTag,
            container: {
                ...clientData.container,
                contentletsId
            }
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

    protected draggedPayload: DraggedPalettePayload;

    dragItem: EmaDragItem;

    get queryParams(): DotPageApiParams {
        return this.activatedRouter.snapshot.queryParams as DotPageApiParams;
    }

    isVTLPage = toSignal(this.store.clientHost$.pipe(map((clientHost) => !clientHost)));
    $isInlineEditing = toSignal(
        this.store.editorMode$.pipe(map((mode) => mode === EDITOR_MODE.INLINE_EDITING))
    );

    ngOnInit(): void {
        this.handleReloadContent();

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

                    if (mode === EDITOR_MODE.EDIT || mode === EDITOR_MODE.INLINE_EDITING) {
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

    /**
     * Handles the reload of content in the editor.
     * If the editor state is LOADED and the content is not VTL, it reloads the iframe.
     * If the content is VTL, it loads the VTL iframe content.
     * @memberof EditEmaEditorComponent
     */
    handleReloadContent() {
        this.store.contentState$
            .pipe(
                takeUntil(this.destroy$),
                filter(({ state }) => state === EDITOR_STATE.IDLE)
            )
            .subscribe(({ code }) => {
                // If we are idle then we are not dragging
                this.resetDragProperties();

                if (!this.isVTLPage()) {
                    // Only reload if is Headless.
                    // If is VTL, the content is updated by store.code$
                    this.reloadIframe();
                } else {
                    this.setIframeContent(code);
                }
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
        const scriptString = `<script src="/html/js/editor-js/sdk-editor.esm.js"></script>`;
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

        [data-dot-object="container"]:empty::after {
            content: '${this.dotMessageService.get('editpage.container.is.empty')}';
        }
        </style>
        `;

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
        let newFile = this.addEditorPageScript(rendered);

        newFile = this.addCustomStyles(newFile);

        return newFile;
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
     * Move contentlet to a new position
     *
     * @param {ActionPayload} item
     * @memberof EditEmaEditorComponent
     */
    moveContentlet(item: ActionPayload) {
        this.store.updateEditorState(EDITOR_STATE.DRAGGING);

        this.draggedPayload = {
            type: 'contentlet',
            item: {
                container: item.container,
                contentlet: item.contentlet
            },
            move: true
        };

        this.dragItem = {
            baseType: 'CONTENT',
            contentType: item.contentlet.contentType
        };

        this.iframe.nativeElement.contentWindow?.postMessage(
            NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS,
            this.host
        );
    }

    /**
     * Handle palette start drag event
     *
     * @param {DragEvent} event
     * @memberof EditEmaEditorComponent
     */
    onDragStart(event: DragEvent) {
        this.store.updateEditorState(EDITOR_STATE.DRAGGING);

        const dataset = (event.target as HTMLDivElement).dataset as unknown as Pick<
            ContentletDragPayload,
            'type'
        > & {
            item: string;
        };

        const item = JSON.parse(dataset.item);

        this.dragItem = {
            baseType: item.baseType,
            contentType: item.contentType
        };

        this.draggedPayload = {
            type: dataset.type,
            item,
            move: false
        };

        this.iframe.nativeElement.contentWindow?.postMessage(
            NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS,
            this.host
        );
    }

    /**
     * Reset rows when user stop dragging
     *
     * @memberof EditEmaEditorComponent
     */
    onDragEnd(event: DragEvent) {
        // If the dropEffect is none then the user didn't drop the item in the dropzone
        if (event.dataTransfer.dropEffect === 'none') {
            this.store.updateEditorState(EDITOR_STATE.IDLE);
        }
    }

    /**
     * When the user drop a palette item in the dropzone
     *
     * @param {PositionPayload} positionPayload
     * @return {*}  {void}
     * @memberof EditEmaEditorComponent
     */
    onPlaceItem(positionPayload: PositionPayload): void {
        let payload = this.getPageSavePayload(positionPayload);

        const destinationContainer = payload.container;
        const pivotContentlet = payload.contentlet;
        const positionToInsert = positionPayload.position;

        if (this.draggedPayload.type === 'contentlet') {
            const draggedPayload = this.draggedPayload;
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
        }

        this.dialog.createContentletFromPalette({ ...this.draggedPayload.item, payload });
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
    setIframeContent(code) {
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
                this.resetDragProperties();
                this.store.updateEditorState(EDITOR_STATE.IDLE);
            },
            [CUSTOMER_ACTIONS.PING_EDITOR]: () => {
                this.iframe?.nativeElement?.contentWindow.postMessage(
                    NOTIFY_CUSTOMER.EMA_EDITOR_PONG,
                    this.host
                );
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
                    this.store.setEditorMode(EDITOR_MODE.EDIT);

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
    reloadIframe() {
        this.iframe?.nativeElement?.contentWindow?.postMessage(
            NOTIFY_CUSTOMER.EMA_RELOAD_PAGE,
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
        this.draggedPayload = undefined;
        this.dragItem = null;

        this.store.setContentletArea(null);
        this.store.setBounds([]);
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
}
