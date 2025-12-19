import { AsyncPipe, NgClass } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
    inject,
    input,
    computed,
    untracked
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { Listbox, ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { ListboxChangeEvent } from 'primeng/types/listbox';

import { map } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-ema-language-selector',
    imports: [PopoverModule, ListboxModule, ButtonModule, AsyncPipe, NgClass],
    templateUrl: './edit-ema-language-selector.component.html',
    styleUrls: ['./edit-ema-language-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaLanguageSelectorComponent implements AfterViewInit {
    @ViewChild('listbox') listbox: Listbox;
    @Output() selected: EventEmitter<number> = new EventEmitter();

    language = input<DotLanguage>();

    selectedLanguage = computed(() => {
        const selected = {
            ...this.language(),
            label: this.createLanguageLabel(this.language())
        };
        // This method internally set a signal. We need to use untracked to avoid unnecessary updates
        untracked(() => this.listbox?.updateModel(selected, null));

        return selected;
    });

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

    ngAfterViewInit(): void {
        this.listbox.updateModel(this.selectedLanguage(), null);
    }

    /**
     * Handle the change of the language
     *
     * @param {ListboxChangeEvent} { value }
     * @memberof EmaLanguageSelectorComponent
     */
    onChange({ value }: ListboxChangeEvent) {
        this.selected.emit(value.id);
        // Note: updateModel is not called here as the computed signal handles it via untracked
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
