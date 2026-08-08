import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    Input,
    OnChanges,
    signal
} from '@angular/core';

import { DotCMSBasicContentlet, DotCMSColumnContainer, EditableContainerData } from '@dotcms/types';
import { DotContainerAttributes } from '@dotcms/types/internal';
import {
    getContainersData,
    getContentletsInContainer,
    getDotContainerAttributes
} from '@dotcms/uve/internal';

import { ContainerNotFoundComponent } from './components/container-not-found/container-not-found.component';
import { EmptyContainerComponent } from './components/empty-container/empty-container.component';

import { DotCMSStore } from '../../../../store/dotcms.store';
import { ContentletComponent } from '../../components/contentlet/contentlet.component';

/**
 * @description This component renders a container with all its content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @internal
 * @class ContainerComponent
 */
@Component({
    selector: 'dotcms-container',
    imports: [ContainerNotFoundComponent, EmptyContainerComponent, ContentletComponent],
    template: `
        @if (!$containerData()) {
            <dotcms-container-not-found [identifier]="container.identifier" />
        } @else if ($isEmpty()) {
            <dotcms-empty-container />
        } @else {
            @for (contentlet of $contentlets(); track contentlet.identifier) {
                <dotcms-contentlet [contentlet]="contentlet" [containerData]="$containerData()!" />
            }
        }
    `,
    // Container metadata is editor-only — bound only in edit mode so it never
    // leaks into live output. $dotAttributes is empty outside edit mode.
    host: {
        '[attr.data-dot-object]': "$isDevMode() ? 'container' : null",
        '[attr.data-dot-accept-types]': "$dotAttributes()['data-dot-accept-types'] ?? null",
        '[attr.data-dot-identifier]': "$dotAttributes()['data-dot-identifier'] ?? null",
        '[attr.data-max-contentlets]': "$dotAttributes()['data-max-contentlets'] ?? null",
        '[attr.data-dot-uuid]': "$dotAttributes()['data-dot-uuid'] ?? null"
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContainerComponent implements OnChanges {
    /**
     * The container data to be rendered
     */
    @Input({ required: true }) container!: DotCMSColumnContainer;

    #dotCMSStore = inject(DotCMSStore);

    $containerData = signal<EditableContainerData | null>(null);
    $contentlets = signal<DotCMSBasicContentlet[]>([]);
    $isEmpty = computed(() => this.$contentlets().length === 0);
    $isDevMode = this.#dotCMSStore.$isDevMode;
    $dotAttributes = computed<DotContainerAttributes>(() => {
        const containerData = this.$containerData();

        if (!containerData || !this.$isDevMode()) {
            return {} as DotContainerAttributes;
        }

        return getDotContainerAttributes(containerData);
    });

    ngOnChanges() {
        const { page } = this.#dotCMSStore.store ?? {};

        if (!page) {
            return;
        }

        this.$containerData.set(getContainersData(page, this.container));
        this.$contentlets.set(getContentletsInContainer(page, this.container));
    }
}
