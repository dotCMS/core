import { of } from 'rxjs';

import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    output,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Listbox, ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';

import { catchError } from 'rxjs/operators';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona } from '@dotcms/types';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotPageService } from '../../../service/dot-page.service';
import { UVEStore } from '../../../store/dot-uve.store';

interface PersonaSelector {
    items: DotPersona[];
    totalRecords: number;
    itemsPerPage: number;
}

const DEFAULT_PERSONAS = {
    items: [],
    totalRecords: 0,
    itemsPerPage: 0
};

@Component({
    selector: 'dot-uve-persona-selector',
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
    templateUrl: './dot-uve-persona-selector.component.html',
    styleUrl: './dot-uve-persona-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePersonaSelectorComponent {
    readonly #pageService = inject(DotPageService);
    readonly #store = inject(UVEStore);

    readonly listbox = viewChild<Listbox>('listbox');
    readonly $personas = signal<PersonaSelector>(DEFAULT_PERSONAS);

    readonly $pageId = this.#store.$pageIdentifier;
    readonly $persona = this.#store.$viewAsPersona;

    readonly $photo = computed(() => this.$persona()?.photo?.versionPath || '');
    readonly $showPaginator = computed(() => this.$personas().totalRecords > 10);

    selected = output<DotCMSViewAsPersona & { pageId: string }>();
    despersonalize = output<DotCMSViewAsPersona & { pageId: string; selected: boolean }>();

    constructor() {
        effect(() => {
            if (this.$pageId()) {
                untracked(() => {
                    this.fetchPersonas();
                    this.setListboxValue();
                });
            }
        });
    }

    /**
     * Handle the change of the persona
     *
     * @param {{ value: DotCMSViewAsPersona }} { value }
     * @memberof EditEmaPersonaSelectorComponent
     */
    protected onSelect({ value }: { value: DotCMSViewAsPersona }) {
        if (value.identifier === this.$persona().identifier) {
            return;
        }

        this.selected.emit({ ...value, pageId: this.$pageId() });
    }

    /**
     * Reset the value of the listbox
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    protected setListboxValue(): void {
        this.listbox()?.writeValue(this.$persona());
    }

    /**
     * Handle the remove of the persona
     *
     * @param {MouseEvent} event
     * @param {DotCMSViewAsPersona} persona
     * @memberof EditEmaPersonaSelectorComponent
     */
    protected onRemove(event: MouseEvent, persona: DotCMSViewAsPersona) {
        const selected = persona.identifier === this.$persona().identifier;
        event.stopPropagation();

        this.despersonalize.emit({
            ...persona,
            selected,
            pageId: this.$pageId()
        });
    }

    /**
     * Fetch personas from the API
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    protected fetchPersonas(page = 0) {
        this.#pageService
            .getPagePersonas(this.$pageId(), {
                perPage: 5000,
                page
            })
            .pipe(
                catchError(() =>
                    of({
                        personas: [],
                        pagination: {
                            currentPage: 0,
                            perPage: 0,
                            totalEntries: 0
                        }
                    })
                )
            )
            .subscribe(({ personas, pagination }) =>
                this.$personas.set({
                    items: personas,
                    totalRecords: pagination.totalEntries,
                    itemsPerPage: pagination.perPage
                })
            );
    }

    /**
     * Handle the paginate of the personas
     *
     * @param {PaginatorState} event
     * @memberof DotUvePersonaSelectorComponent
     */
    protected onPaginate(event: PaginatorState) {
        const page = event.page || 0;
        this.fetchPersonas(page + 1);
    }
}
