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
    ViewChild,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Params, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { takeUntil } from 'rxjs/operators';

import { CUSTOMER_ACTIONS } from '@dotcms/client';
import { DotPersonalizeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotDevice, DotPersona } from '@dotcms/dotcms-models';
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { SafeUrlPipe, DotSpinnerModule, DotMessagePipe } from '@dotcms/ui';

import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaDeviceDisplayComponent } from './components/dot-ema-device-display/dot-ema-device-display.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { EditEmaToolbarComponent } from './components/edit-ema-toolbar/edit-ema-toolbar.component';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaFormSelectorComponent } from './components/ema-form-selector/ema-form-selector.component';
import {
    ContentletArea,
    EmaPageDropzoneComponent,
    Row
} from './components/ema-page-dropzone/ema-page-dropzone.component';

import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DEFAULT_PERSONA, WINDOW } from '../shared/consts';
import { NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import { ActionPayload, SetUrlPayload } from '../shared/models';
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
        DialogModule,
        DotSpinnerModule,
        ConfirmDialogModule,
        EditEmaPersonaSelectorComponent,
        EditEmaLanguageSelectorComponent,
        EditEmaToolbarComponent,
        ClipboardModule,
        DotMessagePipe,
        EmaPageDropzoneComponent,
        EditEmaPaletteComponent,
        EmaContentletToolsComponent,
        EmaFormSelectorComponent,
        DotDeviceSelectorSeoComponent,
        DotEmaDeviceDisplayComponent,
        DotEmaBookmarksComponent
    ]
})
export class EditEmaEditorComponent implements OnInit, OnDestroy {
    @ViewChild('dialogIframe') dialogIframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('personaSelector')
    personaSelector!: EditEmaPersonaSelectorComponent;

    private readonly router = inject(Router);
    private readonly store = inject(EditEmaStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly personalizeService = inject(DotPersonalizeService);
    private readonly messageService = inject(MessageService);
    private readonly window = inject(WINDOW);
    private readonly cd = inject(ChangeDetectorRef);

    readonly dialogState$ = this.store.dialogState$;
    readonly editorState$ = this.store.editorState$;
    readonly destroy$ = new Subject<boolean>();

    readonly host = '*';

    private savePayload: ActionPayload;
    private draggedPayload: DraggedPalettePayload;

    rows: Row[] = [];
    contentlet!: ContentletArea;
    dragItemType: string;

    // This should be in the store, but experienced an issue that triggers a reload in the whole store when the device is updated
    currentDevice: DotDevice & { icon?: string };

    ngOnInit(): void {
        fromEvent(this.window, 'message')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: MessageEvent) => {
                this.handlePostMessage(event)?.();
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle the dialog iframe load event
     *
     * @param {CustomEvent} event
     * @memberof DotEmaComponent
     */
    onIframeLoad() {
        this.store.setDialogIframeLoading(false);

        // This event is destroyed when you close the dialog
        fromEvent(
            // The events are getting sended to the document
            this.dialogIframe.nativeElement.contentWindow.document,
            'ng-event'
        )
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: CustomEvent) => {
                this.handleNgEvent(event)?.();
            });
    }

    /**
     * Handle the dialog close event
     *
     * @memberof DotEmaComponent
     */
    resetDialogIframeData() {
        this.store.resetDialog();
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
        this.dragItemType = item?.contentType;

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
    }

    /**
     * When the user drop a palette item in the dropzone
     *
     * @param {ActionPayload} event
     * @return {*}  {void}
     * @memberof EditEmaEditorComponent
     */
    onPlaceItem(event: ActionPayload): void {
        if (this.draggedPayload.type === 'contentlet') {
            const pageContainers = insertContentletInContainer({
                ...event,
                newContentletId: this.draggedPayload.item.identifier
            });

            this.store.savePage({
                pageContainers,
                pageId: event.pageId,
                whenSaved: () => {
                    this.reloadIframe();
                    this.draggedPayload = undefined;
                }
            });

            return;
        }

        this.savePayload = event;

        this.store.createContentFromPalette(this.draggedPayload.item);
    }

