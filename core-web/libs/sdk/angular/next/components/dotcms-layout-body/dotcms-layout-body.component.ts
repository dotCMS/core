import { ChangeDetectionStrategy, Component, inject, Input } from '@angular/core';

import { DotCMSPageRendererMode } from '@dotcms/uve/types';

import { ErrorMessageComponent } from './components/error-message/error-message.component';

import { DotCMSPageAsset, DotCMSPageComponent } from '../../models';
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
    imports: [ErrorMessageComponent],
    providers: [DotCMSContextService],
    template: `
        @if (!pageAsset) {
            <div>
                <error-message [mode]="mode"></error-message>
            </div>
        } @else {
            <div>ROW</div>
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
