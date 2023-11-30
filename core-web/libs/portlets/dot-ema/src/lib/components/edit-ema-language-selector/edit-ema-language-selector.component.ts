import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { map } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

interface DotLanguageWithLabel extends DotLanguage {
    label: string;
}

@Component({
    selector: 'dot-edit-ema-language-selector',
    standalone: true,
    imports: [CommonModule, OverlayPanelModule, ListboxModule, ButtonModule],
    templateUrl: './edit-ema-language-selector.component.html',
    styleUrls: ['./edit-ema-language-selector.component.scss']
})
export class EmaLanguageSelectorComponent {
    @Output() selected: EventEmitter<number> = new EventEmitter();
    @Input() language: DotLanguage;

    get selectedLanguage() {
        return {
            ...this.language,
            label: this.createLanguageLabel(this.language)
        };
    }

    languages$ = inject(DotLanguagesService)
        .get()
        .pipe(
            map((languages) =>
                languages.map((lang) => ({
                    ...lang,
                    label: this.createLanguageLabel(lang)
                }))
            )
        );

    /**
     * Set the selected language in the store
     *
     * @param {{ event: Event; value:DotLanguageWithLabel }} { value }
     * @memberof EmaLanguageSelectorComponent
     */
    onChange({ value }: { event: Event; value: DotLanguageWithLabel }) {
        this.selected.emit(value.id);
    }

    /**
     * Create the label for the language
     *
     * @private
     * @param {DotLanguage} lang
     * @return {*}  {string}
     * @memberof EmaLanguageSelectorComponent
     */
    private createLanguageLabel(lang: DotLanguage): string {
        return lang.countryCode.trim().length
            ? `${lang.language} - ${lang.countryCode}`
            : lang.language;
    }
}
