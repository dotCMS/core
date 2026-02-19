import { signalStore, withHooks, withState, withMethods } from '@ngrx/signals';

import { inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentletVersion,
    DotCMSContentType,
    DotCMSWorkflow,
    DotCMSWorkflowAction,
    DotContentletDepths,
    DotCurrentUser,
    DotLanguage,
    WorkflowStep,
    WorkflowTask
} from '@dotcms/dotcms-models';

import { withActivities } from './features/activities/activities.feature';
import { withContent, DialogInitializationOptions } from './features/content/content.feature';
import { withForm } from './features/form/form.feature';
import { withHistory } from './features/history/history.feature';
import { withInformation } from './features/information/information.feature';
import { withLocales } from './features/locales/locales.feature';
import { withLock } from './features/lock/lock.feature';
import { withSidebar } from './features/sidebar/sidebar.feature';
import { withUI } from './features/ui/ui.feature';
import { withUser } from './features/user/user.feature';
import { withWorkflow } from './features/workflow/workflow.feature';

import { CurrentContentActionsWithScheme } from '../models/dot-edit-content-field.type';
import { FormValues } from '../models/dot-edit-content-form.interface';
import {
    Activity,
    DotContentletState,
    UIState,
    DotPushPublishHistoryItem
} from '../models/dot-edit-content.model';

export interface EditContentState {
    // Root state
    state: ComponentStatus;
    error: string | null;
    isDialogMode: boolean;

    // Content state
    contentType: DotCMSContentType | null;
    contentlet: DotCMSContentlet | null;
    compareContentlet: DotCMSContentlet | null;
    schemes: Record<
        string,
        {
            scheme: DotCMSWorkflow;
            actions: DotCMSWorkflowAction[];
            firstStep: WorkflowStep;
        }
    >;
    initialContentletState: DotContentletState;

    // Workflow state
    currentSchemeId: string | null;
    currentContentActions: CurrentContentActionsWithScheme;
    currentStep: WorkflowStep | null;
    lastTask: WorkflowTask | null;
    workflow: {
        status: ComponentStatus;
        error: string | null;
    };
    workflowActionSuccess: DotCMSContentlet | null;

    // User state
    currentUser: DotCurrentUser;

    // UI state
    uiState: UIState;

    // Information state
    information: {
        status: ComponentStatus;
        error: string | null;
        relatedContent: string;
    };

    // Lock state
    lockError: string | null;
    canLock: boolean;
    lockSwitchLabel: string;

    // Form state
    formValues: FormValues;
    formStatus: 'init' | 'valid' | 'invalid';

    // Locales state
    locales: DotLanguage[] | null;
    systemDefaultLocale: DotLanguage | null;
    currentLocale: DotLanguage | null;
    currentIdentifier: string | null;
    localesStatus: {
        status: ComponentStatus;
        error: string;
    };

    // Activities state
    activities: Activity[];
    activityViewState: 'idle' | 'create';
    activitiesStatus: {
        status: ComponentStatus;
        error: string | null;
    };

    // Versions state
    versions: DotCMSContentletVersion[]; // All accumulated versions for infinite scroll
    versionsPagination: {
        currentPage: number;
        perPage: number;
        totalEntries: number;
    } | null;
    versionsStatus: {
        status: ComponentStatus;
        error: string | null;
    };

    // Push Publish History state
    pushPublishHistory: DotPushPublishHistoryItem[]; // All accumulated push publish history items
    pushPublishHistoryPagination: {
        currentPage: number;
        perPage: number;
        totalEntries: number;
    } | null;
    pushPublishHistoryStatus: {
        status: ComponentStatus;
        error: string | null;
    };

    // Historical version viewing state
    isViewingHistoricalVersion: boolean;
    historicalVersionInode: string | null;
    originalContentlet: DotCMSContentlet | null;
}

