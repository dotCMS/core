import { patchState, signalState } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    linkedSignal,
    OnInit,
    output
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    CHIP_FILTER_SCROLL_HEIGHT
} from '../dot-chip-filter/constants';
import { DotChipFilterComponent } from '../dot-chip-filter/dot-chip-filter.component';
import { DotFilterListItemComponent } from '../dot-filter-list-item/dot-filter-list-item.component';

/**
 * Locale chip filter shared across Content Drive and AssetPicker.
 *
 * Owns the language catalog (fetched through {@link DotLanguagesService}) but not the selection:
 * the host passes the selected ids in and receives every change back through `selectionChange`.
 */
@Component({
    selector: 'dot-language-filter',
    imports: [
        FormsModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-language-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // `contents` keeps the host out of the layout, so the chip and its popover sit directly in
    // whatever row the consumer lays out.
    host: { class: 'contents' }
})
export class DotLanguageFilterComponent implements OnInit {
    readonly #dotLanguagesService = inject(DotLanguagesService);

    /**
     * Currently selected language ids, owned by the host.
     * @type {number[]}
     * @alias selectedLanguageIds
     */
    readonly $selectedLanguageIds = input<number[]>([], { alias: 'selectedLanguageIds' });

    /**
     * i18n key for the chip title.
     * @type {string}
     * @alias title
     */
    readonly $title = input('content-drive.language-selector.placeholder', { alias: 'title' });

    /**
     * Whether the chip offers its "remove" X.
     *
     * Owned by the host because only the host knows whether the current selection is meaningful to
     * clear: Content Drive seeds the environment's default language, and removing that selection
     * simply re-seeds the same value, so the X would do nothing visible. This component has no
     * concept of a default, so it does not try to infer it.
     * @alias removable
     */
    readonly $removable = input<boolean>(true, { alias: 'removable' });

    /** Emits the full selection on every change. An empty array means "no locale filter". */
    readonly selectionChange = output<number[]>();

    /**
     * Working copy of the selection so the listbox can two-way bind. Re-seeds whenever the host
     * pushes a different set (URL restore, "clear all").
     */
    readonly $selectedLanguages = linkedSignal(() => this.$selectedLanguageIds() ?? []);

    readonly $state = signalState<{ languages: DotLanguage[] }>({
        languages: []
    });

    protected readonly LISTBOX_SCROLL_HEIGHT = CHIP_FILTER_SCROLL_HEIGHT;
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
        this.selectionChange.emit(this.$selectedLanguages() ?? []);
    }

    onRemoveAll() {
        this.$selectedLanguages.set([]);
        this.onChange();
    }
}
