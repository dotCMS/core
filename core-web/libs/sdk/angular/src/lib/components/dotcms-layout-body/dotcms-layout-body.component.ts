import {
    ChangeDetectionStrategy,
    Component,
    inject,
    Input,
    OnChanges,
    signal
} from '@angular/core';

import { DotCMSPageAsset, DotCMSPageRendererMode, DotPageAssetLayoutRow } from '@dotcms/types';

import { PageErrorMessageComponent } from './components/page-error-message/page-error-message.component';
import { RowComponent } from './components/row/row.component';

import { DotCMSPageComponent } from '../../models';
import { DotCMSStore } from '../../store/dotcms.store';
/**
 * @description This component is used to render the layout for a DotCMS page.
 * @param {DotCMSPageAsset} page - The page to render the layout for
 * @param {DotCMSPageComponent} components - The components to render the layout for
 * @param {DotCMSPageRendererMode} mode - The mode to render the layout for
 *
 * @example
 * <dotcms-layout-body [page]="page" [components]="components" [mode]="'development'" />
 *
 * @export
 * @implements {OnChanges}
 * @class DotCMSLayoutBodyComponent
 */
@Component({
    selector: 'dotcms-layout-body',
    imports: [PageErrorMessageComponent, RowComponent],
    providers: [DotCMSStore],
    template: `
        @if ($isEmpty() && $isDevMode()) {
            <dotcms-page-error-message />
        } @else {
            @for (row of $rows(); track $index) {
                <dotcms-row [row]="row" />
            }
        }
    `,
    styleUrl: './dotcms-layout-body.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCMSLayoutBodyComponent implements OnChanges {
    @Input({ required: true }) page!: DotCMSPageAsset;
    @Input({ required: true }) components: DotCMSPageComponent = {};
    @Input() mode: DotCMSPageRendererMode = 'production';

    #dotCMSStore = inject(DotCMSStore);

    $isDevMode = this.#dotCMSStore.$isDevMode;

    $rows = signal<DotPageAssetLayoutRow[]>([]);

    $isEmpty = signal(false);

    ngOnChanges() {
        this.#dotCMSStore.setStore({
            page: this.page,
            components: this.components,
            mode: this.mode
        });

        this.$isEmpty.set(!this.page?.layout?.body);

        this.$rows.set(this.page?.layout?.body?.rows ?? []);
    }
}
