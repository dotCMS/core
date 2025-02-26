import { signalStore, withHooks, withState } from '@ngrx/signals';

import { inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ComponentStatus, DotContentletDepths } from '@dotcms/dotcms-models';

import { withContent } from './features/content.feature';
import { withForm } from './features/form.feature';
import { withInformation } from './features/information.feature';
import { withLocales } from './features/locales.feature';
import { withLock } from './features/lock.feature';
import { withSidebar } from './features/sidebar.feature';
import { withUI } from './features/ui.feature';
import { withWorkflow } from './features/workflow.feature';

export interface EditContentRootState {
    state: ComponentStatus;
    error: string | null;
}

export const initialRootState: EditContentRootState = {
    state: ComponentStatus.INIT,
    error: null
};

/**
 * The DotEditContentStore is a state management store used in the DotCMS content editing application.
 * It provides state, computed properties, methods, and hooks for managing the application state
 * related to content editing and workflow actions.
 */
export const DotEditContentStore = signalStore(
    withState(initialRootState),
    withContent(),
    withUI(),
    withSidebar(),
    withInformation(),
    withWorkflow(),
    withForm(),
    withLocales(),
    withLock(),
    withHooks({
        onInit(store) {
            const activatedRoute = inject(ActivatedRoute);
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
    })
);
