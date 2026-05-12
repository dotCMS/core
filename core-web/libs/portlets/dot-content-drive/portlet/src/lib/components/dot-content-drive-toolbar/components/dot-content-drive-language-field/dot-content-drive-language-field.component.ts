import { patchState, signalState } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    computed,
    inject,
    linkedSignal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { PANEL_SCROLL_HEIGHT } from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-language-field',
    imports: [
        FormsModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-content-drive-language-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveLanguageFieldComponent implements OnInit {
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

    protected readonly LISTBOX_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;

    protected readonly $selectedLanguageNames = computed(() => {
        const ids = this.$selectedLanguages() ?? [];
        const languages = this.$state.languages();

        return ids
            .map((id) => languages.find((language) => language.id === id))
            .filter((language): language is DotLanguage => !!language)
            .map(
                (language) => `${language.language} (${language.isoCode ?? language.countryCode})`
            );
    });

    ngOnInit(): void {
        this.#dotLanguagesService.get().subscribe((languages) => {
            patchState(this.$state, { languages });
        });
    }

    onChange() {
        const value = this.$selectedLanguages() ?? [];
        if (value.length > 0) {
            this.#store.patchFilters({
                languageId: value.map((language) => language.toString())
            });
        } else {
            this.#store.removeFilter('languageId');
        }
    }

    onRemoveAll() {
        this.$selectedLanguages.set([]);
        this.onChange();
    }
}
