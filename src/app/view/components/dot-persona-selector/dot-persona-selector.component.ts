import {
    Component,
    ViewChild,
    Output,
    EventEmitter,
    Input,
    OnInit,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { PaginatorService } from '@services/paginator';
import { SearchableDropdownComponent, PaginationEvent } from '@components/_common/searchable-dropdown/component';
import { DotPersona } from '@shared/models/dot-persona/dot-persona.model';
import { take } from 'rxjs/operators';

/**
 * It is dropdown of personas, it handle pagination and global search
 *
 * @export
 * @class DotPersonaSelectorComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-persona-selector',
    styleUrls: ['./dot-persona-selector.component.scss'],
    templateUrl: 'dot-persona-selector.component.html'
})
export class DotPersonaSelectorComponent implements OnInit, OnChanges {
    @Input()
    pageId: string;

    @Input()
    value: DotPersona;

    @Output()
    selected: EventEmitter<DotPersona> = new EventEmitter();

    @Output()
    delete: EventEmitter<DotPersona> = new EventEmitter();

    @ViewChild('searchableDropdown')
    searchableDropdown: SearchableDropdownComponent;

    totalRecords: number;
    paginationPerPage = 5;
    personas: DotPersona[];
    messagesKey: { [key: string]: string } = {};
    addAction: (item: DotPersona) => void;

    constructor(public paginationService: PaginatorService) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (!changes.value.firstChange) {
            this.getPersonasList();
        }
    }

    ngOnInit(): void {
        this.addAction = () => {
            // TODO Implement + action
        };
        this.paginationService.url = `v1/page/${this.pageId}/personas`;
        this.paginationService.paginationPerPage = this.paginationPerPage;
        this.getPersonasList();
    }

    /**
     * Call when the global search changed
     *
     * @param {string} filter
     * @memberof DotPersonaSelectorComponent
     */
    handleFilterChange(filter: string): void {
        this.getPersonasList(filter);
    }

    /**
     * Call when the current page changed
     *
     * @param {PaginationEvent} event
     * @memberof DotPersonaSelectorComponent
     */
    handlePageChange(event: PaginationEvent): void {
        this.getPersonasList(event.filter, event.first);
    }

    /**
     * Call to load a new page.
     *
     * @param {string} [filter='']
     * @param {number} [offset=0]
     * @memberof DotPersonaSelectorComponent
     */
    getPersonasList(filter = '', offset = 0): void {
        // Set filter if undefined
        this.paginationService.filter = filter;
        this.paginationService
            .getWithOffset(offset)
            .pipe(take(1))
            .subscribe((items: DotPersona[]) => {
                this.personas = items;
                this.totalRecords = this.totalRecords || this.paginationService.totalRecords;
            });
    }

    /**
     * Call when the selected persona changed and the change event is emmited
     *
     * @param {DotPersona} persona
     * @memberof DotPersonaSelectorComponent
     */
    personaChange(persona: DotPersona): void {
        this.selected.emit(persona);
        this.searchableDropdown.toggleOverlayPanel();
    }
}
