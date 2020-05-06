import { Component, AfterViewInit } from '@angular/core';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import {DotCustomEventHandlerService} from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

@Component({
    providers: [],
    selector: 'dot-contentlets',
    template: '<dot-edit-contentlet (close)="onCloseEditor()" (custom)="onCustomEvent($event)"></dot-edit-contentlet>'
})
export class DotContentletsComponent implements AfterViewInit {
    constructor(
        private dotContentletEditorService: DotContentletEditorService,
        private dotRouterService: DotRouterService,
        private dotIframeService: DotIframeService,
        private route: ActivatedRoute,
        private dotCustomEventHandlerService: DotCustomEventHandlerService
    ) {}

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.dotContentletEditorService.edit({
                data: {
                    inode: this.route.snapshot.params.asset
                }
            });
        }, 0);
    }

    /**
     * Handle close event from the iframe
     *
     * @memberof DotContentletsComponent
     */
    onCloseEditor(): void {
        this.dotRouterService.gotoPortlet(`/c/${this.dotRouterService.currentPortlet.id}`);
        this.dotIframeService.reloadData(this.dotRouterService.currentPortlet.id);
    }

    /**
     * Handle the custom events emmited by the Edit Contentlet
     *
     * @param CustomEvent $event
     * @memberof DotContentletsComponent
     */
    onCustomEvent($event: CustomEvent): void {
        this.dotCustomEventHandlerService.handle($event);
    }
}
