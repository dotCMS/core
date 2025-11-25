import { from, Observable, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MenuItem, SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { Menu, MenuModule } from 'primeng/menu';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import { switchMap, take } from 'rxjs/operators';

import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import {
    DotAlertConfirmService,
    DotMessageService,
    DotPageStateService,
    DotPersonalizeService,
    DotPropertiesService
} from '@dotcms/data-access';
import {
    DotDevice,
    DotExperimentStatus,
    DotPageMode,
    DotPageRenderOptions,
    DotPageRenderState,
    DotVariantData,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe, DotSafeHtmlPipe, DotTabButtonsComponent } from '@dotcms/ui';

import { DotEditPageLockInfoSeoComponent } from './components/dot-edit-page-lock-info-seo/dot-edit-page-lock-info-seo.component';

enum DotConfirmationType {
    LOCK,
    PERSONALIZATION,
    RUNNING_EXPERIMENT
}

@Component({
    selector: 'dot-edit-page-state-controller-seo',
    templateUrl: './dot-edit-page-state-controller-seo.component.html',
    styleUrls: ['./dot-edit-page-state-controller-seo.component.scss'],
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        InputSwitchModule,
        SelectButtonModule,
        DotSafeHtmlPipe,
        DotMessagePipe,
        TooltipModule,
        ButtonModule,
        DotDeviceSelectorSeoComponent,
        DotEditPageLockInfoSeoComponent,
        DotTabButtonsComponent,
        MenuModule
    ]
})
export class DotEditPageStateControllerSeoComponent implements OnInit, OnChanges {
    @ViewChild('pageLockInfo', { static: true })
    pageLockInfo: DotEditPageLockInfoSeoComponent;
    @ViewChild('deviceSelector') deviceSelector: DotDeviceSelectorSeoComponent;
    @ViewChild('menu') menu: Menu;

    @Input() pageState: DotPageRenderState;
    @Output() modeChange = new EventEmitter<DotPageMode>();
    @Input() variant: DotVariantData | null = null;
    @Input() apiLink: string;

    lock: boolean;
    lockWarn = false;
    featureFlagEditURLContentMapIsOn = false;
    mode: DotPageMode;
    options: SelectItem[] = [];
    menuItems: MenuItem[] = [];

    readonly dotPageMode = DotPageMode;

    private readonly menuOpenActions: Record<
        DotPageMode,
        (event: PointerEvent, target?: HTMLElement) => void
    > = {
        [DotPageMode.EDIT]: (event: PointerEvent) => {
            this.menu.toggle(event);
        },
        [DotPageMode.PREVIEW]: (event: PointerEvent, target?: HTMLElement) => {
            this.deviceSelector.openMenu(event, target);
        },
        [DotPageMode.LIVE]: (_: PointerEvent) => {
            // No logic
        }
    };

