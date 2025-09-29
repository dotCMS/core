import { of } from 'rxjs';

import { NgClass, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    output,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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

const ITEMS_PER_PAGE = 5000;
const PAGINATOR_THRESHOLD = 10;
const DEFAULT_PAGE = 0;

const DEFAULT_PERSONAS: PersonaSelector = {
    items: [],
    totalRecords: 0,
    itemsPerPage: 0
};

@Component({
    selector: 'dot-uve-persona-selector',
    imports: [
        NgClass,
        NgTemplateOutlet,
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
export class DotUVEPersonaSelectorComponent {
    readonly #destroyRef = inject(DestroyRef);
    readonly #pageService = inject(DotPageService);
    readonly #store = inject(UVEStore);

    readonly listbox = viewChild<Listbox>('listbox');
    readonly $personas = signal<PersonaSelector>(DEFAULT_PERSONAS);
    readonly $isLoading = signal(true);

    readonly $pageId = this.#store.$pageIdentifier;
    readonly $persona = this.#store.$viewAsPersona;

    readonly $photo = computed(() => this.$persona()?.photo?.versionPath || '');
    readonly $showPaginator = computed(() => this.$personas().totalRecords > PAGINATOR_THRESHOLD);
    readonly $hasPersonas = computed(() => this.$personas().items.length > 0);

    selected = output<DotCMSViewAsPersona & { pageId: string }>();
    despersonalize = output<DotCMSViewAsPersona & { pageId: string; selected: boolean }>();

    constructor() {
        effect(() => {
            const pageId = this.$pageId();
            if (pageId) {
                untracked(() => {
                    this.#loadPersonas();
                    this.#setListboxValue();
                });
            }
        });
    }

    /**
     * Handle the selection of a persona
     *
     * @param {{ value: DotCMSViewAsPersona }} { value } - The selected persona
     * @memberof DotUvePersonaSelectorComponent
     */
    protected onSelect({ value }: { value: DotCMSViewAsPersona }): void {
        if (value.identifier === this.$persona()?.identifier) {
            return;
        }

        this.selected.emit({ ...value, pageId: this.$pageId() });
    }

    /**
     * Handle the removal of a persona
     *
     * @param {MouseEvent} event - The click event
     * @param {DotCMSViewAsPersona} persona - The persona to remove
     * @memberof DotUvePersonaSelectorComponent
     */
    protected onRemove(event: MouseEvent, persona: DotCMSViewAsPersona): void {
        const selected = persona.identifier === this.$persona()?.identifier;
        event.stopPropagation();

        this.despersonalize.emit({
            ...persona,
            selected,
            pageId: this.$pageId()
        });
    }

    /**
     * Handle pagination changes
     *
     * @param {PaginatorState} event - The pagination event
     * @memberof DotUvePersonaSelectorComponent
     */
    protected onPaginate(event: PaginatorState): void {
        const page = (event.page || DEFAULT_PAGE) + 1;
        this.#loadPersonas(page);
    }

    /**
     * Reset the value of the listbox to match current persona
     *
     * @private
     * @memberof DotUvePersonaSelectorComponent
     */
    #setListboxValue(): void {
        this.listbox()?.writeValue(this.$persona());
    }

    /**
     * Load personas from the API with proper error handling and cleanup
     *
     * @param {number} page - The page number to load (1-based)
     * @private
     * @memberof DotUvePersonaSelectorComponent
     */
    #loadPersonas(page = 1): void {
        this.$isLoading.set(true);
        this.#pageService
            .getPagePersonas(this.$pageId(), {
                perPage: ITEMS_PER_PAGE,
                page
            })
            .pipe(
                catchError(() => {
                    return of({
                        personas: [],
                        pagination: {
                            currentPage: DEFAULT_PAGE,
                            perPage: 0,
                            totalEntries: 0
                        }
                    });
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ personas, pagination }) => {
                this.$personas.set({
                    items: personas,
                    totalRecords: pagination.totalEntries,
                    itemsPerPage: pagination.perPage
                });
                this.$isLoading.set(false);
            });
    }
}
