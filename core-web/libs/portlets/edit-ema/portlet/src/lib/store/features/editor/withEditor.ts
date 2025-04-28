import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, untracked } from '@angular/core';

import { UVE_MODE } from '@dotcms/client';
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
import { withUVEToolbar } from './toolbar/withUVEToolbar';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { BASE_IFRAME_MEASURE_UNIT } from '../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS, PALETTE_CLASSES } from '../../../shared/enums';
import {
    ActionPayload,
    ContainerPayload,
    ContentletPayload,
    PositionPayload
} from '../../../shared/models';
import {
    mapContainerStructureToArrayOfContainers,
    getPersonalization,
    areContainersEquals,
    getEditorStates,
    createPageApiUrlWithQueryParams,
    sanitizeURL
} from '../../../utils';
import { UVEState } from '../../models';
import { withClient } from '../client/withClient';

const buildIframeURL = ({ pageURI, params, isTraditionalPage }) => {
    if (isTraditionalPage) {
        // Force iframe reload on every page load to avoid caching issues and window dirty state
        // We need a new reference to avoid the iframe to be cached
        // More reference: https://github.com/dotCMS/core/issues/30981
        return new String('');
    }

    // Remove trailing slash from host
    const host = (params.clientHost || window.location.origin).replace(/\/$/, '');
    const pageAPIQueryParams = createPageApiUrlWithQueryParams(pageURI, params);
    const url = new URL(pageAPIQueryParams, host);

    return url.toString();
};

const initialState: EditorState = {
    bounds: [],
    state: EDITOR_STATE.IDLE,
    contentletArea: null,
    dragItem: null,
    ogTags: null,
    paletteOpen: true
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
        withUVEToolbar(),
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
                    const params = store.pageParams();
                    const isTraditionalPage = store.isTraditionalPage();
                    const isClientReady = store.isClientReady();
                    const contentletArea = store.contentletArea();
                    const bounds = store.bounds();
                    const dragItem = store.dragItem();
                    const isEditState = store.isEditState();
                    const paletteOpen = store.paletteOpen();

                    const isPreview = params?.editorMode === UVE_MODE.PREVIEW;
                    const isPageReady = isTraditionalPage || isClientReady || isPreview;
                    const isLoading = !isPageReady || store.status() === UVE_STATUS.LOADING;

                    const { dragIsActive, isScrolling } = getEditorStates(state);

                    const showDialogs = canEditPage && isEditState;
                    const showBlockEditorSidebar = canEditPage && isEditState && isEnterprise;

                    const canUserHaveContentletTools =
                        !!contentletArea &&
                        canEditPage &&
                        isEditState &&
                        !isScrolling &&
                        !isPreview;

                    const showDropzone = canEditPage && state === EDITOR_STATE.DRAGGING;
                    const showPalette = isEnterprise && canEditPage && isEditState && !isPreview;

                    const shouldShowSeoResults = socialMedia && ogTags;

                    const iframeOpacity = isLoading || !isPageReady ? '0.5' : '1';

                    return {
                        showDialogs,
                        showBlockEditorSidebar,
                        showEditorContent: !socialMedia,
                        iframe: {
                            opacity: iframeOpacity,
                            pointerEvents: dragIsActive ? 'none' : 'auto',
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
                                  languageId: pageAPIResponse?.viewAs.language.id,
                                  paletteClass: paletteOpen
                                      ? PALETTE_CLASSES.OPEN
                                      : PALETTE_CLASSES.CLOSED
                              }
                            : null,

                        seoResults: shouldShowSeoResults
                            ? {
                                  ogTags,
                                  socialMedia
                              }
                            : null
                    };
                }),
                $iframeURL: computed<string | InstanceType<typeof String>>(() => {
                    const page = store.pageAPIResponse().page;
                    const vanityURL = store.pageAPIResponse().vanityUrl?.url;

                    const sanitizedURL = sanitizeURL(vanityURL ?? page?.pageURI);
                    const url = buildIframeURL({
                        pageURI: sanitizedURL,
                        params: store.pageParams(),
                        isTraditionalPage: untracked(() => store.isTraditionalPage())
                    });

                    return url;
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
                },
                setPaletteOpen(paletteOpen: boolean) {
                    patchState(store, { paletteOpen });
                }
            };
        })
    );
}
