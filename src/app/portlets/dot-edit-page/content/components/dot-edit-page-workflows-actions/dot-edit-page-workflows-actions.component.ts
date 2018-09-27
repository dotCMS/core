import { of as observableOf, Observable } from 'rxjs';
import {
    Component,
    Input,
    OnChanges,
    SimpleChanges,
    OnInit,
    Output,
    EventEmitter
} from '@angular/core';
import { MenuItem } from 'primeng/primeng';
import { DotWorkflowAction } from '@models/dot-workflow-action/dot-workflow-action.model';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotPage } from '../../../shared/models/dot-page.model';
import { DotMessageService } from '@services/dot-messages-service';

import { tap, map, mergeMap, catchError, pluck } from 'rxjs/operators';

@Component({
    selector: 'dot-edit-page-workflows-actions',
    templateUrl: './dot-edit-page-workflows-actions.component.html',
    styleUrls: ['./dot-edit-page-workflows-actions.component.scss']
})
export class DotEditPageWorkflowsActionsComponent implements OnInit, OnChanges {
    @Input()
    page: DotPage;

    @Output()
    fired: EventEmitter<any> = new EventEmitter();

    actionsAvailable: boolean;
    actions: Observable<MenuItem[]>;

    constructor(
        private workflowsService: DotWorkflowService,
        private dotMessageService: DotMessageService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotGlobalMessageService: DotGlobalMessageService
    ) {}

    ngOnInit() {
        this.dotMessageService.getMessages(['editpage.actions.fire.confirmation']).subscribe();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.page) {
            this.actions = this.getWorkflowActions(this.page.workingInode);
        }
    }

    private getWorkflowActions(inode: string): Observable<MenuItem[]> {
        return this.workflowsService.getContentWorkflowActions(inode).pipe(
            tap((workflows: DotWorkflowAction[]) => {
                this.actionsAvailable = !!workflows.length;
            }),
            map((newWorkflows: DotWorkflowAction[]) => {
                return newWorkflows.length !== 0 ? this.getWorkflowOptions(newWorkflows) : [];
            })
        );
    }

    private getWorkflowOptions(workflows: DotWorkflowAction[]): MenuItem[] {
        return workflows.map((workflow: DotWorkflowAction) => {
            return {
                label: workflow.name,
                command: () => {
                    const currentMenuActions = this.actions;
                    this.actions = this.workflowsService
                        .fireWorkflowAction(this.page.workingInode, workflow.id)
                        .pipe(
                            pluck('inode'),
                            tap(() => {
                                this.dotGlobalMessageService.display(
                                    this.dotMessageService.get(
                                        'editpage.actions.fire.confirmation',
                                        workflow.name
                                    )
                                );
                            }),
                            // TODO: A better implementation needs to be done to handle workflow actions errors, which are edge cases
                            catchError(() => observableOf(null)),
                            mergeMap((inode: string) => {
                                const newInode = inode || this.page.workingInode;
                                this.fired.emit();
                                return this.getWorkflowActions(newInode);
                            }),
                            catchError((error) => {
                                this.httpErrorManagerService.handle(error);
                                return currentMenuActions;
                            })
                        );
                }
            };
        });
    }
}
