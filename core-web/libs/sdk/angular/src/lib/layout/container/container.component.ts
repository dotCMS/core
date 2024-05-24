import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    computed,
    inject,
    signal
} from '@angular/core';

import { NoComponentComponent } from '../../components/no-component/no-component.component';
import { DotCMSContainer, DotCMSContentlet, DynamicComponentEntity } from '../../models';
import { PageContextService } from '../../services/dotcms-context/page-context.service';
import { getContainersData } from '../../utils';

interface DotContainer {
    acceptTypes: string;
    identifier: string;
    maxContentlets: number;
    uuid: string;
    variantId?: string;
}

const EMPTY_CONTAINER_EDIT_MODE_STYLES = {
    width: '100%',
    backgroundColor: '#ECF0FD',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    color: '#030E32',
    height: '10rem'
};

@Component({
    selector: 'dotcms-container',
    standalone: true,
    imports: [AsyncPipe, NgComponentOutlet, NoComponentComponent],
    templateUrl: './container.component.html',
    styleUrl: './container.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContainerComponent implements OnChanges {
    @Input({ required: true }) container!: DotCMSContainer;

    private readonly pageContextService: PageContextService = inject(PageContextService);
    protected readonly emptyContainerStyles: Record<string, string> =
        EMPTY_CONTAINER_EDIT_MODE_STYLES;
    protected readonly NoComponentComponent = NoComponentComponent;
    protected readonly isInsideEditor = signal<boolean>(false);

    protected componentsMap!: Record<string, DynamicComponentEntity>;
    protected contentlets = signal<DotCMSContentlet[]>([]);
    protected dotContainer = signal<DotContainer | null>(null);
    protected dotContainerAsString = computed(() => JSON.stringify(this.dotContainer()));

    ngOnChanges() {
        const { containers, isInsideEditor } = this.pageContextService.pageContextValue;
        const { acceptTypes, maxContentlets, variantId, path, contentlets } = getContainersData(
            containers,
            this.container
        );
        const { identifier, uuid } = this.container;

        this.componentsMap = this.pageContextService.getComponentMap();

        this.isInsideEditor.set(isInsideEditor);
        this.contentlets.set(contentlets);
        this.dotContainer.set({
            identifier: path ?? identifier,
            acceptTypes,
            maxContentlets,
            variantId,
            uuid
        });
    }
}
