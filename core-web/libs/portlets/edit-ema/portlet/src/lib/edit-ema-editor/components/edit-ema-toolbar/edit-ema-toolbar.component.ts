import { tapResponse } from '@ngrx/operators';

import { ClipboardModule } from '@angular/cdk/clipboard';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { Params } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import {
    DotContentletLockerService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotDevice, DotLanguage, DotPersona } from '@dotcms/dotcms-models';
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_PERSONA } from '../../../shared/consts';
import { DotPage } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { compareUrlPaths } from '../../../utils';
import { DotEmaBookmarksComponent } from '../dot-uve-toolbar/components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-uve-toolbar/components/dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-uve-toolbar/components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaLanguageSelectorComponent } from '../dot-uve-toolbar/components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from '../dot-uve-toolbar/components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { DotUveWorkflowActionsComponent } from '../dot-uve-workflow-actions/dot-uve-workflow-actions.component';

@Component({
    selector: 'dot-edit-ema-toolbar',
    standalone: true,
    imports: [
        MenuModule,
        ButtonModule,
        ToolbarModule,
        DotDeviceSelectorSeoComponent,
        DotMessagePipe,
        DotEmaBookmarksComponent,
        DotEmaRunningExperimentComponent,
        EditEmaPersonaSelectorComponent,
        EditEmaLanguageSelectorComponent,
        DotEmaInfoDisplayComponent,
        DotUveWorkflowActionsComponent,
        ClipboardModule
    ],
    providers: [DotPersonalizeService],
    templateUrl: './edit-ema-toolbar.component.html',
    styleUrls: ['./edit-ema-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaToolbarComponent {
    @Output() readonly translatePage = new EventEmitter<{ page: DotPage; newLanguage: number }>();
    @Output() readonly editUrlContentMap = new EventEmitter<DotCMSContentlet>();

    @ViewChild('personaSelector')
    personaSelector!: EditEmaPersonaSelectorComponent;

    @ViewChild('languageSelector')
    languageSelector!: EditEmaLanguageSelectorComponent;

    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotContentletLockerService = inject(DotContentletLockerService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);

    readonly uveStore = inject(UVEStore);

    protected readonly $toolbarProps = this.uveStore.$toolbarProps;

    /**
     * Update the current device
     *
     * @param {DotDevice} [device]
     * @memberof EditEmaToolbarComponent
     */
    updateCurrentDevice(device: DotDevice & { icon?: string }) {
        this.uveStore.setDevice(device);
    }

    /**
     * Preview RRSS media cards
     *
     * @param {string} seoMedia
     * @memberof EditEmaToolbarComponent
     */
    onSeoMediaChange(seoMedia: string) {
        this.uveStore.setSocialMedia(seoMedia);
    }

    /**
     * Handle the copy URL action
     *
     * @memberof EditEmaToolbarComponent
     */
    triggerCopyToast() {
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('Copied'),
            life: 3000
        });
    }

    /**
     * Handle the language selection
     *
     * @param {number} language
     * @memberof DotEmaComponent
     */
    onLanguageSelected(language: number) {
        const language_id = language.toString();

        const languages = this.uveStore?.languages();
        const currentLanguage = languages.find((lang) => lang.id === language);

        const languageHasTranslation = languages.find(
            (lang) => lang.id.toString() === language_id
        )?.translated;

        if (!languageHasTranslation) {
            // Show confirmation dialog to create a new translation
            this.createNewTranslation(currentLanguage, this.uveStore?.pageAPIResponse()?.page);

            return;
        }

        this.uveStore.loadPageAsset({ language_id });
    }

    /**
     * Handle the persona selection
     *
     * @param {DotPersona} persona
     * @memberof DotEmaComponent
     */
    onPersonaSelected(persona: DotPersona & { pageId: string }) {
        if (persona.identifier === DEFAULT_PERSONA.identifier || persona.personalized) {
            this.uveStore.loadPageAsset({
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
                            this.uveStore.loadPageAsset({
                                'com.dotmarketing.persona.id': persona.identifier
                            });

                            this.personaSelector.fetchPersonas();
                        }); // This does a take 1 under the hood
                },
                reject: () => {
                    this.personaSelector.resetValue();
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
                        this.personaSelector.fetchPersonas();

                        if (persona.selected) {
                            this.uveStore.loadPageAsset({
                                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                            });
                        }
                    }); // This does a take 1 under the hood
            }
        });
    }

    /**
     * Handle a new page event. This event is triggered when the page changes for a Workflow Action
     * Update the query params if the url or the language id changed
     *
     * @param {DotCMSContentlet} page
     * @memberof EditEmaToolbarComponent
     */
    handleNewPage(page: DotCMSContentlet): void {
        const { pageURI, url, languageId } = page;
        const params = {
            url: pageURI ?? url,
            language_id: languageId?.toString()
        };

        if (this.shouldNavigateToNewPage(params)) {
            this.uveStore.loadPageAsset(params);

            return;
        }

        this.uveStore.reloadCurrentPage();
    }

    /**
     * Unlocks a page with the specified inode.
     *
     * @param {string} inode
     * @memberof EditEmaToolbarComponent
     */
    unlockPage(inode: string) {
        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('edit.ema.page.unlock'),
            detail: this.#dotMessageService.get('edit.ema.page.is.being.unlocked')
        });

        this.#dotContentletLockerService
            .unlock(inode)
            .pipe(
                tapResponse({
                    next: () => {
                        this.#messageService.add({
                            severity: 'success',
                            summary: this.#dotMessageService.get('edit.ema.page.unlock'),
                            detail: this.#dotMessageService.get('edit.ema.page.unlock.success')
                        });
                    },
                    error: () => {
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get('edit.ema.page.unlock'),
                            detail: this.#dotMessageService.get('edit.ema.page.unlock.error')
                        });
                    }
                })
            )
            .subscribe(() => this.uveStore.reloadCurrentPage());
    }

    /**
     * Determines whether navigation to a new page is necessary based on URL and language changes.
     *
     * @param {Params} params - The incoming parameters, including a new URL and language ID.
     * @returns {boolean} - True if navigation to a new page is needed.
     */
    private shouldNavigateToNewPage(params: Params): boolean {
        const { url: newUrl, language_id: newLanguageId } = params;
        const { url: currentUrl, language_id: currentLanguageId } = this.uveStore.pageParams();

        // Determine the target URL, prioritizing the content map URL if available
        const urlContentMap = this.uveStore.pageAPIResponse().urlContentMap;
        const targetUrl = urlContentMap?.URL_MAP_FOR_CONTENT || newUrl;

        // Return true if the URL paths are different or the language has changed
        return !compareUrlPaths(currentUrl, targetUrl) || newLanguageId != currentLanguageId;
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
                this.languageSelector?.listbox.writeValue(this.$toolbarProps().currentLanguage);
            }
        });
    }
}
