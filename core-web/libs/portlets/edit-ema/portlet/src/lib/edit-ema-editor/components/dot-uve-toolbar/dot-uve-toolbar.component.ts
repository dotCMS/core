import { tapResponse } from '@ngrx/operators';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    EventEmitter,
    inject,
    Output,
    viewChild,
    Signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { ChipModule } from 'primeng/chip';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { map } from 'rxjs/operators';

import {
    DotContentletLockerService,
    DotDevicesService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import {
    DotPersona,
    DotLanguage,
    DotDeviceListItem,
    DotCMSContentlet
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { UVE_MODE } from '@dotcms/uve/types';

import { DotEditorModeSelectorComponent } from './components/dot-editor-mode-selector/dot-editor-mode-selector.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from './components/dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveDeviceSelectorComponent } from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';

import { DEFAULT_DEVICES, DEFAULT_PERSONA, PERSONA_KEY } from '../../../shared/consts';
import { DotPage } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';

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
        ClipboardModule,
        CalendarModule,
        SplitButtonModule,
        FormsModule,
        ReactiveFormsModule,
        EditEmaPersonaSelectorComponent,
        EditEmaLanguageSelectorComponent,
        ClipboardModule,
        DotUveDeviceSelectorComponent,
        DotMessagePipe,
        DotUveWorkflowActionsComponent,
        ChipModule,
        DotEditorModeSelectorComponent
    ],
    providers: [DotPersonalizeService, DotDevicesService],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    $personaSelector = viewChild<EditEmaPersonaSelectorComponent>('personaSelector');
    $languageSelector = viewChild<EditEmaLanguageSelectorComponent>('languageSelector');

    @Output() translatePage = new EventEmitter<{ page: DotPage; newLanguage: number }>();
    @Output() editUrlContentMap = new EventEmitter<DotCMSContentlet>();

    readonly #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);
    readonly #deviceService = inject(DotDevicesService);
    readonly #dotContentletLockerService = inject(DotContentletLockerService);

    readonly $toolbar = this.#store.$uveToolbar;
    readonly $showWorkflowActions = this.#store.$showWorkflowsActions;
    readonly $isPreviewMode = this.#store.$isPreviewMode;
    readonly $isLiveMode = this.#store.$isLiveMode;
    readonly $apiURL = this.#store.$apiURL;
    readonly $personaSelectorProps = this.#store.$personaSelector;
    readonly $infoDisplayProps = this.#store.$infoDisplayProps;
    readonly $unlockButton = this.#store.$unlockButton;
    readonly $socialMedia = this.#store.socialMedia;
    readonly $urlContentMap = this.#store.$urlContentMap;

    readonly $devices: Signal<DotDeviceListItem[]> = toSignal(
        this.#deviceService.get().pipe(map((devices = []) => [...DEFAULT_DEVICES, ...devices])),
        {
            initialValue: null
        }
    );

    protected readonly $pageParams = this.#store.pageParams;
    protected readonly $previewDate = computed<Date>(() => {
        return this.$pageParams().publishDate
            ? new Date(this.$pageParams().publishDate)
            : new Date();
    });

    readonly $pageInode = computed(() => {
        return this.#store.pageAPIResponse()?.page.inode;
    });

    readonly $actions = this.#store.workflowLoading;
    readonly $workflowLoding = this.#store.workflowLoading;

    defaultDevices = DEFAULT_DEVICES;
    get MIN_DATE() {
        const currentDate = new Date();

        // We need to set this to 0 so the minDate does not collide with the previewDate value when we are initializing
        // This prevents the input from being empty on init
        currentDate.setHours(0, 0, 0, 0);

        return currentDate;
    }

    /**
     * Fetch the page on a given date
     * @param {Date} publishDate
     * @memberof DotUveToolbarComponent
     */
    protected fetchPageOnDate(publishDate: Date = new Date()) {
        this.#store.loadPageAsset({
            mode: UVE_MODE.LIVE,
            publishDate: publishDate?.toISOString()
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

    /**
     * Trigger the copy toasts
     *
     * @memberof DotUveToolbarComponent
     */
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
        const existPersona =
            persona.identifier === DEFAULT_PERSONA.identifier || persona.personalized;

        if (existPersona) {
            this.#store.loadPageAsset({ [PERSONA_KEY]: persona.identifier });

            return;
        }

        const confirmationData = {
            header: this.#dotMessageService.get('editpage.personalization.confirm.header'),
            message: this.#dotMessageService.get(
                'editpage.personalization.confirm.message',
                persona.name
            ),
            acceptLabel: this.#dotMessageService.get('dot.common.dialog.accept'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject')
        };

        this.#confirmationService.confirm({
            ...confirmationData,
            accept: () => {
                this.#personalizeService.personalized(persona.pageId, persona.keyTag).subscribe({
                    next: () => {
                        this.#store.loadPageAsset({ [PERSONA_KEY]: persona.identifier });
                        this.$personaSelector().fetchPersonas();
                    },
                    error: () => {
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get('error'),
                            detail: this.#dotMessageService.get('uve.personalize.empty.page.error')
                        });

                        this.$personaSelector().resetValue();
                    }
                });
            },
            reject: () => {
                this.$personaSelector().resetValue();
            }
        });
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
                                [PERSONA_KEY]: DEFAULT_PERSONA.identifier
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
            .subscribe(() => this.#store.reloadCurrentPage());
    }
}
