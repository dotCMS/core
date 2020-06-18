import { Component, OnInit, Input, ViewChild, OnChanges, SimpleChanges, Output, EventEmitter } from '@angular/core';

import { take, switchMap } from 'rxjs/operators';
import { Observable, of, from } from 'rxjs';

import { SelectItem } from 'primeng/primeng';

import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';
import { DotPageRenderState, DotPageMode } from '@portlets/dot-edit-page/shared/models';
import { DotPersonalizeService } from '@services/dot-personalize/dot-personalize.service';
import { DotPageRenderOptions } from '@services/dot-page-render/dot-page-render.service';

enum DotConfirmationType {
    LOCK,
    PERSONALIZATION
}

@Component({
    selector: 'dot-edit-page-state-controller',
    templateUrl: './dot-edit-page-state-controller.component.html',
    styleUrls: ['./dot-edit-page-state-controller.component.scss']
})
export class DotEditPageStateControllerComponent implements OnInit, OnChanges {
    @ViewChild('pageLockInfo') pageLockInfo: DotEditPageLockInfoComponent;

    @Input() pageState: DotPageRenderState;
    @Output() modeChange = new EventEmitter<DotPageMode>();

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

    ngOnInit() {}

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
                        const pageId = this.pageState.page.identifier;
                        const personaKeyTag = this.pageState.viewAs.persona.keyTag;

                        return type === DotConfirmationType.PERSONALIZATION
                            ? this.dotPersonalizeService.personalized(pageId, personaKeyTag)
                            : of(null);
                    })
                )
                .subscribe(
                    () => {
                        this.updatePageState(
                            {
                                mode: this.mode
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
                    mode: this.mode
                },
                lock
            );
        }
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
        return ['edit', 'preview', 'live'].map((mode: string) =>
            this.getModeOption(mode, pageState)
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
            new Promise((resolve, reject) => {
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

    private showLockConfirmDialog(): Promise<any> {
        return new Promise((resolve, reject) => {
            this.dotAlertConfirmService.confirm({
                accept: resolve,
                reject: reject,
                header: this.dotMessageService.get('editpage.content.steal.lock.confirmation.message.header'),
                message: this.dotMessageService.get('editpage.content.steal.lock.confirmation.message')
            });
        });
    }

    private showPersonalizationConfirmDialog(): Promise<any> {
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
