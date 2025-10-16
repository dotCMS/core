import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnInit,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-uve-language-selector',
    imports: [OverlayPanelModule, ListboxModule, ButtonModule],
    templateUrl: './dot-uve-language-selector.component.html',
    styleUrl: './dot-uve-language-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUVELanguageSelectorComponent implements OnInit {
    readonly #languageService = inject(DotLanguagesService);
    readonly #store = inject(UVEStore);

    readonly $currentLanguage = this.#store.$currentLanguage;
    readonly $pageLanguages = this.#store.pageLanguages;
    readonly $systemLanguages = signal<DotLanguage[]>([]);
    readonly $listOptions = computed(() => {
        return this.$systemLanguages().map((lang) => ({
            ...lang,
            label: this.getLanguageLabel(lang)
        }));
    });

    ngOnInit(): void {
        this.#languageService.get().subscribe((languages) => {
            this.$systemLanguages.set(languages);
        });
    }

    /**
     * Create the label for the language
     *
     * @private
     * @param {DotLanguage} lang
     * @return {*}  {string}
     * @memberof DotUVELanguageSelectorComponent
     */
    protected getLanguageLabel(lang: DotLanguage | null): string {
        if (!lang) {
            return 'Loading...';
        }

        const countryCode = lang.countryCode?.trim();

        if (countryCode) {
            return `${lang.language} - ${countryCode}`;
        }

        return lang.language;
    }

    /**
     * Handle the selection of a language
     *
     * @param {DotLanguage} language
     * @memberof DotUVELanguageSelectorComponent
     */
    protected onSelect(_language: DotLanguage) {
        // const isPageTranslated = this.$pageLanguages().find((lang) => lang.id === language.id);
        // Do the translation here
    }
}
