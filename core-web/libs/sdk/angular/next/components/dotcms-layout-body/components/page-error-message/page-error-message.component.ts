import {
    ChangeDetectionStrategy,
    Component,
    inject,
    Input,
    OnChanges,
    signal
} from '@angular/core';

import { DotCMSPageRendererMode } from '@dotcms/uve/types';

import { DotCMSContextService } from '../../../../services/dotcms-context/dotcms-context.service';

/**
 *
 * `PageErrorMessageComponent` is a class that represents the error message for a DotCMS page.
 *
 * @internal
 * @class PageErrorMessageComponent
 */
@Component({
    selector: 'dotcms-page-error-message',
    standalone: true,
    imports: [],
    template: `
        @if (isDevMode()) {
            <div
                data-testid="error-message"
                style="padding: 1rem; border: 1px solid #e0e0e0; border-radius: 4px;">
                <p style="margin: 0 0 0.5rem; color: #666;">
                    The
                    <code>page</code>
                    is missing the required
                    <code>layout.body</code>
                    property.
                </p>
                <p style="margin: 0; color: #666;">
                    Make sure the page asset is properly loaded and includes a layout configuration.
                </p>
            </div>
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageErrorMessageComponent implements OnChanges {
    @Input() mode: DotCMSPageRendererMode = 'production';

    private dotCMSContextService = inject(DotCMSContextService);

    isDevMode = signal(false);

    ngOnChanges() {
        console.warn('Missing required layout.body property in page');

        const isDevMode = this.dotCMSContextService.isDevMode(this.mode);

        this.isDevMode.set(isDevMode);
    }
}
