import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    EventEmitter,
    inject,
    Output,
    viewChild,
    Signal,
    signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DatePickerModule } from 'primeng/datepicker';
import { PopoverModule } from 'primeng/popover';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { map } from 'rxjs/operators';

import { DotDevicesService, DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotLanguage, DotDeviceListItem } from '@dotcms/dotcms-models';
import { DotCMSPage, DotCMSURLContentMap, DotCMSViewAsPersona, UVE_MODE } from '@dotcms/types';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditorModeSelectorComponent } from './components/dot-editor-mode-selector/dot-editor-mode-selector.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from './components/dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotToggleLockButtonComponent } from './components/dot-toggle-lock-button/dot-toggle-lock-button.component';
import { DotUveDeviceSelectorComponent } from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';

import { DEFAULT_DEVICES, DEFAULT_PERSONA, PERSONA_KEY } from '../../../shared/consts';
import { UVEStore } from '../../../store/dot-uve.store';
import { convertLocalTimeToUTC, convertUTCToLocalTime, createFullURL } from '../../../utils';

@Component({
    selector: 'dot-uve-toolbar',
    imports: [
        NgClass,
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        DatePickerModule,
        ChipModule,
        ClipboardModule,
        PopoverModule,
        ToolbarModule,
        TooltipModule,
        SplitButtonModule,
        DotMessagePipe,
        DotEditorModeSelectorComponent,
        DotEmaBookmarksComponent,
        DotEmaInfoDisplayComponent,
        DotEmaRunningExperimentComponent,
        DotToggleLockButtonComponent,
        DotUveDeviceSelectorComponent,
        DotUveWorkflowActionsComponent,
        EditEmaLanguageSelectorComponent,
        EditEmaPersonaSelectorComponent
    ],
    providers: [DotPersonalizeService, DotDevicesService],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    $personaSelector = viewChild<EditEmaPersonaSelectorComponent>('personaSelector');
    $languageSelector = viewChild<EditEmaLanguageSelectorComponent>('languageSelector');

    @Output() translatePage = new EventEmitter<{ page: DotCMSPage; newLanguage: number }>();
    @Output() editUrlContentMap = new EventEmitter<DotCMSURLContentMap>();

    readonly #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);
    readonly #deviceService = inject(DotDevicesService);

    readonly $toolbar = this.#store.$uveToolbar;
    readonly $showWorkflowActions = this.#store.$showWorkflowsActions;
    readonly $isEditMode = this.#store.$isEditMode;
    readonly $isPreviewMode = this.#store.$isPreviewMode;
    readonly $isLiveMode = this.#store.$isLiveMode;
    readonly $apiURL = this.#store.$apiURL;
    readonly $personaSelectorProps = this.#store.$personaSelector;
    readonly $infoDisplayProps = this.#store.$infoDisplayProps;
    readonly $unlockButton = this.#store.$unlockButton;
    readonly $socialMedia = this.#store.socialMedia;
    readonly $urlContentMap = this.#store.$urlContentMap;
    readonly $isPaletteOpen = this.#store.palette.open;
    readonly $canEditPage = this.#store.$canEditPage;

    readonly $devices: Signal<DotDeviceListItem[]> = toSignal(
        this.#deviceService.get().pipe(map((devices = []) => [...DEFAULT_DEVICES, ...devices])),
        {
            initialValue: null
        }
    );

    protected readonly $pageParams = this.#store.pageParams;
    protected readonly $previewDate = computed<Date>(() => {
        const publishDate = this.$pageParams().publishDate;
        const previewDate = publishDate ? convertUTCToLocalTime(new Date(publishDate)) : new Date();

        return previewDate;
    });

    readonly $pageURLS: Signal<{ label: string; value: string }[]> = computed(() => {
        const params = this.$pageParams();
        const siteId = this.#store.pageAPIResponse()?.site?.identifier;
        const host = params.clientHost || window.location.origin;
        const path = params.url?.replace(/\/index(\.html)?$/, '') || '/';

        return [
            {
                label: 'uve.toolbar.page.live.url',
                value: new URL(path, host).toString()
            },
            {
                label: 'uve.toolbar.page.current.view.url',
                value: createFullURL(params, siteId)
            }
        ];
    });

    readonly $pageInode = computed(() => {
        return this.#store.pageAPIResponse()?.page.inode;
    });

    readonly $actions = this.#store.workflowLoading;
    readonly $workflowLoding = this.#store.workflowLoading;

    protected defaultDevices = DEFAULT_DEVICES;
    protected $MIN_DATE = signal(this.#getMinDate());

    /**
     * Fetch the page on a given date
     * @param {Date} publishDate
     * @memberof DotUveToolbarComponent
     */
    protected fetchPageOnDate(publishDate: Date = new Date()) {
        const publishDateUTC = convertLocalTimeToUTC(publishDate);

        this.#store.trackUVECalendarChange({ selectedDate: publishDateUTC });

        this.#store.loadPageAsset({
            mode: UVE_MODE.LIVE,
            publishDate: publishDateUTC
        });
    }

    protected togglePalette(): void {
        this.#store.setPaletteOpen(!this.$isPaletteOpen());
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
     * @param {DotCMSViewAsPersona} persona
     * @memberof DotEmaComponent
     */
    onPersonaSelected(persona: DotCMSViewAsPersona & { pageId: string }) {
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
     * @param {(DotCMSViewAsPersona & { pageId: string })} persona
     * @memberof EditEmaToolbarComponent
     */
    onDespersonalize(persona: DotCMSViewAsPersona & { pageId: string; selected: boolean }) {
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
    private createNewTranslation(language: DotLanguage, page: DotCMSPage): void {
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
                this.$languageSelector()?.resetModel(this.$toolbar().currentLanguage);
            }
        });
    }

    /**
     * Gets the minimum allowed date for the calendar component.
     * Sets hours/minutes/seconds/milliseconds to 0 to avoid collisions with preview date
     * when initializing, which would cause the input to be empty.
     *
     * @returns {Date} The minimum allowed date with time set to midnight
     * @private
     */
    #getMinDate() {
        const currentDate = new Date();

        // We need to set this to 0 so the minDate does not collide with the previewDate value when we are initializing
        // This prevents the input from being empty on init
        currentDate.setHours(0, 0, 0, 0);

        return currentDate;
    }
}
