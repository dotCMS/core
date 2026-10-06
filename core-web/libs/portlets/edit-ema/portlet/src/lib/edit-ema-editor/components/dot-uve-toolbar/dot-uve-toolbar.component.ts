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
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DatePickerModule } from 'primeng/datepicker';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { map, take } from 'rxjs/operators';

import {
    DotDevicesService,
    DotMessageService,
    DotPersonalizeService,
    DotPropertiesService
} from '@dotcms/data-access';
import {
    DotDeviceListItem,
    DotExperiment,
    DotExperimentStatus,
    DotLanguage,
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    CONFIGURE_SECTION_PARAM,
    CONFIGURE_SECTION_VARIANTS,
    EXPERIMENT_RETURN_PARAM,
    EXPERIMENT_RETURN_PORTLET
} from '@dotcms/dotcms-models';
import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';
import { DotCMSPage, DotCMSURLContentMap, DotCMSViewAsPersona, UVE_MODE } from '@dotcms/types';
import { DotLanguageSelectorComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditorModeSelectorComponent } from './components/dot-editor-mode-selector/dot-editor-mode-selector.component';
import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from './components/dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotToggleLockButtonComponent } from './components/dot-toggle-lock-button/dot-toggle-lock-button.component';
import { DotUveDeviceSelectorComponent } from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DeviceSelectorChange } from './components/dot-uve-device-selector/dot-uve-device-selector.models';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';

import { DEFAULT_DEVICES, DEFAULT_PERSONA, PERSONA_KEY } from '../../../shared/consts';
import { InfoOptions } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import {
    convertLocalTimeToUTC,
    convertUTCToLocalTime,
    createFavoritePagesURL
} from '../../../utils';
import { readExperimentsPortletSwitch } from '../../../utils/experiments-portlet-switch.util';
import { CLEARED_VARIANT_PARAMS, leaveTheVariant } from '../../../utils/leave-the-variant.util';

