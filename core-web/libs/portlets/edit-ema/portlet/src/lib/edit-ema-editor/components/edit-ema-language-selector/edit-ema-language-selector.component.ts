import { AsyncPipe, NgClass } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    ViewChild,
    inject
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { Listbox, ListboxChangeEvent, ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { map } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-ema-language-selector',
    standalone: true,
    imports: [OverlayPanelModule, ListboxModule, ButtonModule, AsyncPipe, NgClass],
    templateUrl: './edit-ema-language-selector.component.html',
    styleUrls: ['./edit-ema-language-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaLanguageSelectorComponent implements AfterViewInit, OnChanges {
    @ViewChild('listbox') listbox: Listbox;
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

    ngOnChanges(): void {
        // To select the correct language when the page is reloaded with no queryParams
        if (this.listbox) {
            this.listbox.writeValue(this.selectedLanguage);
        }
    }

    ngAfterViewInit(): void {
        this.listbox.writeValue(this.selectedLanguage);
    }

    /**
     * Handle the change of the language
     *
     * @param {ListboxChangeEvent} { value }
     * @memberof EmaLanguageSelectorComponent
     */
    onChange({ value }: ListboxChangeEvent) {
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
