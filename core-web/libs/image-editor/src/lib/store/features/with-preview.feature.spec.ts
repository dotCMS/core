import { patchState, signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';
import { of } from 'rxjs';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withFocalPoint } from './with-focal-point.feature';
import { withPreview } from './with-preview.feature';

import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorToolEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

// Compose preview + focal point so focal moves can be dispatched and read through
// `isDirty`, which compares the live focal point against the seeded baseline.
const PreviewStore = signalStore(
    withState(initialImageEditorState),
    withFocalPoint(),
    withPreview()
);

describe('withPreview - isDirty with focal point', () => {
    let store: InstanceType<typeof PreviewStore>;
    let tools: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;

    beforeEach(() => {
        // The debounced `resolveSize$` effect schedules timers; isolate them.
        jest.useFakeTimers();

        TestBed.configureTestingModule({
            providers: [
                PreviewStore,
                Dispatcher,
                {
                    provide: DotImageEditorService,
                    useValue: { getFileSize: jest.fn().mockReturnValue(of(1000)) }
                }
            ]
        });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(PreviewStore);
        runInInjectionContext(injector, () => {
            tools = injectDispatch(imageEditorToolEvents);
        });

        // Seed a baseline focal point so a move-and-back can be detected as pristine.
        patchState(store, {
            focalPoint: { x: 0.4, y: 0.6 },
            seededFocalPoint: { x: 0.4, y: 0.6 }
        });
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('is not dirty when the focal point matches the seeded baseline', () => {
        expect(store.isDirty()).toBe(false);
    });

    it('is dirty when the focal point moves away from the seeded baseline', () => {
        tools.focalPointSet({ x: 0.1, y: 0.9 });

        expect(store.isDirty()).toBe(true);
    });

    it('is not dirty when the focal point moves and then returns to the seeded baseline', () => {
        tools.focalPointSet({ x: 0.1, y: 0.9 });
        expect(store.isDirty()).toBe(true);

        tools.focalPointSet({ x: 0.4, y: 0.6 });
        expect(store.isDirty()).toBe(false);
    });
});
