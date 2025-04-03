import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    Input,
    OnChanges
} from '@angular/core';

import { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/uve/types';

import { PageErrorMessageComponent } from './components/page-error-message/page-error-message.component';
import { RowComponent } from './components/row/row.component';

import { DotCMSPageComponent } from '../../models';
import { DotCMSContextService } from '../../services/dotcms-context/dotcms-context.service';
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
    providers: [DotCMSContextService],
    template: `
        @if (!pageAsset) {
            <dotcms-page-error-message [mode]="mode"></dotcms-page-error-message>
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
    @Input({ required: true }) pageAsset!: DotCMSPageAsset;
    @Input({ required: true }) components: DotCMSPageComponent = {};
    @Input() mode: DotCMSPageRendererMode = 'production';

    #dotCMSContextService = inject(DotCMSContextService);

    $rows = computed(() => this.pageAsset?.layout.body?.rows ?? []);

    ngOnChanges() {
        this.#dotCMSContextService.setContext({
            pageAsset: this.pageAsset,
            components: this.components,
            mode: this.mode
        });
    }
}
