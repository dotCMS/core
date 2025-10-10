import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { delay, take } from 'rxjs/operators';

import { DotSessionStorageService, PaginatorService } from '@dotcms/data-access';
import { DotPageMode, DotPageRenderState, DotPersona } from '@dotcms/dotcms-models';
import { DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import {
    PaginationEvent,
    SearchableDropdownComponent
} from '../_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotAddPersonaDialogComponent } from '../dot-add-persona-dialog/dot-add-persona-dialog.component';
import { DotPersonaSelectedItemComponent } from '../dot-persona-selected-item/dot-persona-selected-item.component';
import { DotPersonaSelectorOptionComponent } from '../dot-persona-selector-option/dot-persona-selector-option.component';

export const DEFAULT_PERSONA_IDENTIFIER_BY_BACKEND = 'modes.persona.no.persona';

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
    templateUrl: 'dot-persona-selector.component.html',
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        TooltipModule,
        SharedModule,
        DotIconComponent,
        DotMessagePipe,
        DotSafeHtmlPipe,
        SearchableDropdownComponent,
        DotPersonaSelectedItemComponent,
        DotPersonaSelectorOptionComponent,
        DotAddPersonaDialogComponent
    ],
    providers: [PaginatorService, IframeOverlayService]
})
export class DotPersonaSelectorComponent implements OnInit {
    paginationService = inject(PaginatorService);
    iframeOverlayService = inject(IframeOverlayService);
    private dotSessionStorageService = inject(DotSessionStorageService);

    @Input() disabled: boolean;
    @Input() readonly: boolean;

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
    defaultPersonaIdentifier = DEFAULT_PERSONA_IDENTIFIER_BY_BACKEND;
    private personaSeachQuery: string;

    private _pageState: DotPageRenderState;

    get pageState(): DotPageRenderState {
        return this._pageState;
    }

    @Input()
    set pageState(value: DotPageRenderState) {
        this._pageState = value;
        this.paginationService.paginationPerPage = this.paginationPerPage;

        const currentVariantName = this.dotSessionStorageService.getVariationId();

        this.paginationService.url = `v1/page/${this.pageState.page.identifier}/personas`;
        this.isEditMode = this.pageState.state.mode === DotPageMode.EDIT;
        this.paginationService.setExtraParams('respectFrontEndRoles', !this.isEditMode);
        this.paginationService.setExtraParams('variantName', currentVariantName);
        this.value = this.pageState.viewAs && this.pageState.viewAs.persona;
        this.canDespersonalize = this.pageState.page.canEdit || this.pageState.page.canLock;
        this.reloadPersonasListCurrentPage();
    }

    ngOnInit(): void {
        this.addAction = () => {
            this.searchableDropdown.toggleOverlayPanel();
            this.personaDialog.visible = true;
            this.personaDialog.personaName = this.personas.length ? '' : this.personaSeachQuery;
        };

        this.paginationService.paginationPerPage = this.paginationPerPage;
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
        if (!this.value || this.value?.identifier !== persona.identifier) {
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
