import { AfterViewInit, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotRouterService, DotIframeService } from '@dotcms/data-access';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotEditContentletComponent } from '../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

@Component({
    selector: 'dot-contentlets',
    template:
        '<dot-edit-contentlet (shutdown)="onCloseEditor()" (custom)="onCustomEvent($event)"></dot-edit-contentlet>',
    imports: [DotEditContentletComponent]
})
export class DotContentletsComponent implements AfterViewInit {
    private dotContentletEditorService = inject(DotContentletEditorService);
    private dotRouterService = inject(DotRouterService);
    private dotIframeService = inject(DotIframeService);
    private route = inject(ActivatedRoute);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);

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
        const portletUrl = this.dotRouterService.currentPortlet.url
            .split('/')
            .slice(0, -1)
            .join('/');

        this.dotRouterService.gotoPortlet(portletUrl, { queryParamsHandling: 'preserve' });
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
