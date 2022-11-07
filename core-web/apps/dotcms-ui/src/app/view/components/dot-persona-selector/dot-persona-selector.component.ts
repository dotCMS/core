import { Component, ViewChild, Output, EventEmitter, Input, OnInit } from '@angular/core';
import { PaginatorService } from '@services/paginator';
import {
    SearchableDropdownComponent,
    PaginationEvent
} from '@components/_common/searchable-dropdown/component';
import { DotPersona } from '@shared/models/dot-persona/dot-persona.model';
import { delay, take } from 'rxjs/operators';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import { DotAddPersonaDialogComponent } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.component';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';

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
export class DotPersonaSelectorComponent implements OnInit {
    @Input() disabled: boolean;

    @Output() selected: EventEmitter<DotPersona> = new EventEmitter();

    @Output() delete: EventEmitter<DotPersona> = new EventEmitter();

    @ViewChild('searchableDropdown', { static: true })
    searchableDropdown: SearchableDropdownComponent;
    @ViewChild('personaDialog', { static: true }) personaDialog: DotAddPersonaDialogComponent;

    addAction: (item: DotPersona) => void;
    canDespersonalize = false;
    isEditMode = false;
    paginationPerPage = 10;
    personas: DotPersona[] = [];
    totalRecords: number;
    value: DotPersona;

    private _pageState: DotPageRenderState;
    private personaSeachQuery: string;
    constructor(
        public paginationService: PaginatorService,
        public iframeOverlayService: IframeOverlayService
    ) {}

    ngOnInit(): void {
        this.addAction = () => {
            this.searchableDropdown.toggleOverlayPanel();
            this.personaDialog.visible = true;
            this.personaDialog.personaName = this.personas.length ? '' : this.personaSeachQuery;
        };

        this.paginationService.paginationPerPage = this.paginationPerPage;
    }

    @Input()
    set pageState(value: DotPageRenderState) {
        this._pageState = value;
        this.paginationService.paginationPerPage = this.paginationPerPage;
        this.paginationService.url = `v1/page/${this.pageState.page.identifier}/personas`;
        this.isEditMode = this.pageState.state.mode === DotPageMode.EDIT;
        this.paginationService.setExtraParams('respectFrontEndRoles', !this.isEditMode);
        this.value = this.pageState.viewAs && this.pageState.viewAs.persona;
        this.canDespersonalize = this.pageState.page.canEdit || this.pageState.page.canLock;
        this.reloadPersonasListCurrentPage();
    }

    get pageState(): DotPageRenderState {
        return this._pageState;
    }

    /**
     * Call when the global search changed
     *
     * @param {string} filter
     * @memberof DotPersonaSelectorComponent
     */
    handleFilterChange(filter: string): void {
        this.personaSeachQuery = filter.trim();
        this.getPersonasList(this.personaSeachQuery);
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
     * Call when the selected persona changed and the change event is emmited
     *
     * @param {DotPersona} persona
     * @memberof DotPersonaSelectorComponent
     */
    personaChange(persona: DotPersona): void {
        if (!this.value || this.value.identifier !== persona.identifier) {
            this.selected.emit(persona);
        }

        this.searchableDropdown.toggleOverlayPanel();
    }

    /**
     * Refresh the current page in the persona list option
     *
     * @memberof DotPersonaSelectorComponent
     */
    reloadPersonasListCurrentPage(): void {
        this.paginationService.getCurrentPage().pipe(take(1)).subscribe(this.setList.bind(this));
    }

    /**
     * Replace the persona receive in the current page list of personas
     *
     * @param {DotPersona} persona
     * @memberof DotPersonaSelectorComponent
     */
    updatePersonaInCurrentList(persona: DotPersona): void {
        this.personas = this.personas.map((currentPersona: DotPersona) => {
            return currentPersona.identifier === persona.identifier ? persona : currentPersona;
        });
    }

    /**
     * Propagate the new persona and refresh the persona list
     *
     * @param {DotPersona} persona
     * @memberof DotPersonaSelectorComponent
     */
    handleNewPersona(persona: DotPersona): void {
        this.searchableDropdown.resetPanelMinHeight();
        this.personaChange(persona);
        this.getPersonasList();
    }

    private getPersonasList(filter = '', offset = 0): void {
        // Set filter if undefined
        this.paginationService.filter = filter;
        this.paginationService
            .getWithOffset(offset)
            .pipe(take(1), delay(0))
            .subscribe(this.setList.bind(this));
    }

    private setList(items: DotPersona[]): void {
        this.personas = items;
        this.totalRecords = this.totalRecords || this.paginationService.totalRecords;
    }
}
