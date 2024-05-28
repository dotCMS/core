import { ChangeDetectionStrategy, Component, HostBinding, Input } from '@angular/core';

import { DotCMSContentlet } from '../../models';

/**
 * This is part of the Angular SDK.
 * This is a component for the `NoComponentComponent` component.
 */
@Component({
    selector: 'app-no-component',
    standalone: true,
    template: `No Component for {{ contentlet.contentType }}`,
    styleUrl: './no-component.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NoComponentComponent {
    @Input() contentlet!: DotCMSContentlet;
    @HostBinding('attr.data-testid') testId = 'no-component';
}
