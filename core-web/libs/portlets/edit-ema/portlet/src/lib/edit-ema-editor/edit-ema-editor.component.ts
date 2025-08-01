import { tapResponse } from '@ngrx/operators';
import { EMPTY, Observable, Subject, fromEvent, of } from 'rxjs';

import { NgClass, NgStyle } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    ViewChild,
    WritableSignal,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressBarModule } from 'primeng/progressbar';

import { takeUntil, catchError, filter, map, switchMap, tap, take } from 'rxjs/operators';

import {
    DotMessageService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotContentletService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService,
    DotAlertConfirmService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
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
    DotCMSUVEAction
} from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { SafeUrlPipe, DotSpinnerModule, DotCopyContentModalService } from '@dotcms/ui';
import { isEqual, WINDOW } from '@dotcms/utils';

import { DotUvePageVersionNotFoundComponent } from './components/dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import {
    EmaDragItem,
    ClientContentletArea,
    Container,
    InlineEditingContentletDataset,
    UpdatedContentlet
} from './components/ema-page-dropzone/types';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiService } from '../services/dot-page-api.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import { DEFAULT_PERSONA, IFRAME_SCROLL_ZONE, PERSONA_KEY } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import {
    ActionPayload,
    PositionPayload,
    ClientData,
    SetUrlPayload,
    VTLFile,
    DeletePayload,
    InsertPayloadFromDelete,
    DialogAction,
    PostMessage,
    ReorderMenuPayload
} from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import {
    SDK_EDITOR_SCRIPT_SOURCE,
    TEMPORAL_DRAG_ITEM,
    createReorderMenuURL,
    compareUrlPaths,
    deleteContentletFromContainer,
    getDragItemData,
    insertContentletInContainer,
    getTargetUrl,
    shouldNavigate,
    convertClientParamsToPageParams
} from '../utils';

