import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed, signal } from '@angular/core';

import { withSelectionAnchor } from './withSelectionAnchor';

import { Container } from '../../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE } from '../../../shared/enums';
import { ActionPayload, PositionPayload, SelectedContentlet } from '../../../shared/models';
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
const setEditorBoundsSpy = jest.fn();
const setSelectedSpy = jest.fn();
const setEditorStateSpy = jest.fn();
const getPageSavePayloadSpy = jest.fn(
    (positionPayload: PositionPayload) => positionPayload as unknown as ActionPayload
);

// Minimum state shape needed by withSelectionAnchor.
const initialState = {
    editorBounds: [],
    editorState: EDITOR_STATE.IDLE,
    editorContentArea: null,
    editorSelected: null
} as unknown as UVEState;

const TestStore = signalStore(
    { protectedState: false },
    withState(initialState),
    withComputed(() => ({
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
    })),
    withSelectionAnchor()
);

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
    });
});
