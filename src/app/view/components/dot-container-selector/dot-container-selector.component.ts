import { DotContainer } from '../../../shared/models/container/dot-container.model';
import { MessageService } from './../../../api/services/messages-service';
import { Site } from 'dotcms-js/dotcms-js';
import { PaginatorService } from './../../../api/services/paginator/paginator.service';
import { Observable } from 'rxjs/Observable';
import { Component, OnInit, Output, EventEmitter, Input, ViewEncapsulation } from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-container-selector',
    templateUrl: './dot-container-selector.component.html',
    styleUrls: ['./dot-container-selector.component.scss']
})
export class DotContainerSelectorComponent implements OnInit {
    @Input() selectedContainersList: DotContainer[] = [];
    @Output() change: EventEmitter<DotContainer[]> = new EventEmitter();
    @Output() remove: EventEmitter<any> = new EventEmitter();

    totalRecords: number;
    currentContainers: DotContainer[] = [];


    constructor(public paginationService: PaginatorService, public messageService: MessageService) { }

    ngOnInit(): void {
        this.paginationService.url = 'v1/containers';
        this.messageService.getMessages([
            'editpage.container.add-container'
        ]).subscribe();
    }

    /**
     * Called when the selected site changed and the change event is emmited
     *
     * @param {DotContainer} container
     * @memberof DotContainerSelectorComponent
     */
    containerChange(container: DotContainer): void {
        if (!this.isContainerSelected(container)) {
            this.selectedContainersList.push(container);
            this.change.emit(this.selectedContainersList);
        }
    }

    /**
     * Call to handle filter containers from list
     *
     * @param {string} filter
     * @memberof DotContainerSelectorComponent
     */
    handleFilterChange(filter: string): void {
        this.getContainersList(filter);
    }

    /**
     * Call when the current page changed
     * @param {any} event
     * @memberof DotContainerSelectorComponent
     */
    handlePageChange(event: any): void {
        this.getContainersList(event.filter, event.first);
    }

    /**
     * Remove container item from selected containers and emit selected containers
     * @param {number} i
     * @memberof DotContainerSelectorComponent
     */
    removeContainerItem(i: number): void {
        this.selectedContainersList.splice(i, 1);
        this.change.emit(this.selectedContainersList);
    }

    /**
     * Check if a container was already added to the list
     *
     * @param {DotContainer} container
     * @returns {boolean}
     * @memberof DotContainerSelectorComponent
     */
    isContainerSelected(container: DotContainer): boolean {
        return this.selectedContainersList.some(containerItem => containerItem.identifier === container.identifier);
    }

    private getContainersList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService.getWithOffset(offset).subscribe(items => {
            this.currentContainers = items.splice(0);
            this.totalRecords = this.totalRecords || this.paginationService.totalRecords;
        });
    }
}
