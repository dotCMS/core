import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { switchMap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotContentletDepth,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EDIT_CONTENT_HOST } from '../../../services/host/edit-content-host.model';
import { transformFormDataFn } from '../../../utils/functions.util';
import { parseCurrentActions, parseWorkflows } from '../../../utils/workflows.utils';
import { EditContentState } from '../../edit-content.store';

export function withContent() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withComputed((store) => ({
            /**
             * Computed property that determines if the content is new.
             * Content is considered new when there is no contentlet in the store.
             *
             * @returns {boolean} True if content is new, false otherwise
             */
            isNew: computed(() => store.initialContentletState() === 'new'),

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
             * Computed property that determines if the current content type is an HTML Page.
             * Single source of truth shared by the form and sidebar components.
             *
             * @returns {boolean} True when the content type's base type is HTMLPAGE.
             */
            isPage: computed(
                () => store.contentType()?.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE
            ),

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
            isSaving: computed(() => store.state() === ComponentStatus.SAVING),

            /**
             * True while new content is being fetched but the previously loaded
             * content is still in the store (an in-place reload — e.g. navigating
             * the related-content breadcrumb). `initializeExistingContent` sets the
             * state to LOADING without clearing `contentType`/`contentlet`, so the
             * layout can keep the old data on screen behind a non-destructive
             * loading overlay (stale-while-revalidate) instead of collapsing the
             * whole editor to a blank screen. False on the very first load, when
             * there is no prior content to show.
             */
            isReloading: computed(
                () => store.state() === ComponentStatus.LOADING && !!store.contentType()
            ),

            /**
             * True when BOTH sides of the editor are ready: the content (left) is
             * LOADED and the sidebar's initial data (right — reference pages and
             * activities) has settled. Used to hold the initial loading state until
             * the whole screen is populated, instead of revealing the form while the
             * sidebar still loads.
             *
             * There is nothing to wait for — so it returns as soon as the content is
             * LOADED — when the sidebar is closed (its data never loads) or the
             * content is new / has no identifier (reference pages and activities only
             * load for existing content). ERROR counts as settled so a failed sidebar
             * request never hangs the editor behind the loader.
             */
            isFullyLoaded: computed(() => {
                if (store.state() !== ComponentStatus.LOADED) {
                    return false;
                }

                if (!store.uiState().isSidebarOpen || !store.contentlet()?.identifier) {
                    return true;
                }

                const settled = (status: ComponentStatus) =>
                    status === ComponentStatus.LOADED || status === ComponentStatus.ERROR;

                return (
                    settled(store.information().status) && settled(store.activitiesStatus().status)
                );
            })
        })),
        withMethods(
            (
                store,
                dotContentTypeService = inject(DotContentTypeService),
                dotEditContentService = inject(DotEditContentService),
                workflowActionService = inject(DotWorkflowsActionsService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService),
                router = inject(Router),
                dotWorkflowService = inject(DotWorkflowService),
                dotMessageService = inject(DotMessageService),
                host = inject(EDIT_CONTENT_HOST)
            ) => ({
                /**
                 * Initializes the state for creating new content of a specified type.
                 * This method orchestrates the following operations:
                 *
                 * 1. Sets the component state to loading
                 * 2. Makes parallel API calls to:
                 *    - Fetch the complete content type definition
                 *    - Retrieve all available workflow schemes and their default actions
                 * 3. Processes the workflow schemes:
                 *    - Parses and organizes schemes by their IDs
                 *    - Automatically selects default scheme if only one exists
                 *    - Sets up initial available actions based on the default scheme
                 *
                 * @param {string} contentType - The identifier of the content type to initialize
                 * @returns {Observable} An observable that completes when all initialization data is loaded and processed
                 * @throws Will set error state and display error message if initialization fails
                 */
                initializeNewContent: rxMethod<string>(
                    pipe(
                        switchMap((contentType) => {
                            patchState(store, {
                                state: ComponentStatus.LOADING,
                                hiddenFields: {}
                            });

                            return forkJoin({
                                contentType:
                                    dotContentTypeService.getContentTypeWithRender(contentType),
                                schemes: workflowActionService.getDefaultActions(contentType)
                            }).pipe(
                                tapResponse({
                                    next: ({ contentType, schemes }) => {
                                        // Convert the schemes to an object with the schemeId as the key
                                        const parsedSchemes = parseWorkflows(schemes);
                                        const schemeIds = Object.keys(parsedSchemes);
                                        // If we have only one scheme, we set it as the default one
                                        const defaultSchemeId =
                                            schemeIds.length === 1 ? schemeIds[0] : null;
                                        // Parse the actions as an object with the schemeId as the key
                                        const parsedCurrentActions = parseCurrentActions(
                                            parsedSchemes[defaultSchemeId]?.actions || []
                                        );

                                        const titleString = `${dotMessageService.get('New')} ${contentType.variable}`;

                                        // The host decides whether these apply: the full-screen
                                        // host updates the title/breadcrumb, the dialog host
                                        // no-ops them (it overlays another route context).
                                        host.setContentTitle(titleString);
                                        host.addBreadcrumb({
                                            label: titleString,
                                            url: `/dotAdmin/#/content/new/${contentType.variable}`
                                        });

                                        patchState(store, {
                                            contentType,
                                            schemes: parsedSchemes,
                                            currentSchemeId: defaultSchemeId,
                                            currentContentActions: parsedCurrentActions,
                                            state: ComponentStatus.LOADED,
                                            initialContentletState: 'new',
                                            error: null,
                                            hiddenFields: {}
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
                 * Initializes and loads all necessary data for an existing content by its inode.
                 * This method orchestrates multiple API calls to set up the complete content state:
                 *
                 * 1. Fetches the contentlet data using the inode
                 * 2. Based on the contentlet's content type:
                 *    - Loads the full content type definition
                 *    - Retrieves available workflow actions for the current inode
                 *    - Fetches all possible workflow schemes for the content type
                 *    - Gets the current workflow status including step and task information
                 *
                 * All this information is then consolidated and stored in the state to manage
                 * the content's workflow progression and available actions.
                 *
                 * @param {string} inode - The unique identifier for the content to be loaded
                 * @returns {Observable<string>} An observable that emits the content's inode when initialization is complete
                 * @throws Will redirect to /c/content and show error if initialization fails
                 */
                initializeExistingContent: rxMethod<{ inode: string; depth: DotContentletDepth }>(
                    pipe(
                        switchMap(({ inode, depth }) => {
                            patchState(store, {
                                state: ComponentStatus.LOADING,
                                hiddenFields: {},
                                // The full-screen editor now reuses its component across
                                // content navigations, so the store persists. Clear the
                                // previous content's volatile, content-scoped slices to
                                // prevent leaks (version/push-publish lists accumulate for
                                // infinite scroll; the compare/historical views belong to
                                // the old inode). `contentlet`/`contentType` are kept on
                                // purpose so the previous content stays rendered until the
                                // new data loads (stale-while-revalidate).
                                //
                                // Reference pages + activities are reset to LOADING so that,
                                // during an in-place reload, `isFullyLoaded` stays false until
                                // the sidebar re-fetches them for the new content (the sidebar's
                                // identifier effect refires when the contentlet swaps). Without
                                // this, their stale LOADED status from the previous content would
                                // make `isFullyLoaded` briefly true and drop the reload overlay
                                // before the sidebar actually reloaded.
                                information: {
                                    status: ComponentStatus.LOADING,
                                    error: null,
                                    relatedContent: '0'
                                },
                                activitiesStatus: {
                                    status: ComponentStatus.LOADING,
                                    error: null
                                },
                                versions: [],
                                versionsPagination: null,
                                versionsStatus: {
                                    status: ComponentStatus.INIT,
                                    error: null
                                },
                                pushPublishHistory: [],
                                pushPublishHistoryPagination: null,
                                pushPublishHistoryStatus: {
                                    status: ComponentStatus.INIT,
                                    error: null
                                },
                                isViewingHistoricalVersion: false,
                                historicalVersionInode: null,
                                originalContentlet: null,
                                compareContentlet: null,
                                translationSourceInode: null
                            });

                            return dotEditContentService.getContentById({ id: inode, depth }).pipe(
                                switchMap((contentlet) => {
                                    const { contentType } = contentlet;

                                    return forkJoin({
                                        contentType: dotContentTypeService.getContentTypeWithRender(
                                            contentType,
                                            inode
                                        ),
                                        // Allowed actions for this inode
                                        currentContentActions: workflowActionService.getByInode(
                                            inode,
                                            DotRenderMode.EDITING
                                        ),
                                        // Allowed actions for this content type
                                        schemes:
                                            workflowActionService.getWorkFlowActions(contentType),
                                        contentlet: of(contentlet),
                                        // Workflow status for this inode
                                        workflowStatus: dotWorkflowService.getWorkflowStatus(inode)
                                    });
                                }),
                                tapResponse({
                                    next: ({
                                        contentType,
                                        currentContentActions,
                                        schemes,
                                        contentlet,
                                        workflowStatus
                                    }) => {
                                        // Convert the schemes to an object with the schemeId as the key
                                        const parsedSchemes = parseWorkflows(schemes);
                                        // Parse the actions as an object with the schemeId as the key
                                        const parsedCurrentActions =
                                            parseCurrentActions(currentContentActions);

                                        const { step, task, scheme } = workflowStatus;
                                        // If there's only one workflow scheme, use that scheme's ID
                                        // Otherwise use the ID from the workflow status if available
                                        const schemeIds = Object.keys(parsedSchemes);
                                        const currentSchemeId =
                                            schemeIds.length === 1
                                                ? schemeIds[0]
                                                : scheme?.id || null;

                                        // If there's no scheme or step, content is considered in 'reset' state
                                        const initialContentletState =
                                            !scheme || !step ? 'reset' : 'existing';

                                        const titleString = `${contentlet.title}`;

                                        // The host decides whether these apply: the full-screen
                                        // host updates the title/breadcrumb, the dialog host
                                        // no-ops them (it overlays another route context).
                                        host.setContentTitle(titleString);
                                        host.addBreadcrumb({
                                            label: titleString,
                                            url: `/dotAdmin/#/content/${contentlet.inode}`
                                        });

                                        patchState(store, {
                                            contentType,
                                            currentSchemeId,
                                            schemes: parsedSchemes,
                                            currentContentActions: parsedCurrentActions,
                                            contentlet,
                                            state: ComponentStatus.LOADED,
                                            currentStep: step,
                                            lastTask: task,
                                            initialContentletState,
                                            hiddenFields: {}
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
                ),

                /**
                 * Disables the new content editor for the current content type by updating the CONTENT_EDITOR2_ENABLED flag to false in the content type's metadata.
                 *
                 * This method retrieves the current contentlet from the store, constructs the appropriate payload, and calls the content type update API.
                 * On success, it redirects the user to the legacy edit content page for the content type with the configuration panel open.
                 * On error, it displays an error message using the DotHttpErrorManagerService.
                 */
                disableNewContentEditor: rxMethod<void>(
                    pipe(
                        switchMap(() => {
                            // Access the contentlet from the store and its id before calling the API
                            const contentlet = store.contentlet();
                            const contentType = store.contentType();

                            const payload = {
                                ...contentType,
                                metadata: {
                                    ...contentType.metadata,
                                    CONTENT_EDITOR2_ENABLED: false
                                },
                                workflow: contentType.workflows.map((workflow) => workflow.id) // The Content Types endpoint returns workflows (plural) but receive workflow (singular)
                            };

                            return dotContentTypeService
                                .updateContentType(contentType.id, payload)
                                .pipe(
                                    tapResponse({
                                        next: () => {
                                            // Redirect to legacy edit content page
                                            router.navigate([`/c/content/`, contentlet.inode]);
                                        },
                                        error: (error: HttpErrorResponse) => {
                                            dotHttpErrorManagerService.handle(error);
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
