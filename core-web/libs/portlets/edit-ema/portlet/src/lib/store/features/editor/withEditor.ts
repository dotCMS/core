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
    PositionPayload,
    SelectedContentlet
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
                // Layout editing only works on standard (drawed) templates.
                // Advanced templates are hand-coded HTML/CSS and have no
                // structured row/column layout the editor can mutate, so
                // even a user with full page-edit permission can't edit
                // their layout — the nav button stays disabled with the
                // "advanced-template" tooltip.
                const canDrawTemplate = store.pageAsset()?.template?.drawed;
                const isExperimentRunning = [
                    DotExperimentStatus.RUNNING,
                    DotExperimentStatus.SCHEDULED
                ].includes(store.pageExperiment()?.status);

                return (
                    canEditPage &&
                    canDrawTemplate &&
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
                    const selected = store.editorSelected();
                    const canEditPage = editorCanEditContent();
                    const isIdle = store.editorState() === EDITOR_STATE.IDLE;

                    return (!!hovered || !!selected) && canEditPage && isIdle;
                }),
                $styleSchema: computed<StyleEditorFormSchema>(() => {
                    const selected = store.editorSelected();
                    const styleSchemas = store.editorStyleSchemas();
                    const contentSchema = styleSchemas.find(
                        (schema) =>
                            schema.contentType === selected?.payload?.contentlet?.contentType
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
                /**
                 * "The iframe layout is mid-flux; bounds are stale." True
                 * during scroll, scroll+drag, and any kind of resize
                 * (canvas, device, zoom, manual handle, sidebar reflow).
                 *
                 * Consumers should treat this as a lock: hide overlays,
                 * skip layout-dependent computations. The lock releases
                 * when SET_BOUNDS arrives with fresh coords (the actions
                 * handler flips state back to IDLE).
                 *
                 * Naming this predicate decouples consumers from the
                 * specific enum members that compose it — if the state
                 * machine grows new transient phases, only this computed
                 * needs to know about them.
                 */
                $iframeLayoutLocked: computed<boolean>(() => {
                    const editorState = store.editorState();
                    return (
                        editorState === EDITOR_STATE.SCROLLING ||
                        editorState === EDITOR_STATE.SCROLL_DRAG ||
                        editorState === EDITOR_STATE.RESIZING
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
                        enableInlineEdit: editorEnableInlineEdit(),
                        pageAssetRef: store.pageAsset()
                    }),
                    {
                        // Effects that depend on this fire on every signal
                        // cycle when reading reactive parents — even if values
                        // are unchanged. Compare by field so the reload effect
                        // only runs when something actually changed.
                        //
                        // Include `pageAssetRef` so contentlet edits / removes
                        // / drag-drops (which patch pageAssetResponse to a new
                        // object via setPageAsset) DO trigger a re-emit — the
                        // headless consumer needs UVE_SET_PAGE_DATA to render
                        // the new structure. Without this, only changes to the
                        // server-rendered `code` (traditional pages) would
                        // fire the effect.
                        equal: (a, b) =>
                            a.code === b.code &&
                            a.pageType === b.pageType &&
                            a.enableInlineEdit === b.enableInlineEdit &&
                            a.pageAssetRef === b.pageAssetRef
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
                    // Keep editorSelected: the SDK's auto-bounds
                    // channel pushes fresh SET_BOUNDS once scrolling settles
                    // and the SET_BOUNDS handler re-anchors the selected
                    // toolbar to the contentlet's new on-screen position.
                    // Hover area is dropped because pointer state is
                    // undefined mid-scroll.
                    patchState(store, {
                        editorBounds: [],
                        editorContentArea: null,
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
                 * Flag the editor as resizing the iframe; clears bounds/hover
                 * area so contentlet-tools and dropzone hide during the drag.
                 *
                 * Keep editorSelected: state = RESIZING already
                 * gates `$showContentletControls` so the toolbar hides; the
                 * inode is needed by the SET_BOUNDS handler to re-anchor the
                 * selected toolbar to fresh coords once the resize ends.
                 */
                updateEditorResizeState() {
                    patchState(store, {
                        editorBounds: [],
                        editorContentArea: null,
                        editorState: EDITOR_STATE.RESIZING
                    });
                },
                /**
                 * Counterpart to updateEditorResizeState — restore IDLE on
                 * release. Used as a safety flip for paths where the SDK's
                 * auto-bounds channel may not emit (e.g. resize-handle
                 * cancellation with no actual size change, component
                 * destroyed mid-drag). When the layout DOES settle, the
                 * SET_BOUNDS handler flips IDLE first; this becomes a no-op.
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
                resetContentletArea() {
                    patchState(store, {
                        editorContentArea: null,
                        editorState: EDITOR_STATE.IDLE
                    });
                },
                /**
                 * Replace the entire selection record (bounds + payload).
                 * Used by the SDK's CONTENTLET_CLICKED handler and the
                 * hover toolbar's bolt / palette buttons.
                 */
                setSelected(selected: SelectedContentlet) {
                    patchState(store, { editorSelected: selected });
                },
                /**
                 * Patch only the payload of the current selection,
                 * preserving bounds. Used after a save / fork where the
                 * contentlet's data changed but its on-screen position
                 * did not. No-op if nothing is currently selected.
                 */
                setSelectedPayload(payload: ActionPayload) {
                    const current = store.editorSelected();
                    if (current) {
                        patchState(store, { editorSelected: { ...current, payload } });
                    }
                },
                resetSelected() {
                    patchState(store, { editorSelected: null });
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
                        editorSelected: null,
                        editorEditPanelOpen: false,
                        editorContentArea: null
                    });
                }
            };
        })
    );
}