    /**
     * Add contentlet
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    addContentlet(payload: ActionPayload): void {
        this.store.initActionAdd({
            containerId: payload.container.identifier,
            acceptTypes: payload.container.acceptTypes ?? '*',
            language_id: payload.language_id
        });
        this.savePayload = payload;
    }

    /**
     * Add Form
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    addForm(payload: ActionPayload): void {
        this.store.initActionAddForm();
        this.savePayload = payload;
    }

    /**
     * Add selected form
     *
     * @param {string} identifier
     * @memberof EditEmaEditorComponent
     */
    addSelectedForm(identifier: string): void {
        this.store.saveFormToPage({
            payload: this.savePayload,
            formId: identifier,
            whenSaved: () => {
                this.resetDialogIframeData();
                this.reloadIframe();
                this.savePayload = undefined;
            }
        });
    }

    /**
     * Add Widget
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    addWidget(payload: ActionPayload): void {
        this.store.initActionAdd({
            containerId: payload.container.identifier,
            acceptTypes: DotCMSBaseTypesContentTypes.WIDGET,
            language_id: payload.language_id
        });
        this.savePayload = payload;
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
                    whenSaved: () => {
                        this.resetDialogIframeData();
                        this.reloadIframe();
                    }
                }); // Save when selected
            }
        });
    }

    /**
     * Edit contentlet
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    editContentlet(payload: ActionPayload) {
        this.store.initActionEdit({
            inode: payload.contentlet.inode,
            title: payload.contentlet.title,
            type: 'content'
        });
    }

    /**
     * Handle the custom events from the iframe
     *
     * @private
     * @param {Event} event
     * @memberof DotEmaComponent
     */
    private handleNgEvent(event: CustomEvent) {
        const { detail } = event;

        return (<Record<NG_CUSTOM_EVENTS, () => void>>{
            [NG_CUSTOM_EVENTS.EDIT_CONTENTLET_LOADED]: () => {
                /* */
            },
            [NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT]: () => {
                const pageContainers = insertContentletInContainer({
                    ...this.savePayload,
                    newContentletId: detail.data.identifier
                });

                // Save when selected
                this.store.savePage({
                    pageContainers,
                    pageId: this.savePayload.pageId,
                    whenSaved: () => {
                        this.resetDialogIframeData();
                        this.reloadIframe();
                        this.savePayload = undefined;
                        this.cd.detectChanges();
                    }
                });
            },
            [NG_CUSTOM_EVENTS.SAVE_PAGE]: () => {
                if (this.savePayload) {
                    const pageContainers = insertContentletInContainer({
                        ...this.savePayload,
                        newContentletId: detail.payload.contentletIdentifier
                    });

                    // Save when created
                    this.store.savePage({
                        pageContainers,
                        pageId: this.savePayload.pageId,
                        whenSaved: () => {
                            this.resetDialogIframeData();
                            this.reloadIframe();
                            this.savePayload = undefined;
                        }
                    });
                } else {
                    this.reloadIframe(); // We still need to reload the iframe because the contentlet is not in the container yet
                }
            },
            [NG_CUSTOM_EVENTS.CREATE_CONTENTLET]: () => {
                this.store.initActionCreate({
                    contentType: detail.data.contentType,
                    url: detail.data.url
                });
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
            payload: ActionPayload | SetUrlPayload | Row[] | ContentletArea;
        };
    }): () => void {
        return (<Record<CUSTOMER_ACTIONS, () => void>>{
            [CUSTOMER_ACTIONS.SET_URL]: () => {
                const payload = <SetUrlPayload>data.payload;

                this.updateQueryParams({
                    url: payload.url
                });
            },
            [CUSTOMER_ACTIONS.SET_BOUNDS]: () => {
                this.rows = <Row[]>data.payload;
                this.cd.detectChanges();
            },
            [CUSTOMER_ACTIONS.SET_CONTENTLET]: () => {
                this.contentlet = <ContentletArea>data.payload;
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
     * Update the query params
     *
     * @private
     * @param {Params} params
     * @memberof EditEmaEditorComponent
     */
    private updateQueryParams(params: Params) {
        this.router.navigate([], {
            // replaceUrl: true,
            // skipLocationChange: false,
            queryParams: params,
            queryParamsHandling: 'merge'
        });
    }
}
