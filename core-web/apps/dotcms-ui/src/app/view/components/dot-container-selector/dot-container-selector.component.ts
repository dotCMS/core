import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { PaginationEvent } from '@components/_common/searchable-dropdown/component';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { PaginatorService } from '@dotcms/data-access';
import { DotContainer } from '@dotcms/dotcms-models';

@Component({
    providers: [PaginatorService],
    selector: 'dot-container-selector',
    templateUrl: './dot-container-selector.component.html',
    styleUrls: ['./dot-container-selector.component.scss']
})
export class DotContainerSelectorComponent implements OnInit {
    @Output() swap: EventEmitter<DotContainer> = new EventEmitter();

    @Input() innerClass = '';

    totalRecords: number;
    currentContainers: Observable<DotContainer[]>;

    constructor(
        public paginationService: PaginatorService,
        private templateContainersCacheService: DotTemplateContainersCacheService
    ) {}

    ngOnInit(): void {
        this.paginationService.url = 'v1/containers';
        this.paginationService.paginationPerPage = 5;
        this.paginationService.setExtraParams('system', true);
    }

    /**
     * Called when the selected site changed and the change event is emmited
     *
     * @param DotContainer container
     * @memberof DotContainerSelectorComponent
     */
    containerChange(container: DotContainer): void {
        this.swap.emit(container);
    }

    /**
     * Call to handle filter containers from list
     *
     * @param string filter
     * @memberof DotContainerSelectorComponent
     */
    handleFilterChange(filter: string): void {
        this.getContainersList(filter);
    }

    /**
     * Call when the current page changed
     * @param PaginationEvent event
     * @memberof DotContainerSelectorComponent
     */
    handlePageChange(event: PaginationEvent): void {
        this.getContainersList(event.filter, event.first);
    }

    private getContainersList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.currentContainers = this.paginationService.getWithOffset(offset).pipe(
            take(1),
            map((items: DotContainer[]) => this.setIdentifierReference(items.splice(0)))
        );
    }

    private setIdentifierReference(items: DotContainer[]): DotContainer[] {
        return items.map((dotContainer) => {
            dotContainer.identifier =
                this.templateContainersCacheService.getContainerReference(dotContainer);

            return dotContainer;
        });
    }
}
