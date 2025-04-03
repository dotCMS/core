import {
    ChangeDetectionStrategy,
    Component,
    inject,
    Input,
    OnChanges,
    signal
} from '@angular/core';

import { DotCMSPageAsset, DotCMSPageRendererMode, DotPageAssetLayoutRow } from '@dotcms/uve/types';

import { PageErrorMessageComponent } from './components/page-error-message/page-error-message.component';
import { RowComponent } from './components/row/row.component';

import { DotCMSPageComponent } from '../../models';
import { DotCMSStore } from '../../store/dotcms.store';
/**
 *
 * `DotCMSLayoutBodyComponent` is a class that represents the layout for a DotCMS page.
 *
 * @export
 * @class DotCMSLayoutBodyComponent
 */
@Component({
    selector: 'dotcms-layout-body',
    standalone: true,
    imports: [PageErrorMessageComponent, RowComponent],
    providers: [DotCMSStore],
    template: `
        @if (!page) {
            <dotcms-page-error-message />
        } @else {
            @for (row of $rows(); track row.identifier) {
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

    $rows = signal<DotPageAssetLayoutRow[]>([]);

    ngOnChanges() {
        this.#dotCMSStore.setStore({
            page: this.page,
            components: this.components,
            mode: this.mode
        });

        this.$rows.set(this.page?.layout?.body?.rows ?? []);
    }
}
