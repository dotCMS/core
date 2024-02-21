import { Subject, fromEvent } from 'rxjs';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { CommonModule } from '@angular/common';
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
    signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressBarModule } from 'primeng/progressbar';

import { takeUntil } from 'rxjs/operators';

import { CUSTOMER_ACTIONS } from '@dotcms/client';
import { DotPersonalizeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotDevice, DotPersona } from '@dotcms/dotcms-models';
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { SafeUrlPipe, DotSpinnerModule, DotMessagePipe } from '@dotcms/ui';

import { DotEditEmaWorkflowActionsComponent } from './components/dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaDeviceDisplayComponent } from './components/dot-ema-device-display/dot-ema-device-display.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { EditEmaToolbarComponent } from './components/edit-ema-toolbar/edit-ema-toolbar.component';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import {
    Row,
    ContentletArea,
    EmaDragItem,
    ClientContentletArea
} from './components/ema-page-dropzone/types';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DotPageApiResponse, DotPageApiParams } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, WINDOW } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import { ActionPayload, PositionPayload, ClientData, SetUrlPayload } from '../shared/models';
import { deleteContentletFromContainer, insertContentletInContainer } from '../utils';

interface BasePayload {
    type: 'contentlet' | 'content-type';
}

interface ContentletPayload extends BasePayload {
    type: 'contentlet';
    item: {
        identifier: string;
    };
}

// Specific interface when type is 'content-type'
interface ContentTypePayload extends BasePayload {
    type: 'content-type';
    item: {
        variable: string;
        name: string;
    };
}

