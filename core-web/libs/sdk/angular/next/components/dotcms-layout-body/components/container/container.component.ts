import {
    ChangeDetectionStrategy,
    Component,
    HostBinding,
    Input,
    OnChanges,
    computed,
    inject,
    signal
} from '@angular/core';

import { DotCMSContainer } from '../../../../models';
import { DotCMSContextService } from '../../../../services/dotcms-context/dotcms-context.service';

interface DotContainer {
    acceptTypes: string;
    identifier: string;
    maxContentlets: number;
    uuid: string;
    variantId?: string;
}

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
    template: `
        <div [class]="customContainerClass" data-dot-object="container">
            @if (contentlets().length) {
                @for (contentlet of contentlets(); track $index) {
                    <div class="contentlet-wrapper" [attr.data-dot-object]="'contentlet'">
                        {{ contentlet.title }}
                    </div>
                }
            } @else {
                <div class="empty-container">This container is empty.</div>
            }
        </div>
    `,
    styleUrl: './container.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContainerComponent implements OnChanges {
    /**
     * The container data to be rendered
     */
    @Input({ required: true }) container!: DotCMSContainer;

    private readonly dotCMSContextService = inject(DotCMSContextService);
    protected readonly contentlets = signal<DotCMSContainer['contentlets']>([]);
    protected readonly $dotContainer = signal<DotContainer | null>(null);
    protected readonly $dotContainerAsString = computed(() => JSON.stringify(this.$dotContainer()));

    /**
     * The custom container class that combines the styleClass from the container data with the base container class
     */
    protected get customContainerClass(): string {
        return 'container';
    }

    /**
     * The accept types for the container component.
     */
    @HostBinding('attr.data-dot-accept-types') acceptTypes: string | null = null;

    /**
     * The identifier for the container component.
     */
    @HostBinding('attr.data-dot-identifier') identifier: string | null = null;

    /**
     * The max contentlets for the container component.
     */
    @HostBinding('attr.data-max-contentlets') maxContentlets: number | null = null;

    /**
     * The uuid for the container component.
     */
    @HostBinding('attr.data-dot-uuid') uuid: string | null = null;

    /**
     * The data-testid attribute used for identifying the component during testing.
     */
    @HostBinding('attr.data-testid') testId = 'dot-container';

    ngOnChanges() {
        const isInsideEditor = this.dotCMSContextService.isDevMode();

        this.contentlets.set(this.container.contentlets || []);
        this.$dotContainer.set({
            identifier: this.container.path ?? this.container.identifier,
            acceptTypes: this.container.acceptTypes,
            maxContentlets: this.container.maxContentlets,
            uuid: this.container.uuid
        });

        if (isInsideEditor) {
            this.acceptTypes = this.container.acceptTypes;
            this.identifier = this.container.identifier;
            this.maxContentlets = this.container.maxContentlets;
            this.uuid = this.container.uuid;
        }
    }
}
