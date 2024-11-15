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

import { inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ComponentStatus } from '@dotcms/dotcms-models';

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

export interface EditContentState {
    state: ComponentStatus;
    error: string | null;
}

const initialState: EditContentState = {
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
    withContent(),
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
