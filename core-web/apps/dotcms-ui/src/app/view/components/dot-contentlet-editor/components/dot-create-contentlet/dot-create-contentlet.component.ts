import { merge, Observable } from 'rxjs';

import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { filter, pluck } from 'rxjs/operators';

import { DotRouterService, DotIframeService } from '@dotcms/data-access';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

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
    styleUrls: ['./dot-create-contentlet.component.scss'],
    standalone: false
})
export class DotCreateContentletComponent implements OnInit {
    private dotRouterService = inject(DotRouterService);
    private dotIframeService = inject(DotIframeService);
    private dotContentletEditorService = inject(DotContentletEditorService);
    private route = inject(ActivatedRoute);

    @Output() shutdown: EventEmitter<unknown> = new EventEmitter();
    url$: Observable<string>;
    @Output()
    custom: EventEmitter<unknown> = new EventEmitter();

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
     * @param {unknown} event
     * @memberof DotCreateContentletComponent
     */
    onClose(event: unknown): void {
        if (this.dotRouterService.currentSavedURL.includes('/c/content/new/')) {
            this.dotRouterService.goToContent();
        }

        if (this.dotRouterService.currentSavedURL.includes('/pages/new/')) {
            this.dotRouterService.gotoPortlet('/pages');
        }

        this.dotIframeService.reloadData(this.dotRouterService.currentPortlet.id);
        this.shutdown.emit(event);
    }

    /**
     * Handle custom event
     * @param {unknown} event
     * @memberof DotCreateContentletComponent
     */
    onCustom(event: unknown): void {
        this.custom.emit(event);
    }
}
