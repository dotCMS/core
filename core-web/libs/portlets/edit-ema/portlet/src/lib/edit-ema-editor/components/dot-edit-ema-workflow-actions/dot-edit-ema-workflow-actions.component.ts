import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    inject,
    signal
} from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { catchError, map, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSWorkflowAction, DotWorkflowPayload } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-ema-workflow-actions',
    standalone: true,
    imports: [DotWorkflowActionsComponent, ButtonModule, DotMessagePipe],
    providers: [
        DotWorkflowActionsFireService,
        DotWorkflowEventHandlerService,
        DotWorkflowsActionsService,
        DotHttpErrorManagerService
    ],
    templateUrl: './dot-edit-ema-workflow-actions.component.html',
    styleUrl: './dot-edit-ema-workflow-actions.component.css'
})
export class DotEditEmaWorkflowActionsComponent implements OnChanges {
    @Input({ required: true }) inode: string;
    @Output() newPage: EventEmitter<DotCMSContentlet> = new EventEmitter();

    protected actions = signal<DotCMSWorkflowAction[]>([]);
    protected loading = signal<boolean>(true);

    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly dotWorkflowsActionsService = inject(DotWorkflowsActionsService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly httpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotWizardService = inject(DotWizardService);
    private readonly dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    private readonly messageService = inject(MessageService);

    private readonly successMessage = {
        severity: 'info',
        summary: this.dotMessageService.get('Workflow-Action'),
        detail: this.dotMessageService.get('edit.content.fire.action.success'),
        life: 2000
    };

    private readonly errorMessage = {
        severity: 'error',
        summary: this.dotMessageService.get('Workflow-Action'),
        detail: this.dotMessageService.get('edit.ema.page.error.executing.workflow.action'),
        life: 2000
    };

    ngOnChanges(changes: SimpleChanges) {
        if (changes.inode) {
            this.loadWorkflowActions(this.inode);
        }
    }

    handleActionTrigger(workflow: DotCMSWorkflowAction): void {
        const { actionInputs = [] } = workflow;
        const isPushPublish = this.dotWorkflowEventHandlerService.containsPushPublish(actionInputs);

        if (!actionInputs.length) {
            this.fireWorkflowAction(workflow);

            return;
        }

        if (!isPushPublish) {
            this.openWizard(workflow);

            return;
        }

        this.dotWorkflowEventHandlerService
            .checkPublishEnvironments()
            .pipe(take(1))
            .subscribe((hasEnviroments: boolean) => {
                if (hasEnviroments) {
                    this.openWizard(workflow);
                }
            });
    }

    private loadWorkflowActions(inode: string): void {
        this.loading.set(true);
        this.dotWorkflowsActionsService
            .getByInode(inode)
            .pipe(
                map((newWorkflows: DotCMSWorkflowAction[]) => {
                    return newWorkflows || [];
                })
            )
            .subscribe((newWorkflows: DotCMSWorkflowAction[]) => {
                this.loading.set(false);
                this.actions.set(newWorkflows);
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
        this.loading.set(true);
        this.messageService.add({
            ...this.successMessage,
            detail: this.dotMessageService.get('edit.ema.page.executing.workflow.action'),
            life: 1000
        });

        this.dotWorkflowActionsFireService
            .fireTo({
                inode: this.inode,
                actionId: workflow.id,
                data
            })
            .pipe(
                catchError((error) => {
                    this.messageService.add(this.errorMessage);

                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            )
            .subscribe((contentlet: DotCMSContentlet) => {
                this.loading.set(false);

                if (!contentlet) {
                    return;
                }

                const { inode } = contentlet;
                this.newPage.emit(contentlet);
                this.inode = inode;
                this.loadWorkflowActions(inode);
                this.messageService.add(this.successMessage);
            });
    }
}
