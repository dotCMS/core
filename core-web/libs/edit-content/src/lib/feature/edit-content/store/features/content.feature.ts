import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { switchMap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotRenderMode,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSWorkflow,
    DotCMSWorkflowAction,
    FeaturedFlags,
    WorkflowStep
} from '@dotcms/dotcms-models';

import { WorkflowState } from './workflow.feature';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { transformFormDataFn } from '../../../../utils/functions.util';
import { parseWorkflows } from '../../../../utils/workflows.utils';
import { EditContentRootState } from '../edit-content.store';

export interface ContentState {
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
}

export const contentInitialState: ContentState = {
    contentType: null,
    contentlet: null,
    schemes: {}
};

export function withContent() {
    return signalStoreFeature(
        { state: type<EditContentRootState & WorkflowState>() },
        withState(contentInitialState),
        withComputed((store) => ({
            /**
             * Computed property that determines if the content is new.
             * Content is considered new when there is no contentlet in the store.
             *
             * @returns {boolean} True if content is new, false otherwise
             */
            isNew: computed(() => !store.contentlet()),

            /**
             * Computed property that determines if the store's status is equal to ComponentStatus.LOADED.
             *
             * @returns {boolean} - Returns true if the store's status is LOADED, otherwise false.
             */
            isLoaded: computed(() => store.state() === ComponentStatus.LOADED),

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
             * Computed property that determines if the new content editor feature is enabled.
             *
             * This function retrieves the content type from the store, accesses its metadata,
             * and checks whether the content editor feature flag is set to true.
             *
             * @returns {boolean} True if the new content editor feature is enabled, false otherwise.
             */
            isEnabledNewContentEditor: computed(() => {
                const contentType = store.contentType();
                const metadata = contentType?.metadata;

                return metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true;
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
             * Computed property that determines if the store's status is equal to ComponentStatus.SAVING.
             */
            isSaving: computed(() => store.state() === ComponentStatus.SAVING)
        })),
        withMethods(
            (
                store,
                dotContentTypeService = inject(DotContentTypeService),
                dotEditContentService = inject(DotEditContentService),
                workflowActionService = inject(DotWorkflowsActionsService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService),
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
                                            error: 'edit.content.sidebar.information.error.initializing.content'
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
                                        contentType:
                                            dotContentTypeService.getContentType(contentType),
                                        currentContentActions: workflowActionService.getByInode(
                                            inode,
                                            DotRenderMode.EDITING
                                        ),
                                        schemes:
                                            workflowActionService.getWorkFlowActions(contentType),
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
                                            error: 'edit.content.sidebar.information.error.initializing.content'
                                        });
                                        dotHttpErrorManagerService.handle(error);
                                        router.navigate(['/c/content']);
                                    }
                                })
                            );
                        })
                    )
                )
            })
        )
    );
}
