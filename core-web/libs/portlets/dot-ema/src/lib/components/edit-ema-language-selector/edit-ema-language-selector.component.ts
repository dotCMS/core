import { combineLatest } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

import { EditEmaStore } from '../../feature/store/dot-ema.store';

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
    selectedLanguage: DotLanguageWithLabel;
    languages: DotLanguageWithLabel[] = [];

    private store = inject(EditEmaStore);
    private languagesService = inject(DotLanguagesService);

    ngOnInit(): void {
        combineLatest([this.languagesService.get(), this.store.language_id$]).subscribe(
            ([languages, language_id]) => {
                this.languages = languages.map((lang) => ({
                    ...lang,
                    label: lang.countryCode.trim().length
                        ? `${lang.language} - ${lang.countryCode}`
                        : lang.language
                }));

                this.selectedLanguage = this.languages.find(
                    (lang) => lang.id == Number(language_id)
                );
            }
        );
    }

    /**
     * Set the selected language in the store
     *
     * @param {{ event: Event; value:DotLanguageWithLabel }} { value }
     * @memberof EmaLanguageSelectorComponent
     */
    onChange({ value }: { event: Event; value: DotLanguageWithLabel }) {
        this.selectedLanguage = value;
        this.store.setLanguage(value.id.toString());
    }
}
