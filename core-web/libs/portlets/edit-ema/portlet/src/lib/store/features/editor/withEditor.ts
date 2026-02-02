import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
} from '@ngrx/signals';

import { computed, inject, untracked } from '@angular/core';

import { DotTreeNode, SeoMetaTags } from '@dotcms/dotcms-models';
import { WINDOW } from '@dotcms/utils';
import { StyleEditorFormSchema } from '@dotcms/uve';

import {
    PageData,
    PageDataContainer,
    ReloadEditorContent
} from './models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { DEFAULT_PERSONA } from '../../../shared/consts';
import { EDITOR_STATE } from '../../../shared/enums';
import {
    ActionPayload,
    ContainerPayload,
    ContentletPayload,
    PositionPayload
} from '../../../shared/models';
import {
    areContainersEquals,
    getContentTypeVarRecord,
    getFullPageURL,
    getPersonalization,
    mapContainerStructureToArrayOfContainers,
    sanitizeURL
} from '../../../utils';
import { PageType, UVEState } from '../../models';
import { PageContextComputed } from '../withPageContext';

const buildIframeURL = ({ url, params, dotCMSHost }) => {
    const host = (params.clientHost || dotCMSHost).replace(/\/$/, '');
    const pageURL = getFullPageURL({ url, params, userFriendlyParams: true });
    const iframeURL = new URL(`${host}/${pageURL}&dotCMSHost=${dotCMSHost}`);

    return iframeURL.toString();
};

