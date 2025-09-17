import { patchState, signalState } from '@ngrx/signals';
import { of } from 'rxjs';

import { Component, inject, linkedSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMultiSelectFilterComponent } from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DEBOUNCE_TIME, PANEL_SCROLL_HEIGHT } from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-language-field',
    imports: [MultiSelectModule, FormsModule, DotMultiSelectFilterComponent, DotMessagePipe],
    templateUrl: './dot-content-drive-language-field.component.html',
    styleUrls: ['./dot-content-drive-language-field.component.scss']
})
export class DotContentDriveLanguageFieldComponent {
    readonly #dotLanguagesService = inject(DotLanguagesService);
    readonly #store = inject(DotContentDriveStore);

    $selectedLanguages = linkedSignal(() => {
        const languageIds = this.#store.getFilterValue('languageId') as string[];

        if (!languageIds) {
            return [];
        }

        return languageIds.map((language) => Number(language));
    });

    readonly $state = signalState<{ languages: DotLanguage[] }>({
        languages: []
    });

    protected readonly MULTISELECT_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;

    ngOnInit(): void {
        this.#dotLanguagesService.get().subscribe((languages) => {
            patchState(this.$state, { languages });
        });
    }

    onChange() {
        of(this.$selectedLanguages() ?? [])
            .pipe(
                debounceTime(DEBOUNCE_TIME), // Debounce to avoid spamming the server
                distinctUntilChanged()
            )
            .subscribe((value) => {
                if (value.length > 0) {
                    this.#store.patchFilters({
                        languageId: value.map((language) => language.toString())
                    });
                } else {
                    this.#store.removeFilter('languageId');
                }
            });
    }
}
