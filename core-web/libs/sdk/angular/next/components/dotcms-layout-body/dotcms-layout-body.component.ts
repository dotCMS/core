import { ChangeDetectionStrategy, Component, inject, Input } from '@angular/core';

import { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/uve/types';

import { ErrorMessageComponent } from './components/error-message/error-message.component';
import { RowComponent } from './components/row/row.component';

import { DotCMSPageComponent } from '../../models';
import { DotCMSContextService } from '../../services/dotcms-context/dotcms-context.service';
/**
 *
 * `DotcmsLayoutBodyComponent` is a class that represents the layout for a DotCMS page.
 *
 * @export
 * @class DotcmsLayoutBodyComponent
 */
@Component({
    selector: 'dotcms-layout-body',
    standalone: true,
    imports: [ErrorMessageComponent, RowComponent],
    providers: [DotCMSContextService],
    template: `
        @if (!pageAsset) {
            <div>
                <error-message [mode]="mode"></error-message>
            </div>
        } @else {
            @for (row of pageAsset.layout.body.rows; track row.identifier) {
                <dotcms-row [row]="row" />
            }
        }
    `,
    styleUrl: './dotcms-layout-body.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotcmsLayoutBodyComponent {
    @Input({ required: true }) pageAsset!: DotCMSPageAsset;
    @Input({ required: true }) components: DotCMSPageComponent = {};
    @Input({ required: true }) mode: DotCMSPageRendererMode = 'production';

    private dotCMSContextService = inject(DotCMSContextService);

    ngOnChanges() {
        this.dotCMSContextService.setContext({
            pageAsset: this.pageAsset,
            components: this.components,
            mode: this.mode
        });
    }
}
