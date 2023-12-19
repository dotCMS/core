import { Subject, fromEvent } from 'rxjs';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Inject,
    OnDestroy,
    OnInit,
    ViewChild,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, Params } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';

import { takeUntil } from 'rxjs/operators';

import { DotPersonalizeService, DotMessageService } from '@dotcms/data-access';
import { DotPersona } from '@dotcms/dotcms-models';
import { SafeUrlPipe, DotSpinnerModule, DotMessagePipe } from '@dotcms/ui';

import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { EditEmaToolbarComponent } from './components/edit-ema-toolbar/edit-ema-toolbar.component';
import {
    EmaPageDropzoneComponent,
    Row
} from './components/ema-page-dropzone/ema-page-dropzone.component';

import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DEFAULT_LANGUAGE_ID, DEFAULT_PERSONA, DEFAULT_URL, HOST, WINDOW } from '../shared/consts';
import { CUSTOMER_ACTIONS, NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import { ActionPayload, SetUrlPayload } from '../shared/models';
import { deleteContentletFromContainer, insertContentletInContainer } from '../utils';

interface BasePayload {
    type: 'contentlet' | 'content-type';
}

interface ContentletPayload extends BasePayload {
    type: 'contentlet';
    item: {
        inode: string;
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
        ToastModule,
        DotMessagePipe,
        EmaPageDropzoneComponent
    ],
    templateUrl: './edit-ema-editor.component.html',
    styleUrls: ['./edit-ema-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaEditorComponent implements OnInit, OnDestroy {
    @ViewChild('dialogIframe') dialogIframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('personaSelector') personaSelector!: EditEmaPersonaSelectorComponent;

    readonly destroy$ = new Subject<boolean>();

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly store = inject(EditEmaStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly personalizeService = inject(DotPersonalizeService);
    private readonly messageService = inject(MessageService);

    readonly dialogState$ = this.store.dialogState$;
    readonly editorState$ = this.store.editorState$;

    readonly host = HOST;

    private savePayload: ActionPayload;
    private draggedPayload: DraggedPalettePayload;

    rows: Row[] = [];

    constructor(@Inject(WINDOW) private window: Window, private cd: ChangeDetectorRef) {}

    ngOnInit(): void {
        this.route.queryParams.subscribe((queryParams: Params) => {
            this.store.load({
                language_id: queryParams['language_id'] ?? DEFAULT_LANGUAGE_ID,
                url: queryParams['url'] ?? DEFAULT_URL,
                persona_id: queryParams['com.dotmarketing.persona.id'] ?? DEFAULT_PERSONA.identifier
            });
        });

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
     * Handle the iframe load event
     *
     * @param {CustomEvent} event
     * @memberof DotEmaComponent
     */
    onIframeLoad(_: Event) {
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
        this.router.navigate([], {
            queryParams: {
                language_id
            },
            queryParamsHandling: 'merge'
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
            this.router.navigate([], {
                queryParams: {
                    'com.dotmarketing.persona.id': persona.identifier
                },
                queryParamsHandling: 'merge'
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
                            this.router.navigate([], {
                                queryParams: {
                                    'com.dotmarketing.persona.id': persona.identifier
                                },
                                queryParamsHandling: 'merge'
                            });
                        }); // This does a take 1 under the hood
                },
                reject: () => {
                    this.personaSelector.resetValue();
                }
            });
        }
    }

    triggerCopyToast() {
        this.messageService.add({
            severity: 'success',
            summary: this.dotMessageService.get('Copied'),
            life: 3000
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
                    contentlet: detail.data
                });

                this.store.savePage({
                    pageContainers,
                    pageId: this.savePayload.pageId,
                    whenSaved: () => {
                        this.resetDialogIframeData();
                        this.reloadIframe();
                        this.savePayload = undefined;
                    }
                }); // Save when selected
            },
            [NG_CUSTOM_EVENTS.SAVE_CONTENTLET]: () => {
                if (this.savePayload) {
                    const pageContainers = insertContentletInContainer({
                        ...this.savePayload,
                        newContentletId: detail.payload.contentletIdentifier
                    }); // This won't add anything if the contentlet is already on the container, so is safe to call it even when we just edited a contentlet

                    this.store.savePage({
                        pageContainers,
                        pageId: this.savePayload.pageId,
                        whenSaved: () => {
                            this.resetDialogIframeData();
                            this.reloadIframe();
                            this.savePayload = undefined;
                        }
                    }); // Save when created
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
        origin = this.host,
        data
    }: {
        origin: string;
        data: {
            action: CUSTOMER_ACTIONS;
            payload: ActionPayload | SetUrlPayload | Row[];
        };
    }): () => void {
        const action = origin !== this.host ? CUSTOMER_ACTIONS.NOOP : data.action;
        const payload = <ActionPayload>data.payload;

        return (<Record<CUSTOMER_ACTIONS, () => void>>{
            [CUSTOMER_ACTIONS.EDIT_CONTENTLET]: () => {
                this.store.initActionEdit({
                    inode: payload.contentlet.inode,
                    title: payload.contentlet.title
                });
            },
            [CUSTOMER_ACTIONS.ADD_CONTENTLET]: () => {
                this.store.initActionAdd({
                    containerId: payload.container.identifier,
                    acceptTypes: payload.container.acceptTypes ?? '*',
                    language_id: payload.language_id
                });

                this.savePayload = payload;
            },
            [CUSTOMER_ACTIONS.DELETE_CONTENTLET]: () => {
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
            },
            [CUSTOMER_ACTIONS.SET_URL]: () => {
                const payload = <SetUrlPayload>data.payload;

                this.router.navigate([], {
                    queryParams: {
                        url: payload.url
                    },
                    queryParamsHandling: 'merge'
                });
            },
            [CUSTOMER_ACTIONS.SET_BOUNDS]: () => {
                this.rows = <Row[]>data.payload;
                this.cd.detectChanges();
            },
            [CUSTOMER_ACTIONS.NOOP]: () => {
                /* Do Nothing because is not the origin we are expecting */
            }
        })[action];
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

        this.draggedPayload = {
            type: dataset.type,
            item: JSON.parse(dataset.item)
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
                newContentletId: this.draggedPayload.item.inode
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
     * Notify the user to reload the iframe
     *
     * @private
     * @memberof DotEmaComponent
     */
    private reloadIframe() {
        this.iframe.nativeElement.contentWindow?.postMessage(
            NOTIFY_CUSTOMER.EMA_RELOAD_PAGE,
            this.host
        );
    }
}