@Component({
    selector: 'dot-edit-ema-editor',
    standalone: true,
    templateUrl: './edit-ema-editor.component.html',
    styleUrls: ['./edit-ema-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        NgClass,
        NgStyle,
        FormsModule,
        SafeUrlPipe,
        DotSpinnerModule,
        DotEmaDialogComponent,
        ConfirmDialogModule,
        EmaPageDropzoneComponent,
        EditEmaPaletteComponent,
        EmaContentletToolsComponent,
        ProgressBarModule,
        DotResultsSeoToolComponent,
        DotUveToolbarComponent,
        DotBlockEditorSidebarComponent,
        DotUvePageVersionNotFoundComponent
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
    @ViewChild('blockSidebar') blockSidebar: DotBlockEditorSidebarComponent;

    protected readonly uveStore = inject(UVEStore);
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
    private readonly dotPageApiService = inject(DotPageApiService);
    readonly #dotAlertConfirmService = inject(DotAlertConfirmService);

    readonly destroy$ = new Subject<boolean>();

    readonly host = '*';
    readonly $ogTags: WritableSignal<SeoMetaTags> = signal(undefined);
    readonly $editorProps = this.uveStore.$editorProps;

    readonly $isPreviewMode = this.uveStore.$isPreviewMode;
    readonly $editorContentStyles = this.uveStore.$editorContentStyles;
    readonly ogTagsResults$ = toObservable(this.uveStore.ogTagsResults);

    readonly $paletteOpen = this.uveStore.paletteOpen;
    readonly UVE_STATUS = UVE_STATUS;

    get contentWindow(): Window {
        return this.iframe.nativeElement.contentWindow;
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
        const { isTraditionalPage } = this.uveStore.$reloadEditorContent();
        const isClientReady = untracked(() => this.uveStore.isClientReady());

        untracked(() => {
            this.uveStore.resetEditorProperties();
            this.dialog?.resetActionPayload();
        });

        if (isTraditionalPage || !isClientReady) {
            return;
        }

        this.reloadIframeContent();
    });

    readonly $handleIsDraggingEffect = effect(() => {
        const isDragging = this.uveStore.$editorIsInDraggingState();

        if (!isDragging) {
            return;
        }

        this.contentWindow?.postMessage(
            { name: __DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS },
            this.host
        );
    });

    ngOnInit(): void {
        this.handleDragEvents();

        fromEvent(this.window, 'message')
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ data }: MessageEvent) => this.handlePostMessage(data));
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
        const isInlineEditing = this.uveStore.state() === EDITOR_STATE.INLINE_EDITING;

        // If the link is not valid or we are in inline editing mode, we do nothing
        if (!href || isInlineEditing) {
            return;
        }

        const url = new URL(href, location.origin);

        if (url.hostname !== location.hostname) {
            this.window.open(href, '_blank');

            return;
        }

        this.uveStore.loadPageAsset({ url: url.pathname });
        e.preventDefault();
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
        fromEvent(this.window, 'dragstart')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: DragEvent) => {
                const { dataset } = event.target as HTMLDivElement;
                const data = getDragItemData(dataset);

                // Needed to identify if a dotcms dragItem from the window left and came back
                // More info: https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/setData
                event.dataTransfer?.setData('dotcms/item', '');

                // If there is no data, we do nothing because it's not a valid dragItem
                if (!data) {
                    return;
                }

                this.uveStore.setEditorDragItem(data);
            });

        fromEvent(this.window, 'dragenter')
            .pipe(
                takeUntil(this.destroy$),
                // For some reason the fromElement is not in the DragEvent type
                filter((event: DragEvent & { fromElement: HTMLElement }) => !event.fromElement) // I just want to trigger this when we are dragging from the outside
            )
            .subscribe((event: DragEvent) => {
                event.preventDefault();

                const types = event.dataTransfer?.types || [];
                const dragItem = this.uveStore.dragItem();

                // Identify if the dotcms dragItem entered the editor from the outside
                // We do not set dragging state, forcing users to do the dragging action again
                // This check does not apply if users drag something from their computer
                // More info: https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/types
                if (!dragItem && types.includes('dotcms/item')) {
                    return;
                }

                this.uveStore.setEditorState(EDITOR_STATE.DRAGGING);
                this.contentWindow?.postMessage(
                    {
                        name: __DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS
                    },
                    this.host
                );

                if (dragItem) {
                    return;
                }

                this.uveStore.setEditorDragItem(TEMPORAL_DRAG_ITEM);
            });

        fromEvent(this.window, 'dragend')
            .pipe(
                takeUntil(this.destroy$),
                filter((event: DragEvent) => event.dataTransfer.dropEffect === 'none')
            )
            .subscribe(() => {
                this.uveStore.resetEditorProperties();
            });

        fromEvent(this.window, 'dragover')
            .pipe(
                takeUntil(this.destroy$),
                // Check that  `dragItem()` is not empty because there is a scenario where a dragover
                // occurs over the editor after invoking `handleReloadContentEffect`, which clears the dragItem.
                // For more details, refer to the issue: https://github.com/dotCMS/core/issues/29855
                filter((_event: DragEvent) => !!this.uveStore.dragItem())
            )
            .subscribe((event: DragEvent) => {
                event.preventDefault(); // Prevent file opening
                const iframeRect = this.iframe.nativeElement.getBoundingClientRect();

                const isInsideIframe =
                    event.clientX > iframeRect.left && event.clientX < iframeRect.right;

                if (!isInsideIframe) {
                    this.uveStore.setEditorState(EDITOR_STATE.DRAGGING);

                    return;
                }

                let direction: 'up' | 'down';

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
                    this.uveStore.setEditorState(EDITOR_STATE.DRAGGING);

                    return;
                }

                this.uveStore.updateEditorScrollDragState();

                this.contentWindow?.postMessage(
                    { name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME, direction },
                    this.host
                );
            });

        fromEvent(this.window, 'dragleave')
            .pipe(
                takeUntil(this.destroy$),
                filter((event: DragEvent) => !event.relatedTarget) // Just reset when is out of the window
            )
            .subscribe(() => {
                this.uveStore.resetEditorProperties();
            });

        fromEvent(this.window, 'drop')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: DragEvent) => {
                event.preventDefault();
                const target = event.target as HTMLDivElement;
                const { position, payload, dropzone } = target.dataset;

                // If we drop in a container that is not a dropzone, we just reset the editor state
                if (dropzone !== 'true') {
                    this.uveStore.resetEditorProperties();

                    return;
                }

                const data: ClientData = JSON.parse(payload);
                const file = event.dataTransfer?.files[0]; // We are sure that is comes but in the tests we don't have DragEvent class
                const dragItem = this.uveStore.dragItem();

                // If we have a file, we need to upload it
                if (file) {
                    // I need to publish the temp file to use it.
                    this.handleFileUpload({
                        file,
                        data,
                        position,
                        dragItem
                    });

                    return;
                }

                // If we have a dragItem, we need to place it
                if (!isEqual(dragItem, TEMPORAL_DRAG_ITEM)) {
                    const positionPayload = <PositionPayload>{
                        position,
                        ...data
                    };

                    this.placeItem(positionPayload, dragItem);

                    return;
                }

                this.uveStore.resetEditorProperties();
            });
    }

    /**
     * Handle the iframe page load
     *
     * @param {string} clientHost
     * @memberof EditEmaEditorComponent
     */
    onIframePageLoad() {
        if (!this.uveStore.isTraditionalPage()) {
            return;
        }

        this.#insertPageContent();
        this.#setSeoData();

        if (this.uveStore.state() === EDITOR_STATE.INLINE_EDITING) {
            this.inlineEditingService.initEditor();
        }

        this.uveStore.setIsClientReady(true);
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
        if (this.uveStore.isTraditionalPage()) {
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
                this.uveStore.savePage(pageContainers);
            }
        });
    }

    /**
     *
     * Sets the content of the iframe with the provided code.
     * @param code - The code to be added to the iframe.
     * @memberof EditEmaEditorComponent
     */
    #insertPageContent(): void {
        const iframeElement = this.iframe?.nativeElement;
        const doc = iframeElement.contentDocument;

        const enableInlineEdit = this.uveStore.$enableInlineEdit();
        const pageRender = this.uveStore.$pageRender();

        const newDoc = this.inyectCodeToVTL(pageRender);

        if (!doc) {
            return;
        }

        doc.open();
        doc.write(newDoc);
        doc.close();

        this.handleInlineScripts(enableInlineEdit);
    }

    /**
     * Handle the Injection and removal of the inline editing scripts
     *
     * @param {boolean} enableInlineEdit
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    handleInlineScripts(enableInlineEdit: boolean) {
        const win = this.contentWindow;

        fromEvent(win, 'click').subscribe((e: MouseEvent) => {
            this.handleInternalNav(e);
            this.handleInlineEditing(e); // If inline editing is not active this will do nothing
        });

        if (enableInlineEdit) {
            this.inlineEditingService.injectInlineEdit(this.iframe);

            return;
        }

        this.inlineEditingService.removeInlineEdit(this.iframe);
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
                    this.contentWindow?.postMessage(
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
                this.contentWindow?.postMessage(
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
                    this.uveStore.pageAPIResponse().urlContentMap
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
    private handlePostMessage({ action, payload }: PostMessage): void {
        const CLIENT_ACTIONS_FUNC_MAP = {
            [DotCMSUVEAction.NAVIGATION_UPDATE]: (payload: SetUrlPayload) => {
                // When we set the url, we trigger in the shell component a load to get the new state of the page
                // This triggers a rerender that makes nextjs to send the set_url again
                // But this time the params are the same so the shell component wont trigger a load and there we know that the page is loaded
                const isSameUrl = compareUrlPaths(this.uveStore.pageParams()?.url, payload.url);

                if (isSameUrl) {
                    this.uveStore.setEditorState(EDITOR_STATE.IDLE);
                } else {
                    this.uveStore.loadPageAsset({
                        url: payload.url,
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });
                }
            },
            [DotCMSUVEAction.SET_BOUNDS]: (payload: Container[]) => {
                this.uveStore.setEditorBounds(payload);
            },
            [DotCMSUVEAction.SET_CONTENTLET]: (contentletArea: ClientContentletArea) => {
                const payload = this.uveStore.getPageSavePayload(contentletArea.payload);

                this.uveStore.setEditorContentletArea({
                    ...contentletArea,
                    payload
                });
            },
            [DotCMSUVEAction.IFRAME_SCROLL]: () => {
                this.uveStore.updateEditorScrollState();
            },
            [DotCMSUVEAction.IFRAME_SCROLL_END]: () => {
                this.uveStore.updateEditorOnScrollEnd();
            },
            [DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING]: (payload: {
                dataset: InlineEditingContentletDataset;
            }) => {
                // The iframe say the contentlet that the content is queue to be inline edited is in multiple pages
                // So the editor should open the dialog to ask if the edit is in ALL contentlets or only in this page.

                if (this.uveStore.state() === EDITOR_STATE.INLINE_EDITING) {
                    return;
                }

                const { contentlet, container } = this.uveStore.contentletArea().payload;

                const currentTreeNode = this.uveStore.getCurrentTreeNode(container, contentlet);

                this.dotCopyContentModalService
                    .open()
                    .pipe(
                        switchMap(({ shouldCopy }) => {
                            if (!shouldCopy) {
                                return of(null);
                            }

                            return this.handleCopyContent(currentTreeNode);
                        }),
                        tap((res) => {
                            this.uveStore.setEditorState(EDITOR_STATE.INLINE_EDITING);

                            if (res) {
                                this.uveStore.reloadCurrentPage();
                            }
                        })
                    )
                    .subscribe((res: DotCMSContentlet | null) => {
                        const data = {
                            oldInode: payload.dataset.inode,
                            inode: res?.inode || payload.dataset.inode,
                            fieldName: payload.dataset.fieldName,
                            mode: payload.dataset.mode,
                            language: payload.dataset.language
                        };

                        if (!this.uveStore.isTraditionalPage()) {
                            const message = {
                                name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
                                payload: data
                            };

                            this.contentWindow?.postMessage(message, this.host);

                            return;
                        }

                        this.inlineEditingService.setTargetInlineMCEDataset(data);

                        if (!res) {
                            this.inlineEditingService.initEditor();
                        }
                    });
            },
            [DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING]: (payload: UpdatedContentlet) => {
                this.uveStore.setEditorState(EDITOR_STATE.IDLE);

                // If there is no payload, we don't need to do anything
                if (!payload) {
                    return;
                }

                const dataset = payload.dataset;

                const contentlet = {
                    inode: dataset['inode'],
                    [dataset.fieldName]: payload.content
                };

                this.dotPageApiService
                    .saveContentlet({ contentlet })
                    .pipe(
                        take(1),
                        tap(() => {
                            this.uveStore.setUveStatus(UVE_STATUS.LOADING);
                        }),
                        tapResponse(
                            () => {
                                this.messageService.add({
                                    severity: 'success',
                                    summary: this.dotMessageService.get('message.content.saved'),
                                    detail: this.dotMessageService.get(
                                        'message.content.note.already.published'
                                    ),
                                    life: 2000
                                });
                            },
                            (e) => {
                                console.error(e);
                                this.messageService.add({
                                    severity: 'error',
                                    summary: this.dotMessageService.get(
                                        'editpage.content.update.contentlet.error'
                                    ),
                                    life: 2000
                                });
                            }
                        )
                    )
                    .subscribe(() => this.uveStore.reloadCurrentPage());
            },
            [DotCMSUVEAction.CLIENT_READY]: (devConfig) => {
                const isClientReady = this.uveStore.isClientReady();

                if (isClientReady) {
                    return;
                }

                const { graphql, params, query: rawQuery } = devConfig || {};
                const { query = rawQuery, variables } = graphql || {};
                const legacyGraphqlResponse = !!rawQuery;

                if (query || rawQuery) {
                    this.uveStore.setCustomGraphQL({ query, variables }, legacyGraphqlResponse);
                }

                const pageParams = convertClientParamsToPageParams(params);
                this.uveStore.reloadCurrentPage(pageParams);
                this.uveStore.setIsClientReady(true);
            },
            [DotCMSUVEAction.EDIT_CONTENTLET]: (contentlet: DotCMSContentlet) => {
                this.dialog.editContentlet({ ...contentlet, clientAction: action });
            },
            [DotCMSUVEAction.REORDER_MENU]: ({ startLevel, depth }: ReorderMenuPayload) => {
                const urlObject = createReorderMenuURL({
                    startLevel,
                    depth,
                    pagePath: this.uveStore.pageParams().url,
                    hostId: this.uveStore.pageAPIResponse().site.identifier
                });

                this.dialog.openDialogOnUrl(
                    urlObject,
                    this.dotMessageService.get('editpage.content.contentlet.menu.reorder.title')
                );
            },
            [DotCMSUVEAction.INIT_INLINE_EDITING]: (payload) =>
                this.#handleInlineEditingEvent(payload),
            [DotCMSUVEAction.NOOP]: () => {
                /* Do Nothing because is not the origin we are expecting */
            }
        };
        const actionToExecute = CLIENT_ACTIONS_FUNC_MAP[action];
        actionToExecute?.(payload);
    }

    /**
     * Notify the user to reload the iframe
     *
     * @private
     * @memberof DotEmaComponent
     */
    reloadIframeContent() {
        this.iframe?.nativeElement?.contentWindow?.postMessage(
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

    #setSeoData() {
        const iframeElement = this.iframe?.nativeElement;
        const doc = iframeElement.contentDocument;
        this.dotSeoMetaTagsService.getMetaTagsResults(doc).subscribe((results) => {
            const ogTags = this.dotSeoMetaTagsUtilService.getMetaTags(doc);
            this.uveStore.setOgTags(ogTags);
            this.uveStore.setOGTagResults(results);
        });
    }

    #clientPayload() {
        const graphqlResponse = this.uveStore.$customGraphqlResponse();

        if (graphqlResponse) {
            return graphqlResponse;
        }

        return {
            ...this.uveStore.pageAPIResponse(),
            params: this.uveStore.pageParams()
        };
    }
}
