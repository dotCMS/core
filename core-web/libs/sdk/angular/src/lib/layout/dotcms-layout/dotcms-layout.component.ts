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

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    EditorConfig,
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
    template: `
        @for (row of this.pageAsset?.layout?.body?.rows; track $index) {
            <dotcms-row [row]="row" />
        }
    `,
    styleUrl: './dotcms-layout.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotcmsLayoutComponent implements OnInit {
    /**
     * The `pageAsset` property represents the DotCMS page asset.
     *
     * @type {(DotCMSPageAsset)}
     * @memberof DotcmsLayoutComponent
     * @required
     */
    @Input({ required: true }) pageAsset!: DotCMSPageAsset;

    /**
     * The `components` property is a record of dynamic components for each Contentlet on the page.
     *
     * @type {Record<string, DynamicComponentEntity>}
     * @memberof DotcmsLayoutComponent
     * @required
     */
    @Input({ required: true }) components!: Record<string, DynamicComponentEntity>;

    /**
     * The `onReload` property is a function that reloads the page after changes are made.
     *
     * @memberof DotcmsLayoutComponent
     * @deprecated In future implementation we will be listening for the changes from the editor to update the page state so reload will not be needed.
     */
    @Input() onReload!: () => void;

    /**
     *
     * @type {DotCMSFetchConfig}
     * @memberof DotCMSPageEditorConfig
     * @description The configuration custom params for data fetching on Edit Mode.
     * @example <caption>Example with Custom GraphQL query</caption>
     * <dotcms-layout [editor]="{ query: 'query { ... }' }"/>
     *
     * @example <caption>Example usage with Custom Page API parameters</caption>
     * <dotcms-layout [editor]="{ params: { depth: '2' } }"/>;
     */
    @Input() editor!: EditorConfig;

    private readonly route = inject(ActivatedRoute);
    private readonly pageContextService = inject(PageContextService);
    private readonly destroyRef$ = inject(DestroyRef);
    private client!: DotCmsClient;

    ngOnInit() {
        this.setContext(this.pageAsset);

        if (!isInsideEditor()) {
            return;
        }

        this.client = DotCmsClient.instance;
        this.route.url.pipe(takeUntilDestroyed(this.destroyRef$)).subscribe((urlSegments) => {
            const pathname = '/' + urlSegments.join('/');

            initEditor({ pathname });
            updateNavigation(pathname || '/');
        });

        this.client.editor.on('changes', (data) => {
            if (this.onReload) {
                this.onReload();

                return;
            }

            const pageAsset = data as DotCMSPageAsset;
            this.pageAsset = pageAsset; // Update the page asset with the Editor Response
            this.setContext(pageAsset); // Update the context with the new page asset
        });

        postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_READY, payload: this.editor });
    }

    ngOnDestroy() {
        this.client.editor.off('changes');
    }

    private setContext(pageAsset: DotCMSPageAsset) {
        this.pageContextService.setContext(pageAsset, this.components);
    }
}
