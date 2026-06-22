import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withTools } from './with-tools.feature';

import { imageEditorToolEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

const ToolsStore = signalStore(withState(initialImageEditorState), withTools());

describe('withTools', () => {
    let store: InstanceType<typeof ToolsStore>;
    let tool: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [ToolsStore, Dispatcher] });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(ToolsStore);
        runInInjectionContext(injector, () => {
            tool = injectDispatch(imageEditorToolEvents);
        });
    });

    it('selects a tool without touching history', () => {
        tool.toolSelected('crop');
        expect(store.activeTool()).toBe('crop');
        expect(store.history()).toHaveLength(0);

        tool.toolSelected('focal');
        expect(store.activeTool()).toBe('focal');
    });
});
