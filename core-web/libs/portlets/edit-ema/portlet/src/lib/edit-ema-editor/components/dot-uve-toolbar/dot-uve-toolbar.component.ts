import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    Signal,
    computed,
    inject,
    output,
    signal,
    viewChild
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

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
import { DotDeviceListItem, DotExperimentStatus, DotLanguage } from '@dotcms/dotcms-models';
import { DotCMSPage, DotCMSURLContentMap, DotCMSViewAsPersona, UVE_MODE } from '@dotcms/types';
import { DotLanguageSelectorComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditorModeSelectorComponent } from './components/dot-editor-mode-selector/dot-editor-mode-selector.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from './components/dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotToggleLockButtonComponent } from './components/dot-toggle-lock-button/dot-toggle-lock-button.component';
import {
    DeviceSelectorChange,
    DotUveDeviceSelectorComponent
} from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';

import { DEFAULT_DEVICES, DEFAULT_PERSONA, PERSONA_KEY } from '../../../shared/consts';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import {
    convertLocalTimeToUTC,
    convertUTCToLocalTime,
    createFavoritePagesURL
} from '../../../utils';

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
        EditEmaPersonaSelectorComponent,
        DotLanguageSelectorComponent
    ],
    providers: [DotPersonalizeService, DotDevicesService],
    templateUrl: './dot-uve-toolbar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    $personaSelector = viewChild<EditEmaPersonaSelectorComponent>('personaSelector');
    $languageSelector = viewChild<DotLanguageSelectorComponent>('languageSelector');

    translatePage = output<{ page: DotCMSPage; newLanguage: number }>();
    editUrlContentMap = output<DotCMSURLContentMap>();

    readonly #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);
    readonly #deviceService = inject(DotDevicesService);
    readonly #router = inject(Router);

    // Expose enum for template usage
    readonly UVE_MODE = UVE_MODE;

    // Component builds its own toolbar props locally
    protected readonly $bookmarksUrl = computed<string>(() => {
        const params = this.#store.pageParams();
        const site = this.#store.pageAsset()?.site;

        return createFavoritePagesURL({
            languageId: Number(params?.language_id),
            pageURI: params?.url,
            siteId: site?.identifier
        });
    });

    // Use store's pageLanguage instead of redefining it
    protected readonly $currentLanguage = this.#store.pageLanguage;

    protected readonly $runningExperiment = computed(() => {
        const experiment = this.#store.pageExperiment?.();
        const isExperimentRunning = experiment?.status === DotExperimentStatus.RUNNING;

        return isExperimentRunning ? experiment : null;
    });

    readonly $showWorkflowActions = this.#store.$showWorkflowsActions;
    readonly $mode = this.#store.viewMode;
    readonly $apiURL = this.#store.$apiURL;
    readonly $personaSelectorProps = this.#store.$personaSelector;
    readonly $infoDisplayProps = this.#store.$infoDisplayProps;
    readonly $socialMedia = this.#store.viewSocialMedia;
    readonly $urlContentMap = this.#store.$urlContentMap;
    readonly $isPaletteOpen = this.#store.editorPaletteOpen;
    readonly $canEditPage = this.#store.editorCanEditContent;

    /**
     * Popover passthrough styles for the "Copy URLs" popover.
     * Keeps the popover compact and prevents long URLs from stretching the overlay.
     */
    readonly copyUrlPopoverPt = {
        root: { class: 'w-full max-w-[25rem]' },
        content: { class: '!p-3' }
    };

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

    protected readonly $showDeviceSelector = computed(() => {
        const isEditMode = this.$pageParams().mode === UVE_MODE.EDIT;
        return !isEditMode && this.$devices()?.length > 0;
    });

    protected readonly $showUrlContentMap = computed(() => {
        const isEditMode = this.$pageParams().mode === UVE_MODE.EDIT;
        return isEditMode && this.$urlContentMap();
    });

    readonly $pageInode = computed(() => {
        return this.#store.pageAsset()?.page?.inode;
    });

    readonly $actions = this.#store.workflowIsLoading;
    readonly $workflowLoding = this.#store.workflowIsLoading;

    protected defaultDevices = DEFAULT_DEVICES;
    protected $MIN_DATE = signal(this.#getMinDate());

    // Computed properties for presentational children
    readonly isTraditionalPage = computed(() => this.#store.pageType() === PageType.TRADITIONAL);

    // Build unified device selector state
    readonly $deviceSelectorState = computed(() => {
        return {
            device: this.#store.viewDevice(),
            socialMedia: this.#store.viewSocialMedia(),
            orientation: this.#store.viewDeviceOrientation()
        };
    });

    // Build complete toggle lock options for presentational component
    readonly $lockOptions = computed(() => {
        if (!this.#store.$lockFeatureEnabled()) {
            return null;
        }

        const storeLockOptions = this.#store.$lockOptions();

        if (!storeLockOptions) {
            return null;
        }

        const loading = this.#store.workflowLockIsLoading();
        const disabled = !storeLockOptions.canLock;
        const message = storeLockOptions.canLock
            ? 'editpage.toolbar.page.release.lock.locked.by.user'
            : 'editpage.locked-by';
        const args = storeLockOptions.lockedBy ? [storeLockOptions.lockedBy] : [];

        return {
            inode: storeLockOptions.inode,
            isLocked: storeLockOptions.isLocked,
            isLockedByCurrentUser: storeLockOptions.isLockedByCurrentUser,
            canLock: storeLockOptions.canLock,
            loading,
            disabled,
            message,
            args
        };
    });

    /**
     * Fetch the page on a given date
     * @param {Date | string | number} publishDate - Date, ISO string, or timestamp (defaults to now)
     * @memberof DotUveToolbarComponent
     */
    protected fetchPageOnDate(publishDate: Date | string | number = new Date()) {
        let dateObj: Date;
        const asDate = publishDate as Date;
        const hasGetTime =
            typeof publishDate === 'object' &&
            publishDate !== null &&
            typeof (asDate as { getTime?: unknown }).getTime === 'function';
        if (hasGetTime) {
            const time = (asDate as Date).getTime();
            dateObj = Number.isFinite(time) ? new Date(time) : new Date();
        } else if (typeof publishDate === 'number' || typeof publishDate === 'string') {
            dateObj = new Date(publishDate);
        } else {
            dateObj = new Date();
        }
        if (Number.isNaN(dateObj.getTime())) {
            dateObj = new Date();
        }
        const publishDateUTC = convertLocalTimeToUTC(dateObj);

        this.#store.trackUVECalendarChange({ selectedDate: publishDateUTC });

        this.#store.pageLoad({
            mode: UVE_MODE.LIVE,
            publishDate: publishDateUTC
        });
    }

    protected togglePalette(): void {
        this.#store.setPaletteOpen(!this.$isPaletteOpen());
    }

    /**
     * Handle toggle lock event from presentational DotToggleLockButtonComponent
     * @param event Lock toggle event with inode and lock states
     */
    handleToggleLock(event: {
        inode: string;
        isLocked: boolean;
        isLockedByCurrentUser: boolean;
        lockedBy?: string;
    }) {
        this.#store.workflowToggleLock(
            event.inode,
            event.isLocked,
            event.isLockedByCurrentUser,
            event.lockedBy
        );
    }

    /**
     * Handle unified state change event from presentational DotUveDeviceSelectorComponent
     * Uses discriminated union to handle different types of changes type-safely
     * @param change Device selector state change event
     */
    handleDeviceSelectorChange(change: DeviceSelectorChange) {
        switch (change.type) {
            case 'device':
                this.#store.viewSetDevice(change.device);
                break;
            case 'socialMedia':
                this.#store.viewSetSEO(change.socialMedia);
                break;
            case 'orientation':
                this.#store.viewSetOrientation(change.orientation);
                break;
        }
    }

    /**
     * Handle info display action event from presentational DotEmaInfoDisplayComponent
     * @param optionId The ID of the action option (e.g., 'device', 'socialMedia', 'variant')
     */
    handleInfoDisplayAction(optionId: string) {
        if (optionId === 'device' || optionId === 'socialMedia') {
            this.#store.viewClearDeviceAndSocialMedia();

            return;
        }

        // Handle variant action - navigate to experiment configuration
        const currentExperiment = this.#store.pageExperiment();

        if (currentExperiment) {
            this.#router.navigate(
                [
                    '/edit-page/experiments/',
                    currentExperiment.pageId,
                    currentExperiment.id,
                    'configuration'
                ],
                {
                    queryParams: {
                        mode: null,
                        variantName: null,
                        experimentId: null
                    },
                    queryParamsHandling: 'merge'
                }
            );
        }
    }

    /**
     * Handle the language selection
     *
     * @param {number} language
     * @memberof DotEmaComponent
     */
    onLanguageSelected(language: DotLanguage) {
        const language_id = language.id.toString();
        const languages = this.#store.pageLanguages();

        // pageLanguages has the translated flag; fall back to the selector's language object
        // when this language has never been created for this page (not in pageLanguages yet)
        const currentLanguage = languages.find((lang) => lang.id === language.id) ?? language;
        const languageHasTranslation = currentLanguage.translated;

        if (!languageHasTranslation) {
            // Show confirmation dialog to create a new translation
            const page = this.#store.pageAsset()?.page;
            if (page) {
                this.createNewTranslation(currentLanguage, page);
            }

            return;
        }

        this.#store.pageLoad({ language_id });
    }

    /**
     * Trigger the copy toasts
     *
     * @memberof DotUveToolbarComponent
     */

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
            this.#store.pageLoad({ [PERSONA_KEY]: persona.identifier });

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
                        this.#store.pageLoad({ [PERSONA_KEY]: persona.identifier });
                        this.$personaSelector().fetchPersonas();
                    },
                    error: (err: unknown) => {
                        const detail =
                            (err instanceof HttpErrorResponse
                                ? this.#getPersonalizeErrorDetail(err)
                                : null) ??
                            this.#dotMessageService.get('uve.personalize.empty.page.error');
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get('error'),
                            detail
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
                            this.#store.pageLoad({
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
                this.$languageSelector()?.value.set(this.#store.pageLanguage());
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

    /**
     * Extracts a user- and support-friendly error detail from the personalization API error.
     * Uses backend message when available (header or body); returns null for generic i18n fallback.
     */
    #getPersonalizeErrorDetail(err: HttpErrorResponse): string | null {
        const headerMessage = err.headers?.get('error-message')?.trim();
        if (headerMessage) {
            return headerMessage;
        }
        const bodyError = err.error?.error;
        if (typeof bodyError === 'string') {
            const afterColon = bodyError.indexOf(': ');
            const trimmed =
                afterColon >= 0 ? bodyError.slice(afterColon + 2).trim() : bodyError.trim();
            if (trimmed) {
                return trimmed;
            }
        }
        return null;
    }
}
