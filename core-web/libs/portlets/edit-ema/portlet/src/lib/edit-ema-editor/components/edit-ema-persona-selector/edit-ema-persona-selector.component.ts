import { of } from 'rxjs';

import { NgClass } from '@angular/common';
import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
    inject,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Listbox, ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PaginatorModule } from 'primeng/paginator';

import { catchError } from 'rxjs/operators';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotPageApiService } from '../../../services/dot-page-api.service';

interface PersonaSelector {
    items: DotPersona[];
    totalRecords: number;
    itemsPerPage: number;
}

@Component({
    selector: 'dot-edit-ema-persona-selector',
    standalone: true,
    imports: [
        NgClass,
        ButtonModule,
        AvatarModule,
        OverlayPanelModule,
        DotAvatarDirective,
        DotMessagePipe,
        ListboxModule,
        ConfirmDialogModule,
        FormsModule,
        ChipModule,
        PaginatorModule
    ],
    templateUrl: './edit-ema-persona-selector.component.html',
    styleUrls: ['./edit-ema-persona-selector.component.scss']
})
export class EditEmaPersonaSelectorComponent implements AfterViewInit, OnChanges {
    @ViewChild('listbox') listbox: Listbox;

    private readonly pageApiService = inject(DotPageApiService);

    readonly MAX_PERSONAS_PER_PAGE = 10;

    $personas = signal<PersonaSelector>({
        items: [],
        totalRecords: 0,
        itemsPerPage: 0
    });

    @Input() pageId: string;
    @Input() value: DotPersona;

    @Output() selected: EventEmitter<DotPersona & { pageId: string }> = new EventEmitter();
    @Output() despersonalize: EventEmitter<DotPersona & { pageId: string; selected: boolean }> =
        new EventEmitter();

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.pageId) {
            this.fetchPersonas();
        }

        // To select the correct persona when the page is reloaded with no queryParams
        if (this.listbox) {
            this.resetValue();
        }
    }

    ngAfterViewInit(): void {
        this.resetValue();
    }

    /**
     * Handle the change of the persona
     *
     * @param {{ value: DotPersona }} { value }
     * @memberof EditEmaPersonaSelectorComponent
     */
    onSelect({ value }: { value: DotPersona }) {
        if (value.identifier !== this.value.identifier) {
            this.selected.emit({
                ...value,
                pageId: this.pageId
            });
        }
    }

    /**
     * Reset the value of the listbox
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    resetValue(): void {
        this.listbox.value = this.value;
        this.listbox.cd.detectChanges();
    }

    /**
     * Handle the remove of the persona
     *
     * @param {MouseEvent} event
     * @param {DotPersona} persona
     * @memberof EditEmaPersonaSelectorComponent
     */
    onRemove(event: MouseEvent, persona: DotPersona, selected: boolean) {
        event.stopPropagation();

        this.despersonalize.emit({
            ...persona,
            selected,
            pageId: this.pageId
        });
    }

    /**
     * Fetch personas from the API
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    fetchPersonas(page = 0) {
        this.pageApiService
            .getPersonas({
                pageId: this.pageId,
                perPage: 5000,
                page
            })
            .pipe(
                catchError(() =>
                    of({
                        data: [],
                        pagination: {
                            currentPage: 0,
                            perPage: 0,
                            totalEntries: 0
                        }
                    })
                )
            )
            .subscribe((res) =>
                this.$personas.set({
                    items: res.data,
                    totalRecords: res.pagination.totalEntries,
                    itemsPerPage: res.pagination.perPage
                })
            );
    }

    onPaginate(event) {
        // PrimeNG paginator starts at 0, but the API starts at 1
        this.fetchPersonas(event.page + 1);
    }
}
