import {
    ChangeDetectionStrategy,
    Component,
    computed,
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
        @if (!containerData()) {
            <dotcms-container-not-found [identifier]="container.identifier" />
        } @else if (isEmpty()) {
            <dotcms-empty-container [dotAttributes]="dotAttributes()" />
        } @else {
            <div
                [attr.data-dot-object]="dotAttributes()['data-dot-object']"
                [attr.data-dot-accept-types]="dotAttributes()['data-dot-accept-types']"
                [attr.data-dot-identifier]="dotAttributes()['data-dot-identifier']"
                [attr.data-max-contentlets]="dotAttributes()['data-max-contentlets']"
                [attr.data-dot-uuid]="dotAttributes()['data-dot-uuid']">
                @for (contentlet of contentlets(); track contentlet.identifier) {
                    <dotcms-contentlet
                        [contentlet]="contentlet"
                        [container]="container.identifier" />
                }
            </div>
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

    containerData = signal<EditableContainerData | null>(null);
    contentlets = signal<DotCMSContentlet[]>([]);
    isEmpty = computed(() => this.contentlets().length === 0);
    dotAttributes = computed<DotContainerAttributes>(() => {
        const containerData = this.containerData();

        if (!containerData) {
            return {} as DotContainerAttributes;
        }

        return getDotContainerAttributes(containerData);
    });

    ngOnChanges() {
        const { pageAsset } = this.#dotcmsContextService.context ?? {};

        if (!pageAsset) {
            return;
        }

        this.containerData.set(getContainersData(pageAsset, this.container));
        this.contentlets.set(getContentletsInContainer(pageAsset, this.container));
    }
}
