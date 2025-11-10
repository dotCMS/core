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
    PALETTE_TABS,
    ReloadEditorContent
} from './models';
import { withUVEToolbar } from './toolbar/withUVEToolbar';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { DEFAULT_PERSONA, UVE_FEATURE_FLAGS } from '../../../shared/consts';
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
import { withFlags } from '../flags/withFlags';

const buildIframeURL = ({ url, params, dotCMSHost }) => {
    const host = (params.clientHost || dotCMSHost).replace(/\/$/, '');
    const pageURL = getFullPageURL({ url, params, userFriendlyParams: true });
    const iframeURL = new URL(`${host}/${pageURL}&dotCMSHost=${dotCMSHost}`);

    return iframeURL.toString();
};

const initialState: EditorState = {
    bounds: [],
    styleConfigurations: {},
    state: EDITOR_STATE.IDLE,
    contentletArea: null,
    dragItem: null,
    ogTags: null,
    palette: {
        isOpen: true,
        currentTab: PALETTE_TABS.CONTENTTYPE,
        styleConfig: {}
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
            state: type<UVEState>()
        },
        withState<EditorState>(initialState),
        withUVEToolbar(),
        withFlags(UVE_FEATURE_FLAGS),
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

                    const isEditMode = params?.mode === UVE_MODE.EDIT;

                    const isPageReady = isTraditionalPage || isClientReady || !isEditMode;
                    const isLoading = !isPageReady || store.status() === UVE_STATUS.LOADING;

                    const { dragIsActive, isScrolling } = getEditorStates(state);

                    const showDialogs = canEditPage && isEditState;
                    const showBlockEditorSidebar = canEditPage && isEditState && isEnterprise;

                    const isLockFeatureEnabled = store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK;
                    const isPageLockedByUser =
                        pageAPIResponse?.page.lockedBy === store.currentUser()?.userId;
                    const canEditDueToLock = !isLockFeatureEnabled || isPageLockedByUser;

                    const canUserHaveContentletTools =
                        !!contentletArea &&
                        canEditPage &&
                        isEditState &&
                        !isScrolling &&
                        isEditMode &&
                        canEditDueToLock;

                    const showDropzone = canEditPage && state === EDITOR_STATE.DRAGGING;

                    const shouldShowSeoResults = socialMedia && ogTags;

                    const iframeOpacity = isLoading || !isPageReady ? '0.5' : '1';

                    const wrapper = getWrapperMeasures(device, store.orientation());

                    const shouldDisableDeleteButton =
                        pageAPIResponse?.numberContents === 1 && // If there is only one content, we should disable the delete button
                        pageAPIResponse?.viewAs?.persona && // If there is a persona, we should disable the delete button
                        pageAPIResponse?.viewAs?.persona?.identifier !== DEFAULT_PERSONA.identifier; // If the persona is not the default persona, we should disable the delete button

                    const message = 'uve.disable.delete.button.on.personalization';

                    const disableDeleteButton = shouldDisableDeleteButton ? message : null;

                    return {
                        showDialogs,
                        showBlockEditorSidebar,
                        iframe: {
                            opacity: iframeOpacity,
                            pointerEvents: dragIsActive ? 'none' : 'auto',
                            wrapper: device ? wrapper : null
                        },
                        progressBar: isLoading,
                        contentletTools: canUserHaveContentletTools
                            ? {
                                  isEnterprise,
                                  contentletArea,
                                  hide: dragIsActive,
                                  disableDeleteButton
                              }
                            : null,
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
                openPalette({
                    tab = PALETTE_TABS.CONTENTTYPE,
                    variableName = ''
                }: { tab?: PALETTE_TABS; variableName?: string } = {}) {
                    const styleConfig = store.styleConfigurations()[variableName];
                    patchState(store, { palette: { isOpen: true, currentTab: tab, styleConfig } });
                },
                closePalette() {
                    patchState(store, {
                        palette: {
                            isOpen: false,
                            currentTab: PALETTE_TABS.CONTENTTYPE,
                            styleConfig: {}
                        }
                    });
                },
                setPaletteCurrentTab(currentTab: PALETTE_TABS) {
                    patchState(store, { palette: { ...store.palette(), currentTab: currentTab } });
                },
                registerStyleConfiguration(
                    variableName: string,
                    styleConfiguration: Record<string, unknown>
                ) {
                    const currentStyleConfigurations = store.styleConfigurations();
                    const newStyleConfigurations = {
                        ...currentStyleConfigurations,
                        [variableName]: styleConfiguration
                    };
                    patchState(store, { styleConfigurations: newStyleConfigurations });
                },
                getStyleConfiguration(variableName: string): Record<string, unknown> | undefined {
                    const currentStyleConfigurations = store.styleConfigurations();
                    return currentStyleConfigurations[variableName];
                }
            };
        })
    );
}
