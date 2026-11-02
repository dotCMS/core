import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { withEditorInsert } from './withEditorInsert';

import { CONTAINER_INSERT_ERROR } from '../../../shared/enums';
import { ActionPayload, PageContainer, SelectedContentlet } from '../../../shared/models';
import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';

// What the feature calls on the root store: `editorSave` from withPageApi, and `resetSelected` and
// `resetEditorProperties` from withEditor. Module-scope spies, cleared before every test.
const editorSave = vi.fn();
const resetSelected = vi.fn();
const resetEditorProperties = vi.fn();

/**
 * The insert feature with the methods it calls standing in as spies, composed last as in the root
 * store. `setSelected` stands in for withEditor's, so a test can select a contentlet.
 */
const uveStoreMock = signalStore(
    withState<UVEState>(createInitialUVEState()),
    withMethods((store) => ({
        editorSave,
        resetSelected,
        resetEditorProperties,
        setSelected: (selected: SelectedContentlet) =>
            patchState(store, { editorSelected: selected })
    })),
    withEditorInsert()
);

/**
 * A position in `container-1`, which holds `existing-1`; `container-2` holds `other-1`. Built
 * fresh for every test, because inserting writes into the containers it is given.
 */
const createInto = (overrides: Partial<ActionPayload> = {}): ActionPayload => ({
    language_id: '1',
    pageId: 'page-1',
    pageContainers: [
        { identifier: 'container-1', uuid: '1', contentletsId: ['existing-1'] },
        { identifier: 'container-2', uuid: '1', contentletsId: ['other-1'] }
    ],
    container: {
        identifier: 'container-1',
        uuid: '1',
        acceptTypes: '*',
        maxContentlets: 5,
        contentletsId: ['existing-1']
    },
    ...overrides
});

/** A selection on `contentletId`, picked from `containerId`. */
const createSelection = (contentletId: string, containerId: string): SelectedContentlet => ({
    bounds: { x: 0, y: 0, width: 100, height: 100 },
    payload: createInto({
        contentlet: {
            identifier: contentletId,
            inode: `${contentletId}-inode`,
            title: contentletId,
            contentType: 'test'
        },
        container: { identifier: containerId, uuid: '1', acceptTypes: '*', maxContentlets: 5 }
    })
});

/** The containers the page was saved with. */
const savedContainers = (): PageContainer[] => editorSave.mock.calls[0][0];

describe('withEditorInsert', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({ service: uveStoreMock });

    beforeEach(() => {
        editorSave.mockClear();
        resetSelected.mockClear();
        resetEditorProperties.mockClear();
        spectator = createService();
        store = spectator.service;
    });

    describe('editorInsertContentlet', () => {
        describe('when the container takes the contentlet', () => {
            it("should save the page's containers with the contentlet added to its container", () => {
                store.editorInsertContentlet(createInto(), 'new-1');

                expect(editorSave).toHaveBeenCalledTimes(1);
                expect(savedContainers()).toEqual([
                    expect.objectContaining({
                        identifier: 'container-1',
                        contentletsId: ['existing-1', 'new-1']
                    }),
                    expect.objectContaining({
                        identifier: 'container-2',
                        contentletsId: ['other-1']
                    })
                ]);
            });

            it('should place the contentlet before the one the position points at', () => {
                store.editorInsertContentlet(
                    createInto({
                        position: 'before',
                        contentlet: {
                            identifier: 'existing-1',
                            inode: 'existing-1-inode',
                            title: 'existing-1',
                            contentType: 'test'
                        }
                    }),
                    'new-1'
                );

                expect(savedContainers()[0].contentletsId).toEqual(['new-1', 'existing-1']);
            });

            it('should report that it went in', () => {
                expect(store.editorInsertContentlet(createInto(), 'new-1')).toEqual({
                    didInsert: true
                });
            });

            it('should keep the selection when the selected contentlet is still in its container', () => {
                store.setSelected(createSelection('other-1', 'container-2'));

                store.editorInsertContentlet(createInto(), 'new-1');

                expect(resetSelected).not.toHaveBeenCalled();
            });

            it('should clear the selection when the selected contentlet is no longer in its container', () => {
                // What a move leaves behind: the selection still points at the container the
                // contentlet was dragged out of.
                store.setSelected(createSelection('moved-1', 'container-2'));

                store.editorInsertContentlet(createInto(), 'moved-1');

                expect(resetSelected).toHaveBeenCalledTimes(1);
                expect(editorSave).toHaveBeenCalled();
            });

            it('should leave an empty selection alone', () => {
                store.editorInsertContentlet(createInto(), 'new-1');

                expect(resetSelected).not.toHaveBeenCalled();
            });

            it('should leave the drag and hover state alone', () => {
                store.editorInsertContentlet(createInto(), 'new-1');

                expect(resetEditorProperties).not.toHaveBeenCalled();
            });
        });

        describe('when the container is full', () => {
            const createFullInto = () => {
                const into = createInto();
                into.container.maxContentlets = 1;

                return into;
            };

            it('should report that the container is full, and how many contentlets it holds', () => {
                expect(store.editorInsertContentlet(createFullInto(), 'new-1')).toEqual({
                    didInsert: false,
                    errorCode: CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED,
                    maxContentlets: 1
                });
            });

            it('should not save', () => {
                store.editorInsertContentlet(createFullInto(), 'new-1');

                expect(editorSave).not.toHaveBeenCalled();
            });
        });

        describe('when the container already holds the contentlet', () => {
            it('should report that it is already there', () => {
                expect(store.editorInsertContentlet(createInto(), 'existing-1')).toEqual({
                    didInsert: false,
                    errorCode: CONTAINER_INSERT_ERROR.DUPLICATE_CONTENT
                });
            });

            it('should not save', () => {
                store.editorInsertContentlet(createInto(), 'existing-1');

                expect(editorSave).not.toHaveBeenCalled();
            });
        });

        describe('when it does not go in', () => {
            it('should end the drag and clear the hovered contentlet', () => {
                store.editorInsertContentlet(createInto(), 'existing-1');

                expect(resetEditorProperties).toHaveBeenCalledTimes(1);
            });

            it('should keep the selection', () => {
                store.setSelected(createSelection('moved-1', 'container-2'));

                store.editorInsertContentlet(createInto(), 'existing-1');

                expect(resetSelected).not.toHaveBeenCalled();
            });
        });
    });
});
