import { ChangeDetectionStrategy, Component, inject, Input, OnInit, signal } from '@angular/core';

import { DotCMSPageRendererMode, UVE_MODE } from '@dotcms/uve/types';
import { DotCMSContextService } from 'libs/sdk/angular/next/services/dotcms-context/dotcms-context.service';

/**
 *
 * `ErrorMessageComponent` is a class that represents the error message for a DotCMS page.
 *
 * @internal
 * @class ErrorMessageComponent
 */
@Component({
    selector: 'error-message',
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
export class ErrorMessageComponent implements OnInit {
    @Input() mode: DotCMSPageRendererMode = 'production';

    private dotCMSContextService = inject(DotCMSContextService);

    isDevMode = signal(false);

    ngOnInit(): void {
        console.warn('Missing required layout.body property in page');

        const isDevMode = this.dotCMSContextService.isDevMode(this.mode);

        this.isDevMode.set(isDevMode);
    }
}
