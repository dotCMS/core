import { patchState, signalStore, withHooks, withState, withMethods } from '@ngrx/signals';

import { inject } from '@angular/core';

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
import { withContent } from './features/content/content.feature';
import { withFieldVisibility } from './features/field-visibility/field-visibility.feature';
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
import { EDIT_CONTENT_HOST } from '../services/host/edit-content-host.model';

export interface EditContentState {
    // Root state
    state: ComponentStatus;
    error: string | null;

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
    isManualTranslation: boolean;

    // Workflow state
    currentSchemeId: string | null;
    currentContentActions: CurrentContentActionsWithScheme;
    currentStep: WorkflowStep | null;
    lastTask: WorkflowTask | null;
    workflow: {
        status: ComponentStatus;
        error: string | null;
    };
    // Status of the allowed-actions re-fetch (updateCurrentContentActions). Lets the UI
    // disable the workflow actions while the list is being refreshed (e.g. after a lock toggle).
    actionsStatus: {
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
    lockStatus: ComponentStatus;
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

    /**
     * Map of field variable names currently hidden via the BridgeAPI show()/hide() methods.
     * A key present with `true` means the field is hidden; absent keys are visible.
     * Uses Record<string, boolean> instead of Set for JSON serializability
     * (Redux DevTools, state snapshots, hydration).
     */
    hiddenFields: Record<string, boolean>;

    /**
     * Query params captured from the URL when initializing as a portlet.
     * Used to pre-fill form fields (e.g., folderPath for Host or Folder).
     */
    queryParams: EditContentQueryParams;
}

/**
 * Supported query params for the edit-content route.
 * Add new properties here as more params are needed.
 */
export interface EditContentQueryParams {
    /** Pre-fill path for the Host or Folder field. Format: "hostname/folder1/folder2/" */
    folderPath?: string;
}

export const initialRootState: EditContentState = {
    // Root state
    state: ComponentStatus.INIT,
    error: null,

    // Content state
    contentType: null,
    contentlet: null,
    compareContentlet: null,
    schemes: {},
    initialContentletState: 'new',
    isManualTranslation: false,

    // Workflow state
    currentSchemeId: null,
    currentContentActions: {},
    currentStep: null,
    lastTask: null,
    workflow: {
        status: ComponentStatus.INIT,
        error: null
    },
    actionsStatus: {
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
        isBetaMessageVisible: true,
        localeSelectorTab: 'all'
    },

    // Information state
    information: {
        status: ComponentStatus.INIT,
        error: null,
        relatedContent: '0'
    },

    // Lock state
    lockError: null,
    lockStatus: ComponentStatus.IDLE,
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
    originalContentlet: null,

    // Field visibility state (controlled by BridgeAPI)
    hiddenFields: {} as Record<string, boolean>,

    // Query params from URL
    queryParams: {}
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
    withFieldVisibility(),
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
    withMethods((store, host = inject(EDIT_CONTENT_HOST)) => ({
        /**
         * Initializes the editor from the identity resolved by the host — the
         * route params in full-screen, the dialog config in overlay mode. This is
         * the single, presentation-agnostic entry point (there is no separate
         * route-vs-dialog path). Called once by the layout after the store exists.
         */
        initialize(): void {
            const { inode, contentTypeId, folderPath } = host.resolveIdentity();

            // Store query params first (synchronous) so they are available when the
            // async content initialization completes and the form reads them.
            const supportedQueryParams: EditContentQueryParams = {};
            if (folderPath) {
                supportedQueryParams.folderPath = folderPath;
            }

            patchState(store, { queryParams: supportedQueryParams });

            if (inode) {
                store.initializeExistingContent({ inode, depth: DotContentletDepths.TWO });
            } else if (contentTypeId) {
                store.initializeNewContent(contentTypeId);
            }
        }
    }))
);
