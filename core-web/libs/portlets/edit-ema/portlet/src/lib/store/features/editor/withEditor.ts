import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed, inject, Signal, untracked } from '@angular/core';

import {
    DotExperimentStatus,
    DotTreeNode,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/types/internal';
import { WINDOW } from '@dotcms/utils';

import { PageData, PageDataContainer, ReloadEditorContent } from './models';

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
import { PageComputed } from '../page/withPage';

import type { WorkflowLockComputed } from '../workflow/withWorkflow';

export interface ViewComputed {
    viewMode: Signal<UVE_MODE>;
}

export interface EditorComputed {
    editorCanEditContent: Signal<boolean>;
    editorCanEditLayout: Signal<boolean>;
    editorCanEditStyles: Signal<boolean>;
    editorEnableInlineEdit: Signal<boolean>;
    editorHasAccessToEditMode: Signal<boolean>;
}

export interface SetSeoDataParams {
    ogTags: SeoMetaTags;
    ogTagsResults: SeoMetaTagsResult[];
}

const buildIframeURL = ({ url, params, dotCMSHost }) => {
    const host = (params.clientHost || dotCMSHost).replace(/\/$/, '');
    const pageURL = getFullPageURL({ url, params, userFriendlyParams: true });
    const iframeURL = new URL(`${host}/${pageURL}&dotCMSHost=${dotCMSHost}`);

    return iframeURL.toString();
};

/**
 * Editor feature - Manages editor UI state and capabilities
 *
 * Responsibilities:
 * - Editor UI state (drag/drop, bounds, active contentlet, content area)
 * - Editor panel preferences (palette, right sidebar)
 * - Edit capabilities (canEditContent, canEditLayout, canEditStyles)
 * - Inline editing mode (based on edit mode)
 * - Page render data and iframe URL generation
 * - Style schemas for contentlet styling
 * - SEO/OG tags management
 *
 * Dependencies:
 * - UVEState: Flat editor state (editorDragItem, editorBounds, etc.)
 * - PageComputed: Access to page data for edit checks
 * - WorkflowLockComputed: Access to lock state for permission checks
 * - ViewComputed: Access to viewMode for edit/preview mode checks
 */
export function withEditor() {
    return signalStoreFeature(
        {
            state: type<UVEState>(),
            props: type<PageComputed & WorkflowLockComputed & ViewComputed>()
        },
        withComputed((store) => {
            const dotWindow = inject(WINDOW);

            const editorHasAccessToEditMode = computed(() => {
                const isPageEditable = store.pageAsset()?.page?.canEdit;
                const isExperimentRunning = [
                    DotExperimentStatus.RUNNING,
                    DotExperimentStatus.SCHEDULED
                ].includes(store.pageExperiment()?.status);

                if (!isPageEditable || isExperimentRunning) {
                    return false;
                }

                // When feature flag is enabled, always allow access (user can toggle lock)
                if (store.$lockFeatureEnabled()) {
                    return true;
                }

                // Legacy behavior: block access if page is locked
                return !store.$lockIsPageLocked();
            });

            const hasPermissionToEditLayout = computed(() => {
                const canEditPage = store.pageAsset()?.page?.canEdit;
                const canDrawTemplate = store.pageAsset()?.template?.drawed;
                const isExperimentRunning = [
                    DotExperimentStatus.RUNNING,
                    DotExperimentStatus.SCHEDULED
                ].includes(store.pageExperiment()?.status);

                return (
                    (canEditPage || canDrawTemplate) &&
                    !isExperimentRunning &&
                    !store.$lockIsPageLocked()
                );
            });

            // Public capabilities (exported via EditorComputed interface)
            const editorCanEditContent = computed(() => {
                return editorHasAccessToEditMode() && store.viewMode() === UVE_MODE.EDIT;
            });

            const editorCanEditLayout = computed(() => {
                return hasPermissionToEditLayout();
            });

            const editorCanEditStyles = computed(() => {
                return store.flags()?.FEATURE_FLAG_UVE_STYLE_EDITOR;
            });

            const editorEnableInlineEdit = computed(() => {
                return store.viewMode() === UVE_MODE.EDIT;
            });

            const $isEmaLegacyScriptInjectionEnabled = computed(
                () => store.flags()?.FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION === true
            );

            return {
                editorCanEditContent,
                editorCanEditLayout,
                editorCanEditStyles,
                editorEnableInlineEdit,
                $isEmaLegacyScriptInjectionEnabled,
                editorHasAccessToEditMode,

                $allowContentDelete: computed<boolean>(() => {
                    const numberContents = store.pageAsset()?.numberContents;
                    const viewAs = store.pageAsset()?.viewAs;
                    const persona = viewAs?.persona;
                    const isDefaultPersona = persona?.identifier === DEFAULT_PERSONA.identifier;

                    return numberContents > 1 || !persona || isDefaultPersona;
                }),
                $allowedContentTypes: computed<Record<string, true>>(() => {
                    return getContentTypeVarRecord(store.pageAsset()?.containers);
                }),
                $showContentletControls: computed<boolean>(() => {
                    const hovered = store.editorContentArea();
                    const selected = store.editorSelectedContentletArea();
                    const canEditPage = editorCanEditContent();
                    const isIdle = store.editorState() === EDITOR_STATE.IDLE;

                    // Show as long as there's something to anchor to: a hovered
                    // contentlet OR a selected one. The previous logic gated on
                    // hovered only, which broke selection for contentlets the
                    // user clicked without a prior pointermove (e.g. headless
                    // re-renders that swap out the hovered DOM node).
                    return (!!hovered || !!selected) && canEditPage && isIdle;
                }),
                $styleSchema: computed<StyleEditorFormSchema>(() => {
                    const activeContentlet = store.editorActiveContentlet();
                    const styleSchemas = store.editorStyleSchemas();
                    const contentSchema = styleSchemas.find(
                        (schema) => schema.contentType === activeContentlet?.contentlet?.contentType
                    );
                    return contentSchema;
                }),
                $isDragging: computed<boolean>(() => {
                    const editorState = store.editorState();
                    return (
                        editorState === EDITOR_STATE.DRAGGING ||
                        editorState === EDITOR_STATE.SCROLL_DRAG
                    );
                }),
                $areaContentType: computed<string>(() => {
                    return store.editorContentArea()?.payload?.contentlet?.contentType ?? '';
                }),
                $pageData: computed<PageData>(() => {
                    const page = store.pageAsset()?.page;
                    const viewAs = store.pageAsset()?.viewAs;
                    const containersData = store.pageAsset()?.containers ?? {};

                    const containers: PageDataContainer[] =
                        mapContainerStructureToArrayOfContainers(containersData);
                    const personalization = getPersonalization(viewAs?.persona);

                    return {
                        containers,
                        personalization,
                        id: page?.identifier,
                        languageId: viewAs?.language?.id,
                        personaTag: viewAs?.persona?.keyTag
                    };
                }),
                $reloadEditorContent: computed<ReloadEditorContent>(
                    () => ({
                        code: store.pageAsset()?.page?.rendered,
                        pageType: store.pageType(),
                        enableInlineEdit: editorEnableInlineEdit()
                    }),
                    {
                        // Effects that depend on this fire on every signal cycle
                        // when reading reactive parents — even if the values are
                        // unchanged. Compare by field so the reload effect only
                        // runs when something actually changed (not on every
                        // contentlet click, etc.). Without this, every click
                        // posts UVE_SET_PAGE_DATA, which re-mounts the consumer
                        // page tree and flashes images.
                        equal: (a, b) =>
                            a.code === b.code &&
                            a.pageType === b.pageType &&
                            a.enableInlineEdit === b.enableInlineEdit
                    }
                ),
                $pageRender: computed<string>(() => {
                    return store.pageAsset()?.page?.rendered;
                }),
                $editorIsInDraggingState: computed<boolean>(() => {
                    return store.editorState() === EDITOR_STATE.DRAGGING;
                }),
                $iframeURL: computed<string | InstanceType<typeof String>>(() => {
                    /*
                        Here we need to trigger recomputation when page data changes.
                        This should change in future UVE improvements.
                        More info: https://github.com/dotCMS/core/issues/31475 and https://github.com/dotCMS/core/issues/32139
                     */
                    const vanityUrlData = store.pageAsset()?.vanityUrl;
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
            };
        }),
        withMethods((store) => {
            return {
                updateEditorScrollState() {
                    const dragItem = store.editorDragItem();
                    patchState(store, {
                        editorBounds: [],
                        editorContentArea: null,
                        editorSelectedContentletArea: null,
                        editorState: dragItem ? EDITOR_STATE.SCROLL_DRAG : EDITOR_STATE.SCROLLING
                    });
                },
                updateEditorOnScrollEnd() {
                    const dragItem = store.editorDragItem();
                    patchState(store, {
                        editorState: dragItem ? EDITOR_STATE.DRAGGING : EDITOR_STATE.IDLE
                    });
                },
                updateEditorScrollDragState() {
                    patchState(store, {
                        editorState: EDITOR_STATE.SCROLL_DRAG,
                        editorBounds: []
                    });
                },
                /**
                 * Flag the editor as resizing the iframe; clears bounds/content
                 * area so contentlet-tools and dropzone hide during the drag.
                 */
                updateEditorResizeState() {
                    patchState(store, {
                        editorBounds: [],
                        editorContentArea: null,
                        editorSelectedContentletArea: null,
                        editorState: EDITOR_STATE.RESIZING
                    });
                },
                /**
                 * Counterpart to updateEditorResizeState — restore IDLE on release.
                 * Bounds re-emit through the usual REQUEST_BOUNDS path once the
                 * SDK is asked.
                 */
                updateEditorOnResizeEnd() {
                    patchState(store, { editorState: EDITOR_STATE.IDLE });
                },
                setEditorState(state: EDITOR_STATE) {
                    patchState(store, {
                        editorState: state
                    });
                },
                setEditorDragItem(dragItem: EmaDragItem) {
                    patchState(store, {
                        editorDragItem: dragItem,
                        editorState: EDITOR_STATE.DRAGGING
                    });
                },
                setEditorBounds(bounds: Container[]) {
                    patchState(store, {
                        editorBounds: bounds
                    });
                },
                setStyleSchemas(styleSchemas: StyleEditorFormSchema[]) {
                    patchState(store, {
                        editorStyleSchemas: styleSchemas
                    });
                },
                resetEditorProperties() {
                    patchState(store, {
                        editorDragItem: null,
                        editorContentArea: null,
                        editorBounds: [],
                        editorState: EDITOR_STATE.IDLE
                    });
                },
                setContentletArea(contentArea: ContentletArea) {
                    const currentArea = store.editorContentArea();
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
                        editorContentArea: contentArea,
                        editorState: EDITOR_STATE.IDLE
                    });
                },
                setActiveContentlet(contentlet: ActionPayload) {
                    patchState(store, {
                        editorActiveContentlet: contentlet
                    });
                },
                resetActiveContentlet() {
                    patchState(store, {
                        editorActiveContentlet: null
                    });
                },
                resetContentletArea() {
                    patchState(store, {
                        editorContentArea: null,
                        editorState: EDITOR_STATE.IDLE
                    });
                },
                /**
                 * Persist the bounds + payload of the contentlet the user just
                 * clicked. The floating action toolbar reads this. Driven by
                 * the SDK's CONTENTLET_CLICKED event so the editor's hover
                 * overlay can stay pointer-events: none and let wheel events
                 * pass through to the iframe.
                 */
                setSelectedContentletArea(area: ContentletArea) {
                    patchState(store, { editorSelectedContentletArea: area });
                },
                resetSelectedContentletArea() {
                    patchState(store, { editorSelectedContentletArea: null });
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
                        uuid: relationType,
                        contentletsId,
                        identifier: containerId
                    } = container;

                    const { personalization, id: pageId } = store.$pageData();
                    const variantId = store.pageVariantId();
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
                    patchState(store, {
                        editorOgTags: ogTags
                    });
                },
                /** Updates both editor OG tags and view OG tag results in a single state update. */
                setSeoData({ ogTags, ogTagsResults }: SetSeoDataParams) {
                    patchState(store, {
                        editorOgTags: ogTags,
                        viewOgTagsResults: ogTagsResults
                    });
                },
                setPaletteOpen(open: boolean) {
                    patchState(store, {
                        editorPaletteOpen: open
                    });
                },
                setEditPanelOpen(open: boolean) {
                    patchState(store, {
                        editorEditPanelOpen: open
                    });
                },
                cancelContentletEdit() {
                    patchState(store, {
                        editorActiveContentlet: null,
                        editorEditPanelOpen: false,
                        editorContentArea: null
                    });
                }
            };
        })
    );
}
