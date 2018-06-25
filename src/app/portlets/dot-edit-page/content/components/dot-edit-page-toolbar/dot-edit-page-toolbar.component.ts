import { DotAlertConfirmService } from '../../../../../api/services/dot-alert-confirm/dot-alert-confirm.service';
import { Component, OnInit, Input, EventEmitter, Output, ViewChild, SimpleChanges, OnChanges } from '@angular/core';
import { SelectItem, InputSwitch } from 'primeng/primeng';
import * as _ from 'lodash';
import { DotEditPageState } from '../../../../../shared/models/dot-edit-page-state/dot-edit-page-state.model';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { PageMode } from '../../../shared/models/page-mode.enum';
import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges {
    @ViewChild('locker') locker: InputSwitch;
    @ViewChild('pageLockInfo') pageLockInfo: DotEditPageLockInfoComponent;

    @Input() pageState: DotRenderedPageState;

    @Output() changeState = new EventEmitter<DotEditPageState>();
    @Output() actionFired = new EventEmitter<any>();
    @Output() cancel = new EventEmitter<any>();

    states: SelectItem[] = [];
    lockerModel: boolean;
    mode: PageMode;

    private debounceStateSelector = _.debounce((pageState: PageMode) => this.setSelectorState(pageState), 500, { leading: true });

    constructor(public dotMessageService: DotMessageService, private dotDialogService: DotAlertConfirmService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'dot.common.cancel',
                'editpage.content.steal.lock.confirmation.message',
                'editpage.content.steal.lock.confirmation.message.header',
                'editpage.toolbar.edit.page',
                'editpage.toolbar.live.page',
                'editpage.toolbar.preview.page'
            ])
            .subscribe(() => {
                this.setFieldsModels(this.pageState);
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.pageState && !changes.pageState.firstChange) {
            this.setFieldsModels(changes.pageState.currentValue);
        }
    }

    /**
     * Habdle action fired from dot-edit-page-workflows-actions
     *
     * @memberof DotEditPageToolbarComponent
     */
    handleActionFired(): void {
        this.actionFired.emit();
    }

    /**
     * Handle the click to the locker switch
     *
     * @param {any} $event
     * @memberof DotEditPageToolbarComponent
     */
    onLockerClick(_$event): void {
        if (this.locker.disabled) {
            this.pageLockInfo.blinkLockMessage();
        }
    }

    /**
     * Handler locker change event
     *
     * @param {any} $event
     * @memberof DotEditPageToolbarComponent
     */
    lockPageHandler(_event): void {
        if (this.shouldConfirmToLock()) {
            this.showLockConfirmDialog(() => {
                this.setLockerState();
            });
        } else {
            this.setLockerState();
        }
    }

    /**
     * Handle state selector change event
     *
     * @param {any} $event
     * @memberof DotEditPageToolbarComponent
     */
    stateSelectorHandler(pageState: PageMode): void {
        if (this.mode === PageMode.EDIT && this.shouldConfirmToLock()) {
            this.showLockConfirmDialog(() => {
                this.setSelectorState(pageState);
            });
        } else {
            this.debounceStateSelector(pageState);
        }
    }

    private canTakeLock(pageState: DotRenderedPageState): boolean {
        return pageState.page.canLock && pageState.state.lockedByAnotherUser;
    }

    private getStateModeOptions(pageState: DotRenderedPageState): SelectItem[] {
        return ['edit', 'preview', 'live'].map((mode: string) => this.getModeOption(mode, pageState));
    }

    private getModeOption(mode: string, pageState: DotRenderedPageState): SelectItem {
        const modeMap = {
            edit: this.getEditOption.bind(this),
            preview: this.getPreviewOption.bind(this),
            live: this.getLiveOption.bind(this)
        };

        return modeMap[mode](pageState);
    }

    private getEditOption(pageState: DotRenderedPageState): SelectItem {
        return {
            label: this.dotMessageService.get('editpage.toolbar.edit.page'),
            value: PageMode.EDIT,
            styleClass: !pageState.page.canEdit || !pageState.page.canLock ? 'edit-page-toolbar__state-selector-item--disabled' : ''
        };
    }

    private getLiveOption(pageState: DotRenderedPageState): SelectItem {
        return {
            label: this.dotMessageService.get('editpage.toolbar.live.page'),
            value: PageMode.LIVE,
            styleClass: !pageState.page.liveInode ? 'edit-page-toolbar__state-selector-item--disabled' : ''
        };
    }

    private getPreviewOption(_pageState: DotRenderedPageState): SelectItem {
        return {
            label: this.dotMessageService.get('editpage.toolbar.preview.page'),
            value: PageMode.PREVIEW
        };
    }

    private setFieldsModels(pageState: DotRenderedPageState): void {
        this.lockerModel = pageState.state.locked && !this.canTakeLock(pageState);
        this.mode = pageState.state.mode;
        this.states = this.getStateModeOptions(pageState);
    }

    private setLockerState() {
        if (!this.lockerModel && this.mode === PageMode.EDIT) {
            this.mode = PageMode.PREVIEW;
        }

        this.changeState.emit({
            locked: this.lockerModel,
            mode: this.mode
        });
    }

    private setSelectorState(pageMode: PageMode) {
        const toEmit: DotEditPageState = {
            mode: pageMode
        };

        if (pageMode === PageMode.EDIT) {
            this.lockerModel = true;
            toEmit.locked = true;
        }

        this.changeState.emit(toEmit);
    }

    private shouldConfirmToLock(): boolean {
        return this.pageState.page.canLock && this.pageState.state.lockedByAnotherUser;
    }

    private showLockConfirmDialog(acceptCallback: Function): void {
        this.dotDialogService.confirm({
            accept: acceptCallback,
            reject: () => {
                this.lockerModel = false;
                this.mode = this.pageState.state.mode;
            },
            header: this.dotMessageService.get('editpage.content.steal.lock.confirmation.message.header'),
            message: this.dotMessageService.get('editpage.content.steal.lock.confirmation.message')
        });
    }
}
