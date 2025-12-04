import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { UVE_MODE } from '@dotcms/types';

import { EditorState } from './editor/models';

import { ContentletArea } from '../../edit-ema-editor/components/ema-page-dropzone/types';
import { DEFAULT_PERSONA } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { UVEState } from '../models';
// import { EDITOR_STATE } from '../../shared/enums';

interface ActiveContentState {
    identifier?: string;
    contentArea?: ContentletArea;
}

const isDefaultPersona = (persona) => persona?.identifier === DEFAULT_PERSONA.identifier;

/**
 * Add load and reload method to the store
 *
 * @export
 * @return {*}
 */
export function withActiveContent() {
    return signalStoreFeature(
        {
            state: type<UVEState & Pick<EditorState, 'state'>>()
        },
        withState<ActiveContentState>({
            identifier: '',
            contentArea: null
        }),
        withComputed((store) => {
            const pageEntity = computed(() => store.pageAPIResponse());
            // This can general computed as well
            const $isDragging = computed(() => store.state() === EDITOR_STATE.DRAGGING);
            const $isScrolling = computed(
                () =>
                    store.state() === EDITOR_STATE.SCROLL_DRAG ||
                    store.state() === EDITOR_STATE.SCROLLING
            );
            const $isEditMode = computed(() => store.pageParams()?.mode === UVE_MODE.EDIT);
            const $isPageLocked = computed(() => pageEntity()?.page?.locked);
            const $isLockedByCurrentUser = computed(() => {
                return pageEntity()?.page?.lockedBy === store.currentUser()?.userId;
            });
            return {
                $allowContentDelete: computed<boolean>(() => {
                    const numberContents = pageEntity()?.numberContents;
                    const persona = pageEntity()?.viewAs?.persona;
                    return numberContents > 1 || !persona || isDefaultPersona(persona);
                }),
                $showContentletControls: computed<boolean>(() => {
                    const contentletPosition = store.contentArea();
                    const canEditPage = store.canEditPage();
                    const isDragging = $isDragging();
                    const isScrolling = $isScrolling();
                    const isEditMode = $isEditMode();
                    // MISSING: const canEditDueToLock = !isLockFeatureEnabled || isPageLockedByUser;
                    const canEditLockedPage = $isPageLocked() && $isLockedByCurrentUser();

                    return (
                        !!contentletPosition &&
                        canEditPage &&
                        !isDragging &&
                        !isScrolling &&
                        isEditMode &&
                        canEditLockedPage
                    );
                })
            };
        }),
        withMethods((store) => ({
            setActiveContentArea(contentArea: ContentletArea) {
                patchState(store, {
                    contentArea,
                    state: EDITOR_STATE.IDLE
                });
            },
            unsetActiveContentArea() {
                patchState(store, {
                    contentArea: null,
                    state: EDITOR_STATE.IDLE
                });
            }
        }))
    );
}
