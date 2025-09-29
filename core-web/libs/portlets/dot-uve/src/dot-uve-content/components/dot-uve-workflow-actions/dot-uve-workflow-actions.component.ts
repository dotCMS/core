import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';

import { MessageService } from 'primeng/api';

import { catchError, filter, map, switchMap, take, tap } from 'rxjs/operators';

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
        severity: 'info',
        summary: this.#dotMessageService.get('Workflow-Action'),
        detail: this.#dotMessageService.get('edit.content.fire.action.success'),
        life: 2000
    };

    private readonly errorMessage = {
        severity: 'error',
        summary: this.#dotMessageService.get('Workflow-Action'),
        detail: this.#dotMessageService.get('edit.ema.page.error.executing.workflow.action'),
        life: 2000
    };

    constructor() {
        // This can probable come from the UVE Toolbar as input
        toObservable(this.$inode)
            .pipe(
                filter((inode) => !!inode),
                tap(() => this.$workflowLoading.set(true)),
                switchMap((inode) => this.#dotWorkflowsActionsService.getByInode(inode as string))
            )
            .subscribe((workflowActions) => {
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
                filter((hasEnviroments: boolean) => hasEnviroments)
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
            .pipe(take(1))
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
        // this.#uveStore.setWorkflowActionLoading(true);
        this.#messageService.add({
            ...this.successMessage,
            detail: this.#dotMessageService.get('edit.ema.page.executing.workflow.action'),
            life: 1000
        });

        this.#dotWorkflowActionsFireService
            .fireTo({
                inode: this.$inode(),
                actionId: workflow.id,
                data
            })
            .pipe(
                catchError((error) => {
                    this.#messageService.add(this.errorMessage);

                    return this.#httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
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
     * Update the query params if the url or the language id changed
     *
     * @param {DotCMSContentlet} _pageAsset
     * @memberof EditEmaToolbarComponent
     */
    protected handleNewContent(_pageAsset: DotCMSContentlet): void {
        // TODO: Check this case using the MOVE Workflow Action
        // const currentParams = this.#uveStore.configuration();
        // const url = getPageURI(pageAsset);
        // const language_id = pageAsset.languageId?.toString();
        // const urlChanged = !compareUrlPaths(url, currentParams.url);
        // const languageChanged = language_id !== currentParams.language_id;
        // if (urlChanged || languageChanged) {
        //     this.#uveStore.loadPageAsset({
        //         url,
        //         language_id
        //     });
        //     return;
        // }
        // this.#uveStore.reloadCurrentPage();
    }
}
