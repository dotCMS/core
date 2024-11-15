import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotFireActionOptions,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSWorkflowAction,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import { withInformation } from './features/information.feature';
import { withSidebar } from './features/sidebar.feature';
import { withWorkflow } from './features/workflow.feature';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { transformFormDataFn } from '../../../utils/functions.util';

export interface EditContentState {
    actions: DotCMSWorkflowAction[];
    contentType: DotCMSContentType | null;
    contentlet: DotCMSContentlet | null;
    state: ComponentStatus;

    error: string | null;
}

const initialState: EditContentState = {
    contentType: null,
    contentlet: null,
    actions: [],
    state: ComponentStatus.INIT,
    error: null
};

/**
 * The DotEditContentStore is a state management store used in the DotCMS content editing application.
 * It provides state, computed properties, methods, and hooks for managing the application state
 * related to content editing and workflow actions.
 */
export const DotEditContentStore = signalStore(
    withState<EditContentState>(initialState),
    withComputed((store) => ({
        /**
         * Computed property that determines if the new content editor feature is enabled.
         *
         * This function retrieves the content type from the store, accesses its metadata,
         * and checks whether the content editor feature flag is set to true.
         *
         * @returns {boolean} True if the new content editor feature is enabled, false otherwise.
         */
        isEnabledNewContentEditor: computed(() => {
            const contentType = store.contentType();
            if (!contentType?.metadata) {
                return false;
            }

            return (
                contentType.metadata[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true
            );
        }),

        /**
         * A computed property that checks if the store is in a loading or saving state.
         *
         * @returns {boolean} True if the store's status is either LOADING or SAVING, false otherwise.
         */
        isLoading: computed(
            () =>
                store.state() === ComponentStatus.LOADING ||
                store.state() === ComponentStatus.SAVING
        ),

        /**
         * Computed property that determines if the store's status is equal to ComponentStatus.LOADED.
         *
         * @returns {boolean} - Returns true if the store's status is LOADED, otherwise false.
         */
        isLoaded: computed(() => store.state() === ComponentStatus.LOADED),

        /**
         * Computed property that determines if the store's status is equal to ComponentStatus.SAVING.
         */
        isSaving: computed(() => store.state() === ComponentStatus.SAVING),

        /**
         * A computed property that checks if an error exists in the store.
         *
         * @returns {boolean} True if there is an error in the store, false otherwise.
         */
        hasError: computed(() => !!store.error()),

        /**
         * Returns computed form data.
         *
         * @return {Object} The form data containing `contentlet` and `contentType`.
         * - contentlet: The current contentlet from the store.
         * - contentType: The current content type from the store.
         */
        formData: computed(() => {
            return {
                contentlet: store.contentlet(),
                contentType: store.contentType()
            };
        }),

        /**
         * Computed property that transforms the layout of the current content type
         * into tabs and returns them.
         */
        tabs: computed(() => transformFormDataFn(store.contentType()))
    })),
    withMethods(
        (
            store,
            workflowActionService = inject(DotWorkflowsActionsService),
            workflowActionsFireService = inject(DotWorkflowActionsFireService),
            dotContentTypeService = inject(DotContentTypeService),
            dotEditContentService = inject(DotEditContentService),
            dotHttpErrorManagerService = inject(DotHttpErrorManagerService),
            messageService = inject(MessageService),
            dotMessageService = inject(DotMessageService),

            router = inject(Router)
        ) => ({
            /**
             * Method to initialize new content of a given type.
             * New content
             *
             * @param {string} contentType - The type of content to initialize.
             * @returns {Observable} An observable that completes when the initialization is done.
             */
            initializeNewContent: rxMethod<string>(
                pipe(
                    switchMap((contentType) => {
                        patchState(store, { state: ComponentStatus.LOADING });

                        return forkJoin({
                            contentType: dotContentTypeService.getContentType(contentType),
                            actions: workflowActionService.getDefaultActions(contentType)
                        }).pipe(
                            tapResponse({
                                next: ({ contentType, actions }) => {
                                    patchState(store, {
                                        contentType,
                                        actions,
                                        state: ComponentStatus.LOADED,
                                        error: null
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        state: ComponentStatus.ERROR,
                                        error: 'Error initializing content'
                                    });
                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            ),

            /**
             * Initializes the existing content by loading its details and updating the state.
             * Content existing
             *
             * @returns {Observable<string>} An observable that emits the content ID.
             */
            initializeExistingContent: rxMethod<string>(
                pipe(
                    switchMap((inode: string) => {
                        patchState(store, { state: ComponentStatus.LOADING });

                        return dotEditContentService.getContentById(inode).pipe(
                            switchMap((contentlet) => {
                                const { contentType } = contentlet;

                                return forkJoin({
                                    contentType: dotContentTypeService.getContentType(contentType),
                                    actions: workflowActionService.getByInode(
                                        inode,
                                        DotRenderMode.EDITING
                                    ),
                                    contentlet: of(contentlet)
                                });
                            }),
                            tapResponse({
                                next: ({ contentType, actions, contentlet }) => {
                                    patchState(store, {
                                        contentType,
                                        actions,
                                        contentlet,
                                        state: ComponentStatus.LOADED
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        state: ComponentStatus.ERROR,
                                        error: 'Error initializing content'
                                    });
                                    dotHttpErrorManagerService.handle(error);
                                    router.navigate(['/c/content']);
                                }
                            })
                        );
                    })
                )
            ),

            /**
             * Fires a workflow action and updates the component state accordingly.
             *
             * This method triggers a sequence of events to fire a workflow action
             * and handles the response or error. If the action is successful,
             * it navigates to the content view with the updated contentlet and actions.
             * In case of an error, it updates the state with an error message.
             *
             * @param options The options required to fire the workflow action.
             */
            fireWorkflowAction: rxMethod<DotFireActionOptions<{ [key: string]: string | object }>>(
                pipe(
                    tap(() => patchState(store, { state: ComponentStatus.SAVING })),
                    switchMap((options) => {
                        return workflowActionsFireService.fireTo(options).pipe(
                            tap((contentlet) => {
                                if (!contentlet.inode) {
                                    router.navigate(['/c/content']);
                                }
                            }),
                            switchMap((contentlet) => {
                                return forkJoin({
                                    actions: workflowActionService.getByInode(
                                        contentlet.inode,
                                        DotRenderMode.EDITING
                                    ),
                                    contentlet: of(contentlet)
                                });
                            }),
                            tapResponse({
                                next: ({ contentlet, actions }) => {
                                    router.navigate(['/content', contentlet.inode], {
                                        replaceUrl: true,
                                        queryParamsHandling: 'preserve'
                                    });

                                    patchState(store, {
                                        contentlet,
                                        actions,
                                        state: ComponentStatus.LOADED,
                                        error: null
                                    });
                                    messageService.add({
                                        severity: 'success',
                                        summary: dotMessageService.get('success'),
                                        detail: dotMessageService.get(
                                            'edit.content.success.workflow.message'
                                        )
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        state: ComponentStatus.LOADED,
                                        error: 'Error firing workflow action'
                                    });
                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            )
        })
    ),
    withSidebar(),
    withInformation(),
    withWorkflow(),
    withHooks({
        onInit(store) {
            const activatedRoute = inject(ActivatedRoute);
            const params = activatedRoute.snapshot?.params;

            if (params) {
                const contentType = params['contentType'];
                const inode = params['id'];

                // TODO: refactor this when we will use EditContent as sidebar
                if (inode) {
                    store.initializeExistingContent(inode);
                } else if (contentType) {
                    store.initializeNewContent(contentType);
                }
            }
        }
    })
);
