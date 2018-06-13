import { Injectable } from '@angular/core';
import { DotLoadingIndicatorService } from '../../dot-loading-indicator/dot-loading-indicator.service';
import { DotRouterService } from '../../../../../../api/services/dot-router/dot-router.service';
import { DotContentletEditorService } from '../../../../dot-contentlet-editor/services/dot-contentlet-editor.service';

/**
 * Handle events triggered by the iframe in the IframePortletLegacyComponent
 *
 * @export
 * @class DotIframeEventsHandler
 */
@Injectable()
export class DotIframeEventsHandler {
    private readonly handlers;

    constructor(
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private dotRouterService: DotRouterService,
        private dotContentletEditorService: DotContentletEditorService
    ) {
        if (!this.handlers) {
            this.handlers = {
                'edit-page': this.goToEditPage.bind(this),
                'edit-contentlet': this.editContentlet.bind(this),
                'create-contentlet': this.createContentlet.bind(this)
            };
        }
    }

    /**
     * Handle custom events from the iframe portlets
     *
     * @param {CustomEvent} event
     * @memberof DotIframeEventsHandler
     */
    handle(event: CustomEvent): void {
        if (this.handlers[event.detail.name]) {
            this.handlers[event.detail.name](event);
        }
    }

    private createContentlet($event: CustomEvent): void {
        this.dotContentletEditorService.create({
            data: $event.detail.data
        });
    }

    private goToEditPage($event: CustomEvent): void {
        this.dotLoadingIndicatorService.show();
        this.dotRouterService.goToEditPage($event.detail.data.url);
    }

    private editContentlet($event: CustomEvent): void {
        this.dotRouterService.goToEditContentlet($event.detail.data.inode);
    }
}
