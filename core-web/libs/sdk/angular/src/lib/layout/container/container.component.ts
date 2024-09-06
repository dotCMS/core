import { AsyncPipe, NgComponentOutlet } from '@angular/common';
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

import { NoComponent } from '../../components/no-component/no-component.component';
import { DynamicComponentEntity } from '../../models';
import { DotCMSContainer, DotCMSContentlet } from '../../models/dotcms.model';
import { PageContextService } from '../../services/dotcms-context/page-context.service';
import { getContainersData } from '../../utils';
import { ContentletComponent } from '../contentlet/contentlet.component';

interface DotContainer {
    acceptTypes: string;
    identifier: string;
    maxContentlets: number;
    uuid: string;
    variantId?: string;
}

@Component({
    selector: 'dotcms-container',
    standalone: true,
    imports: [AsyncPipe, NgComponentOutlet, NoComponent, ContentletComponent],
    templateUrl: './container.component.html',
    styleUrl: './container.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContainerComponent implements OnChanges {
    @Input({ required: true }) container!: DotCMSContainer;

    private readonly pageContextService: PageContextService = inject(PageContextService);
    protected readonly NoComponent = NoComponent;
    protected readonly $isInsideEditor = signal<boolean>(false);

    protected componentsMap!: Record<string, DynamicComponentEntity>;
    protected $contentlets = signal<DotCMSContentlet[]>([]);
    protected $dotContainer = signal<DotContainer | null>(null);
    protected $dotContainerAsString = computed(() => JSON.stringify(this.$dotContainer()));

    @HostBinding('attr.data-dot-accept-types') acceptTypes: string | null = null;
    @HostBinding('attr.data-dot-identifier') identifier: string | null = null;
    @HostBinding('attr.data-max-contentlets') maxContentlets: number | null = null;
    @HostBinding('attr.data-dot-uuid') uuid: string | null = null;
    @HostBinding('class') class: string | null = null;
    @HostBinding('attr.data-dot-object') dotObject: string | null = null;
    @HostBinding('attr.data-testid') testId = 'dot-container';

    ngOnChanges() {
        const { pageAsset, components, isInsideEditor } = this.pageContextService.context;
        const { acceptTypes, maxContentlets, variantId, path, contentlets } = getContainersData(
            pageAsset.containers,
            this.container
        );
        const { identifier, uuid } = this.container;

        this.componentsMap = components;

        this.$isInsideEditor.set(isInsideEditor);
        this.$contentlets.set(contentlets);
        this.$dotContainer.set({
            identifier: path ?? identifier,
            acceptTypes,
            maxContentlets,
            variantId,
            uuid
        });

        if (this.$isInsideEditor()) {
            this.acceptTypes = acceptTypes;
            this.identifier = identifier;
            this.maxContentlets = maxContentlets;
            this.uuid = uuid;
            this.class = this.$contentlets().length ? null : 'empty-container';
            this.dotObject = 'container';
        }
    }
}
