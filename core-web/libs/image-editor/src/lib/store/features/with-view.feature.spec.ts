import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withView } from './with-view.feature';

import { imageEditorToolEvents, imageEditorViewEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

const ViewStore = signalStore(withState(initialImageEditorState), withView());

describe('withView', () => {
    let store: InstanceType<typeof ViewStore>;
    let tool: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;
    let view: ReturnType<typeof injectDispatch<typeof imageEditorViewEvents>>;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [ViewStore, Dispatcher] });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(ViewStore);
        runInInjectionContext(injector, () => {
            tool = injectDispatch(imageEditorToolEvents);
            view = injectDispatch(imageEditorViewEvents);
        });
    });

    it('selects a tool without touching history', () => {
        tool.toolSelected('crop');
        expect(store.activeTool()).toBe('crop');
        expect(store.history()).toHaveLength(0);

        tool.toolSelected('focal');
        expect(store.activeTool()).toBe('focal');
    });

    it('toggles full-screen on and off', () => {
        expect(store.isFullscreen()).toBe(false);

        view.fullscreenToggled();
        expect(store.isFullscreen()).toBe(true);

        view.fullscreenToggled();
        expect(store.isFullscreen()).toBe(false);
    });
});
