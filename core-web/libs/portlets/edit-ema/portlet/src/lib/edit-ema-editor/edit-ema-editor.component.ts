import { Observable, Subject, fromEvent, of } from 'rxjs';

import { ClipboardModule } from '@angular/cdk/clipboard';
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
    DotPersonalizeService,
    DotMessageService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotContentletService
} from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotDevice,
    DotPersona,
    DotTreeNode,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import {
    DotDeviceSelectorSeoComponent,
    DotResultsSeoToolComponent
} from '@dotcms/portlets/dot-ema/ui';
import {
    SafeUrlPipe,
    DotSpinnerModule,
    DotMessagePipe,
    DotCopyContentModalService
} from '@dotcms/ui';

import { DotEditEmaWorkflowActionsComponent } from './components/dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaDeviceDisplayComponent } from './components/dot-ema-device-display/dot-ema-device-display.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { EditEmaToolbarComponent } from './components/edit-ema-toolbar/edit-ema-toolbar.component';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import {
    ContentletArea,
    EmaDragItem,
    ClientContentletArea,
    Container
} from './components/ema-page-dropzone/types';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DotPageApiParams } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, WINDOW } from '../shared/consts';
import { EDITOR_MODE, EDITOR_STATE, NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import {
    ActionPayload,
    PositionPayload,
    ClientData,
    SetUrlPayload,
    ContainerPayload,
    ContentletPayload,
    PageContainer,
    VTLFile
} from '../shared/models';
import {
    areContainersEquals,
    deleteContentletFromContainer,
    insertContentletInContainer
} from '../utils';

interface DeletePayload {
    payload: ActionPayload;
    originContainer: ContainerPayload;
    contentletToMove: ContentletPayload;
}

interface InsertPayloadFromDelete {
    payload: ActionPayload;
    pageContainers: PageContainer[];
    contentletsId: string[];
    destinationContainer: ContainerPayload;
    pivotContentlet: ContentletPayload;
    positionToInsert: 'before' | 'after';
}

interface BasePayload {
    type: 'contentlet' | 'content-type';
}

interface ContentletDragPayload extends BasePayload {
    type: 'contentlet';
    item: {
        container?: ContainerPayload;
        contentlet: ContentletPayload;
    };
    move: boolean;
}

// Specific interface when type is 'content-type'
interface ContentTypeDragPayload extends BasePayload {
    type: 'content-type';
    item: {
        variable: string;
        name: string;
    };
}

type DraggedPalettePayload = ContentletDragPayload | ContentTypeDragPayload;

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
        EditEmaPersonaSelectorComponent,
        EditEmaLanguageSelectorComponent,
        EditEmaToolbarComponent,
        ClipboardModule,
        DotMessagePipe,
        EmaPageDropzoneComponent,
        EditEmaPaletteComponent,
        EmaContentletToolsComponent,
        DotDeviceSelectorSeoComponent,
        DotEmaDeviceDisplayComponent,
        DotEmaBookmarksComponent,
        DotEditEmaWorkflowActionsComponent,
        ProgressBarModule,
        DotResultsSeoToolComponent,
        DotEmaRunningExperimentComponent
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
    @ViewChild('personaSelector')
    personaSelector!: EditEmaPersonaSelectorComponent;

    private readonly router = inject(Router);
    private readonly activatedRouter = inject(ActivatedRoute);
    private readonly store = inject(EditEmaStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly personalizeService = inject(DotPersonalizeService);
    private readonly messageService = inject(MessageService);
    private readonly window = inject(WINDOW);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly dotCopyContentModalService = inject(DotCopyContentModalService);
    private readonly dotCopyContentService = inject(DotCopyContentService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotSeoMetaTagsService = inject(DotSeoMetaTagsService);
    private readonly dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);
    private readonly dotContentletService = inject(DotContentletService);

    readonly editorState$ = this.store.editorState$.pipe(
        tap((state) => {
            // I can edit the variant if the variant is the default one (default can be undefined as well) or if there is no running experiment
            this.canEditVariant.set(
                !this.queryParams.variantName ||
                    this.queryParams.variantName === DEFAULT_VARIANT_ID ||
                    !state.runningExperiment
            );
        })
    );
    readonly destroy$ = new Subject<boolean>();
    protected ogTagsResults$: Observable<SeoMetaTagsResult[]>;

    readonly pageData = toSignal(this.store.pageData$);

    readonly ogTags: WritableSignal<SeoMetaTags> = signal(undefined);

    readonly canEditVariant: WritableSignal<boolean> = signal(true);

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

    protected draggedPayload: DraggedPalettePayload;

    containers: Container[] = [];
    contentlet!: ContentletArea;
    dragItem: EmaDragItem;

    get queryParams(): DotPageApiParams {
        return this.activatedRouter.snapshot.queryParams as DotPageApiParams;
    }

    isVTLPage = toSignal(this.store.clientHost$.pipe(map((clientHost) => !clientHost)));

    ngOnInit(): void {
        this.handleReloadContent();

        fromEvent(this.window, 'message')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: MessageEvent) => {
                this.handlePostMessage(event)?.();
            });
        // Think is not necessary, if is Headless, it init as loading. If is VTL, init as Loaded
        // So here is re-set to loading in Headless and prevent VTL to hide the progressbar
        // this.store.updateEditorState(EDITOR_STATE.LOADING);
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
    onIframePageLoad() {
        this.store.updateEditorState(EDITOR_STATE.IDLE);
    }

    /**
     * Add the editor page script to VTL pages
     *
     * @param {string} rendered
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    addEditorPageScript(rendered: string) {
        const scriptString = `<script src="/html/js/editor-js/sdk-editor.esm.js"></script>`;
        const updatedRendered = rendered?.replace('</body>', scriptString + '</body>');

        return updatedRendered;
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
     * Handle the persona selection
     *
     * @param {DotPersona} persona
     * @memberof DotEmaComponent
     */
    onPersonaSelected(persona: DotPersona & { pageId: string }) {
        if (persona.identifier === DEFAULT_PERSONA.identifier || persona.personalized) {
            this.updateQueryParams({
                'com.dotmarketing.persona.id': persona.identifier
            });
        } else {
            this.confirmationService.confirm({
                header: this.dotMessageService.get('editpage.personalization.confirm.header'),
                message: this.dotMessageService.get(
                    'editpage.personalization.confirm.message',
                    persona.name
                ),
                acceptLabel: this.dotMessageService.get('dot.common.dialog.accept'),
                rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
                accept: () => {
                    this.personalizeService
                        .personalized(persona.pageId, persona.keyTag)
                        .subscribe(() => {
                            this.updateQueryParams({
                                'com.dotmarketing.persona.id': persona.identifier
                            });

                            this.personaSelector.fetchPersonas();
                        }); // This does a take 1 under the hood
                },
                reject: () => {
                    this.personaSelector.resetValue();
                }
            });
        }
    }

    /**
     * Handle the persona despersonalization
     *
     * @param {(DotPersona & { pageId: string })} persona
     * @memberof EditEmaEditorComponent
     */
    onDespersonalize(persona: DotPersona & { pageId: string; selected: boolean }) {
        this.confirmationService.confirm({
            header: this.dotMessageService.get('editpage.personalization.delete.confirm.header'),
            message: this.dotMessageService.get(
                'editpage.personalization.delete.confirm.message',
                persona.name
            ),
            acceptLabel: this.dotMessageService.get('dot.common.dialog.accept'),
            rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
            accept: () => {
                this.personalizeService
                    .despersonalized(persona.pageId, persona.keyTag)
                    .subscribe(() => {
                        this.personaSelector.fetchPersonas();

                        if (persona.selected) {
                            this.updateQueryParams({
                                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                            });
                        }
                    }); // This does a take 1 under the hood
            }
        });
    }

    /**
     * Update the current device
     *
     * @param {DotDevice} [device]
     * @memberof EditEmaEditorComponent
     */
    updateCurrentDevice(device: DotDevice & { icon?: string }) {
        this.store.updatePreviewState({
            editorMode: EDITOR_MODE.PREVIEW,
            device
        });
    }

    goToEditMode() {
        this.store.updatePreviewState({
            editorMode: EDITOR_MODE.EDIT
        });
    }

    /**
     * Handle the copy URL action
     *
     * @memberof EditEmaEditorComponent
     */
    triggerCopyToast() {
        this.messageService.add({
            severity: 'success',
            summary: this.dotMessageService.get('Copied'),
            life: 3000
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
                doc.open();
                doc.write(this.addEditorPageScript(code));
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
            payload: ActionPayload | SetUrlPayload | Container[] | ClientContentletArea;
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
                    this.personaSelector.fetchPersonas(); // We need to fetch the personas again because the page is loaded
                    this.store.updateEditorState(EDITOR_STATE.IDLE);
                } else {
                    this.updateQueryParams({
                        url: payload.url,
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    });
                }
            },
            [CUSTOMER_ACTIONS.SET_BOUNDS]: () => {
                this.containers = <Container[]>data.payload;
                this.cd.detectChanges();
            },
            [CUSTOMER_ACTIONS.SET_CONTENTLET]: () => {
                const contentletArea = <ClientContentletArea>data.payload;

                const payload = this.getPageSavePayload(contentletArea.payload);

                this.contentlet = {
                    ...contentletArea,
                    payload
                };

                this.cd.detectChanges();
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
     * Handle a new page event. This event is triggered when the page changes for a Workflow Action
     * Update the query params if the url or the language id changed
     *
     * @param {DotCMSContentlet} page
     * @memberof EditEmaEditorComponent
     */
    handleNewPage(page: DotCMSContentlet): void {
        const { pageURI, url, languageId } = page;
        const params = {
            ...this.updateQueryParams,
            url: pageURI ?? url,
            language_id: languageId?.toString()
        };

        if (this.shouldReload(params)) {
            this.updateQueryParams(params);
        }
    }

    onSeoMediaChange(seoMedia: string) {
        this.store.updatePreviewState({
            editorMode: EDITOR_MODE.PREVIEW,
            socialMedia: seoMedia
        });
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
     * Check if the url or the language id changed
     *
     * @private
     * @param {Params} params
     * @return {*}  {boolean}
     * @memberof EditEmaEditorComponent
     */
    private shouldReload(params: Params): boolean {
        const { url: newUrl, language_id: newLanguageId } = params;
        const { url, language_id } = this.queryParams;

        return newUrl != url || newLanguageId != language_id;
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

        if (!(Number(onNumberOfPages) > 1)) {
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
        this.contentlet = null;
        this.containers = [];
        this.dragItem = null;
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
