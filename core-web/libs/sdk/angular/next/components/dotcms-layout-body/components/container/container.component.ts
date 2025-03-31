import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSColumnContainer } from '@dotcms/uve/types';

/**
 * This component renders a container with all its content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @internal
 */
@Component({
    selector: 'dotcms-container',
    standalone: true,
    template: ``,
    styleUrl: './container.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContainerComponent {
    /**
     * The container data to be rendered
     */
    @Input({ required: true }) container!: DotCMSColumnContainer;
}
