import { CommonModule, DOCUMENT } from '@angular/common';
import {
    AfterViewInit,
    Component,
    ElementRef,
    Inject,
    OnInit,
    ViewChild,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { Dialog } from 'primeng/dialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { SafeUrlPipe } from '@dotcms/ui';

import { EditEmaStore } from './store/dot-ema.store';

import { DotPageApiService } from '../services/dot-page-api.service';
import { CUSTOM_EVENTS, MESSAGE_ACTIONS } from '../shared/models';

@Component({
    selector: 'dot-ema',
    standalone: true,

    imports: [CommonModule, FormsModule, SafeUrlPipe],
    providers: [EditEmaStore, DotPageApiService],

    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss']
})
export class DotEmaComponent implements OnInit, AfterViewInit {
    @ViewChild('dialog') dialog!: Dialog;
    @ViewChild('dialogIframe') dialogIframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

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
            name: 'Page One',
            value: 'page-one'
        },
        {
            name: 'Page Two',
            value: 'page-two'
        }
    ];

    route = inject(ActivatedRoute);
    router = inject(Router);
    store = inject(EditEmaStore);
    readonly host = 'http://localhost:3000';

    iframeUrl$ = this.store.iframeUrl$;
    language_id$ = this.store.language_id$;
    title$ = this.store.pageTitle$;
    url$ = this.store.url$;

    visible = false;
    header = '';

    constructor(@Inject(DOCUMENT) private document: Document) {}

    ngOnInit(): void {
        this.route.queryParams.subscribe(({ language_id, url }: Params) => {
            this.store.load({
                language_id,
                url
            });
        });
    }

    ngAfterViewInit(): void {
        this.document.defaultView?.addEventListener('message', (event: MessageEvent) => {
            // This should be the host the user uses for nextjs, because this can trigger react dev tools messages
            if (event.origin !== this.host) {
                return;
            }

            this.handlePostMessage(event.data)();
        });
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
        this.dialogIframe.nativeElement.contentWindow?.removeEventListener(
            'ng-event',
            this.handleNgEvent.bind(this)
        );

        this.dialogIframe.nativeElement.contentWindow?.document.addEventListener(
            'ng-event',
            this.handleNgEvent.bind(this)
        );
    }

    /**
     * Handle the custom events from the iframe
     *
     * @private
     * @param {Event} event
     * @memberof DotEmaComponent
     */
    private handleNgEvent(event: Event) {
        const { detail } = event as CustomEvent;

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
    private handlePostMessage(data: {
        action: MESSAGE_ACTIONS;
        payload: DotCMSContentlet;
    }): () => void {
        return {
            [MESSAGE_ACTIONS.EDIT_CONTENTLET]: () => {
                this.visible = true;
                this.header = data.payload.title;
                this.dialogIframe.nativeElement.src = this.createEditContentletUrl(
                    data.payload.inode
                );
            }
        }[data.action];
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

            case 'url':
                this.store.setUrl(value);
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
