import { from, Observable, of } from 'rxjs';

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

import { MenuItem, SelectItem } from 'primeng/api';
import { Menu } from 'primeng/menu';

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
    DotExperimentStatus,
    DotPageMode,
    DotPageRenderOptions,
    DotPageRenderState,
    DotVariantData,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { DotEditPageLockInfoComponent } from '@portlets/dot-edit-page/content/components/dot-edit-page-state-controller/components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';

enum DotConfirmationType {
    LOCK,
    PERSONALIZATION,
    RUNNING_EXPERIMENT
}

@Component({
    selector: 'dot-edit-page-state-controller',
    templateUrl: './dot-edit-page-state-controller.component.html',
    styleUrls: ['./dot-edit-page-state-controller.component.scss']
})
export class DotEditPageStateControllerComponent implements OnChanges, OnInit {
    @ViewChild('pageLockInfo', { static: true }) pageLockInfo: DotEditPageLockInfoComponent;
    @ViewChild('menu') menu: Menu;

    @Input() pageState: DotPageRenderState;
    @Output() modeChange = new EventEmitter<DotPageMode>();
    @Input() variant: DotVariantData | null = null;

    lock: boolean;
    lockWarn = false;
    mode: DotPageMode;
    options: SelectItem[] = [];
    featureFlagEditURLContentMapIsOn = false;
    menuItems: MenuItem[] = [];

    readonly dotPageMode = DotPageMode;
    readonly featureFlagEditURLContentMap = FeaturedFlags.FEATURE_FLAG_EDIT_URL_CONTENT_MAP;

    private readonly menuOpenActions: Record<DotPageMode.EDIT, (event: PointerEvent) => void> = {
        [DotPageMode.EDIT]: (event: PointerEvent) => {
            this.menu.toggle(event);
        }
    };

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
            this.showLockConfirmDialog().then(() => {
                this.setLockerState();
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
     * Handle the click event on the dropdowns
     *
     * @param {{ event: PointerEvent; menuId: string }} { event, menuId }
     * @memberof DotEditPageStateControllerSeoComponent
     */
    handleMenuOpen({ event, menuId }: { event: PointerEvent; menuId: string }): void {
        this.menuOpenActions[menuId as DotPageMode]?.(event);
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
        if (
            mode === DotPageMode.EDIT &&
            this.featureFlagEditURLContentMapIsOn &&
            Boolean(pageState.params.urlContentMap)
        )
            return true;

        return false;
    }

    private canTakeLock(pageState: DotPageRenderState): boolean {
        return pageState.page.canLock && pageState.state.lockedByAnotherUser;
    }

    private getModeOption(mode: string, pageState: DotPageRenderState): SelectItem {
        const disabled = {
            edit: !pageState.page.canEdit || !pageState.page.canLock,
            preview: !pageState.page.canRead,
            live: !pageState.page.liveInode
        };

        const enumMode = DotPageMode[mode.toUpperCase()];

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

    private getStateModeOptions(pageState: DotPageRenderState): SelectItem[] {
        const items = this.variant
            ? this.getModesBasedOnVariant(pageState)
            : ['edit', 'preview', 'live'];

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
        return pageState.state.locked && !this.canTakeLock(pageState);
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
