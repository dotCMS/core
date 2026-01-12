import { AsyncPipe, NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    inject,
    input,
    computed,
    effect,
    model
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { ListboxChangeEvent } from 'primeng/types/listbox';

import { map } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

interface DotLanguageWithLabel extends DotLanguage {
    label: string;
}

@Component({
    selector: 'dot-edit-ema-language-selector',
    imports: [PopoverModule, ListboxModule, ButtonModule, AsyncPipe, NgClass, FormsModule],
    templateUrl: './edit-ema-language-selector.component.html',
    styleUrls: ['./edit-ema-language-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaLanguageSelectorComponent {
    @Output() selected: EventEmitter<number> = new EventEmitter();

    language = input<DotLanguage>();

    // Model signal for two-way binding with listbox
    selectedLanguageModel = model<DotLanguageWithLabel | null>(null);

    selectedLanguage = computed(() => {
        const lang = this.language();
        if (!lang) return null;

        return {
            ...lang,
            label: this.createLanguageLabel(lang)
        };
    });

    /**
     * Popover PT to match PrimeNG default padded popover look (the `edit-ema-selector`
     * styleClass is used elsewhere to *remove* padding globally).
     */
    readonly languagePopoverPt = {
        content: { class: '!p-2' }
    };

    readonly languageListboxPt = {
        root: { class: '!border-0 !shadow-none' }
    };

    constructor() {
        // Sync input signal to model signal when language changes from parent
        effect(() => {
            const lang = this.language();
            if (lang) {
                const currentModel = this.selectedLanguageModel();
                // Only update if the language actually changed to avoid loops
                if (!currentModel || currentModel.id !== lang.id) {
                    this.selectedLanguageModel.set({
                        ...lang,
                        label: this.createLanguageLabel(lang)
                    });
                }
            }
        });
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
     * Handle the change of the language
     *
     * @param {ListboxChangeEvent} { value }
     * @memberof EmaLanguageSelectorComponent
     */
    onChange({ value }: ListboxChangeEvent) {
        // Emit to parent when user selects a language
        // Note: ngModel already updates selectedLanguageModel automatically
        this.selected.emit(value.id);
    }

    /**
     * Reset the model to a specific language
     * Used by parent component to reset the selector when needed
     *
     * @param {DotLanguage} lang - The language to reset to
     * @memberof EmaLanguageSelectorComponent
     */
    resetModel(lang: DotLanguage): void {
        this.selectedLanguageModel.set({
            ...lang,
            label: this.createLanguageLabel(lang)
        });
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
