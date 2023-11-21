import { Subject, fromEvent } from 'rxjs';

import { AsyncPipe, NgFor, NgIf } from '@angular/common';
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

import { DialogModule } from 'primeng/dialog';

import { takeUntil } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotSpinnerModule, SafeUrlPipe } from '@dotcms/ui';

import { EditEmaStore } from './store/dot-ema.store';

import { DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { CUSTOMER_ACTIONS, NG_CUSTOM_EVENTS, NOTIFY_CUSTOMER } from '../shared/enums';
import { AddContentletPayload } from '../shared/models';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [NgFor, NgIf, AsyncPipe, FormsModule, SafeUrlPipe, DialogModule, DotSpinnerModule],
    providers: [
        EditEmaStore,
        DotPageApiService,
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

    languages = [
        {
            name: 'English',
            value: '1'
        },
        {
            name: 'Spanish',
            value: '2'
        }
    ];

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly store = inject(EditEmaStore);
    private readonly pageApi = inject(DotPageApiService);

    private savePayload: AddContentletPayload;

    readonly host = 'http://localhost:3000';
    readonly vm$ = this.store.vm$;

    constructor(@Inject(WINDOW) private window: Window) {}

    ngOnInit(): void {
        this.route.queryParams.subscribe(({ language_id, url }: Params) => {
            const queryParams = {};

            if (!language_id) {
                queryParams['language_id'] = '1';
            }

            if (!url) {
                queryParams['url'] = 'index';
            }

            if (Object.keys(queryParams).length > 0) {
                this.router.navigate([], {
                    queryParams,
                    queryParamsHandling: 'merge'
                });
            }

            this.store.load({
                language_id: language_id || '1',
                url: url || 'index'
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
     * Updates store value and navigates with updated query parameters on select element change event.
     *
     * @param {Event} e
     * @memberof DotEmaComponent
     */
    onChange(e: Event) {
        const name = (e.target as HTMLSelectElement).name;
        const value = (e.target as HTMLSelectElement).value;

        switch (name) {
            case 'language_id':
                this.store.setLanguage(value);
                break;
        }

        this.router.navigate([], {
            queryParams: {
                [name]: value
            },
            queryParamsHandling: 'merge'
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
                this.store.save({
                    ...this.savePayload,
                    contentletID: detail.data.identifier,
                    whenSaved: () => {
                        this.resetDialogIframeData();
                        this.reloadIframe();
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

                this.store.initEditIframeDialog({
                    inode: payload.inode,
                    title: payload.title
                });
            },
            [CUSTOMER_ACTIONS.ADD_CONTENTLET]: () => {
                const payload = <AddContentletPayload>data.payload;

                this.store.initAddIframeDialog({
                    containerID: payload.container.identifier,
                    acceptTypes: payload.container.acceptTypes ?? '*'
                });

                this.savePayload = payload;
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