type DraggedPalettePayload = ContentletPayload | ContentTypePayload;

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
        ProgressBarModule
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

    readonly editorState$ = this.store.editorState$;
    readonly destroy$ = new Subject<boolean>();

    readonly pageData = toSignal(this.store.pageData$);

    readonly clientData: WritableSignal<ClientData> = signal(undefined);

    readonly actionPayload: Signal<ActionPayload> = computed(() => {
        const clientData = this.clientData();
        const { containers, languageId, id, personaTag } = this.pageData();
        const { contentletsId } = containers.find((container) => {
            return (
                container.identifier === clientData.container.identifier &&
                container.uuid === clientData.container.uuid
            );
        });

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

    readonly host = '*';
    readonly editorState = EDITOR_STATE;

    private draggedPayload: DraggedPalettePayload;

    rows: Row[] = [];
    contentlet!: ContentletArea;
    dragItem: EmaDragItem;

    // This should be in the store, but experienced an issue that triggers a reload in the whole store when the device is updated
    currentDevice: DotDevice & { icon?: string };

    get queryParams(): DotPageApiParams {
        return this.activatedRouter.snapshot.queryParams as DotPageApiParams;
    }

    ngOnInit(): void {
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
     * Handle the iframe page load
     *
     * @param {string} clientHost
     * @memberof EditEmaEditorComponent
     */
    onIframePageLoad({ clientHost, editor }: { clientHost: string; editor: DotPageApiResponse }) {
        if (!clientHost) {
            // Is VTL
            this.iframe.nativeElement.contentDocument.open();
            this.iframe.nativeElement.contentDocument.write(editor.page.rendered);
            this.iframe.nativeElement.contentDocument.close();
        }
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
        this.handleNgEvent({ event, payload: payload })?.();
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
    updateCurrentDevice(device?: DotDevice & { icon?: string }) {
        this.currentDevice = device;
        this.rows = []; // We need to reset the rows when we change the device
        this.contentlet = null; // We need to reset the contentlet when we change the device
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
     * Handle palette start drag event
     *
     * @param {DragEvent} event
     * @memberof EditEmaEditorComponent
     */
    onDragStart(event: DragEvent) {
        const dataset = (event.target as HTMLDivElement).dataset as unknown as Pick<
            ContentletPayload,
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
            item
        };

        this.iframe.nativeElement.contentWindow?.postMessage(
            NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS,
            this.host
        );
    }

    /**
     * Reset rows when user stop dragging
     *
     * @param {DragEvent} _event
     * @memberof EditEmaEditorComponent
     */
    onDragEnd(_event: DragEvent) {
        this.rows = [];
        this.dragItem = {
            baseType: '',
            contentType: ''
        };
    }

    /**
     * When the user drop a palette item in the dropzone
     *
     * @param {PositionPayload} positionPayload
     * @return {*}  {void}
     * @memberof EditEmaEditorComponent
     */
    onPlaceItem(positionPayload: PositionPayload): void {
        const payload = this.getPageSavePayload(positionPayload);

        if (this.draggedPayload.type === 'contentlet') {
            const { pageContainers, didInsert } = insertContentletInContainer({
                ...payload,
                newContentletId: this.draggedPayload.item.identifier
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
                    this.reloadIframe();
                    this.draggedPayload = undefined;
                }
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
        const newPageContainers = deleteContentletFromContainer(payload);

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
                    pageContainers: newPageContainers,
                    pageId: payload.pageId,
                    params: this.queryParams,
                    whenSaved: () => {
                        this.dialog.resetDialog();
                        this.reloadIframe();
                    }
                }); // Save when selected
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
                        this.reloadIframe();
                        this.cd.detectChanges();
                    }
                });
            },
            [NG_CUSTOM_EVENTS.SAVE_PAGE]: () => {
                if (payload) {
                    const { pageContainers, didInsert } = insertContentletInContainer({
                        ...payload,
                        newContentletId: detail.payload.contentletIdentifier
                    });

                    if (!didInsert) {
                        this.handleDuplicatedContentlet();

                        return;
                    }

                    // Save when created
                    this.store.savePage({
                        pageContainers,
                        pageId: payload.pageId,
                        params: this.queryParams,
                        whenSaved: () => {
                            this.dialog.resetDialog();
                            this.reloadIframe();
                        }
                    });
                } else {
                    this.reloadIframe(); // We still need to reload the iframe because the contentlet is not in the container yet
                }
            },
            [NG_CUSTOM_EVENTS.CREATE_CONTENTLET]: () => {
                this.dialog.createContentlet({
                    contentType: detail.data.contentType,
                    url: detail.data.url
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
                        this.reloadIframe();
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
            payload: ActionPayload | SetUrlPayload | Row[] | ClientContentletArea;
        };
    }): () => void {
        return (<Record<CUSTOMER_ACTIONS, () => void>>{
            [CUSTOMER_ACTIONS.CONTENT_CHANGE]: () => {
                // This event is sent when the mutation observer detects a change in the content

                this.store.updateEditorState(EDITOR_STATE.LOADED);
            },

            [CUSTOMER_ACTIONS.SET_URL]: () => {
                const payload = <SetUrlPayload>data.payload;

                // When we set the url, we trigger in the shell component a load to get the new state of the page
                // This triggers a rerender that makes nextjs to send the set_url again
                // But this time the params are the same so the shell component wont trigger a load and there we know that the page is loaded
                const isSameUrl = this.queryParams.url === payload.url;

                if (isSameUrl) {
                    this.store.updateEditorState(EDITOR_STATE.LOADED);
                    this.personaSelector.fetchPersonas(); // We need to fetch the personas again because the page is loaded
                } else {
                    this.store.updateEditorState(EDITOR_STATE.LOADING);
                }

                this.updateQueryParams({
                    url: payload.url,
                    ...(isSameUrl
                        ? {}
                        : { 'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier })
                });
            },
            [CUSTOMER_ACTIONS.SET_BOUNDS]: () => {
                this.rows = <Row[]>data.payload;
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
                this.contentlet = null;
                this.rows = [];
                this.cd.detectChanges();
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
        this.iframe.nativeElement.contentWindow?.postMessage(
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

    /**
     * Update the query params
     *
     * @private
     * @param {Params} params
     * @memberof EditEmaEditorComponent
     */
    private updateQueryParams(params: Params) {
        this.router.navigate([], {
            queryParams: params,
            queryParamsHandling: 'merge'
        });

        // Reset this on queryParams update
        this.rows = [];
        this.contentlet = null;
    }

    private handleDuplicatedContentlet() {
        this.messageService.add({
            severity: 'info',
            summary: this.dotMessageService.get('editpage.content.add.already.title'),
            detail: this.dotMessageService.get('editpage.content.add.already.message'),
            life: 2000
        });

        this.store.updateEditorState(EDITOR_STATE.LOADED);
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
}
