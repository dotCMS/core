import { merge, Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { filter, map } from 'rxjs/operators';

import { DotRouterService, DotIframeService } from '@dotcms/data-access';
import { mapParamsFromEditContentlet } from '@dotcms/utils';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';

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
    imports: [DotContentletWrapperComponent, AsyncPipe]
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
            this.route.data.pipe(map((x) => x?.url))
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
            // If opened from Content Drive, the URL carries CD_-prefixed params (filters/path).
            // Return there with the filters preserved — same behavior as editing a contentlet
            // (DotContentletWrapperComponent.onClose). Otherwise fall back to the content listing.
            const searchParams = new URL(
                this.dotRouterService.currentPortlet.url,
                window.location.origin
            ).searchParams;
            const contentDriveParams = mapParamsFromEditContentlet(searchParams);

            if (Object.keys(contentDriveParams).length) {
                this.dotRouterService.gotoPortlet('content-drive', {
                    queryParams: contentDriveParams
                });
            } else {
                this.dotRouterService.goToContent();
            }
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
