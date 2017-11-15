import { Container } from './container-selector.model';
import { MessageService } from './../../../api/services/messages-service';
import { Site } from 'dotcms-js/dotcms-js';
import { PaginatorService } from './../../../api/services/paginator/paginator.service';
import { Observable } from 'rxjs/Observable';
import { Component, OnInit, Output, EventEmitter, Input, ViewEncapsulation } from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-container-selector',
    templateUrl: './container-selector.component.html',
    styleUrls: ['./container-selector.component.scss']
})
export class ContainerSelectorComponent implements OnInit {
    @Output() change: EventEmitter<Container[]> = new EventEmitter();
    @Output() remove: EventEmitter<any> = new EventEmitter();

    totalRecords: number;
    currentContainers: Container[] = [];
    selectedContainersList: Container[] = [];

    constructor(public paginationService: PaginatorService, public messageService: MessageService) { }

    ngOnInit(): void {
        this.paginationService.url = 'v1/containers';
        this.messageService.getMessages([
            'editpage.container.add-container'
        ]).subscribe();
    }

    /**
     * Call when the selected site changed and the change event is emmited
     * @param {*} container
     * @memberof ContainerSelectorComponent
     */
    containerChange(container: Container): void {
        if (this.selectedContainersList.indexOf(container) < 0) {
            this.selectedContainersList.push(container);
            this.change.emit(this.selectedContainersList);
        }
    }

    /**
     * Call to handle filter containers from list
     * @param {any} filter
     * @memberof ContainerSelectorComponent
     */
    handleFilterChange(filter: string): void {
        this.getContainersList(filter);
    }

    /**
     * Call when the current page changed
     * @param {any} event
     * @memberof ContainerSelectorComponent
     */
    handlePageChange(event: any): void {
        this.getContainersList(event.filter, event.first);
    }

    /**
     * Remove container item from selected containers and emit selected containers
     * @param {number} i
     * @memberof ContainerSelectorComponent
     */
    removeContainerItem(i: number): void {
        this.selectedContainersList.splice(i, 1);
        this.change.emit(this.selectedContainersList);
    }

    private getContainersList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService.getWithOffset(offset).subscribe(items => {
            this.currentContainers = items.splice(0);
            this.totalRecords = this.totalRecords || this.paginationService.totalRecords;
        });
    }
}
