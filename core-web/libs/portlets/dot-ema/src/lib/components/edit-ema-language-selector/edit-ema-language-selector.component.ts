import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

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
export class EmaLanguageSelectorComponent implements OnInit {
    @Output() languageSelected: EventEmitter<number> = new EventEmitter();
    @Input() language: DotLanguage;

    languages: DotLanguageWithLabel[] = [];

    get selectedLanguage() {
        return {
            ...this.language,
            label: this.language.countryCode.trim().length
                ? `${this.language.language} - ${this.language.countryCode}`
                : this.language.language
        };
    }

    private languagesService = inject(DotLanguagesService);

    ngOnInit(): void {
        this.languagesService.get().subscribe((languages) => {
            this.languages = languages.map((lang) => ({
                ...lang,
                label: lang.countryCode.trim().length
                    ? `${lang.language} - ${lang.countryCode}`
                    : lang.language
            }));
        });
    }

    /**
     * Set the selected language in the store
     *
     * @param {{ event: Event; value:DotLanguageWithLabel }} { value }
     * @memberof EmaLanguageSelectorComponent
     */
    onChange({ value }: { event: Event; value: DotLanguageWithLabel }) {
        this.languageSelected.emit(value.id);
    }
}
