import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { merge, Observable } from 'rxjs';
import { filter, pluck } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';

/**
 * Allow user to add a contentlet to DotCMS instance
 *
 * @export
 * @class DotCreateContentletComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-create-contentlet',
    templateUrl: './dot-create-contentlet.component.html',
    styleUrls: ['./dot-create-contentlet.component.scss']
})
export class DotCreateContentletComponent implements OnInit {
    @Output() close: EventEmitter<any> = new EventEmitter();
    url$: Observable<string>;
    @Output()
    custom: EventEmitter<any> = new EventEmitter();

    constructor(
        private dotRouterService: DotRouterService,
        private dotIframeService: DotIframeService,
        private dotContentletEditorService: DotContentletEditorService,
        private route: ActivatedRoute
    ) {}

    ngOnInit() {
        this.url$ = merge(
            this.dotContentletEditorService.createUrl$,
            this.route.data.pipe(pluck('url'))
        ).pipe(
            filter((url: string) => {
                return url !== undefined;
            })
        );
    }

    /**
     * Handle close event
     * @param {Event} event
     * @memberof DotCreateContentletComponent
     */
    onClose(event: Event): void {
        if (this.dotRouterService.currentSavedURL.includes('/c/content/new/')) {
            this.dotRouterService.goToContent();
        }
        this.dotIframeService.reloadData(this.dotRouterService.currentPortlet.id);
        this.close.emit(event);
    }

    /**
     * Handle custom event
     * @param {Event} event
     * @memberof DotCreateContentletComponent
     */
    onCustom(event: Event): void {
        this.custom.emit(event);
    }
}
