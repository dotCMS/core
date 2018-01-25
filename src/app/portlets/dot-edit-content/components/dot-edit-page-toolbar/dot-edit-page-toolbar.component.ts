import { Component, OnInit, Input, EventEmitter, Output, ViewChild } from '@angular/core';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { SelectItem, MenuItem, SplitButton, InputSwitch } from 'primeng/primeng';
import { Workflow } from '../../../../shared/models/workflow/workflow.model';

export enum PageState {
    EDIT,
    PREVIEW,
    LIVE
}

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit {
    @Input() canSave: boolean;
    @Input() pageLocked: boolean;
    @Input() pageTitle: string;
    @Input() pageUrl: string;
    @Input() pageWorkflows: Workflow[];

    @Output() lockPage = new EventEmitter<boolean>();
    @Output() pageState = new EventEmitter<PageState>();
    @Output() save = new EventEmitter<MouseEvent>();

    states: SelectItem[] = [];
    stateSelected = PageState.PREVIEW;
    workflowsActions: MenuItem[] = [];

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'editpage.toolbar.primary.action',
                'editpage.toolbar.edit.page',
                'editpage.toolbar.preview.page',
                'editpage.toolbar.live.page',
                'editpage.toolbar.primary.workflow.actions'
            ])
            .subscribe((res) => {
                this.states = [
                    { label: res['editpage.toolbar.edit.page'], value: PageState.EDIT },
                    { label: res['editpage.toolbar.preview.page'], value: PageState.PREVIEW },
                    { label: res['editpage.toolbar.live.page'], value: PageState.LIVE }
                ];
            });

        if (this.pageWorkflows) {
            this.workflowsActions = this.pageWorkflows.map((workflow: Workflow) => {
                return {
                    label: workflow.name
                };
            });
        }
    }

    /**
     * Handler locker change event
     *
     * @param {any} $event
     * @memberof DotEditPageToolbarComponent
     */
    lockPageHandler($event): void {
        this.lockPage.emit(this.pageLocked);

        if (!this.pageLocked && this.stateSelected === PageState.EDIT) {
            this.setState(PageState.PREVIEW);
        } else if (this.pageLocked && this.stateSelected === PageState.PREVIEW) {
            this.setState(PageState.EDIT);
        }
    }

    /**
     * Handle state selector change event
     *
     * @param {any} $event
     * @memberof DotEditPageToolbarComponent
     */
    stateSelectorHandler(state: PageState): void {
        this.pageState.emit(this.stateSelected);

        if (!this.pageLocked) {
            this.pageLocked = state === PageState.EDIT;
            this.lockPage.emit(this.pageLocked);
        }
    }

    private setState(state: PageState) {
        this.stateSelected = state;
        this.pageState.emit(this.stateSelected);
    }
}
