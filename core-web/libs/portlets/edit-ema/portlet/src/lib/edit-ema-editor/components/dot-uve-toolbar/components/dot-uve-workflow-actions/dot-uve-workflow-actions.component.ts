import { Component, computed, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { catchError, map, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSWorkflowAction, DotWorkflowPayload } from '@dotcms/dotcms-models';
import { DotWorkflowActionsComponent } from '@dotcms/ui';

import { UVEStore } from '../../../../../store/dot-uve.store';
import { getPageURI, compareUrlPaths } from '../../../../../utils';

@Component({
    selector: 'dot-uve-workflow-actions',
    imports: [DotWorkflowActionsComponent, ButtonModule],
    providers: [
        DotWorkflowActionsFireService,
        DotWorkflowEventHandlerService,
        DotHttpErrorManagerService
    ],
    templateUrl: './dot-uve-workflow-actions.component.html',
    styleUrl: './dot-uve-workflow-actions.component.css'
})
export class DotUveWorkflowActionsComponent {
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly httpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotWizardService = inject(DotWizardService);
    private readonly dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    private readonly messageService = inject(MessageService);
    readonly #uveStore = inject(UVEStore);

    inode = computed(() => this.#uveStore.pageAPIResponse()?.page.inode);
    actions = this.#uveStore.workflowActions;
    loading = this.#uveStore.workflowLoading;
    canEdit = this.#uveStore.canEditPage;

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
        this.#uveStore.setWorkflowActionLoading(true);
        this.messageService.add({
            ...this.successMessage,
            detail: this.dotMessageService.get('edit.ema.page.executing.workflow.action'),
            life: 1000
        });

        this.dotWorkflowActionsFireService
            .fireTo({
                inode: this.inode(),
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
                if (!contentlet) {
                    return;
                }

                this.handleNewContent(contentlet);
                this.messageService.add(this.successMessage);
            });
    }

    /**
     * Handle a new page event. This event is triggered when the page changes for a Workflow Action
     * Update the query params if the url or the language id changed
     *
     * @param {DotCMSContentlet} page
     * @memberof EditEmaToolbarComponent
     */
    protected handleNewContent(pageAsset: DotCMSContentlet): void {
        const currentParams = this.#uveStore.pageParams();

        const url = getPageURI(pageAsset);
        const language_id = pageAsset.languageId?.toString();

        const urlChanged = !compareUrlPaths(url, currentParams.url);
        const languageChanged = language_id !== currentParams.language_id;

        if (urlChanged || languageChanged) {
            this.#uveStore.loadPageAsset({
                url,
                language_id
            });

            return;
        }

        this.#uveStore.reloadCurrentPage();
    }
}
