import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, untracked } from '@angular/core';

import { DotTreeNode, SeoMetaTags } from '@dotcms/dotcms-models';

import {
    EditorProps,
    EditorState,
    PageData,
    PageDataContainer,
    ReloadEditorContent
} from './models';
import { withSave } from './save/withSave';
import { withEditorToolbar } from './toolbar/withEditorToolbar';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { BASE_IFRAME_MEASURE_UNIT } from '../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import {
    ActionPayload,
    ContainerPayload,
    ContentletPayload,
    PositionPayload
} from '../../../shared/models';
import {
    sanitizeURL,
    createPageApiUrlWithQueryParams,
    mapContainerStructureToArrayOfContainers,
    getPersonalization,
    areContainersEquals,
    getEditorStates
} from '../../../utils';
import { UVEState } from '../../models';
import { withClient } from '../client/withClient';

const initialState: EditorState = {
    bounds: [],
    state: EDITOR_STATE.IDLE,
    contentletArea: null,
    dragItem: null,
    ogTags: null
};

/**
 * Add computed and methods to handle the Editor UI
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
        withSave(),
        withClient(),
        withComputed((store) => {
            return {
                $pageData: computed<PageData>(() => {
                    const pageAPIResponse = store.pageAPIResponse();

                    const containers: PageDataContainer[] =
                        mapContainerStructureToArrayOfContainers(pageAPIResponse.containers);
                    const personalization = getPersonalization(pageAPIResponse.viewAs?.persona);

                    return {
                        containers,
                        personalization,
                        id: pageAPIResponse.page.identifier,
                        languageId: pageAPIResponse.viewAs.language.id,
                        personaTag: pageAPIResponse.viewAs.persona?.keyTag
                    };
                }),
                $reloadEditorContent: computed<ReloadEditorContent>(() => {
                    return {
                        code: store.pageAPIResponse()?.page?.rendered,
                        isClientReady: store.isClientReady(),
                        isTraditionalPage: untracked(() => store.isTraditionalPage()),
                        enableInlineEdit:
                            store.isEditState() && untracked(() => store.isEnterprise())
                    };
                }),
                $editorIsInDraggingState: computed<boolean>(
                    () => store.state() === EDITOR_STATE.DRAGGING
                ),
                $editorProps: computed<EditorProps>(() => {
                    const pageAPIResponse = store.pageAPIResponse();
                    const socialMedia = store.socialMedia();
                    const ogTags = store.ogTags();
                    const device = store.device();
                    const canEditPage = store.canEditPage();
                    const isEnterprise = store.isEnterprise();
                    const state = store.state();
                    const params = store.params();
                    const isTraditionalPage = store.isTraditionalPage();
                    const isClientReady = store.isClientReady();
                    const contentletArea = store.contentletArea();
                    const bounds = store.bounds();
                    const dragItem = store.dragItem();
                    const isEditState = store.isEditState();
                    const isLoading = !isClientReady || store.status() === UVE_STATUS.LOADING;

                    const isPageReady = isTraditionalPage || isClientReady;

                    const { dragIsActive, isScrolling } = getEditorStates(state);

                    const url = sanitizeURL(params?.url);

                    const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);

                    const showDialogs = canEditPage && isEditState;

                    const canUserHaveContentletTools =
                        !!contentletArea && canEditPage && isEditState && !isScrolling;

                    const showDropzone = canEditPage && state === EDITOR_STATE.DRAGGING;

                    const showPalette = isEnterprise && canEditPage && isEditState;

                    const shouldShowSeoResults = socialMedia && ogTags;

                    const iframeOpacity = isLoading || !isPageReady ? '0.5' : '1';
                    const origin = params.clientHost || window.location.origin;
                    const iframeURL = new URL(pageAPIQueryParams, origin);

                    return {
                        showDialogs: showDialogs,
                        showEditorContent: !socialMedia,
                        iframe: {
                            opacity: iframeOpacity,
                            pointerEvents: dragIsActive ? 'none' : 'auto',
                            src: !isTraditionalPage ? iframeURL.href : '',
                            wrapper: device
                                ? {
                                      width: `${device.cssWidth}${BASE_IFRAME_MEASURE_UNIT}`,
                                      height: `${device.cssHeight}${BASE_IFRAME_MEASURE_UNIT}`
                                  }
                                : null
                        },
                        progressBar: isLoading,
                        contentletTools: canUserHaveContentletTools
                            ? {
                                  isEnterprise,
                                  contentletArea,
                                  hide: dragIsActive
                              }
                            : null,
                        dropzone: showDropzone
                            ? {
                                  bounds,
                                  dragItem
                              }
                            : null,
                        palette: showPalette
                            ? {
                                  variantId: params?.variantName,
                                  containers: pageAPIResponse?.containers,
                                  languageId: pageAPIResponse?.viewAs.language.id
                              }
                            : null,

                        seoResults: shouldShowSeoResults
                            ? {
                                  ogTags,
                                  socialMedia
                              }
                            : null
                    };
                })
            };
        }),
        withMethods((store) => {
            return {
                setIsClientReady(value: boolean) {
                    patchState(store, {
                        isClientReady: value
                    });
                },
                updateEditorScrollState() {
                    patchState(store, {
                        bounds: [],
                        contentletArea: null,
                        state: store.dragItem() ? EDITOR_STATE.SCROLL_DRAG : EDITOR_STATE.SCROLLING
                    });
                },
                updateEditorOnScrollEnd() {
                    patchState(store, {
                        state: store.dragItem() ? EDITOR_STATE.DRAGGING : EDITOR_STATE.IDLE
                    });
                },
                updateEditorScrollDragState() {
                    patchState(store, { state: EDITOR_STATE.SCROLL_DRAG, bounds: [] });
                },
                setEditorState(state: EDITOR_STATE) {
                    patchState(store, { state: state });
                },
                setEditorDragItem(dragItem: EmaDragItem) {
                    patchState(store, { dragItem, state: EDITOR_STATE.DRAGGING });
                },
                setEditorContentletArea(contentletArea: ContentletArea) {
                    const currentContentletArea = store.contentletArea();

                    if (
                        currentContentletArea?.x === contentletArea.x &&
                        currentContentletArea?.y === contentletArea.y
                    ) {
                        // Prevent updating the state if the contentlet area is the same
                        // This is because in inline editing, when we select to not copy the content and edit global
                        // The contentlet area is updated on focus with the same values and IDLE
                        // Losing the INLINE_EDITING state and making the user to open the dialog for checking whether to copy the content or not
                        // Which is an awful UX

                        return;
                    }

                    patchState(store, {
                        contentletArea: contentletArea,
                        state: EDITOR_STATE.IDLE
                    });
                },
                setEditorBounds(bounds: Container[]) {
                    patchState(store, { bounds });
                },
                resetEditorProperties() {
                    patchState(store, {
                        dragItem: null,
                        contentletArea: null,
                        bounds: [],
                        state: EDITOR_STATE.IDLE
                    });
                },
                getPageSavePayload(positionPayload: PositionPayload): ActionPayload {
                    const { containers, languageId, id, personaTag } = store.$pageData();

                    const { contentletsId } = containers.find((container) =>
                        areContainersEquals(container, positionPayload.container)
                    ) ?? { contentletsId: [] };

                    const container = positionPayload.container
                        ? {
                              ...positionPayload.container,
                              contentletsId
                          }
                        : null;

                    return {
                        ...positionPayload,
                        language_id: languageId.toString(),
                        pageId: id,
                        pageContainers: containers,
                        personaTag,
                        container
                    };
                },
                getCurrentTreeNode(
                    container: ContainerPayload,
                    contentlet: ContentletPayload
                ): DotTreeNode {
                    const { identifier: contentId } = contentlet;
                    const {
                        variantId,
                        uuid: relationType,
                        contentletsId,
                        identifier: containerId
                    } = container;

                    const { personalization, id: pageId } = store.$pageData();

                    const treeOrder = contentletsId.findIndex((id) => id === contentId).toString();

                    return {
                        contentId,
                        containerId,
                        relationType,
                        variantId,
                        personalization,
                        treeOrder,
                        pageId
                    };
                },
                setOgTags(ogTags: SeoMetaTags) {
                    patchState(store, { ogTags });
                }
            };
        })
    );
}