export const initialRootState: EditContentState = {
    // Root state
    state: ComponentStatus.INIT,
    error: null,
    isDialogMode: false,

    // Content state
    contentType: null,
    contentlet: null,
    compareContentlet: null,
    schemes: {},
    initialContentletState: 'new',

    // Workflow state
    currentSchemeId: null,
    currentContentActions: {},
    currentStep: null,
    lastTask: null,
    workflow: {
        status: ComponentStatus.INIT,
        error: null
    },
    workflowActionSuccess: null,

    // User state
    currentUser: null,

    // UI state
    uiState: {
        view: 'form',
        activeTab: 0,
        isSidebarOpen: true,
        activeSidebarTab: 0,
        isBetaMessageVisible: true
    },

    // Information state
    information: {
        status: ComponentStatus.INIT,
        error: null,
        relatedContent: '0'
    },

    // Lock state
    lockError: null,
    canLock: false,
    lockSwitchLabel: 'edit.content.unlocked',

    // Form state
    formValues: {},
    formStatus: 'init',

    // Locales state
    locales: null,
    systemDefaultLocale: null,
    currentLocale: null,
    currentIdentifier: null,
    localesStatus: {
        status: ComponentStatus.INIT,
        error: null
    },

    // Activities state
    activities: [],
    activityViewState: 'idle',
    activitiesStatus: {
        status: ComponentStatus.INIT,
        error: null
    },

    // Versions state
    versions: [], // All accumulated versions for infinite scroll
    versionsPagination: null,
    versionsStatus: {
        status: ComponentStatus.INIT,
        error: null
    },

    // Push Publish History state
    pushPublishHistory: [], // All accumulated push publish history items
    pushPublishHistoryPagination: null,
    pushPublishHistoryStatus: {
        status: ComponentStatus.INIT,
        error: null
    },

    // Historical version viewing state
    isViewingHistoricalVersion: false,
    historicalVersionInode: null,
    originalContentlet: null
};

/**
 * The DotEditContentStore is a state management store used in the DotCMS content editing application.
 * It provides state, computed properties, methods, and hooks for managing the application state
 * related to content editing and workflow actions.
 */
export const DotEditContentStore = signalStore(
    withState<EditContentState>(initialRootState),
    withUser(),
    withUI(),
    withContent(),
    withWorkflow(),
    withSidebar(),
    withInformation(),
    withLock(),
    withForm(),
    withLocales(),
    withActivities(),
    withHistory(),
    withHooks({
        onInit(store) {
            // Always load the current user
            store.loadCurrentUser();
        }
    }),
    // Add methods after all features to have access to all store methods
    // Now that withUI comes before withContent, this method can access both UI and content methods
    withMethods((store) => {
        // Inject ActivatedRoute in the proper injection context (within the factory function)
        const activatedRoute = inject(ActivatedRoute);

        return {
            /**
             * Initializes the store for dialog mode with the provided parameters.
             * This method handles all the logic for dialog initialization including:
             * - Enabling dialog mode
             * - Initializing content based on provided parameters
             * - Handling both new content creation and existing content editing
             *
             * @param options - The dialog initialization options
             * @param options.contentTypeId - Content type ID for creating new content
             * @param options.contentletInode - Contentlet inode for editing existing content
             */
            initializeDialogMode(options: DialogInitializationOptions): void {
                const { contentTypeId, contentletInode } = options;

                // Enable dialog mode to prevent route-based initialization
                store.enableDialogMode();

                // Initialize based on provided parameters
                if (contentTypeId) {
                    store.initializeNewContent(contentTypeId);
                } else if (contentletInode) {
                    store.initializeExistingContent({
                        inode: contentletInode,
                        depth: DotContentletDepths.TWO
                    });
                }
            },

            /**
             * Initializes the store for route-based mode using ActivatedRoute parameters.
             * This method should be called by route-based components after the store is created.
             * It will only initialize if dialog mode is not enabled.
             */
            initializeAsPortlet(): void {
                // Skip route-based initialization if in dialog mode
                if (store.isDialogMode()) {
                    return;
                }

                // Use the ActivatedRoute that was injected in the closure
                const params = activatedRoute.snapshot?.params;

                if (params) {
                    const contentType = params['contentType'];
                    const inode = params['id'];

                    // TODO: refactor this when we will use EditContent as sidebar
                    if (inode) {
                        store.initializeExistingContent({ inode, depth: DotContentletDepths.TWO });
                    } else if (contentType) {
                        store.initializeNewContent(contentType);
                    }
                }
            }
        };
    })
);
