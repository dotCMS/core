import { signalStore, withHooks, withState } from '@ngrx/signals';

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
import { withContent } from './features/content/content.feature';
import { withForm } from './features/form/form.feature';
import { withInformation } from './features/information/information.feature';
import { withLocales } from './features/locales/locales.feature';
import { withLock } from './features/lock/lock.feature';
import { withSidebar } from './features/sidebar/sidebar.feature';
import { withUI } from './features/ui/ui.feature';
import { withUser } from './features/user/user.feature';
import { withWorkflow } from './features/workflow/workflow.feature';

import { CurrentContentActionsWithScheme } from '../models/dot-edit-content-field.type';
import { Activity } from '../models/dot-edit-content-file.model';
import { FormValues } from '../models/dot-edit-content-form.interface';
import { DotContentletState } from '../models/dot-edit-content.model';

export interface EditContentState {
    // Root state
    state: ComponentStatus;
    error: string | null;

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

    // User state
    currentUser: DotCurrentUser;

    // UI state
    uiState: {
        activeTab: number;
        isSidebarOpen: boolean;
        activeSidebarTab: number;
    };

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
    activitiesStatus: {
        status: ComponentStatus;
        error: string | null;
    };
}

export const initialRootState: EditContentState = {
    // Root state
    state: ComponentStatus.INIT,
    error: null,

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

    // User state
    currentUser: null,

    // UI state
    uiState: {
        activeTab: 0,
        isSidebarOpen: true,
        activeSidebarTab: 0
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
    withContent(),
    withWorkflow(),
    withUser(),
    withUI(),
    withSidebar(),
    withInformation(),
    withLock(),
    withForm(),
    withLocales(),
    withActivities(),
    withHooks({
        onInit(store) {
            const activatedRoute = inject(ActivatedRoute);
            const params = activatedRoute.snapshot?.params;

            // Load the current user
            store.loadCurrentUser();

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
    })
);
