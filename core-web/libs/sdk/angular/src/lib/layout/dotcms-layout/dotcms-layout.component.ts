/* eslint-disable @typescript-eslint/no-explicit-any */
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    HostListener,
    Input,
    OnInit,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { filter } from 'rxjs/operators';

import {
    CUSTOMER_ACTIONS,
    initEditor,
    isInsideEditor,
    postMessageToEditor,
    updateNavigation
} from '@dotcms/client';

import { DynamicComponentEntity } from '../../models';
import { DotCMSPageAsset } from '../../models/dotcms.model';
import { PageContextService } from '../../services/dotcms-context/page-context.service';
import { RowComponent } from '../row/row.component';

/**
 * `DotcmsLayoutComponent` is a class that represents the layout for a DotCMS page.
 *  It includes a `pageAsset` property that represents the DotCMS page asset and a `components` property that represents the dynamic components for the page.
 *
 * @export
 * @class DotcmsLayoutComponent
 */
@Component({
    selector: 'dotcms-layout',
    standalone: true,
    imports: [RowComponent],
    template: ` @for (row of this.pageAssetData()?.layout?.body?.rows; track $index) {
        <dotcms-row [row]="row" />
    }`,
    styleUrl: './dotcms-layout.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotcmsLayoutComponent implements OnInit {
    @Input({ required: true }) pageAsset!: DotCMSPageAsset;
    @Input({ required: true }) components!: Record<string, DynamicComponentEntity>;

    private readonly route = inject(ActivatedRoute);
    private readonly pageContextService = inject(PageContextService);
    private readonly destroyRef$ = inject(DestroyRef);

    pageAssetData = signal<any>(null);

    @HostListener('window:message', ['$event'])
    onMessage(event: MessageEvent) {
        if (!isInsideEditor()) {
            return;
        }

        if (event.data.name === 'SET_PAGE_INFO') {
            this.pageAssetData.set(event.data.payload);
            this.pageContextService.setContext(this.pageAssetData(), this.components);
        }
    }

    ngOnInit() {
        this.route.url
            .pipe(
                takeUntilDestroyed(this.destroyRef$),
                filter(() => isInsideEditor())
            )
            .subscribe((urlSegments) => {
                const pathname = '/' + urlSegments.join('/');

                initEditor();
                updateNavigation(pathname || '/');

                //Sent the path to the editor
                postMessageToEditor({
                    action: CUSTOMER_ACTIONS.GET_PAGE_INFO,
                    payload: {
                        pathname
                    }
                });
            });
    }

    ngOnChanges() {
        this.pageAssetData.set(this.pageAsset);
        //Each time the layout changes, we need to update the context
        this.pageContextService.setContext(this.pageAsset, this.components);
    }
}
