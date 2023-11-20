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
import { NG_CUSTOM_EVENTS, POST_MESSAGE_ACTIONS, WINDOW } from '../shared/models';

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

    pages = [
        {
            name: 'Home',
            value: 'index'
        },
        {
            name: 'Page One',
            value: 'page-one'
        },
        {
            name: 'Page Two',
            value: 'page-two'
        }
    ];

    readonly route = inject(ActivatedRoute);
    readonly router = inject(Router);
    readonly store = inject(EditEmaStore);
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
                this.handlePostMessage(event)();
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
                this.handleNgEvent(event);
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
    resetIframeData() {
        this.store.resetDialog();
    }

    /**
     * Create the url to edit a contentlet
     *
     * @private
     * @param {string} inode
     * @return {*}
     * @memberof DotEmaComponent
     */
    private createEditContentletUrl(inode: string): string {
        return `/c/portal/layout?p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=edit&inode=${inode}`;
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

        // Skip the loaded event
        if (detail.name === NG_CUSTOM_EVENTS.EDIT_CONTENTLET_LOADED) return;

        // This forces a reload in the iframe
        this.iframe.nativeElement.contentWindow?.postMessage(
            POST_MESSAGE_ACTIONS.EMA_RELOAD_PAGE,
            this.host
        );
    }

    /**
     * Handle the post message event
     *
     * @private
     * @param {{ action: POST_MESSAGE_ACTIONS; payload: DotCMSContentlet }} data
     * @return {*}
     * @memberof DotEmaComponent
     */
    private handlePostMessage({
        origin = this.host,
        data
    }: {
        origin: string;
        data: { action: POST_MESSAGE_ACTIONS; payload: DotCMSContentlet };
    }): () => void {
        const action = origin !== this.host ? POST_MESSAGE_ACTIONS.NOOP : data.action;

        return {
            [POST_MESSAGE_ACTIONS.EDIT_CONTENTLET]: () => {
                this.store.patchState({
                    dialogVisible: true,
                    dialogHeader: data.payload.title,
                    dialogIframeLoading: true,
                    dialogIframeURL: this.createEditContentletUrl(data.payload.inode)
                });
            },
            [POST_MESSAGE_ACTIONS.NOOP]: () => {
                /* Do Nothing because is not the origin we are expecting */
            }
        }[action];
    }
}
