import { from, Observable, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import { switchMap, take } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import {
    DotDevice,
    DotExperimentStatus,
    DotPageMode,
    DotPageRenderOptions,
    DotPageRenderState,
    DotVariantData
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotPageStateService } from '@portlets/dot-edit-page/content/services/dot-page-state/dot-page-state.service';

import { DotEditPageLockInfoSeoComponent } from './components/dot-edit-page-lock-info-seo/dot-edit-page-lock-info-seo.component';

import { DotDeviceSelectorSeoComponent } from '../dot-device-selector-seo/dot-device-selector-seo.component';

enum DotConfirmationType {
    LOCK,
    PERSONALIZATION
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
        DotPipesModule,
        DotMessagePipe,
        TooltipModule,
        ButtonModule,
        DotDeviceSelectorSeoComponent,
        DotEditPageLockInfoSeoComponent
    ]
})
export class DotEditPageStateControllerSeoComponent implements OnChanges {
    @ViewChild('pageLockInfo', { static: true }) pageLockInfo: DotEditPageLockInfoSeoComponent;

    @Input() pageState: DotPageRenderState;
    @Output() modeChange = new EventEmitter<DotPageMode>();
    @Input() variant: DotVariantData | null = null;

    lock: boolean;
    lockWarn = false;
    mode: DotPageMode;
    options: SelectItem[] = [];

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private dotPageStateService: DotPageStateService,
        private dotPersonalizeService: DotPersonalizeService
    ) {}

    ngOnChanges(changes: SimpleChanges) {
        const pageState = changes.pageState.currentValue;
        this.options = this.getStateModeOptions(pageState);
        /*
When the page is lock but the page is being load from an user that can lock the page
we want to show the lock off so the new user can steal the lock
*/
        this.lock = this.isLocked(pageState);
        this.lockWarn = this.shouldWarnLock(pageState);
        this.mode = pageState.state.mode;
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
    stateSelectorHandler(mode: DotPageMode): void {
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

        return {
            label: this.dotMessageService.get(`editpage.toolbar.${mode}.page`),
            value: DotPageMode[mode.toLocaleUpperCase()],
            disabled: disabled[mode]
        };
    }

    private getStateModeOptions(pageState: DotPageRenderState): SelectItem[] {
        const items = this.variant ? this.getModesBasedOnVariant() : ['edit', 'preview'];

        return items.map((mode: string) => this.getModeOption(mode, pageState));
    }

    private getModesBasedOnVariant(): string[] {
        return [...(this.canEditVariant() ? ['edit'] : []), 'preview'];
    }

    private canEditVariant(): boolean {
        return (
            !this.variant.variant.isOriginal &&
            this.variant.experimentStatus === DotExperimentStatus.DRAFT
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

    private shouldAskPersonalization(): boolean {
        return this.pageState.viewAs.persona && !this.isPersonalized();
    }

    private shouldShowConfirmation(mode: DotPageMode): boolean {
        return (
            mode === DotPageMode.EDIT && (this.shouldAskToLock() || this.shouldAskPersonalization())
        );
    }

    private shouldWarnLock(pageState: DotPageRenderState): boolean {
        return pageState.page.canLock && pageState.state.lockedByAnotherUser;
    }

    private showConfirmation(): Observable<DotConfirmationType> {
        return from(
            new Promise<DotConfirmationType>((resolve, reject) => {
                if (this.shouldAskToLock()) {
                    this.showLockConfirmDialog()
                        .then(() => {
                            resolve(DotConfirmationType.LOCK);
                        })
                        .catch(() => reject());
                }

                if (this.shouldAskPersonalization()) {
                    this.showPersonalizationConfirmDialog()
                        .then(() => {
                            resolve(DotConfirmationType.PERSONALIZATION);
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

    private getPersonalizationConfirmMessage(): string {
        let message = this.dotMessageService.get(
            'editpage.personalization.confirm.message',
            this.pageState.viewAs.persona.name
        );

        if (this.shouldAskToLock()) {
            message += this.dotMessageService.get(
                'editpage.personalization.confirm.with.lock',
                this.pageState.page.lockedByName
            );
        }

        return message;
    }

    private updatePageState(options: DotPageRenderOptions, lock: boolean = null) {
        this.dotPageStateService.setLock(options, lock);
    }
}
