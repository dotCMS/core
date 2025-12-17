import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, distinctUntilChanged, map, startWith } from 'rxjs/operators';

import { DotPageTypesService, DotRouterService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-create-page-dialog',
    imports: [
        CommonModule,
        DotAutofocusDirective,
        DotMessagePipe,
        InputTextModule,
        ReactiveFormsModule,
        DotMessagePipe,
        DialogModule,
        IconFieldModule,
        InputIconModule
    ],
    providers: [DotPageTypesService],
    templateUrl: './dot-create-page-dialog.component.html',
    styleUrls: ['./dot-create-page-dialog.component.scss']
})
export class DotCreatePageDialogComponent {
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotPageTypesService = inject(DotPageTypesService);

    /**
     * Whether the dialog is visible
     * @type {boolean}
     */
    readonly $visibility = input.required<boolean>({ alias: 'visibility' });
    /**
     * Emits the visibility change
     * @type {boolean}
     */
    readonly visibilityChange = output<boolean>();

    readonly searchControl = new FormControl('', { nonNullable: true });
    readonly $pageTypes = signal<DotCMSContentType[]>([]);

    /**
     * Signal for the search term
     * @type {string}
     * @returns {string}
     */
    readonly $searchTerm = toSignal(
        this.searchControl.valueChanges.pipe(
            startWith(this.searchControl.value),
            debounceTime(300),
            map((value) => value.trim().toLowerCase()),
            distinctUntilChanged()
        ),
        { initialValue: '' }
    );

    /**
     * Computed property for the filtered page types
     * @returns {DotCMSContentType[]}
     */
    readonly $filteredPageTypes = computed(() => {
        const term = this.$searchTerm();
        const pageTypes = this.$pageTypes();

        if (!term) {
            return pageTypes;
        }

        return pageTypes.filter((type) => {
            const name = (type.name ?? '').toLowerCase();
            const variable = (type.variable ?? '').toLowerCase();
            return name.includes(term) || variable.includes(term);
        });
    });

    constructor() {
        this.#dotPageTypesService
            .getPageContentTypes()
            .subscribe((pageTypes: DotCMSContentType[]) => this.$pageTypes.set(pageTypes));
    }

    /**
     * Redirect to Create content page
     * @param {string} variableName
     *
     * @memberof DotPagesCreatePageDialogComponent
     */
    goToCreatePage(variableName: string): void {
        this.visibilityChange.emit(false);
        this.#dotRouterService.goToURL(`/pages/new/${variableName}`);
    }
}
