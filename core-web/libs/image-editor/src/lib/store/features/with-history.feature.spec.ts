import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withHistory } from './with-history.feature';

import { EditableSlices, ImageEditorHistoryEntry } from '../../models/image-editor.models';
import { imageEditorHistoryEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';
import { initialEditableSlices } from '../image-editor.store-utils';

function entry(
    id: string,
    category: ImageEditorHistoryEntry['category'],
    patch: Partial<EditableSlices>
): ImageEditorHistoryEntry {
    return { id, category, label: id, snapshot: { ...initialEditableSlices, ...patch } };
}

const brightness20 = { ...initialEditableSlices.adjust, brightness: 20 };
// Cumulative snapshots: a (brightness 20), b (brightness 20 + rotate 45), c (+ flipH).
const a = entry('a', 'adjust', { adjust: brightness20 });
const b = entry('b', 'rotate', {
    adjust: brightness20,
    transform: { ...initialEditableSlices.transform, rotateDeg: 45 }
});
const c = entry('c', 'flip', {
    adjust: brightness20,
    transform: { ...initialEditableSlices.transform, rotateDeg: 45, flipH: true }
});

const HistoryStoreAB = signalStore(
    withState({ ...initialImageEditorState, history: [a, b], historyIndex: 1 }),
    withHistory()
);
const HistoryStoreABCHeadFirst = signalStore(
    withState({ ...initialImageEditorState, history: [a, b, c], historyIndex: 0 }),
    withHistory()
);

function setup<T>(StoreClass: Type<T>) {
    TestBed.configureTestingModule({ providers: [StoreClass, Dispatcher] });
    const injector = TestBed.inject(Injector);
    const store = TestBed.inject(StoreClass);
    let history!: ReturnType<typeof injectDispatch<typeof imageEditorHistoryEvents>>;
    runInInjectionContext(injector, () => {
        history = injectDispatch(imageEditorHistoryEvents);
    });

    return { store, history };
}

describe('withHistory', () => {
    describe('editRemoved', () => {
        it('removes the head entry and rebuilds the surviving slices', () => {
            const { store, history } = setup(HistoryStoreAB);

            history.editRemoved({ id: 'b' });

            expect(store.history().map((e) => e.id)).toEqual(['a']);
            expect(store.historyIndex()).toBe(0);
            expect(store.adjust().brightness).toBe(20);
            expect(store.transform().rotateDeg).toBe(0);
        });

        it('is a no-op when the id does not exist', () => {
            const { store, history } = setup(HistoryStoreAB);

            history.editRemoved({ id: 'missing' });

            expect(store.history()).toHaveLength(2);
            expect(store.historyIndex()).toBe(1);
        });

        it('keeps the head when removing an entry past it (redo tail)', () => {
            const { store, history } = setup(HistoryStoreABCHeadFirst); // head at index 0

            history.editRemoved({ id: 'c' }); // index 2 > head

            expect(store.history().map((e) => e.id)).toEqual(['a', 'b']);
            expect(store.historyIndex()).toBe(0);
        });
    });

    describe('undo / redo', () => {
        it('undoes to the previous snapshot and redoes forward', () => {
            const { store, history } = setup(HistoryStoreAB);

            history.undoRequested();
            expect(store.historyIndex()).toBe(0);
            expect(store.transform().rotateDeg).toBe(0);
            expect(store.adjust().brightness).toBe(20);

            history.redoRequested();
            expect(store.historyIndex()).toBe(1);
            expect(store.transform().rotateDeg).toBe(45);
        });

        it('does not undo past the start nor redo past the end', () => {
            const { store, history } = setup(HistoryStoreAB);

            history.undoRequested();
            history.undoRequested();
            history.undoRequested();
            expect(store.historyIndex()).toBe(-1);
            expect(store.adjust().brightness).toBe(0);

            history.redoRequested();
            history.redoRequested();
            history.redoRequested();
            expect(store.historyIndex()).toBe(1);
        });
    });

    it('resets the whole history and the editable slices', () => {
        const { store, history } = setup(HistoryStoreAB);

        history.resetRequested();

        expect(store.history()).toEqual([]);
        expect(store.historyIndex()).toBe(-1);
        expect(store.adjust().brightness).toBe(0);
        expect(store.transform().rotateDeg).toBe(0);
    });

    describe('computed selectors', () => {
        it('appliedEdits lists the entries up to the head', () => {
            const { store, history } = setup(HistoryStoreAB);

            expect(store.appliedEdits().map((e) => e.id)).toEqual(['a', 'b']);

            history.undoRequested();
            expect(store.appliedEdits().map((e) => e.id)).toEqual(['a']);
        });

        it('canUndo / canRedo reflect the head position', () => {
            const { store, history } = setup(HistoryStoreAB);

            expect(store.canUndo()).toBe(true);
            expect(store.canRedo()).toBe(false);

            history.undoRequested();
            expect(store.canUndo()).toBe(true);
            expect(store.canRedo()).toBe(true);

            history.undoRequested();
            expect(store.canUndo()).toBe(false);
        });
    });
});
