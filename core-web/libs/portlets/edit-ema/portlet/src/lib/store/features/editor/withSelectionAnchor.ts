import { signalStoreFeature, type, withMethods } from '@ngrx/signals';

import { Signal } from '@angular/core';

import { Container } from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';
import { ActionPayload, PositionPayload, SelectedContentlet } from '../../../shared/models';
import { UVEState } from '../../models';

/**
 * Computed dependencies this feature consumes from sibling features
 * (`withEditor` for `$iframeLayoutLocked`).
 */
export interface SelectionAnchorDeps {
    $iframeLayoutLocked: Signal<boolean>;
}

/**
 * Methods this feature calls on the store. These come from `withEditor`,
 * which is composed before `withSelectionAnchor` in the root store. We
 * use a type assertion (rather than declaring them in `props`) because
 * the props type is reserved for computeds in this codebase, and
 * declaring methods there fights ngrx's inference. Composition order is
 * enforced at the root-store level — see dot-uve.store.ts.
 */
interface SelectionAnchorMethodDeps {
    setEditorBounds: (bounds: Container[]) => void;
    setSelected: (selected: SelectedContentlet) => void;
    setEditorState: (state: EDITOR_STATE) => void;
    getPageSavePayload: (positionPayload: PositionPayload) => ActionPayload;
}

type StoreWithMethodDeps<T> = T & SelectionAnchorMethodDeps;

/**
 * Shape of the parsed client-data payload that drives selection
 * matching. Only the fields actually compared against `editorSelected`
 * are typed; the SDK ships richer data we don't read here.
 */
interface ParsedClientData {
    contentlet?: { inode?: string };
    container?: { identifier?: string; uuid?: string };
}

/**
 * Decode the SDK's contentlet-bound payload, which arrives as a JSON
 * string in postMessage payloads but may be passed as an object in
 * tests.
 */
function safeParseClientData(raw: unknown): ParsedClientData | null {
    if (raw == null) return null;
    if (typeof raw === 'object') {
        return raw as ParsedClientData;
    }
    if (typeof raw !== 'string') return null;
    try {
        return JSON.parse(raw);
    } catch {
        return null;
    }
}

/**
 * Selection-anchor feature: owns the logic that re-positions
 * `editorSelected.bounds` against a fresh bounds snapshot.
 *
 * The SDK's auto-bounds channel pushes a complete `Container[]` whenever
 * the iframe layout settles. This feature picks the contentlet that
 * matches the currently-selected (inode + container identifier + uuid)
 * out of that payload, computes its absolute on-screen coords, and
 * patches `editorSelected` so the floating toolbar
 * re-anchors to the correct position.
 *
 * It also owns the "release the iframe layout lock" responsibility:
 * once fresh bounds are applied, if the editor was in a transient
 * layout state (SCROLLING / SCROLL_DRAG / RESIZING), this feature
 * flips state back to IDLE so the overlay reappears at the right spot.
 *
 * Used to live inside `DotUveActionsHandlerService.SET_BOUNDS`. Lifting
 * it into the store keeps the actions handler a thin dispatcher and
 * makes the re-anchor logic independently testable.
 */
export function withSelectionAnchor() {
    return signalStoreFeature(
        {
            state: type<UVEState>(),
            props: type<SelectionAnchorDeps>()
        },
        withMethods((store) => {
            const s = store as StoreWithMethodDeps<typeof store>;
            return {
                /**
                 * Apply a fresh bounds snapshot. Persists it to
                 * `editorBounds`, re-anchors the selected contentlet's
                 * coords if a match is found, and releases the iframe
                 * layout lock if it was held.
                 *
                 * Match key: contentlet.inode + container.identifier +
                 * container.uuid. Inode alone would re-anchor to whichever
                 * instance iterates first when the same contentlet appears
                 * in multiple containers.
                 */
                applyBoundsForSelection(bounds: Container[]) {
                    s.setEditorBounds(bounds);

                    const wasLocked = s.$iframeLayoutLocked();

                    const selected = s.editorSelected();
                    const selectedInode = selected?.payload?.contentlet?.inode;
                    const selectedContainerId = selected?.payload?.container?.identifier;
                    const selectedContainerUuid = selected?.payload?.container?.uuid;

                    if (!selectedInode || !selectedContainerId || !selectedContainerUuid) {
                        if (wasLocked) {
                            s.setEditorState(EDITOR_STATE.IDLE);
                        }
                        return;
                    }

                    for (const container of bounds) {
                        for (const contentletBound of container.contentlets ?? []) {
                            const parsed = safeParseClientData(contentletBound.payload);
                            if (
                                parsed?.contentlet?.inode !== selectedInode ||
                                parsed?.container?.identifier !== selectedContainerId ||
                                parsed?.container?.uuid !== selectedContainerUuid
                            ) {
                                continue;
                            }
                            const actionPayload = s.getPageSavePayload(parsed as PositionPayload);
                            s.setSelected({
                                bounds: {
                                    x: container.x + contentletBound.x,
                                    y: container.y + contentletBound.y,
                                    width: contentletBound.width,
                                    height: contentletBound.height
                                },
                                payload: actionPayload
                            });
                            if (wasLocked) {
                                s.setEditorState(EDITOR_STATE.IDLE);
                            }
                            return;
                        }
                    }

                    if (wasLocked) {
                        s.setEditorState(EDITOR_STATE.IDLE);
                    }
                }
            };
        })
    );
}
