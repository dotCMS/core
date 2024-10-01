import { tapResponse } from '@ngrx/component-store';
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

import { switchMap, tap } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/angular';
import {
    DotContentTypeService,
    DotFireActionOptions,
    DotHttpErrorManagerService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentType,
    DotCMSWorkflowAction,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { getPersistSidebarState, setPersistSidebarState } from '../../../utils/functions.util';

interface EditContentState {
    actions: DotCMSWorkflowAction[];
    contentType: DotCMSContentType;
    contentlet: DotCMSContentlet;
    status: ComponentStatus;
    showSidebar: boolean;
    error?: string;
}

const initialState: EditContentState = {
    contentType: {} as DotCMSContentType,
    contentlet: {} as DotCMSContentlet,
    actions: [],
    status: ComponentStatus.INIT,
    showSidebar: false,
    error: ''
};

export const DotEditContentStore = signalStore(
    withState(initialState),
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
                store.status() === ComponentStatus.LOADING ||
                store.status() === ComponentStatus.SAVING
        ),

        /**
         * Computed property that determines if the store's status is equal to ComponentStatus.LOADED.
         *
         * @returns {boolean} - Returns true if the store's status is LOADED, otherwise false.
         */
        isLoaded: computed(() => store.status() === ComponentStatus.LOADED),

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
                        patchState(store, { status: ComponentStatus.LOADING });

                        return forkJoin({
                            contentType: dotContentTypeService.getContentType(contentType),
                            actions: workflowActionService.getDefaultActions(contentType)
                        }).pipe(
                            tapResponse({
                                next: ({ contentType, actions }) => {
                                    patchState(store, {
                                        contentType,
                                        actions,
                                        status: ComponentStatus.LOADED,
                                        error: null
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
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
                        patchState(store, { status: ComponentStatus.LOADING });

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
                                        status: ComponentStatus.LOADED
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
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
                    tap(() => patchState(store, { status: ComponentStatus.SAVING })),
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
                                        status: ComponentStatus.LOADED,
                                        error: null
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        error: 'Error firing workflow action'
                                    });
                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            ),

            /**
             * Toggles the visibility of the sidebar by updating the application state
             * and persists the sidebar's state to ensure consistency across sessions.
             */
            toggleSidebar: () => {
                patchState(store, { showSidebar: !store.showSidebar() });
                setPersistSidebarState(store.showSidebar() as unknown as string);
            },

            /**
             * Fetches the persistence data from the local storage and updates the application state.
             * Utilizes the `patchState` function to update the global store with the persisted sidebar state.
             */
            getPersistenceDataFromLocalStore: () => {
                patchState(store, { showSidebar: getPersistSidebarState() });
            }
        })
    ),
    withHooks({
        onInit(store) {
            const activatedRoute = inject(ActivatedRoute);
            const contentType = activatedRoute.snapshot.params['contentType'];
            const inode = activatedRoute.snapshot.params['id'];

            // TODO: refactor this when we will use EditContent as sidebar
            if (inode) {
                store.initializeExistingContent(inode);
            } else if (contentType) {
                store.initializeNewContent(contentType);
            }

            store.getPersistenceDataFromLocalStore();
        }
    })
);
