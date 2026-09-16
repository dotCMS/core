import {
    patchState,
    signalStore,
    signalStoreFeature,
    withMethods,
    withProps,
    withState
} from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { computed, signal } from '@angular/core';

import { withSelectionAnchor } from './withSelectionAnchor';

import { Container } from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';
import { ActionPayload, PositionPayload, SelectedContentlet } from '../../../shared/models';
import { deleteContentletFromContainer } from '../../../utils';
import { UVEState } from '../../models';

const containerKey = (identifier: string, uuid: string) => ({ identifier, uuid });

const makeContentletBound = (
    inode: string,
    container: { identifier: string; uuid: string },
    box: { x: number; y: number; width: number; height: number }
) => ({
    ...box,
    payload: {
        contentlet: { inode },
        container
    } as unknown as ActionPayload
});

const makeContainer = (
    identifier: string,
    uuid: string,
    box: { x: number; y: number; width: number; height: number },
    contentlets: ReturnType<typeof makeContentletBound>[] = []
): Container =>
    ({
        ...box,
        contentlets,
        payload: { container: { identifier, uuid } } as unknown
    }) as Container;

// Module-scope signals so the test store and the test bodies share state.
// Resetting them in beforeEach makes each `it` start from a clean slate.
const iframeLayoutLockedSignal = signal(false);
const setEditorBoundsSpy = vi.fn();
const setSelectedSpy = vi.fn();
const setEditorStateSpy = vi.fn();
const getPageSavePayloadSpy = vi.fn(
    (positionPayload: PositionPayload) => positionPayload as unknown as ActionPayload
);

// Minimum state shape needed by withSelectionAnchor.
const initialState = {
    editorBounds: [],
    editorState: EDITOR_STATE.IDLE,
    editorContentArea: null,
    editorSelected: null
} as unknown as UVEState;

/**
 * The state, props and methods `withSelectionAnchor` composes on top of, bundled into one feature.
 *
 * Passed as four separate arguments to `signalStore`, ngrx stopped carrying the middle features'
 * results forward — the accumulated store arrived at `withSelectionAnchor()` as
 * `InnerSignalStore<UVEState & object, object, MethodsDictionary>`, so the `props:
 * type<SelectionAnchorDeps>()` constraint could not be met. Composing them with
 * `signalStoreFeature` first keeps the inference intact.
 *
 * `withProps` rather than `withComputed` for the same reason: `props` is where the feature declares
 * its dependency.
 */
const withTestDeps = () =>
    signalStoreFeature(
        withState<UVEState>(initialState),
        withProps(() => ({
            $iframeLayoutLocked: computed(() => iframeLayoutLockedSignal())
        })),
        withMethods((store) => ({
            setEditorBounds: (bounds: Container[]) => {
                setEditorBoundsSpy(bounds);
                patchState(store, { editorBounds: bounds });
            },
            setSelected: (selected: SelectedContentlet) => {
                setSelectedSpy(selected);
                patchState(store, { editorSelected: selected });
            },
            setEditorState: (state: EDITOR_STATE) => {
                setEditorStateSpy(state);
                patchState(store, { editorState: state });
            },
            getPageSavePayload: getPageSavePayloadSpy
        }))
    );

const TestStore = signalStore({ protectedState: false }, withTestDeps(), withSelectionAnchor());

const patchStoreState = (store: unknown, state: Partial<UVEState>) =>
    patchState(store as Parameters<typeof patchState>[0], state);

