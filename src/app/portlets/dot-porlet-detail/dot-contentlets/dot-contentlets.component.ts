import { Component, AfterViewInit } from '@angular/core';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { DotIframeService } from '../../../view/components/_common/iframe/service/dot-iframe/dot-iframe.service';

@Component({
    providers: [],
    selector: 'dot-contentlets',
    template: '<dot-edit-contentlet (close)="onCloseEditor()"></dot-edit-contentlet>'
})
export class DotContentletsComponent implements AfterViewInit {
    constructor(
        private dotContentletEditorService: DotContentletEditorService,
        private dotRouterService: DotRouterService,
        private dotIframeService: DotIframeService,
        private route: ActivatedRoute
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
}
