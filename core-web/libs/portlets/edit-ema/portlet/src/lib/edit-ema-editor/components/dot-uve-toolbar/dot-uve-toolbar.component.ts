import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    EventEmitter,
    inject,
    Output,
    viewChild
} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { ChipModule } from 'primeng/chip';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotPersona, DotLanguage } from '@dotcms/dotcms-models';

import { DEFAULT_PERSONA } from '../../../shared/consts';
import { DotPage } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { DotEditEmaWorkflowActionsComponent } from '../dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaLanguageSelectorComponent } from '../edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from '../edit-ema-persona-selector/edit-ema-persona-selector.component';

@Component({
    selector: 'dot-uve-toolbar',
    standalone: true,
    imports: [
        NgClass,
        NgTemplateOutlet,
        ButtonModule,
        ToolbarModule,
        DotEmaBookmarksComponent,
        DotEmaInfoDisplayComponent,
        DotEmaRunningExperimentComponent,
        DotEditEmaWorkflowActionsComponent,
        ClipboardModule,
        CalendarModule,
        SplitButtonModule,
        FormsModule,
        ReactiveFormsModule,
        EditEmaPersonaSelectorComponent,
        EditEmaLanguageSelectorComponent,
        ChipModule
    ],
    providers: [DotPersonalizeService],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    // `viewChild` signal is not supported by ng-mocks. Ticket: https://github.com/help-me-mom/ng-mocks/issues/8634
    $personaSelector = viewChild<EditEmaPersonaSelectorComponent>('personaSelector');
    $languageSelector = viewChild<EditEmaLanguageSelectorComponent>('languageSelector');

    @Output() translatePage = new EventEmitter<{ page: DotPage; newLanguage: number }>();

    readonly #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);

    readonly $toolbar = this.#store.$uveToolbar;
    readonly $isPreviewMode = this.#store.$isPreviewMode;
    readonly $apiURL = this.#store.$apiURL;
    readonly $personaSelectorProps = this.#store.$personaSelector;

    readonly $styleToolbarClass = computed(() => {
        if (!this.$isPreviewMode()) {
            return 'uve-toolbar';
        }

        return 'uve-toolbar uve-toolbar-preview';
    });

    readonly $pageInode = computed(() => {
        return this.#store.pageAPIResponse()?.page.inode;
    });

    readonly $actions = this.#store.$isWorkflowLoading;
    readonly $workflowLoding = this.#store.workflowLoading;

    protected readonly date = new Date();

    /**
     * Set the preview mode
     *
     * @memberof DotUveToolbarComponent
     */
    protected setPreviewMode() {
        this.#store.loadPageAsset({ preview: 'true' });
    }

    /**
     * Set the edit mode
     *
     * @memberof DotUveToolbarComponent
     */
    protected setEditMode() {
        this.#store.loadPageAsset({ preview: null });
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
     * Handle the persona selection
     *
     * @param {DotPersona} persona
     * @memberof DotEmaComponent
     */
    onPersonaSelected(persona: DotPersona & { pageId: string }) {
        if (persona.identifier === DEFAULT_PERSONA.identifier || persona.personalized) {
            this.#store.loadPageAsset({
                'com.dotmarketing.persona.id': persona.identifier
            });
        } else {
            this.#confirmationService.confirm({
                header: this.#dotMessageService.get('editpage.personalization.confirm.header'),
                message: this.#dotMessageService.get(
                    'editpage.personalization.confirm.message',
                    persona.name
                ),
                acceptLabel: this.#dotMessageService.get('dot.common.dialog.accept'),
                rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
                accept: () => {
                    this.#personalizeService
                        .personalized(persona.pageId, persona.keyTag)
                        .subscribe(() => {
                            this.#store.loadPageAsset({
                                'com.dotmarketing.persona.id': persona.identifier
                            });

                            this.$personaSelector().fetchPersonas();
                        }); // This does a take 1 under the hood
                },
                reject: () => {
                    this.$personaSelector().resetValue();
                }
            });
        }
    }

    /**
     * Handle the persona despersonalization
     *
     * @param {(DotPersona & { pageId: string })} persona
     * @memberof EditEmaToolbarComponent
     */
    onDespersonalize(persona: DotPersona & { pageId: string; selected: boolean }) {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('editpage.personalization.delete.confirm.header'),
            message: this.#dotMessageService.get(
                'editpage.personalization.delete.confirm.message',
                persona.name
            ),
            acceptLabel: this.#dotMessageService.get('dot.common.dialog.accept'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            accept: () => {
                this.#personalizeService
                    .despersonalized(persona.pageId, persona.keyTag)
                    .subscribe(() => {
                        this.$personaSelector().fetchPersonas();

                        if (persona.selected) {
                            this.#store.loadPageAsset({
                                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                            });
                        }
                    }); // This does a take 1 under the hood
            }
        });
    }

    /*
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
                this.$languageSelector().listbox.writeValue(this.$toolbar().currentLanguage);
            }
        });
    }
}
