import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';

/**
 * @description This component is used to display a message when a page is missing the required `layout.body` property.
 * @internal
 * @class PageErrorMessageComponent
 */
@Component({
    selector: 'dotcms-page-error-message',
    imports: [],
    template: `
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
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageErrorMessageComponent implements OnInit {
    ngOnInit() {
        console.warn('Missing required layout.body property in page');
    }
}