/**
 * Phase 3.2: Add computed and methods to handle the Editor UI
 * Editor state is now nested under store.editor()
 *
 * Phase 5: Refactored to use only shared contracts from PageContextComputed.
 * No longer requires explicit dependencies - all cross-cutting concerns are
 * accessed through the shared contract interface.
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
        withComputed((store) => {
            const dotWindow = inject(WINDOW);

            return {
                $allowContentDelete: computed<boolean>(() => {
                    const numberContents = store.numberContents();
                    const viewAs = store.viewAs();
                    const persona = viewAs?.persona;
                    const isDefaultPersona = persona?.identifier === DEFAULT_PERSONA.identifier;

                    return numberContents > 1 || !persona || isDefaultPersona;
                }),
                $allowedContentTypes: computed<Record<string, true>>(() => {
                    return getContentTypeVarRecord(store.containers());
                }),
                $showContentletControls: computed<boolean>(() => {
                    const editor = store.editor();
                    const contentletPosition = editor.contentArea;
                    const canEditPage = store.$canEditPageContent();
                    const isIdle = editor.state === EDITOR_STATE.IDLE;

                    return !!contentletPosition && canEditPage && isIdle;
                }),
                $styleSchema: computed<StyleEditorFormSchema>(() => {
                    console.log('styleSchema')
                    const activeContentlet = store.editor().activeContentlet;
                    const styleSchemas = store.editor().styleSchemas;
                    const contentSchema = styleSchemas.find(
                        (schema) => schema.contentType === activeContentlet?.contentlet?.contentType
                    );
                    return contentSchema;
                }),
                $isDragging: computed<boolean>(() => {
                    const editorState = store.editor().state;
                    return (
                        editorState === EDITOR_STATE.DRAGGING ||
                        editorState === EDITOR_STATE.SCROLL_DRAG
                    );
                }),
                $areaContentType: computed<string>(() => {
                    return store.editor().contentArea?.payload?.contentlet?.contentType ?? '';
                }),
                $pageData: computed<PageData>(() => {
                    const page = store.page();
                    const viewAs = store.viewAs();
                    const containersData = store.containers();

                    const containers: PageDataContainer[] =
                        mapContainerStructureToArrayOfContainers(containersData);
                    const personalization = getPersonalization(viewAs?.persona);

                    return {
                        containers,
                        personalization,
                        id: page.identifier,
                        languageId: viewAs.language.id,
                        personaTag: viewAs.persona?.keyTag
                    };
                }),
                $reloadEditorContent: computed<ReloadEditorContent>(() => {
                    return {
                        code: store.page()?.rendered,
                        pageType: store.pageType(),
                        enableInlineEdit: store.$enableInlineEdit()
                    };
                }),
                $pageRender: computed<string>(() => {
                    return store.page()?.rendered;
                }),
                $editorIsInDraggingState: computed<boolean>(() => {
                    return store.editor().state === EDITOR_STATE.DRAGGING;
                }),
                $iframeURL: computed<string | InstanceType<typeof String>>(() => {
                    /*
                        Here we need to trigger recomputation when page data changes.
                        This should change in future UVE improvements.
                        More info: https://github.com/dotCMS/core/issues/31475 and https://github.com/dotCMS/core/issues/32139
                     */
                    const vanityUrlData = store.vanityUrl();
                    const vanityURL = vanityUrlData?.url;
                    const pageType = untracked(() => store.pageType());
                    const params = untracked(() => store.pageParams());

                    if (pageType === PageType.TRADITIONAL) {
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
                })
                // $editorContentStyles removed - moved to component level (Phase 4.3: cross-feature dependency)
            };
        }),
        withMethods((store) => {
            return {
                updateEditorScrollState() {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            bounds: [],
                            contentArea: null,
                            state: editor.dragItem ? EDITOR_STATE.SCROLL_DRAG : EDITOR_STATE.SCROLLING
                        }
                    });
                },
                updateEditorOnScrollEnd() {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            state: editor.dragItem ? EDITOR_STATE.DRAGGING : EDITOR_STATE.IDLE
                        }
                    });
                },
                updateEditorScrollDragState() {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            state: EDITOR_STATE.SCROLL_DRAG,
                            bounds: []
                        }
                    });
                },
                setEditorState(state: EDITOR_STATE) {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            state
                        }
                    });
                },
                setEditorDragItem(dragItem: EmaDragItem) {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            dragItem,
                            state: EDITOR_STATE.DRAGGING
                        }
                    });
                },
                setEditorBounds(bounds: Container[]) {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            bounds
                        }
                    });
                },
                setStyleSchemas(styleSchemas: StyleEditorFormSchema[]) {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            styleSchemas
                        }
                    });
                },
                resetEditorProperties() {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            dragItem: null,
                            contentArea: null,
                            bounds: [],
                            state: EDITOR_STATE.IDLE
                        }
                    });
                },
                setContentletArea(contentArea: ContentletArea) {
                    console.log('setContentletArea')
                    const editor = store.editor();
                    const currentArea = editor.contentArea;
                    const isSameX = currentArea?.x === contentArea?.x;
                    const isSameY = currentArea?.y === contentArea?.y;

                    if (isSameX && isSameY) {
                        // Prevent updating the state if the contentlet area is the same
                        // This is because in inline editing, when we select to not copy the content and edit global
                        // The contentlet area is updated on focus with the same values and IDLE
                        // Losing the INLINE_EDITING state and making the user to open the dialog for checking whether to copy the content or not
                        // Which is an awful UX

                        return;
                    }
                    patchState(store, {
                        editor: {
                            ...editor,
                            contentArea,
                            state: EDITOR_STATE.IDLE
                        }
                    });
                },
                setActiveContentlet(contentlet: ActionPayload) {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            activeContentlet: contentlet,
                            panels: {
                                ...editor.panels,
                                palette: {
                                    open: true
                                    // Tab switching now handled by DotUvePaletteComponent watching activeContentlet
                                }
                            }
                        }
                    });
                },
                resetActiveContentlet() {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            activeContentlet: null
                        }
                    });
                },
                resetContentletArea() {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            contentArea: null,
                            state: EDITOR_STATE.IDLE
                        }
                    });
                },
                getPageSavePayload(positionPayload: PositionPayload): ActionPayload {
                    console.log('getPageSavePayload')
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
                        uuid: relationType,
                        contentletsId,
                        identifier: containerId
                    } = container;

                    const { personalization, id: pageId } = store.$pageData();
                    const variantId = store.$variantId();
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
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            ogTags
                        }
                    });
                },
                setPaletteOpen(open: boolean) {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            panels: {
                                ...editor.panels,
                                palette: { open }
                            }
                        }
                    });
                },
                setRightSidebarOpen(open: boolean) {
                    const editor = store.editor();
                    patchState(store, {
                        editor: {
                            ...editor,
                            panels: {
                                ...editor.panels,
                                rightSidebar: { open }
                            }
                        }
                    });
                }
            };
        })
    );
}