describe('withSelectionAnchor', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;

    const createService = createServiceFactory({ service: TestStore });

    beforeEach(() => {
        iframeLayoutLockedSignal.set(false);
        setEditorBoundsSpy.mockClear();
        setSelectedSpy.mockClear();
        setEditorStateSpy.mockClear();
        getPageSavePayloadSpy.mockClear();
        spectator = createService();
        store = spectator.service;
    });

    describe('applyBoundsForSelection', () => {
        it('always persists the fresh bounds snapshot', () => {
            const bounds = [makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 })];

            store.applyBoundsForSelection(bounds);

            expect(setEditorBoundsSpy).toHaveBeenCalledWith(bounds);
        });

        it('does nothing further when there is no selection', () => {
            store.applyBoundsForSelection([
                makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 })
            ]);

            expect(setSelectedSpy).not.toHaveBeenCalled();
            expect(setEditorStateSpy).not.toHaveBeenCalled();
        });

        it('matches by inode + container.identifier + container.uuid and re-anchors with absolute coords', () => {
            patchStoreState(store, {
                editorSelected: {
                    bounds: { x: 0, y: 0, width: 0, height: 0 },
                    payload: {
                        contentlet: { inode: 'inode-target' },
                        container: { identifier: 'c1', uuid: '1' }
                    } as unknown as ActionPayload
                }
            });

            const target = makeContentletBound('inode-target', containerKey('c1', '1'), {
                x: 50,
                y: 60,
                width: 200,
                height: 80
            });
            const bounds: Container[] = [
                makeContainer('c1', '1', { x: 10, y: 20, width: 500, height: 500 }, [target])
            ];

            store.applyBoundsForSelection(bounds);

            expect(getPageSavePayloadSpy).toHaveBeenCalledWith(target.payload);
            expect(setSelectedSpy).toHaveBeenCalledWith({
                // container.x + contentletBound.x and so on — the floating
                // toolbar lives in the canvas's absolute coordinate space.
                bounds: { x: 60, y: 80, width: 200, height: 80 },
                payload: target.payload
            });
        });

        it('does not match a contentlet with the same inode in a different container', () => {
            // Same contentlet appears in two containers — the wrong instance
            // must not steal the anchor.
            patchStoreState(store, {
                editorSelected: {
                    bounds: { x: 0, y: 0, width: 0, height: 0 },
                    payload: {
                        contentlet: { inode: 'shared-inode' },
                        container: { identifier: 'c2', uuid: '2' }
                    } as unknown as ActionPayload
                }
            });

            const wrongContainerHit = makeContentletBound('shared-inode', containerKey('c1', '1'), {
                x: 0,
                y: 0,
                width: 10,
                height: 10
            });
            const correctContainerHit = makeContentletBound(
                'shared-inode',
                containerKey('c2', '2'),
                { x: 5, y: 5, width: 15, height: 15 }
            );

            store.applyBoundsForSelection([
                makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [
                    wrongContainerHit
                ]),
                makeContainer('c2', '2', { x: 100, y: 100, width: 100, height: 100 }, [
                    correctContainerHit
                ])
            ]);

            expect(setSelectedSpy).toHaveBeenCalledTimes(1);
            const lastCall = setSelectedSpy.mock.calls[0][0] as SelectedContentlet;
            expect(lastCall.bounds).toEqual({ x: 105, y: 105, width: 15, height: 15 });
        });

        it('parses a stringified payload (postMessage transport shape)', () => {
            patchStoreState(store, {
                editorSelected: {
                    bounds: { x: 0, y: 0, width: 0, height: 0 },
                    payload: {
                        contentlet: { inode: 'inode-1' },
                        container: { identifier: 'c1', uuid: '1' }
                    } as unknown as ActionPayload
                }
            });

            const stringifiedHit = {
                x: 10,
                y: 10,
                width: 50,
                height: 50,
                payload: JSON.stringify({
                    contentlet: { inode: 'inode-1' },
                    container: { identifier: 'c1', uuid: '1' }
                })
            } as unknown as ReturnType<typeof makeContentletBound>;

            store.applyBoundsForSelection([
                makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [stringifiedHit])
            ]);

            expect(setSelectedSpy).toHaveBeenCalled();
        });

        it('skips contentlets whose payload is malformed JSON', () => {
            patchStoreState(store, {
                editorSelected: {
                    bounds: { x: 0, y: 0, width: 0, height: 0 },
                    payload: {
                        contentlet: { inode: 'inode-1' },
                        container: { identifier: 'c1', uuid: '1' }
                    } as unknown as ActionPayload
                }
            });

            const malformed = {
                x: 0,
                y: 0,
                width: 10,
                height: 10,
                payload: '{not valid json'
            } as unknown as ReturnType<typeof makeContentletBound>;

            store.applyBoundsForSelection([
                makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [malformed])
            ]);

            expect(setSelectedSpy).not.toHaveBeenCalled();
        });

        describe('iframe layout lock', () => {
            beforeEach(() => {
                iframeLayoutLockedSignal.set(true);
            });

            it('flips editorState back to IDLE after a successful re-anchor', () => {
                patchStoreState(store, {
                    editorSelected: {
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: {
                            contentlet: { inode: 'inode-1' },
                            container: { identifier: 'c1', uuid: '1' }
                        } as unknown as ActionPayload
                    }
                });

                store.applyBoundsForSelection([
                    makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [
                        makeContentletBound('inode-1', containerKey('c1', '1'), {
                            x: 0,
                            y: 0,
                            width: 10,
                            height: 10
                        })
                    ])
                ]);

                expect(setEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
            });

            it('flips IDLE even when there is no selection (early return path)', () => {
                store.applyBoundsForSelection([
                    makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 })
                ]);

                expect(setEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
            });

            it('flips IDLE when the selection has no match in the bounds payload', () => {
                patchStoreState(store, {
                    editorSelected: {
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: {
                            contentlet: { inode: 'missing' },
                            container: { identifier: 'c1', uuid: '1' }
                        } as unknown as ActionPayload
                    }
                });

                store.applyBoundsForSelection([
                    makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [
                        makeContentletBound('other-inode', containerKey('c1', '1'), {
                            x: 0,
                            y: 0,
                            width: 10,
                            height: 10
                        })
                    ])
                ]);

                expect(setSelectedSpy).not.toHaveBeenCalled();
                expect(setEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
            });
        });

        it('does not touch editorState when the lock was not held', () => {
            patchStoreState(store, {
                editorSelected: {
                    bounds: { x: 0, y: 0, width: 0, height: 0 },
                    payload: {
                        contentlet: { inode: 'inode-1' },
                        container: { identifier: 'c1', uuid: '1' }
                    } as unknown as ActionPayload
                }
            });

            store.applyBoundsForSelection([
                makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [
                    makeContentletBound('inode-1', containerKey('c1', '1'), {
                        x: 0,
                        y: 0,
                        width: 10,
                        height: 10
                    })
                ])
            ]);

            expect(setEditorStateSpy).not.toHaveBeenCalled();
        });

        /**
         * AC-011 (#37499). A SET_BOUNDS re-anchor is a POSITIONAL event, and the
         * bounds snapshot the SDK sends carries only a five-field contentlet
         * ({identifier, title, inode, contentType, canEdit} — see
         * getDotCMSContentletsBound). Rebuilding the selected payload from it
         * therefore DEGRADES it: `vtlFiles`, `baseType`, `onNumberOfPages` and
         * `dotStyleProperties` all disappear. That is the root cause of #37499,
         * where the VTL menu opened empty after any layout shift.
         *
         * The fix is a MERGE, not a freeze. Freezing the payload was ruled out:
         * `insertContentletInContainer({...payload})` and
         * `deleteContentletFromContainer(payload)` read `payload.pageContainers`
         * straight off it, so keeping the click-time copy would make add/delete
         * write a stale container tree. Hence the two halves below — preserve the
         * DOM-sourced data, keep refreshing the store-sourced context.
         */
        describe('payload merge on re-anchor', () => {
            const SELECTED_WITH_FULL_PAYLOAD = {
                bounds: { x: 0, y: 0, width: 0, height: 0 },
                payload: {
                    contentlet: {
                        inode: 'inode-1',
                        title: 'Stale title',
                        baseType: 'WIDGET',
                        onNumberOfPages: '3',
                        dotStyleProperties: { padding: '8px' }
                    },
                    container: { identifier: 'c1', uuid: '1' },
                    vtlFiles: [{ inode: 'vtl-inode-1', name: 'uve-vtl-repro.vtl' }],
                    pageContainers: [{ identifier: 'stale-container' }]
                } as unknown as ActionPayload
            };

            // What the SDK actually sends: no vtlFiles, no baseType,
            // no onNumberOfPages, no dotStyleProperties.
            const degradedBound = () =>
                ({
                    x: 0,
                    y: 0,
                    width: 10,
                    height: 10,
                    payload: {
                        contentlet: {
                            inode: 'inode-1',
                            title: 'Fresh title',
                            contentType: 'VtlInclude',
                            canEdit: true
                        },
                        container: { identifier: 'c1', uuid: '1' }
                    }
                }) as unknown as ReturnType<typeof makeContentletBound>;

            const reAnchor = () =>
                store.applyBoundsForSelection([
                    makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [
                        degradedBound()
                    ])
                ]);

            it('preserves DOM-sourced contentlet data the bounds snapshot cannot carry', () => {
                patchStoreState(store, { editorSelected: SELECTED_WITH_FULL_PAYLOAD });

                reAnchor();

                const { payload } = setSelectedSpy.mock.calls[0][0];
                expect(payload.vtlFiles).toEqual([
                    { inode: 'vtl-inode-1', name: 'uve-vtl-repro.vtl' }
                ]);
                expect(payload.contentlet.baseType).toBe('WIDGET');
                expect(payload.contentlet.onNumberOfPages).toBe('3');
                expect(payload.contentlet.dotStyleProperties).toEqual({ padding: '8px' });
            });

            it('still refreshes store-sourced page context from getPageSavePayload', () => {
                // The guard against over-preserving: freezing these would make
                // add/delete write a stale container tree.
                getPageSavePayloadSpy.mockImplementationOnce(
                    (positionPayload: PositionPayload) =>
                        ({
                            ...positionPayload,
                            pageContainers: [{ identifier: 'fresh-container' }],
                            pageId: 'fresh-page-id'
                        }) as unknown as ActionPayload
                );
                patchStoreState(store, { editorSelected: SELECTED_WITH_FULL_PAYLOAD });

                reAnchor();

                const { payload } = setSelectedSpy.mock.calls[0][0];
                expect(payload.pageContainers).toEqual([{ identifier: 'fresh-container' }]);
                expect(payload.pageId).toBe('fresh-page-id');
            });

            it('lets fresh snapshot fields win over the preserved ones', () => {
                patchStoreState(store, { editorSelected: SELECTED_WITH_FULL_PAYLOAD });

                reAnchor();

                const { payload } = setSelectedSpy.mock.calls[0][0];
                expect(payload.contentlet.title).toBe('Fresh title');
                expect(payload.contentlet.contentType).toBe('VtlInclude');
                expect(payload.contentlet.canEdit).toBe(true);
            });

            /**
             * The reason the merge preserves DOM data but never freezes the whole
             * payload. deleteContentletFromContainer and
             * insertContentletInContainer read action.pageContainers straight off
             * this object — and mutate it with .push() when the container is not
             * found — so a merge that carried the click-time container tree
             * forward would silently write stale content back to the page.
             *
             * The sibling test above asserts the payload FIELD; this one runs the
             * real consumer over the merged payload, which is what the field
             * exists for.
             */
            it('leaves the merged payload safe for the save path after a re-anchor', () => {
                const STALE = [
                    { identifier: 'c1', uuid: '1', contentletsId: ['gone-since-selection'] }
                ];
                const FRESH = [
                    {
                        identifier: 'c1',
                        uuid: '1',
                        contentletsId: ['kept-a', 'inode-1-identifier', 'kept-b']
                    }
                ];

                getPageSavePayloadSpy.mockImplementationOnce(
                    (positionPayload: PositionPayload) =>
                        ({
                            ...positionPayload,
                            pageContainers: FRESH
                        }) as unknown as ActionPayload
                );

                patchStoreState(store, {
                    editorSelected: {
                        ...SELECTED_WITH_FULL_PAYLOAD,
                        payload: {
                            ...SELECTED_WITH_FULL_PAYLOAD.payload,
                            pageContainers: STALE
                        } as unknown as ActionPayload
                    }
                });

                store.applyBoundsForSelection([
                    makeContainer('c1', '1', { x: 0, y: 0, width: 100, height: 100 }, [
                        {
                            x: 0,
                            y: 0,
                            width: 10,
                            height: 10,
                            payload: {
                                contentlet: {
                                    inode: 'inode-1',
                                    identifier: 'inode-1-identifier',
                                    title: 'Fresh title'
                                },
                                container: { identifier: 'c1', uuid: '1' }
                            }
                        } as unknown as ReturnType<typeof makeContentletBound>
                    ])
                ]);

                const { payload } = setSelectedSpy.mock.calls[0][0];
                const { contentletsId } = deleteContentletFromContainer(payload);

                // Operated on the refreshed tree. Had the stale pageContainers
                // survived the merge, this would be ['gone-since-selection'].
                expect(contentletsId).toEqual(['kept-a', 'kept-b']);
            });

            /**
             * Only DOM-sourced data is preserved — the transient fields are not.
             *
             * `promoteHoverToSelected()` writes `contentContext()`, which always
             * stamps `position` (defaulting to 'after'), and getPageSavePayload
             * never sets it. A blanket carry-forward would therefore pin
             * `position` on the selection for as long as it lives, and
             * insertContentletInContainer branches on exactly that field:
             *
             *     if (action.position) { return insertPositionedContentletInContainer(action); }
             *
             * Every insert path re-stamps `position` today, so this is latent
             * rather than live — which is the reason to pin it now, while it is
             * still cheap.
             */
            it('drops transient fields instead of pinning them to the selection', () => {
                patchStoreState(store, {
                    editorSelected: {
                        ...SELECTED_WITH_FULL_PAYLOAD,
                        payload: {
                            ...SELECTED_WITH_FULL_PAYLOAD.payload,
                            position: 'after',
                            newContentletId: 'left-over-from-an-add'
                        } as unknown as ActionPayload
                    }
                });

                reAnchor();

                const { payload } = setSelectedSpy.mock.calls[0][0];
                expect(payload.position).toBeUndefined();
                expect(payload.newContentletId).toBeUndefined();
                // …while the DOM-sourced data it sits beside still survives.
                expect(payload.vtlFiles).toBeDefined();
            });
        });
    });
});
