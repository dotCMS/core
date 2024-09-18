import { AsyncPipe } from '@angular/common';
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

import { DotCMSPageComponent } from '../../models';
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
    imports: [RowComponent, AsyncPipe],
    template: `
        @if (pageAsset$ | async; as page) {
            @for (row of this.page?.layout?.body?.rows; track $index) {
                <dotcms-row [row]="row" />
            }
        }
    `,
    styleUrl: './dotcms-layout.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotcmsLayoutComponent implements OnInit {
    private _pageAsset!: DotCMSPageAsset;

    /**
     * Represents the DotCMS page asset.
     *
     * @type {DotCMSPageAsset}
     * @memberof DotcmsLayoutComponent
     */
    @Input({ required: true })
    set pageAsset(value: DotCMSPageAsset) {
        this._pageAsset = value;
        if (!value.layout) {
            console.warn(
                'Warning: pageAsset does not have a `layout` property. Might be using an advaced template or your dotCMS instance not have a enterprise license.'
            );
        }
    }

    /**
     * Returns the DotCMS page asset.
     *
     * @readonly
     * @type {DotCMSPageAsset}
     * @memberof DotcmsLayoutComponent
     */
    get pageAsset(): DotCMSPageAsset {
        return this._pageAsset;
    }

    /**
     * The `components` property is a record of dynamic components for each Contentlet on the page.
     *
     * @type {DotCMSPageComponent}
     * @memberof DotcmsLayoutComponent
     * @required
     */
    @Input({ required: true }) components!: DotCMSPageComponent;

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
    protected readonly pageAsset$ = this.pageContextService.currentPage$;

    ngOnInit() {
        this.pageContextService.setContext(this.pageAsset, this.components);

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

            this.pageContextService.setPageAsset(data as DotCMSPageAsset);
        });

        postMessageToEditor({ action: CUSTOMER_ACTIONS.CLIENT_READY, payload: this.editor });
    }

    ngOnDestroy() {
        if (!isInsideEditor()) {
            return;
        }

        this.client.editor.off('changes');
    }
}
