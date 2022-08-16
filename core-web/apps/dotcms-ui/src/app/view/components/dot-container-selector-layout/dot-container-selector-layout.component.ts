import { DotContainer } from '@models/container/dot-container.model';
import { PaginatorService } from '@services/paginator/paginator.service';
import { Component, OnInit, Output, EventEmitter, Input } from '@angular/core';
import { DotContainerColumnBox } from '@shared/models/dot-edit-layout-designer/dot-container-column-box.model';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';

@Component({
    selector: 'dot-container-selector-layout',
    templateUrl: './dot-container-selector-layout.component.html',
    styleUrls: ['./dot-container-selector-layout.component.scss']
})
export class DotContainerSelectorLayoutComponent implements OnInit {
    @Input() data: DotContainerColumnBox[] = [];
    @Input() multiple: boolean;
    @Output() switch: EventEmitter<DotContainerColumnBox[]> = new EventEmitter();

    totalRecords: number;
    currentContainers: DotContainer[] = [];

    constructor(
        public paginationService: PaginatorService,
        private templateContainersCacheService: DotTemplateContainersCacheService
    ) {}

    ngOnInit(): void {
        this.paginationService.url = 'v1/containers';
    }

    /**
     * Called when the selected site changed and the change event is emmited
     *
     * @param DotContainer container
     * @memberof DotContainerSelectorLayoutComponent
     */
    containerChange(container: DotContainer): void {
        if (this.multiple || !this.isContainerSelected(container)) {
            this.data.push({
                container: container
            });
            this.switch.emit(this.data);
        }
    }

    /**
     * Call to handle filter containers from list
     *
     * @param string filter
     * @memberof DotContainerSelectorLayoutComponent
     */
    handleFilterChange(filter: string): void {
        this.getContainersList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof DotContainerSelectorLayoutComponent
     */
    handlePageChange(event: { filter: string; first: number }): void {
        this.getContainersList(event.filter, event.first);
    }

    /**
     * Remove container item from selected containers and emit selected containers
     * @param number i
     * @memberof DotContainerSelectorLayoutComponent
     */
    removeContainerItem(i: number): void {
        this.data.splice(i, 1);
        this.switch.emit(this.data);
    }

    /**
     * Check if a container was already added to the list
     *
     * @param DotContainer container
     * @returns boolean
     * @memberof DotContainerSelectorLayoutComponent
     */
    isContainerSelected(dotContainer: DotContainer): boolean {
        return this.data.some(
            (containerItem) => containerItem.container.identifier === dotContainer.identifier
        );
    }

    private getContainersList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService
            .getWithOffset<DotContainer[]>(offset)
            .subscribe((items: DotContainer[]) => {
                this.currentContainers = this.setIdentifierReference(items.splice(0));
                this.totalRecords = this.totalRecords || this.paginationService.totalRecords;
            });
    }

    private setIdentifierReference(items: DotContainer[]): DotContainer[] {
        return items.map((dotContainer) => {
            dotContainer.identifier =
                this.templateContainersCacheService.getContainerReference(dotContainer);

            return dotContainer;
        });
    }
}
