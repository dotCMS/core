import { of } from 'rxjs';

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
import { PaginatorModule } from 'primeng/paginator';
import { PopoverModule } from 'primeng/popover';

import { catchError } from 'rxjs/operators';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona } from '@dotcms/types';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotPageApiService } from '../../../../../services/dot-page-api.service';

interface PersonaSelector {
    items: DotPersona[];
    totalRecords: number;
    itemsPerPage: number;
}

@Component({
    selector: 'dot-edit-ema-persona-selector',
    imports: [
        ButtonModule,
        AvatarModule,
        PopoverModule,
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
    @Input() value: DotCMSViewAsPersona;

    @Output() selected: EventEmitter<DotCMSViewAsPersona & { pageId: string }> = new EventEmitter();
    @Output() despersonalize: EventEmitter<
        DotCMSViewAsPersona & { pageId: string; selected: boolean }
    > = new EventEmitter();

    /**
     * PrimeNG v20+ friendly styling (avoid deprecated styleClass inputs) using Tailwind + PT.
     */
    readonly personaPopoverPt = {
        content: { class: '!p-2' }
    };

    readonly personaListboxPt = {
        root: { class: '!border-0 !shadow-none' },
        listContainer: { class: 'max-h-80 overflow-auto' },
        list: { class: '!p-0' },
        option: { class: '!p-0 rounded-md hover:bg-gray-50 focus:bg-gray-50' }
    };

    readonly personaAvatarPt = {
        root: { class: 'h-7 w-7 shrink-0' },
        label: { class: 'text-sm' }
    };

    readonly personaAvatarSelectedPt = {
        root: { class: 'h-6 w-6 shrink-0' },
        label: { class: 'text-sm' }
    };

    readonly personaChipPt = {
        root: { class: 'rounded-full bg-gray-100 text-gray-800 text-xs px-2 py-1 gap-1' },
        removeIcon: { class: 'text-gray-500 hover:text-gray-700' },
        label: { class: 'leading-4' }
    };

    get personaButtonStyleClass(): string {
        const base = 'gap-2 rounded-md px-2 !no-underline hover:!no-underline focus:!no-underline';
        const isDefaultPersona = this.value?.identifier === 'modes.persona.no.persona';
        return isDefaultPersona ? base : `${base} bg-blue-50 text-blue-700`;
    }

    protected photo = '';
    ngOnChanges(changes: SimpleChanges): void {
        if (changes.pageId) {
            this.fetchPersonas();
        }

        // To select the correct persona when the page is reloaded with no queryParams
        if (this.listbox) {
            this.resetValue();
        }

        // We have a discrepancy between the type of the photo in the API and the type of the photo in GQL
        this.photo =
            typeof this.value?.photo == 'string'
                ? this.value?.photo
                : this.value?.photo?.versionPath;
    }

    ngAfterViewInit(): void {
        this.resetValue();
    }

    /**
     * Handle the change of the persona
     *
     * @param {{ value: DotCMSViewAsPersona }} { value }
     * @memberof EditEmaPersonaSelectorComponent
     */
    onSelect({ value }: { value: DotCMSViewAsPersona }) {
        if (value.identifier === this.value.identifier) {
            return;
        }

        this.selected.emit({ ...value, pageId: this.pageId });
    }

    /**
     * Reset the value of the listbox
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    resetValue(): void {
        this.listbox.updateModel(this.value, null);
    }

    /**
     * Handle the remove of the persona
     *
     * @param {MouseEvent} event
     * @param {DotCMSViewAsPersona} persona
     * @memberof EditEmaPersonaSelectorComponent
     */
    onRemove(event: MouseEvent, persona: DotCMSViewAsPersona, selected: boolean) {
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
