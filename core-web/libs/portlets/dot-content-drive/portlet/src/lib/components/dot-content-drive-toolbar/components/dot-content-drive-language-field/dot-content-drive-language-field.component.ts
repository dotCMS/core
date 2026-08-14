import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

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
export class DotContentDriveLanguageFieldComponent {
    readonly #store = inject(DotContentDriveStore);

    $selectedLanguages = linkedSignal(() => {
        const languageIds = this.#store.getFilterValue('languageId') as string[];

        if (!languageIds) {
            return [];
        }

        return languageIds.map((language) => Number(language));
    });

    /**
     * Every configured language, resolved once by the store — which needs the list anyway to find
     * the default one to seed. Read from there rather than fetched here so both share one request.
     */
    protected readonly $languages = this.#store.languages;

    /**
     * Whether the chip should offer its "remove" X. Hidden while the selection is exactly the
     * environment default, because removing it re-selects that same default — the X would do
     * nothing visible.
     */
    protected readonly $removable = computed(() => {
        const selected = this.$selectedLanguages() ?? [];

        return !(selected.length === 1 && selected[0] === this.#store.defaultLanguageId());
    });

    protected readonly LISTBOX_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;

    protected readonly $selectedLanguageNames = computed(() => {
        const ids = this.$selectedLanguages() ?? [];
        const languages = this.$languages();

        return ids
            .map((id) => languages.find((language) => language.id === id))
            .filter((language): language is DotLanguage => !!language)
            .map(
                (language) => `${language.language} (${language.isoCode ?? language.countryCode})`
            );
    });

    /**
     * Applies the selection, snapping back to the environment default when it is emptied.
     *
     * An empty language filter is not the neutral state it looks like: the backend then omits the
     * language term and returns every language version of a contentlet as its own row. So
     * deselecting everything means "back to the default", not "all languages" — and the value is
     * pushed into the signal too, so the listbox re-renders the default as checked rather than
     * relying on the `linkedSignal` recomputing from an unchanged value.
     *
     * Falls back to removing the filter only when no default is known (the languages request
     * failed), which leaves the pre-seeding behaviour intact.
     */
    onChange() {
        const value = this.$selectedLanguages() ?? [];

        if (value.length > 0) {
            this.#store.patchFilters({
                languageId: value.map((language) => language.toString())
            });

            return;
        }

        const defaultLanguageId = this.#store.defaultLanguageId();

        if (!defaultLanguageId) {
            this.#store.removeFilter('languageId');

            return;
        }

        this.$selectedLanguages.set([defaultLanguageId]);
        this.#store.patchFilters({ languageId: [defaultLanguageId.toString()] });
    }

    onRemoveAll() {
        this.$selectedLanguages.set([]);
        this.onChange();
    }
}
