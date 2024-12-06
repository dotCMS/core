import { ClipboardModule } from '@angular/cdk/clipboard';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    inject,
    Output,
    viewChild
} from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

import { DotPage } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaLanguageSelectorComponent } from '../edit-ema-language-selector/edit-ema-language-selector.component';

@Component({
    selector: 'dot-uve-toolbar',
    standalone: true,
    imports: [
        ButtonModule,
        ToolbarModule,
        DotEmaBookmarksComponent,
        DotEmaInfoDisplayComponent,
        DotEmaRunningExperimentComponent,
        ClipboardModule,
        EditEmaLanguageSelectorComponent
    ],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    languageSelector = viewChild<EditEmaLanguageSelectorComponent>('languageSelector');
    #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);

    readonly $toolbar = this.#store.$uveToolbar;
    readonly $apiURL = this.#store.$apiURL;

    @Output() translatePage = new EventEmitter<{ page: DotPage; newLanguage: number }>();

    togglePreviewMode(preview: boolean) {
        this.#store.togglePreviewMode(preview);
    }

    /**
     * Handle the language selection
     *
     * @param {number} language
     * @memberof DotEmaComponent
     */
    onLanguageSelected(language: number) {
        const language_id = language.toString();

        const languages = this.#store.languages();
        const currentLanguage = languages.find((lang) => lang.id === language);

        const languageHasTranslation = languages.find(
            (lang) => lang.id.toString() === language_id
        )?.translated;

        if (!languageHasTranslation) {
            // Show confirmation dialog to create a new translation
            this.createNewTranslation(currentLanguage, this.#store.pageAPIResponse()?.page);

            return;
        }

        this.#store.loadPageAsset({ language_id });
    }

    triggerCopyToast() {
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('Copied'),
            life: 3000
        });
    }

    /**
     * Asks the user for confirmation to create a new translation for a given language.
     *
     * @param {DotLanguage} language - The language to create a new translation for.
     * @private
     *
     * @return {void}
     */
    private createNewTranslation(language: DotLanguage, page: DotPage): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.header'
            ),
            message: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.message',
                language.language
            ),
            rejectIcon: 'hidden',
            acceptIcon: 'hidden',
            key: 'shell-confirm-dialog',
            accept: () => {
                this.translatePage.emit({
                    page: page,
                    newLanguage: language.id
                });
            },
            reject: () => {
                // If is rejected, bring back the current language on selector
                this.languageSelector().listbox.writeValue(this.$toolbar().currentLanguage);
            }
        });
    }
}
