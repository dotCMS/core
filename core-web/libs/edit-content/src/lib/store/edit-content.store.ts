import { signalStore, withHooks, withState, withMethods } from '@ngrx/signals';

import { inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import {
    ComponentStatus,
    DotCMSContentlet,
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
import { withInformation } from './features/information/information.feature';
import { withLocales } from './features/locales/locales.feature';
import { withLock } from './features/lock/lock.feature';
import { withSidebar } from './features/sidebar/sidebar.feature';
import { withUI } from './features/ui/ui.feature';
import { withUser } from './features/user/user.feature';
import { withWorkflow } from './features/workflow/workflow.feature';

import { CurrentContentActionsWithScheme } from '../models/dot-edit-content-field.type';
import { FormValues } from '../models/dot-edit-content-form.interface';
import { Activity, DotContentletState, UIState } from '../models/dot-edit-content.model';

export interface EditContentState {
    // Root state
    state: ComponentStatus;
    error: string | null;
    isDialogMode: boolean;

    // Content state
    contentType: DotCMSContentType | null;
    contentlet: DotCMSContentlet | null;
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
}

export const initialRootState: EditContentState = {
    // Root state
    state: ComponentStatus.INIT,
    error: null,
    isDialogMode: false,

    // Content state
    contentType: null,
    contentlet: null,
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
    }
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
    withHooks({
        onInit(store) {
            // Always load the current user
            store.loadCurrentUser();
            console.log('🏪 [DotEditContentStore] OnInit - user loaded, waiting for explicit initialization');
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
                
                console.log('🏪 [DotEditContentStore] Initializing dialog mode with options:', options);
                
                // Enable dialog mode to prevent route-based initialization
                store.enableDialogMode();
                
                // Initialize based on provided parameters
                if (contentTypeId) {
                    console.log('🏪 [DotEditContentStore] Initializing new content for type:', contentTypeId);
                    store.initializeNewContent(contentTypeId);
                } else if (contentletInode) {
                    console.log('🏪 [DotEditContentStore] Initializing existing content for inode:', contentletInode);
                    store.initializeExistingContent({
                        inode: contentletInode,
                        depth: DotContentletDepths.TWO
                    });
                } else {
                    console.warn('🏪 [DotEditContentStore] No valid initialization parameters provided for dialog mode');
                }
            },

            /**
             * Initializes the store for route-based mode using ActivatedRoute parameters.
             * This method should be called by route-based components after the store is created.
             * It will only initialize if dialog mode is not enabled.
             */
            initializeFromRoute(): void {
                console.log('🏪 [DotEditContentStore] Route initialization requested');
                
                // Skip route-based initialization if in dialog mode
                if (store.isDialogMode()) {
                    console.log('🏪 [DotEditContentStore] Skipping route initialization - dialog mode enabled');
                    return;
                }

                console.log('🏪 [DotEditContentStore] Attempting route-based initialization');

                // Use the ActivatedRoute that was injected in the closure
                const params = activatedRoute.snapshot?.params;

                if (params) {
                    const contentType = params['contentType'];
                    const inode = params['id'];

                    // TODO: refactor this when we will use EditContent as sidebar
                    if (inode) {
                        console.log(
                            '🏪 [DotEditContentStore] Initializing existing content from route:',
                            inode
                        );
                        store.initializeExistingContent({ inode, depth: DotContentletDepths.TWO });
                    } else if (contentType) {
                        console.log(
                            '🏪 [DotEditContentStore] Initializing new content from route:',
                            contentType
                        );
                        store.initializeNewContent(contentType);
                    }
                } else {
                    console.log('🏪 [DotEditContentStore] No route params found');
                }
            }
        };
    })
);
