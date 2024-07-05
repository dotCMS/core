/* eslint-disable @typescript-eslint/no-explicit-any */
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    Input,
    OnInit,
    inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { filter } from 'rxjs/operators';

import { initEditor, isInsideEditor, updateNavigation } from '@dotcms/client';

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
    template: `@for (row of this.pageAsset?.layout?.body?.rows; track $index) {
        <dotcms-row [row]="row" />
    }`,
    styleUrl: './dotcms-layout.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotcmsLayoutComponent implements OnInit {
    @Input({ required: true }) pageAsset: DotCMSPageAsset | null = null;
    @Input({ required: true }) components!: Record<string, DynamicComponentEntity>;

    private readonly route = inject(ActivatedRoute);
    private readonly pageContextService = inject(PageContextService);
    private readonly destroyRef$ = inject(DestroyRef);

    ngOnInit() {
        this.route.url
            .pipe(
                takeUntilDestroyed(this.destroyRef$),
                filter(() => isInsideEditor())
            )
            .subscribe((urlSegments) => {
                const pathname = '/' + urlSegments.join('/');

                initEditor({ pathname });
                updateNavigation(pathname || '/');
            });
    }

    ngOnChanges() {
        //Each time the layout changes, we need to update the context
        if (this.pageAsset !== null) {
            this.pageContextService.setContext(this.pageAsset, this.components);
        }
    }
}
