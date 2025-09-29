import { of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MessageService } from 'primeng/api';

import { catchError, filter, map, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSWorkflowAction, DotWorkflowPayload } from '@dotcms/dotcms-models';
import { DotWorkflowActionsComponent } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';

const MESSAGE_LIFETIMES = {
    SUCCESS: 2000,
    ERROR: 2000,
    EXECUTING: 1000
};

@Component({
    selector: 'dot-uve-workflow-actions',
    imports: [DotWorkflowActionsComponent],
    providers: [
        DotWorkflowActionsFireService,
        DotWorkflowEventHandlerService,
        DotHttpErrorManagerService,
        DotWorkflowsActionsService,
        DotMessageService,
        MessageService
    ],
    templateUrl: './dot-uve-workflow-actions.component.html',
    styleUrl: './dot-uve-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUVEWorkflowActionsComponent {
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #httpErrorManagerService = inject(DotHttpErrorManagerService);
    readonly #dotWizardService = inject(DotWizardService);
    readonly #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    readonly #messageService = inject(MessageService);
    readonly #dotWorkflowsActionsService = inject(DotWorkflowsActionsService);
    readonly #uveStore = inject(UVEStore);

    $canEdit = this.#uveStore.$canEdit;
    $inode = computed(() => this.#uveStore.pageAssetData()?.page.inode || '');
    $workflowActions = signal<DotCMSWorkflowAction[]>([]);
    $workflowLoading = signal(true);

    private readonly successMessage = {
        severity: 'info' as const,
        summary: this.#dotMessageService.get('Workflow-Action'),
        detail: this.#dotMessageService.get('edit.content.fire.action.success'),
        life: MESSAGE_LIFETIMES.SUCCESS
    };

    private readonly errorMessage = {
        severity: 'error' as const,
        summary: this.#dotMessageService.get('Workflow-Action'),
        detail: this.#dotMessageService.get('edit.ema.page.error.executing.workflow.action'),
        life: MESSAGE_LIFETIMES.ERROR
    };

    constructor() {
        // Load workflow actions when inode changes - with automatic cleanup
        effect(() => {
            const inode = this.$inode();
            if (inode) {
                untracked(() => this.#loadWorkflowActions(inode));
            }
        });
    }

    /**
     * Load workflow actions for a given inode
     *
     * @param {string} inode
     * @private
     */
    #loadWorkflowActions(inode: string): void {
        this.$workflowLoading.set(true);
        this.#dotWorkflowsActionsService
            .getByInode(inode)
            .pipe(
                catchError((error) => {
                    console.error('Error loading workflow actions:', error);
                    this.#messageService.add({
                        severity: 'warn',
                        summary: this.#dotMessageService.get('Workflow-Action'),
                        detail: this.#dotMessageService.get(
                            'edit.ema.page.error.loading.workflow.actions'
                        ),
                        life: MESSAGE_LIFETIMES.ERROR
                    });
                    return of([]);
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((workflowActions: DotCMSWorkflowAction[]) => {
                this.$workflowActions.set(workflowActions);
                this.$workflowLoading.set(false);
            });
    }

    /**
     * Handle the action trigger
     *
     * @param {DotCMSWorkflowAction} workflow
     * @memberof DotUveWorkflowActionsComponent
     */
    protected handleActionTrigger(workflow: DotCMSWorkflowAction): void {
        const { actionInputs = [] } = workflow;
        const isPushPublish =
            this.#dotWorkflowEventHandlerService.containsPushPublish(actionInputs);

        if (!actionInputs.length) {
            this.#fireWorkflowAction(workflow);

            return;
        }

        if (!isPushPublish) {
            this.#openWizard(workflow);

            return;
        }

        this.#dotWorkflowEventHandlerService
            .checkPublishEnvironments()
            .pipe(
                take(1),
                filter((hasEnviroments: boolean) => hasEnviroments),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(() => this.#openWizard(workflow));
    }

    /**
     * Open the wizard
     *
     * @param {DotCMSWorkflowAction} workflow
     * @memberof DotUveWorkflowActionsComponent
     */
    #openWizard(workflow: DotCMSWorkflowAction): void {
        const title = this.#dotMessageService.get('Workflow-Action');
        const wizardInput = this.#dotWorkflowEventHandlerService.setWizardInput(workflow, title);

        if (!wizardInput) {
            return;
        }

        this.#dotWizardService
            .open<DotWorkflowPayload>(wizardInput)
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((data: DotWorkflowPayload) => {
                this.#fireWorkflowAction(
                    workflow,
                    this.#dotWorkflowEventHandlerService.processWorkflowPayload(
                        data,
                        workflow.actionInputs
                    )
                );
            });
    }

    /**
     * Fire the workflow action
     *
     * @param {DotCMSWorkflowAction} workflow
     * @param {T} data
     * @memberof DotUveWorkflowActionsComponent
     */
    #fireWorkflowAction<T = { [key: string]: string }>(
        workflow: DotCMSWorkflowAction,
        data?: T
    ): void {
        this.#messageService.add({
            ...this.successMessage,
            detail: this.#dotMessageService.get('edit.ema.page.executing.workflow.action'),
            life: MESSAGE_LIFETIMES.EXECUTING
        });

        this.#dotWorkflowActionsFireService
            .fireTo({
                inode: this.$inode(),
                actionId: workflow.id,
                data
            })
            .pipe(
                catchError((error) => {
                    console.error('Error executing workflow action:', error, workflow);
                    this.#messageService.add({
                        ...this.errorMessage,
                        detail: `${this.errorMessage.detail}: ${workflow.name}`
                    });

                    return this.#httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((contentlet: DotCMSContentlet | null) => {
                if (!contentlet) {
                    return;
                }

                this.handleNewContent(contentlet);
                this.#messageService.add(this.successMessage);
            });
    }

    /**
     * Handle a new page event. This event is triggered when the page changes for a Workflow Action
     * Currently handles contentlet updates after workflow execution.
     *
     * @param {DotCMSContentlet} pageAsset - The updated page contentlet
     * @memberof DotUVEWorkflowActionsComponent
     */
    protected handleNewContent(_pageAsset: DotCMSContentlet): void {
        // TODO: Implement page reload logic for workflow actions that change page structure
        // Test manually this case with the MOVE workflow action that might change URL or language
        // For now, we'll let the UVE store handle content updates
        // Future implementation should check if URL or language changed and reload accordingly
    }
}
