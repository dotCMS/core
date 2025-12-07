import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { EditorState, UVE_PALETTE_TABS } from './editor/models';
import { PageContextComputed } from './withPageContext';

import { ContentletArea } from '../../edit-ema-editor/components/ema-page-dropzone/types';
import { DEFAULT_PERSONA } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { ContentletPayload } from '../../shared/models';
import { UVEState } from '../models';

interface ActiveContentState {
    activeContentlet?: ContentletPayload;
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
            state: type<UVEState & EditorState>(),
            props: type<PageContextComputed>()
        },
        withState<ActiveContentState>({
            activeContentlet: null,
            contentArea: null
        }),
        withComputed((store) => {
            const pageEntity = store.pageAPIResponse;
            // This can general computed as well I think
            const $isScrolling = computed(
                () =>
                    store.state() === EDITOR_STATE.SCROLLING ||
                    store.state() === EDITOR_STATE.SCROLL_DRAG
            );
            return {
                $allowContentDelete: computed<boolean>(() => {
                    const numberContents = pageEntity()?.numberContents;
                    const persona = pageEntity()?.viewAs?.persona;
                    return numberContents > 1 || !persona || isDefaultPersona(persona);
                }),
                $showContentletControls: computed<boolean>(() => {
                    const contentletPosition = store.contentArea();
                    const canEditPage = store.$canEditPage();
                    // const isIdle = $isIdle();

                    return !!contentletPosition && canEditPage && !$isScrolling();
                })
            };
        }),
        withMethods((store) => ({
            setActiveContentArea(contentArea: ContentletArea) {
                const currentArea = store.contentArea();
                const isSameX = currentArea?.x === contentArea.x;
                const isSameY = currentArea?.y === contentArea.y;

                if (isSameX && isSameY) {
                    // Prevent updating the state if the contentlet area is the same
                    // This is because in inline editing, when we select to not copy the content and edit global
                    // The contentlet area is updated on focus with the same values and IDLE
                    // Losing the INLINE_EDITING state and making the user to open the dialog for checking whether to copy the content or not
                    // Which is an awful UX

                    return;
                }
                patchState(store, {
                    contentArea,
                    state: EDITOR_STATE.IDLE
                });
            },
            setActiveContentlet(contentlet: ContentletPayload) {
                patchState(store, {
                    activeContentlet: contentlet,
                    palette: {
                        open: true,
                        currentTab: UVE_PALETTE_TABS.STYLE_EDITOR
                    }
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
