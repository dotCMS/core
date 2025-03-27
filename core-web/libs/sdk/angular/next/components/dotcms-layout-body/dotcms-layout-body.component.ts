import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSPageRendererMode } from '@dotcms/uve/types';

import { DotCMSPageAsset, DotCMSPageComponent } from '../../models';

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
    imports: [],
    template: ``,
    styleUrl: './dotcms-layout-body.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotcmsLayoutBodyComponent {
    @Input() pageAsset: DotCMSPageAsset;
    @Input() components: DotCMSPageComponent;
    @Input() mode: DotCMSPageRendererMode = 'production';
}
