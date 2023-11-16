import { CommonModule, DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, Inject, ViewChild } from '@angular/core';

import { Dialog, DialogModule } from 'primeng/dialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { CUSTOM_EVENTS, MESSAGE_ACTIONS } from '../shared/models';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [CommonModule, DialogModule],
    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss']
})
export class DotEmaComponent implements AfterViewInit {
    @ViewChild('dialog') dialog!: Dialog;
    @ViewChild('dialogIframe') dialogIframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    visible = false;
    header = '';

    private host = 'http://localhost:3000';

    constructor(@Inject(DOCUMENT) private document: Document) {}

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
}
