import { Observable } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    inject
} from '@angular/core';

import { MenuItem } from 'primeng/api';

import { catchError, map, take, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWizardService,
    DotGlobalMessageService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotPage,
    DotWorkflowPayload
} from '@dotcms/dotcms-models';

// Check this component to create the Workflow Action for the Edit Page
@Component({
    selector: 'dot-edit-page-workflows-actions',
    templateUrl: './dot-edit-page-workflows-actions.component.html',
    styleUrls: ['./dot-edit-page-workflows-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DotEditPageWorkflowsActionsComponent implements OnChanges {
    private dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private dotWorkflowsActionsService = inject(DotWorkflowsActionsService);
    private dotMessageService = inject(DotMessageService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotGlobalMessageService = inject(DotGlobalMessageService);
    private dotWizardService = inject(DotWizardService);
    private dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);

    @Input() page: DotPage;

    @Output() fired: EventEmitter<DotCMSContentlet> = new EventEmitter();

    actionsAvailable: boolean;
    actions: Observable<MenuItem[]>;

    ngOnChanges(changes: SimpleChanges) {
        if (changes.page) {
            this.actions = this.getWorkflowActions(this.page.workingInode);
        }
    }

    private getWorkflowActions(inode: string): Observable<MenuItem[]> {
        return this.dotWorkflowsActionsService.getByInode(inode).pipe(
            tap((workflows: DotCMSWorkflowAction[]) => {
                this.actionsAvailable = !!workflows.length;
            }),
            map((newWorkflows: DotCMSWorkflowAction[]) => {
                return newWorkflows.length !== 0 ? this.getWorkflowOptions(newWorkflows) : [];
            })
        );
    }

    private getWorkflowOptions(workflows: DotCMSWorkflowAction[]): MenuItem[] {
        return workflows.map((workflow: DotCMSWorkflowAction) => {
            return {
                label: workflow.name,
                command: () => {
                    if (workflow.actionInputs.length) {
                        if (
                            this.dotWorkflowEventHandlerService.containsPushPublish(
                                workflow.actionInputs
                            )
                        ) {
                            this.dotWorkflowEventHandlerService
                                .checkPublishEnvironments()
                                .pipe(take(1))
                                .subscribe((hasEnviroments: boolean) => {
                                    if (hasEnviroments) {
                                        this.openWizard(workflow);
                                    }
                                });
                        } else {
                            this.openWizard(workflow);
                        }
                    } else {
                        this.fireWorkflowAction(workflow);
                    }
                }
            };
        });
    }

    private openWizard(workflow: DotCMSWorkflowAction): void {
        this.dotWizardService
            .open<DotWorkflowPayload>(
                this.dotWorkflowEventHandlerService.setWizardInput(
                    workflow,
                    this.dotMessageService.get('Workflow-Action')
                )
            )
            .pipe(take(1))
            .subscribe((data: DotWorkflowPayload) => {
                this.fireWorkflowAction(
                    workflow,
                    this.dotWorkflowEventHandlerService.processWorkflowPayload(
                        data,
                        workflow.actionInputs
                    )
                );
            });
    }

    private fireWorkflowAction<T = { [key: string]: string }>(
        workflow: DotCMSWorkflowAction,
        data?: T
    ): void {
        const currentMenuActions = this.actions;
        this.dotWorkflowActionsFireService
            .fireTo({
                inode: this.page.workingInode,
                actionId: workflow.id,
                data
            })
            .pipe(
                take(1),
                catchError((error) => {
                    this.httpErrorManagerService.handle(error);

                    return currentMenuActions;
                })
            )
            .subscribe((contentlet: DotCMSContentlet) => {
                this.dotGlobalMessageService.display(
                    this.dotMessageService.get('editpage.actions.fire.confirmation', workflow.name)
                );
                const newInode = contentlet.inode || this.page.workingInode;
                this.fired.emit(contentlet);
                this.actions = this.getWorkflowActions(newInode);
            });
    }
}
