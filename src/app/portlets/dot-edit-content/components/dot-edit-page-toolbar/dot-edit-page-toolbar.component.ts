import { DotConfirmationService } from './../../../../api/services/dot-confirmation/dot-confirmation.service';
import { PageMode } from './../../shared/page-mode.enum';
import { Component, OnInit, Input, EventEmitter, Output, ViewChild, ElementRef } from '@angular/core';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { SelectItem, MenuItem, InputSwitch } from 'primeng/primeng';
import { Workflow } from '../../../../shared/models/workflow/workflow.model';
import { DotRenderedPage } from '../../../dot-edit-page/shared/models/dot-rendered-page.model';
import { DotEditPageState } from '../../../../shared/models/dot-edit-page-state/dot-edit-page-state.model';
import { DotGlobalMessageService } from '../../../../view/components/_common/dot-global-message/dot-global-message.service';

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit {
    @ViewChild('locker') locker: InputSwitch;
    @ViewChild('lockedPageMessage') lockedPageMessage: ElementRef;

    @Input() canSave: boolean;
    @Input() pageWorkflows: Workflow[] = [];
    @Input() page: DotRenderedPage;
    @Input() mode: PageMode;

    @Output() changeState = new EventEmitter<DotEditPageState>();
    @Output() save = new EventEmitter<MouseEvent>();

    states: SelectItem[] = [];
    workflowsActions: MenuItem[] = [];
    lockerModel: boolean;

    constructor(
        public dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotConfirmationService: DotConfirmationService
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
                'dot.common.message.pageurl.copied.clipboard.error'
            ])
            .subscribe((res) => {
                this.states = [
                    {
                        label: res['editpage.toolbar.edit.page'],
                        value: PageMode.EDIT,
                        styleClass: !this.page.canLock ? 'edit-page-toolbar__state-selector-item--disabled' : ''
                    },
                    { label: res['editpage.toolbar.preview.page'], value: PageMode.PREVIEW },
                    { label: res['editpage.toolbar.live.page'], value: PageMode.LIVE }
                ];
            });

        this.workflowsActions = this.getWorkflowOptions();

        this.lockerModel = this.page.lockedByAnotherUser && this.page.canLock ? false : this.page.locked;
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
        txtArea.value = this.page.pageURI;
        document.body.appendChild(txtArea);
        txtArea.select();

        let result;

        try {
            result = document.execCommand('copy');
            if (result) {
                this.dotGlobalMessageService.display(
                    this.dotMessageService.get('dot.common.message.pageurl.copied.clipboard')
                );
            }
        } catch (err) {
            this.dotGlobalMessageService.error(
                this.dotMessageService.get('dot.common.message.pageurl.copied.clipboard.error')
            );
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
    lockerHandler($event): void {
        const blinkClass = 'edit-page-toolbar__cant-lock-message--blink';

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
            this.dotConfirmationService.confirm({
                accept: () => {
                    this.setLockerState();
                },
                reject: () => {
                    this.lockerModel = false;
                },
                header: this.dotMessageService.get('editpage.content.steal.lock.confirmation_message.header'),
                message: this.dotMessageService.get('editpage.content.steal.lock.confirmation_message.message'),
                footerLabel: {}
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
        const state: DotEditPageState = {
            mode: pageState
        };

        if (!this.lockerModel && pageState === PageMode.EDIT) {
            this.lockerModel = pageState === PageMode.EDIT;
            state.locked = this.lockerModel;
        }

        this.changeState.emit(state);
    }

    private getAutoUpdatedMode(): PageMode {
        if (this.shouldGoToPreview()) {
            return PageMode.PREVIEW;
        }
        if (this.shouldGoToEdit()) {
            return PageMode.EDIT;
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
        const state: DotEditPageState = {
            locked: this.lockerModel
        };

        if (this.mode !== PageMode.LIVE) {
            this.mode = this.getAutoUpdatedMode();
            state.mode = this.mode;
        }

        this.changeState.emit(state);
    }

    private shouldConfirmToLock(): boolean {
        return this.lockerModel && this.page.lockedByAnotherUser;
    }

    private shouldGoToEdit(): boolean {
        return this.lockerModel && this.mode === PageMode.PREVIEW;
    }

    private shouldGoToPreview(): boolean {
        return !this.lockerModel && this.mode === PageMode.EDIT;
    }
}
