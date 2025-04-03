import { ChangeDetectionStrategy, Component, inject, OnChanges } from '@angular/core';

import { DotCMSStore } from '../../../../store/dotcms.store';

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
        @if ($isDevMode()) {
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
    #dotCMSStore = inject(DotCMSStore);

    $isDevMode = this.#dotCMSStore.$isDevMode;

    ngOnChanges() {
        console.warn('Missing required layout.body property in page');
    }
}
