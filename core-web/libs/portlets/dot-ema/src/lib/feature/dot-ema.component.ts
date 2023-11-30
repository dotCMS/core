import { Subject, fromEvent } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Inject,
    OnDestroy,
    OnInit,
    ViewChild,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { takeUntil } from 'rxjs/operators';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotSpinnerModule, SafeUrlPipe } from '@dotcms/ui';

import { EditEmaStore } from './store/dot-ema.store';

import { EmaLanguageSelectorComponent } from '../components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaToolbarComponent } from '../components/edit-ema-toolbar/edit-ema-toolbar.component';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_LANGUAGE_ID, DEFAULT_URL, WINDOW } from '../shared/consts';
import { CUSTOMER_ACTIONS, NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import { AddContentletPayload, DeleteContentletPayload, SetUrlPayload } from '../shared/models';
import { deleteContentletFromContainer, insertContentletInContainer } from '../utils';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        SafeUrlPipe,
        DialogModule,
        DotSpinnerModule,
        ConfirmDialogModule,
        EditEmaToolbarComponent,
        EmaLanguageSelectorComponent
    ],
    providers: [
        EditEmaStore,
        DotPageApiService,
        ConfirmationService,
        DotLanguagesService,
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaComponent implements OnInit, OnDestroy {
    @ViewChild('dialogIframe') dialogIframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    readonly destroy$ = new Subject<boolean>();

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly store = inject(EditEmaStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);

    private savePayload: AddContentletPayload;

    readonly host = 'http://localhost:3000';
    readonly vm$ = this.store.vm$;

    constructor(@Inject(WINDOW) private window: Window) {}

    ngOnInit(): void {
        this.route.queryParams.subscribe(({ language_id, url }: Params) => {
            const queryParams = {};

            if (!language_id) {
                queryParams['language_id'] = DEFAULT_LANGUAGE_ID;
            }

            if (!url) {
                queryParams['url'] = DEFAULT_URL;
            }

            if (Object.keys(queryParams).length > 0) {
                this.router.navigate([], {
                    queryParams,
                    queryParamsHandling: 'merge'
                });
            }

            this.store.load({
                language_id: language_id || DEFAULT_LANGUAGE_ID,
                url: url || DEFAULT_URL
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
                    pageContainers: this.savePayload.pageContainers,
                    container: this.savePayload.container,
                    contentletID: detail.data.identifier
                });

                this.store.savePage({
                    pageContainers,
                    pageID: this.savePayload.pageID,
                    whenSaved: () => {
                        this.resetDialogIframeData();
                        this.reloadIframe();
                        this.savePayload = undefined;
                    }
                }); // Save when selected
            },
            [NG_CUSTOM_EVENTS.CONTENTLET_UPDATED]: () => {
                this.reloadIframe();
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
        data: { action: CUSTOMER_ACTIONS; payload: DotCMSContentlet | AddContentletPayload };
    }): () => void {
        const action = origin !== this.host ? CUSTOMER_ACTIONS.NOOP : data.action;

        return (<Record<CUSTOMER_ACTIONS, () => void>>{
            [CUSTOMER_ACTIONS.EDIT_CONTENTLET]: () => {
                const payload = <DotCMSContentlet>data.payload;

                this.store.initActionEdit({
                    inode: payload.inode,
                    title: payload.title
                });
            },
            [CUSTOMER_ACTIONS.ADD_CONTENTLET]: () => {
                const payload = <AddContentletPayload>data.payload;

                this.store.initActionAdd({
                    containerID: payload.container.identifier,
                    acceptTypes: payload.container.acceptTypes ?? '*'
                });

                this.savePayload = payload;
            },
            [CUSTOMER_ACTIONS.DELETE_CONTENTLET]: () => {
                const { pageContainers, container, contentletId, pageID } = <
                    DeleteContentletPayload
                >data.payload;

                const newPageContainers = deleteContentletFromContainer({
                    pageContainers: pageContainers,
                    container: container,
                    contentletID: contentletId
                });

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
                            pageID,
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
            [CUSTOMER_ACTIONS.NOOP]: () => {
                /* Do Nothing because is not the origin we are expecting */
            }
        })[action];
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
