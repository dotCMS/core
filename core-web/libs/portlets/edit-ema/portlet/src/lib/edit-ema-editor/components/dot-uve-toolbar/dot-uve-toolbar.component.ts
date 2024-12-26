import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    EventEmitter,
    inject,
    OnInit,
    Output,
    signal,
    viewChild,
    WritableSignal,
    model,
    untracked
} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { ChipModule } from 'primeng/chip';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { UVE_MODE } from '@dotcms/client';
import { DotDevicesService, DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotPersona, DotLanguage, DotDeviceListItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEmaBookmarksComponent } from './components/dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from './components/dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveDeviceSelectorComponent } from './components/dot-uve-device-selector/dot-uve-device-selector.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';

import { DEFAULT_DEVICES, DEFAULT_PERSONA } from '../../../shared/consts';
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
        ChipModule
    ],
    providers: [DotPersonalizeService, DotDevicesService],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent implements OnInit {
    $personaSelector = viewChild<EditEmaPersonaSelectorComponent>('personaSelector');
    $languageSelector = viewChild<EditEmaLanguageSelectorComponent>('languageSelector');

    @Output() translatePage = new EventEmitter<{ page: DotPage; newLanguage: number }>();

    readonly #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);
    readonly #deviceService = inject(DotDevicesService);

    readonly $toolbar = this.#store.$uveToolbar;
    readonly $isPreviewMode = this.#store.$isPreviewMode;
    readonly $apiURL = this.#store.$apiURL;
    readonly $personaSelectorProps = this.#store.$personaSelector;
    readonly $infoDisplayProps = this.#store.$infoDisplayProps;
    readonly $devices: WritableSignal<DotDeviceListItem[]> = signal([]);

    protected readonly CURRENT_DATE = new Date();

    readonly $styleToolbarClass = computed(() => {
        if (!this.$isPreviewMode()) {
            return 'uve-toolbar';
        }

        return 'uve-toolbar uve-toolbar-preview';
    });

    protected readonly publishDateParam = this.#store.pageParams().publishDate;
    protected readonly $previewDate = model<Date>(
        this.publishDateParam ? new Date(this.publishDateParam) : null
    );

    readonly $previewDateEffect = effect(
        () => {
            const previewDate = this.$previewDate();

            if (!previewDate) {
                return;
            }

            // If previewDate is minor that the CURRENT DATE, set previewDate to CURRENT DATE
            if (previewDate < this.CURRENT_DATE) {
                this.$previewDate.set(this.CURRENT_DATE);

                return;
            }

            untracked(() => {
                this.#store.loadPageAsset({
                    editorMode: UVE_MODE.PREVIEW,
                    publishDate: previewDate?.toISOString()
                });
            });
        },
        { allowSignalWrites: true }
    );
    readonly $pageInode = computed(() => {
        return this.#store.pageAPIResponse()?.page.inode;
    });

    readonly $actions = this.#store.workflowLoading;
    readonly $workflowLoding = this.#store.workflowLoading;

    protected readonly date = new Date();

    defaultDevices = DEFAULT_DEVICES;

    handleViewParamsEffect = effect(
        () => {
            const devices = this.$devices();

            //We want to be sure that we have devices
            if (!devices.length) return;

            // Get the device and orientation from the URL
            const { device: deviceInode, orientation } = this.#store.viewParams();

            // So we dont open the editor in a device
            if (!this.#store.$isPreviewMode() && (deviceInode || orientation)) {
                this.#store.patchViewParams({ device: null, orientation: null });

                return;

                // If we are in preview mode and we dont have a device, set the default device
            } else if (this.#store.$isPreviewMode() && !deviceInode) {
                this.#store.patchViewParams({ device: 'default', orientation: null });

                return;
            }

            // Find the device in the devices list
            const device = this.$devices().find((d) => d.inode === deviceInode);

            // If we have a device, set it in the store
            if (device) {
                this.#store.setDevice(device, orientation);
            } else {
                // If we dont have a device, clear the device and social media
                this.#store.clearDeviceAndSocialMedia();
            }
        },
        {
            allowSignalWrites: true
        }
    );

    ngOnInit(): void {
        this.#deviceService
            .get()
            .pipe(take(1))
            .subscribe((devices: DotDeviceListItem[] = []) => {
                this.$devices.set([...DEFAULT_DEVICES, ...devices]);
            });
    }

    /**
     * Initialize the preview mode
     *
     * @param {Date} publishDate
     * @memberof DotUveToolbarComponent
     */
    protected triggerPreviewMode(publishDate: Date = new Date()) {
        this.$previewDate.set(publishDate);
    }

    /**
     * Initialize the edit mode
     *
     * @memberof DotUveToolbarComponent
     */
    protected triggerEditMode() {
        this.#store.loadPageAsset({ editorMode: undefined, publishDate: undefined });
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
