import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { withSave } from './save/withSave';
import { withEditorToolbar } from './toolbar/withEditorToolbar';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import {
    sanitizeURL,
    createPageApiUrlWithQueryParams,
    getPageContainers,
    getPersonalization
} from '../../../utils';
import { EditorState, UVEState } from '../../models';

const initialState: EditorState = {
    bounds: [],
    state: EDITOR_STATE.IDLE,
    contentletArea: undefined,
    dragItem: undefined
};

const BASE_MEASURE = 'px';
const BASE_WIDTH = '100%';
const BASE_HEIGHT = '100%';

/**
 * Add computed properties to the store to handle the UVE status
 *
 * @export
 * @return {*}
 */
export function withEditor() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<EditorState>(initialState),
        withEditorToolbar(),
        withComputed((store) => {
            return {
                pageData: computed(() => {
                    const pageAPIResponse = store.pageAPIResponse();

                    const containers = getPageContainers(pageAPIResponse.containers);
                    const personalization = getPersonalization(pageAPIResponse.viewAs?.persona);

                    return {
                        containers,
                        personalization,
                        id: pageAPIResponse.page.identifier,
                        languageId: pageAPIResponse.viewAs.language.id,
                        personaTag: pageAPIResponse.viewAs.persona?.keyTag
                    };
                }),
                reloadEditorContent: computed(() => {
                    return {
                        code: store.pageAPIResponse()?.page.rendered,
                        isLegacyPage: store.isLegacyPage(),
                        isEditState: store.isEditState(),
                        isEnterprise: store.isEnterprise()
                    };
                }),
                editorIsInDraggingState: computed(() => store.state() === EDITOR_STATE.DRAGGING),
                editorState: computed(() => {
                    const pageAPIResponse = store.pageAPIResponse();
                    const socialMedia = store.socialMedia();
                    const device = store.device();
                    const canEditPage = store.canEditPage();
                    const isEnterprise = store.isEnterprise();
                    const state = store.state();
                    const params = store.params();
                    const isLegacyPage = store.isLegacyPage();
                    const contentletArea = store.contentletArea();
                    const bounds = store.bounds();
                    const dragItem = store.dragItem();
                    const isEditState = store.isEditState();

                    const isDragging = state === EDITOR_STATE.DRAGGING;
                    const dragIsActive = isDragging || state === EDITOR_STATE.SCROLL_DRAG;
                    const isLoading = store.status() === UVE_STATUS.LOADING;
                    const isScrolling =
                        state === EDITOR_STATE.SCROLL_DRAG || state === EDITOR_STATE.SCROLLING;

                    const url = sanitizeURL(params.url);

                    const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);

                    const showContentletTools =
                        !!contentletArea && canEditPage && isEditState && !isScrolling;
                    const showDropzone = canEditPage && dragIsActive;
                    const showPalette = isEnterprise && canEditPage && isEditState;
                    const showDialogs = canEditPage && isEditState;

                    return {
                        seoTools: socialMedia && {
                            socialMedia
                        },
                        showEditorContent: !socialMedia,
                        iframeWrapper: {
                            isDevice: !!device,
                            width: device ? `${device.cssWidth}${BASE_MEASURE}` : BASE_WIDTH,
                            height: device ? `${device.cssHeight}${BASE_MEASURE}` : BASE_HEIGHT
                        },
                        iframe: {
                            state,
                            src: !isLegacyPage ? `${params.clientHost}/${pageAPIQueryParams}` : '',
                            pointerEvents: dragIsActive ? 'none' : 'auto',
                            opacity: isLoading ? '0.5' : '1'
                        },
                        progressBar: isLoading,
                        contentletTools: showContentletTools && {
                            contentletArea,
                            hide: dragIsActive,
                            isEnterprise
                        },
                        dropzone: showDropzone && {
                            bounds,
                            dragItem
                        },
                        palette: showPalette && {
                            languageId: pageAPIResponse.viewAs.language.id,
                            containers: pageAPIResponse.containers,
                            variantId: params.variantName
                        },
                        dialogs: showDialogs
                    };
                })
            };
        }),
        withMethods((store) => {
            return {
                updateEditorScrollState() {
                    // We dont want to change the state if the editor is out of bounds
                    // The scroll end event is triggered after the user leaves the window
                    // And that is changing the state in an unnatural way
                    if (store.state() === EDITOR_STATE.OUT_OF_BOUNDS) {
                        return;
                    }

                    patchState(store, {
                        state: store.dragItem() ? EDITOR_STATE.SCROLL_DRAG : EDITOR_STATE.SCROLLING
                    });
                },
                updateEditorOnScrollEnd() {
                    // We dont want to change the state if the editor is out of bounds
                    // The scroll end event is triggered after the user leaves the window
                    // And that is changing the state in an unnatural way

                    if (store.state() === EDITOR_STATE.OUT_OF_BOUNDS) {
                        return;
                    }

                    patchState(store, {
                        state: store.dragItem() ? EDITOR_STATE.DRAGGING : EDITOR_STATE.IDLE
                    });
                },
                updateEditorScrollDragState() {
                    patchState(store, { state: EDITOR_STATE.SCROLL_DRAG, bounds: [] });
                },
                setEditorState(state: EDITOR_STATE) {
                    patchState(store, { state });
                },
                setEditorDragItem(dragItem: EmaDragItem) {
                    patchState(store, { dragItem });
                },
                setEditorContentletArea(contentletArea: ContentletArea) {
                    patchState(store, { contentletArea, state: EDITOR_STATE.IDLE });
                },
                setEditorBounds(bounds: Container[]) {
                    patchState(store, { bounds });
                },
                resetEditorProperties() {
                    patchState(store, {
                        dragItem: undefined,
                        contentletArea: undefined,
                        bounds: [],
                        state: EDITOR_STATE.IDLE
                    });
                }
            };
        }),
        withSave()
    );
}