    private readonly featureFlagEditURLContentMap = FeaturedFlags.FEATURE_FLAG_EDIT_URL_CONTENT_MAP;

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private dotPageStateService: DotPageStateService,
        private dotPersonalizeService: DotPersonalizeService,
        private dotContentletEditor: DotContentletEditorService,
        private dotPropertiesService: DotPropertiesService
    ) {}

    ngOnChanges(changes: SimpleChanges) {
        const pageState = changes.pageState?.currentValue;
        if (pageState) {
            this.options = this.getStateModeOptions(pageState);
            /*
            When the page is lock but the page is being load from an user that can lock the page
            we want to show the lock off so the new user can steal the lock
            */
            this.lock = this.isLocked(pageState);
            this.lockWarn = this.shouldWarnLock(pageState);
            this.mode = pageState.state.mode;

            if (this.featureFlagEditURLContentMapIsOn && pageState.params.urlContentMap) {
                this.menuItems = this.getMenuItems();
            } else if (this.menuItems.length) {
                this.menuItems = []; // We have to clean the menu items because the menu is not re-rendered when the flag is off or the urlContentMap is null
            }
        }
    }

    ngOnInit(): void {
        this.dotPropertiesService
            .getFeatureFlag(this.featureFlagEditURLContentMap)

            .subscribe((result) => {
                this.featureFlagEditURLContentMapIsOn = result;

                if (this.featureFlagEditURLContentMapIsOn && this.pageState.params.urlContentMap) {
                    this.menuItems = this.getMenuItems();
                }

                this.options = this.getStateModeOptions(this.pageState);
            });
    }

    /**
     * Handler locker change event
     *
     * @memberof DotEditPageToolbarComponent
     */
    lockPageHandler(): void {
        if (this.shouldAskToLock()) {
            this.showLockConfirmDialog()
                .then(() => {
                    this.setLockerState();
                })
                .catch(() => {
                    this.lock = this.pageState.state.locked;
                });
        } else {
            this.setLockerState();
        }
    }

    /**
     * Handle the click to the locker switch
     *
     * @memberof DotEditPageStateControllerComponent
     */
    onLockerClick(): void {
        if (!this.pageState.page.canLock) {
            this.pageLockInfo.blinkLockMessage();
        }
    }

    /**
     * Handle state selector change event
     *
     * @param {DotPageMode} mode
     * @memberof DotEditPageStateControllerComponent
     */
    stateSelectorHandler({ optionId }: { optionId: string }): void {
        const mode = optionId as DotPageMode;

        this.modeChange.emit(mode);

        if (this.shouldShowConfirmation(mode)) {
            this.lock = mode === DotPageMode.EDIT;

            this.showConfirmation()
                .pipe(
                    take(1),
                    switchMap((type: DotConfirmationType) => {
                        return type === DotConfirmationType.PERSONALIZATION
                            ? this.dotPersonalizeService.personalized(
                                  this.pageState.page.identifier,
                                  this.pageState.viewAs.persona.keyTag
                              )
                            : of(null);
                    })
                )
                .subscribe(
                    () => {
                        this.updatePageState(
                            {
                                mode
                            },
                            this.lock
                        );
                    },
                    () => {
                        this.lock = this.pageState.state.lockedByAnotherUser
                            ? false
                            : this.pageState.state.locked;
                        this.mode = this.pageState.state.mode;
                    }
                );
        } else {
            const lock = mode === DotPageMode.EDIT || null;
            this.updatePageState(
                {
                    mode
                },
                lock
            );
        }
    }

    /**
     * Handle changes in Device Selector.
     *
     * @param DotDevice device
     * @memberof DotEditPageViewAsControllerComponent
     */
    changeDeviceHandler(device: DotDevice): void {
        this.dotPageStateService.setDevice(device);
        this.dotPageStateService.setSeoMedia(null);
    }

    /**
     * Change SEO Media
     * @param seoMedia
     */
    changeSeoMedia(seoMedia: string): void {
        this.dotPageStateService.setSeoMedia(seoMedia);
    }

    /**
     * Handle the click event on the dropdowns
     *
     * @param {{ event: PointerEvent; menuId: string }} { event, menuId }
     * @memberof DotEditPageStateControllerSeoComponent
     */
    handleMenuOpen({
        event,
        menuId,
        target
    }: {
        event: PointerEvent;
        menuId: string;
        target?: HTMLElement;
    }): void {
        this.menuOpenActions[menuId as DotPageMode]?.(event, target);
    }

    /**
     * Get the menu items for the dropdown
     *
     * @private
     * @return {*}  {MenuItem[]}
     * @memberof DotEditPageStateControllerComponent
     */
    private getMenuItems(): MenuItem[] {
        return [
            {
                label: this.dotMessageService.get('modes.Page'),
                command: () => {
                    this.stateSelectorHandler({ optionId: DotPageMode.EDIT });
                }
            },
            {
                label: `${
                    this.pageState.params.urlContentMap.contentType
                } ${this.dotMessageService.get('Content')}`,
                command: () => {
                    this.dotContentletEditor.edit({
                        data: {
                            inode: this.pageState.params.urlContentMap.inode
                        }
                    });
                }
            }
        ];
    }

    private getModeOption(mode: string, pageState: DotPageRenderState): SelectItem {
        const disabled = {
            edit: !pageState.page.canEdit || !pageState.page.canLock,
            preview: !pageState.page.canRead,
            live: !pageState.page.liveInode
        };

        const enumMode = DotPageMode[mode.toLocaleUpperCase()] as DotPageMode;

        return {
            label: this.dotMessageService.get(`editpage.toolbar.${mode}.page`),
            value: {
                id: enumMode,
                showDropdownButton: this.shouldShowDropdownButton(enumMode, pageState),
                shouldRefresh: enumMode === DotPageMode.PREVIEW
            },
            disabled: disabled[mode]
        };
    }

    /**
     * Check if the dropdown button should be shown
     *
     * @private
     * @param {string} mode
     * @param {DotPageRenderState} pageState
     * @return {*}  {boolean}
     * @memberof DotEditPageStateControllerSeoComponent
     */
    private shouldShowDropdownButton(mode: DotPageMode, pageState: DotPageRenderState): boolean {
        return {
            [DotPageMode.EDIT]:
                this.featureFlagEditURLContentMapIsOn && Boolean(pageState.params.urlContentMap),
            [DotPageMode.PREVIEW]: true, // No logic involved, always show,
            [DotPageMode.LIVE]: false // Don't show for live
        }[mode]; // We get the value from the object using the mode as key
    }

    private getStateModeOptions(pageState: DotPageRenderState): SelectItem[] {
        const items = this.variant ? this.getModesBasedOnVariant(pageState) : ['edit', 'preview'];

        return items.map((mode: string) => this.getModeOption(mode, pageState));
    }

    private getModesBasedOnVariant(pageState: DotPageRenderState): string[] {
        return [...(this.canEditVariant(pageState) ? ['edit'] : []), 'preview'];
    }

    private canEditVariant(pageState: DotPageRenderState): boolean {
        return (
            !this.variant.variant.isOriginal &&
            this.variant.experimentStatus === DotExperimentStatus.DRAFT &&
            !pageState.state.lockedByAnotherUser
        );
    }

    private isLocked(pageState: DotPageRenderState): boolean {
        return pageState.state.locked;
    }

    private isPersonalized(): boolean {
        return this.pageState.viewAs.persona && this.pageState.viewAs.persona.personalized;
    }

    private setLockerState() {
        if (!this.lock && this.mode === DotPageMode.EDIT) {
            this.mode = DotPageMode.PREVIEW;
        }

        this.updatePageState(
            {
                mode: this.mode
            },
            this.lock
        );
    }

    private shouldAskToLock(): boolean {
        return this.pageState.page.canLock && this.pageState.state.lockedByAnotherUser;
    }

    private shouldAskOnRunningExperiment(): boolean {
        return !!this.pageState.state.runningExperiment;
    }

    private shouldAskPersonalization(): boolean {
        return this.pageState.viewAs.persona && !this.isPersonalized();
    }

    private shouldShowConfirmation(mode: DotPageMode): boolean {
        return (
            mode === DotPageMode.EDIT &&
            (this.shouldAskToLock() ||
                this.shouldAskPersonalization() ||
                this.shouldAskOnRunningExperiment())
        );
    }

    private shouldWarnLock(pageState: DotPageRenderState): boolean {
        return pageState.page.canLock && pageState.state.lockedByAnotherUser;
    }

    private showConfirmation(): Observable<DotConfirmationType> {
        return from(
            new Promise<DotConfirmationType>((resolve, reject) => {
                if (this.shouldAskPersonalization()) {
                    this.showPersonalizationConfirmDialog()
                        .then(() => {
                            resolve(DotConfirmationType.PERSONALIZATION);
                        })
                        .catch(() => reject());
                } else if (this.shouldAskOnRunningExperiment()) {
                    this.showRunningExperimentConfirmDialog()
                        .then(() => {
                            resolve(DotConfirmationType.RUNNING_EXPERIMENT);
                        })
                        .catch(() => reject());
                } else if (this.shouldAskToLock()) {
                    this.showLockConfirmDialog()
                        .then(() => {
                            resolve(DotConfirmationType.LOCK);
                        })
                        .catch(() => reject());
                }
            })
        );
    }

    private showLockConfirmDialog(): Promise<string> {
        return new Promise((resolve, reject) => {
            this.dotAlertConfirmService.confirm({
                accept: resolve,
                reject: reject,
                header: this.dotMessageService.get(
                    'editpage.content.steal.lock.confirmation.message.header'
                ),
                message: this.dotMessageService.get(
                    'editpage.content.steal.lock.confirmation.message'
                )
            });
        });
    }

    private showPersonalizationConfirmDialog(): Promise<string> {
        return new Promise((resolve, reject) => {
            this.dotAlertConfirmService.confirm({
                accept: resolve,
                reject: reject,
                header: 'Personalization',
                message: this.getPersonalizationConfirmMessage()
            });
        });
    }

    private showRunningExperimentConfirmDialog(): Promise<string> {
        return new Promise((resolve, reject) => {
            this.dotAlertConfirmService.confirm({
                accept: resolve,
                reject: reject,
                header: this.dotMessageService.get('experiment.running'),
                message: this.getRunningExperimentConfirmMessage()
            });
        });
    }

    private getPersonalizationConfirmMessage(): string {
        let message = this.dotMessageService.get(
            'editpage.personalization.confirm.message',
            this.pageState.viewAs.persona.name
        );

        if (this.shouldAskToLock()) {
            message += this.getBlockedPageNote();
        }

        if (this.shouldAskOnRunningExperiment()) {
            message += this.getRunningExperimentNote();
        }

        return message;
    }

    private getRunningExperimentConfirmMessage(): string {
        let message = this.dotMessageService.get('experiment.running.edit.confirmation');

        if (this.shouldAskToLock()) {
            message += this.getBlockedPageNote();
        }

        return message;
    }

    private getBlockedPageNote(): string {
        return this.dotMessageService.get(
            'editpage.personalization.confirm.with.lock',
            this.pageState.page.lockedByName
        );
    }

    private getRunningExperimentNote(): string {
        return this.dotMessageService.get(
            'experiment.running.edit.lock.confirmation.note',
            this.pageState.page.lockedByName
        );
    }

    private updatePageState(options: DotPageRenderOptions, lock: boolean = null) {
        this.dotPageStateService.setLock(options, lock);
    }
}
