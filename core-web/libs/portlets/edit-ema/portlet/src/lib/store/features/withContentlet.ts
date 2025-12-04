import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, effect, Signal } from '@angular/core';

import { UVE_MODE } from '@dotcms/types';

import { ContentletArea } from '../../edit-ema-editor/components/ema-page-dropzone/types';
import { DEFAULT_PERSONA } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { UVEState } from '../models';
// import { EDITOR_STATE } from '../../shared/enums';

interface ActiveContentState {
    identifier?: string;
    contentletArea?: ContentletArea;
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
            state: type<UVEState>(),
            props: type<{
                state: Signal<EDITOR_STATE>;
            }>()
        },
        withState<ActiveContentState>({
            identifier: '',
            contentletArea: null
        }),
        withComputed((store) => {
            const pageEntity = computed(() => store.pageAPIResponse());
            // This can be props as well
            const isDragging = computed(() => store.state() === EDITOR_STATE.DRAGGING);
            const isScrolling = computed(
                () =>
                    store.state() === EDITOR_STATE.SCROLL_DRAG ||
                    store.state() === EDITOR_STATE.SCROLLING
            );
            const isEditMode = computed(() => store.pageParams()?.mode === UVE_MODE.EDIT);
            const isLockedByCurrentUser = computed(() => {
                const isLocked = pageEntity()?.page?.locked;
                const isLockedByCurrentUser =
                    pageEntity()?.page?.lockedBy === store.currentUser()?.userId;
                return isLocked && isLockedByCurrentUser;
            });
            return {
                $allowContentDelete: computed<boolean>(() => {
                    const numberContents = pageEntity()?.numberContents;
                    const persona = pageEntity()?.viewAs?.persona;
                    return numberContents > 1 || !persona || isDefaultPersona(persona);
                }),
                showContentletControls: computed<boolean>(() => {
                    return (
                        !!store.contentletArea() &&
                        store.canEditPage() &&
                        !isDragging() &&
                        !isScrolling() &&
                        isEditMode() &&
                        !isLockedByCurrentUser()
                    );
                })
            };
        }),
        withMethods((store) => ({
            setActiveContentArea(_contentletArea: ContentletArea) {
                patchState(store, {
                    // contentletArea,
                    // state: EDITOR_STATE.IDLE
                });
            },
            unsetActiveContentArea() {
                patchState(store, {
                    // contentletArea: null,
                    // state: EDITOR_STATE.IDLE
                });
            }
        })),
        withHooks({
            onInit(_store) {
                effect(() => {
                    // console.log('State', store.state());
                });
            }
        })
    );
}
