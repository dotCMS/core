import { Subject, fromEvent } from 'rxjs';

import { AsyncPipe, DOCUMENT, NgFor } from '@angular/common';
import {
    AfterViewInit,
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
import { ActivatedRoute, Params, Router } from '@angular/router';

import { DialogModule } from 'primeng/dialog';

import { takeUntil } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { SafeUrlPipe } from '@dotcms/ui';

import { EditEmaStore } from './store/dot-ema.store';

import { DotPageApiService } from '../services/dot-page-api.service';
import { CUSTOM_EVENTS, MESSAGE_ACTIONS } from '../shared/models';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [NgFor, AsyncPipe, FormsModule, SafeUrlPipe, DialogModule],
    providers: [EditEmaStore, DotPageApiService],
    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss']
})
export class DotEmaComponent implements OnInit, AfterViewInit, OnDestroy {
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

    readonly iframeUrl$ = this.store.iframeUrl$;
    readonly language_id$ = this.store.language_id$;
    readonly title$ = this.store.pageTitle$;
    readonly url$ = this.store.url$;

    visible = false;
    header = '';

    constructor(@Inject(DOCUMENT) private document: Document, private cd: ChangeDetectorRef) {}

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
    }

    ngAfterViewInit(): void {
        fromEvent(this.document.defaultView, 'message')
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
     * Handle the iframe load event
     *
     * @private
     * @param {CustomEvent} event
     * @memberof DotEmaComponent
     */
    onIframeLoad(_: Event) {
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
     * Handle the custom events from the iframe
     *
     * @private
     * @param {Event} event
     * @memberof DotEmaComponent
     */
    private handleNgEvent(event: CustomEvent) {
        const { detail } = event;

        // Skip the loaded event
        if (detail.name === CUSTOM_EVENTS.EDIT_CONTENTLET_LOADED) return;

        // This forces a reload in the iframe
        this.iframe.nativeElement.contentWindow?.postMessage('reload', this.host);
    }

    /**
     * Handle the post message event
     *
     * @private
     * @param {{ action: MESSAGE_ACTIONS; payload: DotCMSContentlet }} data
     * @return {*}
     * @memberof DotEmaComponent
     */
    private handlePostMessage({
        origin,
        data
    }: {
        origin: string;
        data: { action: MESSAGE_ACTIONS; payload: DotCMSContentlet };
    }): () => void {
        const action = origin !== this.host ? 'NOOP' : data.action;

        return {
            [MESSAGE_ACTIONS.EDIT_CONTENTLET]: () => {
                this.visible = true;
                this.header = data.payload.title;
                this.dialogIframe.nativeElement.src = this.createEditContentletUrl(
                    data.payload.inode
                );
            },
            NOOP: () => {
                /* Do Nothing because is not the origin we are expecting */
            }
        }[action];
    }

    /*
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
}
