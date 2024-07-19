import { signalStoreFeature, type, withComputed, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { withEditorToolbar } from './toolbar/withEditorToolbar';

import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import { sanitizeURL, createPageApiUrlWithQueryParams } from '../../../utils';
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

                    const url = sanitizeURL(params.url);

                    const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);

                    const showContentletTools = !!contentletArea && canEditPage && isEditState;
                    const showDropzone = canEditPage && dragIsActive;
                    const showPalette = isEnterprise && canEditPage && isEditState;
                    const showDialogs = canEditPage && isEditState;

                    return {
                        seoTools: socialMedia && {
                            socialMedia
                        },
                        editorContent: {
                            isDevice: !!device,
                            isExpanded: !canEditPage,
                            isHidden: !!socialMedia
                        },
                        iframeWrapper: {
                            isDevice: !!device,
                            width: device ? `${device.cssWidth}${BASE_MEASURE}` : BASE_WIDTH,
                            height: device ? `${device.cssHeight}${BASE_MEASURE}` : BASE_HEIGHT
                        },
                        iframe: {
                            src: !isLegacyPage ? `${params.clientHost}/${pageAPIQueryParams}` : '',
                            pointerEvents: dragIsActive ? 'none' : 'auto',
                            opacity: isLoading ? '0.5' : '1'
                        },
                        progressBar: isLoading,
                        contentletTools: showContentletTools && {
                            contentletArea,
                            hide: isDragging,
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
        })
    );
}
