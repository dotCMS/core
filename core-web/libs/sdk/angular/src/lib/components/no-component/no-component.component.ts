import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSContentlet } from '../../models';

/**
 * This is part of the Angular SDK.
 * This is a component for the `NoComponentComponent` component.
 */
@Component({
    selector: 'app-no-component',
    standalone: true,
    template: `<div data-testid="no-component">No Component for {{ contentlet.contentType }}</div>`,
    styleUrl: './no-component.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NoComponentComponent {
    @Input() contentlet!: DotCMSContentlet;
}
