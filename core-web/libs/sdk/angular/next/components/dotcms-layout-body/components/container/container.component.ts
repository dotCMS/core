import {
    ChangeDetectionStrategy,
    Component,
    computed,
    HostBinding,
    inject,
    Input,
    OnChanges,
    signal
} from '@angular/core';

import {
    getContainersData,
    getContentletsInContainer,
    getDotContainerAttributes
} from '@dotcms/uve/internal';
import {
    DotCMSColumnContainer,
    DotCMSContentlet,
    DotContainerAttributes,
    EditableContainerData
} from '@dotcms/uve/types';

import { ContainerNotFoundComponent } from './components/container-not-found/container-not-found.component';
import { EmptyContainerComponent } from './components/empty-container/empty-container.component';

import { DotCMSContextService } from '../../../../services/dotcms-context/dotcms-context.service';
import { ContentletComponent } from '../../components/contentlet/contentlet.component';

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
    imports: [ContainerNotFoundComponent, EmptyContainerComponent, ContentletComponent],
    template: `
        @if (!$containerData()) {
            <dotcms-container-not-found [identifier]="container.identifier" />
        } @else if ($isEmpty()) {
            <dotcms-empty-container [dotAttributes]="dotAttributes()" />
        } @else {
            @for (contentlet of $contentlets(); track contentlet.identifier) {
                <dotcms-contentlet [contentlet]="contentlet" [container]="container.identifier" />
            }
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContainerComponent implements OnChanges {
    /**
     * The container data to be rendered
     */
    @Input({ required: true }) container!: DotCMSColumnContainer;

    #dotcmsContextService = inject(DotCMSContextService);

    $containerData = signal<EditableContainerData | null>(null);
    $contentlets = signal<DotCMSContentlet[]>([]);
    $isEmpty = computed(() => this.$contentlets().length === 0);
    dotAttributes = computed<DotContainerAttributes>(() => {
        const containerData = this.$containerData();

        if (!containerData || !this.#dotcmsContextService.isDevMode()) {
            return {} as DotContainerAttributes;
        }

        return getDotContainerAttributes(containerData);
    });

    @HostBinding('attr.data-dot-object') dotObject = 'container';
    @HostBinding('attr.data-dot-accept-types') acceptTypes: string | null = null;
    @HostBinding('attr.data-dot-identifier') identifier: string | null = null;
    @HostBinding('attr.data-max-contentlets') maxContentlets: string | null = null;
    @HostBinding('attr.data-dot-uuid') uuid: string | null = null;

    ngOnChanges() {
        const { page } = this.#dotcmsContextService.context ?? {};

        if (!page) {
            return;
        }

        this.$containerData.set(getContainersData(page, this.container));
        this.$contentlets.set(getContentletsInContainer(page, this.container));

        this.acceptTypes = this.dotAttributes()['data-dot-accept-types'];
        this.identifier = this.dotAttributes()['data-dot-identifier'];
        this.maxContentlets = this.dotAttributes()['data-max-contentlets'];
        this.uuid = this.dotAttributes()['data-dot-uuid'];
    }
}
