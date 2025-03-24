import { ChangeDetectionStrategy, Component, HostBinding, Input } from '@angular/core';

import { DotCMSContentlet } from '../../models';

/**
 * This component is responsible to display a message when there is no component for a contentlet.
 *
 * @export
 * @class NoComponent
 */
@Component({
    selector: 'dotcms-no-component',
    standalone: true,
    template: `
        No Component for {{ contentlet.contentType }}
    `,
    styleUrl: './no-component.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NoComponent {
    /**
     * The contentlet object containing content data.
     * The component displays a message based on the content type of this contentlet.
     */
    @Input() contentlet!: DotCMSContentlet;

    /**
     * The data-testid attribute used for identifying the component during testing.
     */
    @HostBinding('attr.data-testid') testId = 'no-component';
}