@Component({
    selector: 'dot-uve-toolbar',
    imports: [
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        DatePickerModule,
        ChipModule,
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
    deviceSelectorChange = output<DeviceSelectorChange>();

    readonly #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);
    readonly #deviceService = inject(DotDevicesService);
    readonly #router = inject(Router);
    /**
     * Provided by the UVE shell, so it is here whenever the editor is (#37478). It is `null` only
     * where the toolbar is rendered outside that shell.
     */
    readonly #panel = inject(DotExperimentsPanelStore, { optional: true });
    readonly #activatedRoute = inject(ActivatedRoute);

    readonly #propertiesService = inject(DotPropertiesService);

    // Expose enum for template usage
    readonly UVE_MODE = UVE_MODE;

    // Component builds its own toolbar props locally
    protected readonly $bookmarksUrl = computed<string>(() => {
        const params = this.#store.pageParams();
        const site = this.#store.pageAsset()?.site;

        // `?? ''` on both: this builds the "add to favourites" URL, and there is no page to
        // favourite before one has loaded — the toolbar renders the button disabled until then.
        return createFavoritePagesURL({
            languageId: Number(params?.language_id),
            pageURI: params?.url ?? '',
            siteId: site?.identifier ?? ''
        });
    });

    // Use store's pageLanguage instead of redefining it
    protected readonly $currentLanguage = this.#store.pageLanguage;

    protected readonly $runningExperiment = computed(() => {
        const experiment = this.#store.pageExperiment?.();
        const isExperimentRunning = experiment?.status === DotExperimentStatus.RUNNING;

        return isExperimentRunning ? experiment : null;
    });

    readonly $toolbar = computed(() => ({
        runningExperiment: this.$runningExperiment(),
        editor: { bookmarksUrl: this.$bookmarksUrl() },
        currentLanguage: this.$currentLanguage()
    }));

    readonly $showWorkflowActions = this.#store.$showWorkflowsActions;
    readonly $mode = this.#store.viewMode;
    readonly $isPreviewMode = this.#store.$isPreviewMode;
    readonly $isLiveMode = this.#store.$isLiveMode;
    readonly $isEditMode = this.#store.$isEditMode;
    readonly $apiURL = this.#store.$apiURL;
    readonly $personaSelectorProps = this.#store.$personaSelector;
    /**
     * Whether this build has the Experiments panel (#37478).
     *
     * Read once per toolbar, not per gesture. The chip below only needs to know whether a panel
     * exists to go back to, and a toolbar is rebuilt on every arrival into a variant — the same
     * lifetime the chip itself has. The *destination* logic still reads the switch at the gesture,
     * where SC-002's under-a-minute promise applies.
     */
    protected readonly $experimentsPanelEnabled = toSignal(
        readExperimentsPortletSwitch(this.#propertiesService),
        { initialValue: false }
    );
    /**
     * The variant chip — the store's own, plus the one case it does not cover (#37005, #37478).
     *
     * `$infoDisplayProps` returns `null` for the DEFAULT variant, which is right for an ordinary
     * page: there is no variant to announce. But the CONTROL is previewed through the same door,
     * and the chip is the only way back — so that preview opened the editor on a screen with no
     * route home.
     *
     * What decides that this case gets a chip depends on the switch, and deliberately so:
     *
     * - **On**, the way back is the panel, and every arrival inside an experiment deserves one. So
     *   it keys on the experiment being named by the address, which is *true*, rather than on
     *   `experimentReturn=portlet`, which is not: the panel builds its variant links through the
     *   same helper and that marker is written unconditionally, so it claims a portlet origin for
     *   trips that began in the panel.
     * - **Off**, the marker still decides, unchanged. The legacy in-UVE screens send the same
     *   `experimentId` and must not gain a chip they never had (FR-042, FR-047).
     *
     * When the switch is retired, so is the marker and so is this branch.
     */
    readonly $infoDisplayProps = computed<InfoOptions | null>(
        () => this.#store.$infoDisplayProps() ?? this.#controlVariantChip()
    );
    readonly $socialMedia = this.#store.viewSocialMedia;
    readonly $urlContentMap = this.#store.$urlContentMap;
    readonly $isPaletteOpen = this.#store.editorPaletteOpen;
    readonly $canEditPage = this.#store.editorCanEditContent;

    // `| null` in the signal's type, because that is the declared `initialValue`: the device list
    // is not known until the service answers.
    readonly $devices: Signal<DotDeviceListItem[] | null> = toSignal(
        this.#deviceService.get().pipe(map((devices = []) => [...DEFAULT_DEVICES, ...devices])),
        {
            initialValue: null
        }
    );

    protected readonly $pageParams = this.#store.pageParams;
    protected readonly $previewDate = computed<Date>(() => {
        const publishDate = this.$pageParams()?.publishDate;
        const previewDate = publishDate ? convertUTCToLocalTime(new Date(publishDate)) : new Date();

        return previewDate;
    });

    protected readonly $showDeviceSelector = computed(() => {
        const isEditMode = this.$pageParams()?.mode === UVE_MODE.EDIT;
        return !isEditMode && (this.$devices()?.length ?? 0) > 0;
    });

    protected readonly $showUrlContentMap = computed(() => {
        const isEditMode = this.$pageParams()?.mode === UVE_MODE.EDIT;
        return isEditMode && this.$urlContentMap();
    });

    readonly $pageInode = computed(() => {
        return this.#store.pageAsset()?.page?.inode;
    });

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
            ...storeLockOptions,
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

        this.#store['pageReload']({ publishDate: publishDateUTC });
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
     * Chip for the control variant previewed from the portlet, or `null` when that is not the
     * case. Same shape the store builds for a real variant, so the action handler and the template
     * need no special case: the id stays `variant` and the back arrow means the same thing.
     *
     * **It reads the store's params, not the route's, and that is what makes the chip go away.**
     * Both hold the same values, but only one of them is ever updated: UVE writes its address with
     * `Location.go`, which does not notify the router, so `snapshot.queryParams` is frozen at the
     * navigation that built this toolbar. The chip has to *disappear* when the editor takes the way
     * back — the params are cleared right there, on a toolbar that stays mounted — and read from
     * the snapshot it never would. `pageParams` is a signal and is the thing UVE mirrors into the
     * address, so the computed re-derives and the bar goes with the state it describes.
     */
    #controlVariantChip(): InfoOptions | null {
        const params = this.#store.pageParams();

        const deserved = this.$experimentsPanelEnabled()
            ? !!params?.['experimentId']
            : params?.[EXPERIMENT_RETURN_PARAM] === EXPERIMENT_RETURN_PORTLET;

        if (!deserved) {
            return null;
        }

        const experiment = this.#store.pageExperiment();

        if (!experiment) {
            return null;
        }

        const mode = this.#store.pageParams()?.mode;
        const controlName =
            experiment.trafficProportion?.variants?.find(
                (variant) => variant.id === DEFAULT_VARIANT_ID
            )?.name ?? DEFAULT_VARIANT_NAME;

        return {
            info: {
                message:
                    mode === UVE_MODE.PREVIEW || mode === UVE_MODE.LIVE
                        ? 'editpage.viewing.variant'
                        : 'editpage.editing.variant',
                args: [controlName]
            },
            icon: 'pi pi-file-edit',
            id: 'variant',
            actionIcon: 'pi pi-arrow-left'
        };
    }

    /**
     * The running-experiment tag was used (#37478, FR-025c, FR-025d, D14).
     *
     * Straight to its results, because that is what the tag is about: it announces what is
     * running, and the only question it raises is how it is doing. Opening it replaces whatever
     * the panel was showing rather than stacking a second surface — the badge always means "show
     * me the running experiment", whichever screen the editor was on.
     *
     * Only reached when the tag is an action, which is only when the panel exists; the
     * switch-off path keeps the legacy reports route it has always had (FR-017 of #37005).
     */
    handleRunningExperimentClick(experiment: DotExperiment): void {
        this.#panel?.openResults(experiment.id);
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

        /**
         * The panel kept the editor's place, so give it back — and do nothing else (#37478,
         * FR-023, FR-025).
         *
         * **First, and above the experiment guard below.** This return needs nothing from
         * `pageExperiment()`, and that signal is null in the exact case this branch exists for: a
         * DRAFT experiment is not running on the page, so once the editor is back on the original
         * there is no experiment on the page asset and no `experimentId` left on the address. The
         * guard below would then swallow the click and the chip would do nothing at all.
         *
         * **No navigation.** Clearing the variant off the address looks harmless and is not: the
         * shell watches those params, so writing them restarts UVE, and the editor watches the
         * page they were reading reload underneath the panel they just reopened.
         *
         * Nothing is recomputed either: the view, the experiment and the card the editor was on
         * are still in the store, so the most faithful return asks for none of them. It needs no
         * answer from the switch — a suspended panel only exists if the panel was being used.
         */
        if (this.#panel?.suspendedForVariant()) {
            leaveTheVariant(this.#store);
            this.#panel.resumeFromVariant();

            return;
        }

        // Handle variant action - navigate to experiment configuration
        const currentExperiment = this.#store.pageExperiment();

        if (!currentExperiment) {
            return;
        }

        // Which configuration screen opened this variant (#37005). The route is the right source
        // here and the wrong one for the chip: a gesture happens on the toolbar the navigation
        // built, where the snapshot still holds, while the chip has to survive the params being
        // cleared underneath it. See `#controlVariantChip`.
        const cameFromPortlet =
            this.#activatedRoute.snapshot.queryParams[EXPERIMENT_RETURN_PARAM] ===
            EXPERIMENT_RETURN_PORTLET;

        /**
         * Where inside Configure to land. The Variants card is where this round-trip started, and
         * Configure is four stacked cards tall — returning to the top of it loses the reader's
         * place. Only set on the portlet destination: the legacy screen's behaviour stays as it is
         * (FR-018).
         */
        const portletReturnParams = {
            ...CLEARED_VARIANT_PARAMS,
            [CONFIGURE_SECTION_PARAM]: CONFIGURE_SECTION_VARIANTS
        };

        /**
         * Decided by the switch, and by nothing else.
         *
         * With it on there is a panel on this page that can show the configuration, so no return
         * leaves the editor — not one that began in the full-screen portlet, and not a pasted
         * variant link with no origin at all. The round trip finishes where the editor already is.
         *
         * Reached only when there is nothing to resume: the round trip began in the full-screen
         * portlet, or a reload during it took the panel's memory with it. The experiment is named
         * by the address for as long as the editor is on the variant, so the return is rebuilt
         * from that instead — the same destination, reconstructed rather than restored.
         *
         * With it off the origin marker still decides, exactly as it did. Branching on the switch
         * alone *there* would make FR-018 and FR-005/FR-027 mutually exclusive on a supported
         * path: with the switch off an editor can still reach the portlet from the main navigation
         * (FR-026) and still open a variant from it (FR-027), and they must not be returned to a
         * legacy screen they never came from.
         *
         * Read now rather than at construction so an operator's flip lands on this gesture
         * (SC-002).
         */
        readExperimentsPortletSwitch(this.#propertiesService)
            .pipe(take(1))
            .subscribe((portletEntryPointEnabled) => {
                if (portletEntryPointEnabled && this.#panel) {
                    /**
                     * The experiment the editor was *previewing*, which is not the same question
                     * as `pageExperiment()` — that one answers "what is running on this page", and
                     * a page commonly runs one experiment while the editor is off previewing a
                     * draft of another. Answering with it reopened the panel on a experiment the
                     * editor never clicked.
                     *
                     * Read from `pageParams` rather than the route snapshot, which UVE never
                     * updates: navigating to another page inside the editor drops the experiment
                     * params, and a snapshot read would resurrect the one the editor had left
                     * behind on a page it is no longer on. The panel's own memory is the fallback
                     * for a reload that took the address with it.
                     */
                    const previewed =
                        this.#store.pageParams()?.['experimentId'] ?? this.#panel.experimentId();

                    if (!previewed) {
                        return;
                    }

                    leaveTheVariant(this.#store);
                    this.#panel.openVariants(previewed);

                    return;
                }

                if (cameFromPortlet) {
                    this.#router.navigate(['/experiments', currentExperiment.id, 'configuration'], {
                        // No `queryParamsHandling`: `url`, `language_id` and the persona key
                        // are UVE's, and the list's `parseViewState` does not recognise them —
                        // they would sit in its address indefinitely.
                        queryParams: portletReturnParams
                    });

                    return;
                }

                // The legacy destination, untouched — which is what makes FR-018's "as before"
                // true by construction rather than by re-derivation.
                this.#router.navigate(
                    [
                        '/edit-page/experiments/',
                        currentExperiment.pageId,
                        currentExperiment.id,
                        'configuration'
                    ],
                    {
                        queryParams: CLEARED_VARIANT_PARAMS,
                        queryParamsHandling: 'merge'
                    }
                );
            });
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

        this.#store['pageLoad']({ language_id });
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
            this.#store['pageLoad']({ [PERSONA_KEY]: persona.identifier });

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
                        this.#store['pageLoad']({ [PERSONA_KEY]: persona.identifier });
                        this.$personaSelector()?.fetchPersonas();
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
                        this.$personaSelector()?.resetValue();
                    }
                });
            },
            reject: () => {
                this.$personaSelector()?.resetValue();
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
                        this.$personaSelector()?.fetchPersonas();

                        if (persona.selected) {
                            this.#store['pageLoad']({
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
                // `?? null` — the selector's `value` model accepts null for "nothing selected",
                // which is what an unloaded page's language is.
                this.$languageSelector()?.value.set(this.#store.pageLanguage() ?? null);
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
