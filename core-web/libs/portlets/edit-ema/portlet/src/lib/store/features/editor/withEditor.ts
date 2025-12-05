import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, inject, untracked } from '@angular/core';

import { DotTreeNode, SeoMetaTags } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';

import {
    EditorProps,
    EditorState,
    PageData,
    PageDataContainer,
    ReloadEditorContent,
    StyleSchema,
    UVE_PALETTE_TABS
} from './models';
import { withUVEToolbar } from './toolbar/withUVEToolbar';

import {
    Container,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
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
    sanitizeURL,
    getWrapperMeasures,
    getFullPageURL
} from '../../../utils';
import { UVEState } from '../../models';
import { withActiveContent } from '../withContentlet';
import { PageContextComputed } from '../withPageContext';

const buildIframeURL = ({ url, params, dotCMSHost }) => {
    const host = (params.clientHost || dotCMSHost).replace(/\/$/, '');
    const pageURL = getFullPageURL({ url, params, userFriendlyParams: true });
    const iframeURL = new URL(`${host}/${pageURL}&dotCMSHost=${dotCMSHost}`);

    return iframeURL.toString();
};

const initialState: EditorState = {
    bounds: [],
    state: EDITOR_STATE.IDLE,
    dragItem: null,
    ogTags: null,
    styleSchemas: [],
    palette: {
        open: true,
        currentTab: UVE_PALETTE_TABS.CONTENT_TYPES
    }
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
            state: type<UVEState>(),
            props: type<PageContextComputed>()
        },
        withState<EditorState>(initialState),
        withUVEToolbar(),
        withActiveContent(),
        withComputed((store) => {
            const dotWindow = inject(WINDOW);

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
                        isTraditionalPage: store.isTraditionalPage(),
                        enableInlineEdit:
                            store.isEditState() && untracked(() => store.isEnterprise())
                    };
                }),
                $pageRender: computed<string>(() => {
                    return store.pageAPIResponse()?.page?.rendered;
                }),
                $enableInlineEdit: computed<boolean>(() => {
                    return store.isEditState() && untracked(() => store.isEnterprise());
                }),
                $editorIsInDraggingState: computed<boolean>(
                    () => store.state() === EDITOR_STATE.DRAGGING
                ),
                $editorProps: computed<EditorProps>(() => {
                    // Use it to create depdencies to the pageAPIResponse
                    // I did a refactor but need more testing before removing this dependency
                    store.pageAPIResponse();
                    const socialMedia = store.socialMedia();
                    const ogTags = store.ogTags();
                    const device = store.device();
                    const canEditPage = store.$canEditPage();
                    const isEnterprise = store.isEnterprise();
                    const state = store.state();
                    const params = store.pageParams();
                    const isTraditionalPage = store.isTraditionalPage();
                    const isClientReady = store.isClientReady();
                    const bounds = store.bounds();
                    const dragItem = store.dragItem();
                    const isEditState = store.isEditState();

                    const isEditMode = params?.mode === UVE_MODE.EDIT;

                    const isPageReady = isTraditionalPage || isClientReady || !isEditMode;
                    const isLoading = !isPageReady || store.status() === UVE_STATUS.LOADING;

                    const { dragIsActive } = getEditorStates(state);

                    const showDialogs = canEditPage && isEditState;
                    const showBlockEditorSidebar = canEditPage && isEditState && isEnterprise;

                    const showDropzone = canEditPage && state === EDITOR_STATE.DRAGGING;

                    const shouldShowSeoResults = socialMedia && ogTags;

                    const iframeOpacity = isLoading || !isPageReady ? '0.5' : '1';

                    const wrapper = getWrapperMeasures(device, store.orientation());

                    return {
                        showDialogs,
                        showBlockEditorSidebar,
                        iframe: {
                            opacity: iframeOpacity,
                            pointerEvents: dragIsActive ? 'none' : 'auto',
                            wrapper: device ? wrapper : null
                        },
                        progressBar: isLoading,
                        dropzone: showDropzone
                            ? {
                                  bounds,
                                  dragItem
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
                    /*
                        Here we need to import pageAPIResponse() to create the computed dependency and have it updated every time a response is received from the PageAPI.
                        This should change in future UVE improvements.
                        More info: https://github.com/dotCMS/core/issues/31475 and https://github.com/dotCMS/core/issues/32139
                     */
                    const pageAPIResponse = store.pageAPIResponse();
                    const vanityURL = pageAPIResponse?.vanityUrl?.url;
                    const isTraditionalPage = untracked(() => store.isTraditionalPage());
                    const params = untracked(() => store.pageParams());

                    if (isTraditionalPage) {
                        // Force iframe reload on every page load to avoid caching issues and window dirty state
                        // We need a new reference to avoid the iframe to be cached
                        // More reference: https://github.com/dotCMS/core/issues/30981
                        return new String('');
                    }

                    const url = sanitizeURL(vanityURL ?? params.url);
                    const dotCMSHost = dotWindow?.location?.origin;

                    return buildIframeURL({
                        url,
                        params,
                        dotCMSHost
                    });
                }),
                $editorContentStyles: computed<Record<string, string>>(() => {
                    const socialMedia = store.socialMedia();

                    return {
                        display: socialMedia ? 'none' : 'block'
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
                setEditorBounds(bounds: Container[]) {
                    patchState(store, { bounds });
                },
                setStyleSchemas(styleSchemas: StyleSchema[]) {
                    patchState(store, { styleSchemas });
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
                setPaletteOpen(open: boolean) {
                    patchState(store, {
                        palette: { open, currentTab: store.palette().currentTab }
                    });
                }
            };
        })
    );
}
