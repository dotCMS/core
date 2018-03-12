import { DotDialogService } from '../../../../../api/services/dot-dialog/dot-dialog.service';
import { Component, OnInit, Input, EventEmitter, Output, ViewChild, ElementRef, SimpleChanges, OnChanges } from '@angular/core';
import { SelectItem, InputSwitch, MenuItem } from 'primeng/primeng';
import { Workflow } from '../../../../../shared/models/workflow/workflow.model';
import { DotEditPageState } from '../../../../../shared/models/dot-edit-page-state/dot-edit-page-state.model';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DotGlobalMessageService } from '../../../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { PageMode } from '../../../shared/models/page-mode.enum';

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges {
    @ViewChild('locker') locker: InputSwitch;
    @ViewChild('lockedPageMessage') lockedPageMessage: ElementRef;

    @Input() canSave: boolean;
    @Input() pageWorkflows: Workflow[] = [];
    @Input() pageState: DotRenderedPageState;

    @Output() changeState = new EventEmitter<DotEditPageState>();
    @Output() save = new EventEmitter<MouseEvent>();

    states: SelectItem[] = [];
    workflowsActions: MenuItem[] = [];
    lockerModel: boolean;
    mode: PageMode;

    constructor(
        public dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotDialogService: DotDialogService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'editpage.toolbar.primary.action',
                'editpage.toolbar.edit.page',
                'editpage.toolbar.preview.page',
                'editpage.toolbar.live.page',
                'editpage.toolbar.page.locked.by.user',
                'editpage.toolbar.primary.workflow.actions',
                'dot.common.message.pageurl.copied.clipboard',
                'dot.common.message.pageurl.copied.clipboard.error',
                'editpage.toolbar.page.cant.edit',
                'editpage.content.steal.lock.confirmation.message.header',
                'editpage.content.steal.lock.confirmation.message'
            ])
            .subscribe(() => {
                this.workflowsActions = this.getWorkflowOptions();
                this.setFieldsModels(this.pageState);
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.page && !changes.page.firstChange) {
            this.setFieldsModels(changes.page.currentValue);
        }
    }

    /**
     * Copy url to clipboard
     *
     * @returns {boolean}
     * @memberof DotEditPageToolbarComponent
     */
    copyUrlToClipboard(): boolean {
        /*
            Aparently this is the only crossbrowser solution so far. If we do this in another place we might have
            to include an npm module.
        */
        const txtArea = document.createElement('textarea');

        txtArea.style.position = 'fixed';
        txtArea.style.top = '0';
        txtArea.style.left = '0';
        txtArea.style.opacity = '0';
        txtArea.value = this.pageState.page.pageURI;
        document.body.appendChild(txtArea);
        txtArea.select();

        let result;

        try {
            result = document.execCommand('copy');
            if (result) {
                this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.pageurl.copied.clipboard'));
            }
        } catch (err) {
            this.dotGlobalMessageService.error(this.dotMessageService.get('dot.common.message.pageurl.copied.clipboard.error'));
        }
        document.body.removeChild(txtArea);

        return result;
    }

    /**
     * Handle the click to the locker switch
     *
     * @param {any} $event
     * @memberof DotEditPageToolbarComponent
     */
    onLockerClick(_$event): void {
        const blinkClass = 'edit-page-toolbar__locked-by-message--blink';

        if (this.locker.disabled) {
            this.lockedPageMessage.nativeElement.classList.add(blinkClass);
            setTimeout(() => {
                this.lockedPageMessage.nativeElement.classList.remove(blinkClass);
            }, 500);
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
            this.dotDialogService.confirm({
                accept: () => {
                    this.setLockerState();
                },
                reject: () => {
                    this.lockerModel = false;
                },
                header: this.dotMessageService.get('editpage.content.steal.lock.confirmation.message.header'),
                message: this.dotMessageService.get('editpage.content.steal.lock.confirmation.message')
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
        if (this.shouldConfirmToLock()) {
            this.dotDialogService.confirm({
                accept: () => {
                    this.setSelectorState(pageState);
                },
                reject: () => {
                    this.lockerModel = false;
                    this.mode = PageMode.PREVIEW;
                },
                header: this.dotMessageService.get('editpage.content.steal.lock.confirmation_message.header'),
                message: this.dotMessageService.get('editpage.content.steal.lock.confirmation_message.message'),
                footerLabel: {}
            });
        } else {
            this.setSelectorState(pageState);
        }
    }

    private getWorkflowOptions(): MenuItem[] {
        return this.pageWorkflows.map((workflow: Workflow) => {
            return {
                label: workflow.name
            };
        });
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
        if (pageMode === PageMode.EDIT) {
            this.lockerModel = true;
        }

        this.changeState.emit({
            mode: pageMode,
            locked: this.lockerModel
        });
    }

    private setFieldsModels(pageState: DotRenderedPageState): void {
        this.lockerModel = pageState.state.mode === PageMode.EDIT;
        this.mode = pageState.state.mode;
        this.states = this.getStateModeOptions(pageState);
    }

    private getStateModeOptions(pageState: DotRenderedPageState): SelectItem[] {
        return [
            {
                label: this.dotMessageService.get('editpage.toolbar.edit.page'),
                value: PageMode.EDIT,
                styleClass:
                    !pageState.page.canEdit || !pageState.page.canLock ? 'edit-page-toolbar__state-selector-item--disabled' : ''
            },
            { label: this.dotMessageService.get('editpage.toolbar.preview.page'), value: PageMode.PREVIEW },
            { label: this.dotMessageService.get('editpage.toolbar.live.page'), value: PageMode.LIVE }
        ];
    }

    private shouldConfirmToLock(): boolean {
        return (this.lockerModel || this.mode === PageMode.EDIT) && this.pageState.state.lockedByAnotherUser;
    }
}
