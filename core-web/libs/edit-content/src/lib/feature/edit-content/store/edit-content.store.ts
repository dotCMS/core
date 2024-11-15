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

import { MessageService, SelectItem } from 'primeng/api';

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
    DotCMSWorkflow,
    DotCMSWorkflowAction,
    FeaturedFlags,
    WorkflowStep,
    WorkflowTask
} from '@dotcms/dotcms-models';

import { withInformation } from './features/information.feature';
import { withSidebar } from './features/sidebar.feature';
import { withWorkflow } from './features/workflow.feature';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { parseWorkflows, transformFormDataFn } from '../../../utils/functions.util';
import { withDebug } from './features/debug.feature';

export interface EditContentState {
    /** ContentType full data */
    contentType: DotCMSContentType | null;
    /** Contentlet full data */
    contentlet: DotCMSContentlet | null;
    /** Schemas available for the content type */
    schemes: {
        [key: string]: {
            scheme: DotCMSWorkflow;
            actions: DotCMSWorkflowAction[];
            firstStep: WorkflowStep;
        };
    };
    /** Current workflow scheme id */
    currentSchemeId: string | null;
    /** Actions available for the current content */
    currentContentActions: DotCMSWorkflowAction[];
    /** Current workflow step */
    currentStep: WorkflowStep | null;
    /** Current workflow task */
    lastTask: WorkflowTask | null;
    state: ComponentStatus;
    error: string | null;
}

const initialState: EditContentState = {
    contentType: null,
    contentlet: null,
    schemes: {},
    currentSchemeId: null,
    currentContentActions: [],
    currentStep: null,
    lastTask: null,
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
        tabs: computed(() => transformFormDataFn(store.contentType())),

        /**
         * Computed property that determines if the workflow selection warning should be shown.
         * Shows warning when content is new AND no workflow scheme has been selected yet.
         *
         * @returns {boolean} True if warning should be shown, false otherwise
         */
        showSelectWorkflowWarning: computed(() => {
            const isNew = !store.contentlet();
            const hasNoSchemeSelected = !store.currentSchemeId();
            const hasMultipleSchemas = Object.keys(store.schemes()).length > 1;

            return isNew && hasMultipleSchemas && hasNoSchemeSelected;
        }),

        /**
         * Computed property that determines if workflow action buttons should be shown.
         * Shows workflow buttons when:
         * - Content type has only one workflow scheme OR
         * - Content is existing AND has a selected workflow scheme OR
         * - Content is new and content type has only one workflow scheme OR
         * - Content is new and has selected a workflow scheme
         * Hides workflow buttons when:
         * - Content is new and has multiple schemes without selection
         *
         * @returns {boolean} True if workflow action buttons should be shown, false otherwise
         */
        showWorkflowActions: computed(() => {
            const hasOneScheme = Object.keys(store.schemes()).length === 1;
            const isExisting = !!store.contentlet();
            const hasSelectedScheme = !!store.currentSchemeId();

            if (hasOneScheme) {
                return true;
            }

            if (isExisting && hasSelectedScheme) {
                return true;
            }

            if (!isExisting && hasSelectedScheme) {
                return true;
            }

            return false;
        }),

        /**
         * Computed property that transforms the workflow schemes into dropdown options
         * @returns Array of options with value (scheme id) and label (scheme name)
         */
        workflowSchemeOptions: computed<SelectItem[]>(() =>
            Object.entries(store.schemes()).map(([id, data]) => ({
                value: id,
                label: data.scheme.name
            }))
        ),

        /**
         * Computed property that determines if the content is new.
         * Content is considered new when there is no contentlet in the store.
         *
         * @returns {boolean} True if content is new, false otherwise
         */
        isNew: computed(() => !store.contentlet()),

        /**
         * Computed property that retrieves the actions for the current workflow scheme.
         *
         * @returns {DotCMSWorkflowAction[]} The actions for the current workflow scheme.
         */
        getActions: computed(() => {
            const isNew = !store.contentlet();
            const currentSchemeId = store.currentSchemeId();
            const schemes = store.schemes();
            const currentContentActions = store.currentContentActions();

            // If no scheme is selected, return empty array
            if (!currentSchemeId || !schemes[currentSchemeId]) {
                return [];
            }

            // For existing content, use specific contentlet actions
            if (!isNew && currentContentActions.length) {
                return currentContentActions;
            }

            // For new content, use scheme actions
            return Object.values(schemes[currentSchemeId].actions).sort((a, b) => {
                if (a.name === 'Save') return -1;
                if (b.name === 'Save') return 1;
                return a.name.localeCompare(b.name);
            });
        }),

        /**
         * Computed property that retrieves the first step of the current workflow scheme.
         *
         * @returns {WorkflowStep} The first step of the current workflow scheme.
         */
        getFirstStep: computed(() => {
            const schemes = store.schemes();
            const currentSchemeId = store.currentSchemeId();

            return schemes[currentSchemeId]?.firstStep;
        })
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
                            schemes: workflowActionService.getDefaultActions(contentType)
                        }).pipe(
                            tapResponse({
                                next: ({ contentType, schemes }) => {
                                    const parsedSchemes = parseWorkflows(schemes);
                                    const schemeIds = Object.keys(parsedSchemes);
                                    const defaultSchemeId =
                                        schemeIds.length === 1 ? schemeIds[0] : null;

                                    patchState(store, {
                                        contentType,

                                        schemes: parsedSchemes,
                                        currentSchemeId: defaultSchemeId,
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
                                    currentContentActions: workflowActionService.getByInode(
                                        inode,
                                        DotRenderMode.EDITING
                                    ),
                                    schemes: workflowActionService.getWorkFlowActions(contentType),
                                    contentlet: of(contentlet)
                                });
                            }),
                            tapResponse({
                                next: ({
                                    contentType,
                                    currentContentActions,
                                    schemes,
                                    contentlet
                                }) => {
                                    const parsedSchemes = parseWorkflows(schemes);
                                    patchState(store, {
                                        contentType,
                                        schemes: parsedSchemes,
                                        currentContentActions,
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
                                    currentContentActions: workflowActionService.getByInode(
                                        contentlet.inode,
                                        DotRenderMode.EDITING
                                    ),
                                    contentlet: of(contentlet)
                                });
                            }),
                            tapResponse({
                                next: ({ contentlet, currentContentActions }) => {
                                    router.navigate(['/content', contentlet.inode], {
                                        replaceUrl: true,
                                        queryParamsHandling: 'preserve'
                                    });

                                    patchState(store, {
                                        contentlet,
                                        currentContentActions,
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
    withDebug(),
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
