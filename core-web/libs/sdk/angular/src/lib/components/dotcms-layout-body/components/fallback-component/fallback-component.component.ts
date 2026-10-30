import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSBasicContentlet } from '@dotcms/types';

import { DynamicComponentEntity } from '../../../../models';

/**
 * @description Fallback component that renders when no custom component is found for a contentlet
 * @category Components
 * @internal
 * @class FallbackComponent
 */
@Component({
    selector: 'dotcms-fallback-component',
    imports: [AsyncPipe, NgComponentOutlet],
    template: `
        @if (UserNoComponent) {
            <ng-container *ngComponentOutlet="UserNoComponent | async; inputs: { contentlet }" />
        } @else {
            <div data-testid="dotcms-fallback-component">
                <p>No component found for content type: {{ contentlet.contentType }}</p>
            </div>
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class FallbackComponent {
    @Input() UserNoComponent: DynamicComponentEntity | null = null;
    @Input() contentlet!: DotCMSBasicContentlet;
}
