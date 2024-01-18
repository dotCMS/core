import { AsyncPipe } from '@angular/common';
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
    imports: [AsyncPipe, DotWorkflowActionsComponent, ButtonModule, DotMessagePipe],
    templateUrl: './dot-edit-ema-workflow-actions.component.html',
    styleUrl: './dot-edit-ema-workflow-actions.component.css'
})
export class DotEditEmaWorkflowActionsComponent implements OnChanges {
    @Input({ required: true }) inodeOrIdentifier: string;
    @Output() newPage: EventEmitter<DotCMSContentlet> = new EventEmitter();

    protected actions = signal<DotCMSWorkflowAction[]>(null);
    protected loading = signal<boolean>(false);

    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly dotWorkflowsActionsService = inject(DotWorkflowsActionsService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly httpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotWizardService = inject(DotWizardService);
    private readonly dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);

    ngOnChanges(changes: SimpleChanges) {
        if (changes.inodeOrIdentifier) {
            this.loadWorkflowActions(this.inodeOrIdentifier);
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
        this.dotWorkflowActionsFireService
            .fireTo({
                inode: this.inodeOrIdentifier,
                actionId: workflow.id,
                data
            })
            .pipe(catchError((error) => this.httpErrorManagerService.handle(error)))
            .subscribe((contentlet: DotCMSContentlet) => {
                const inode = contentlet?.inode;
                this.newPage.emit(contentlet);
                if (inode !== this.inodeOrIdentifier) {
                    this.inodeOrIdentifier = inode;
                    this.loadWorkflowActions(inode);
                } else {
                    this.loading.set(false);
                }
            });
    }
}
