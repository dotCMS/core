import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorHistoryPanelComponent } from './dot-image-editor-history-panel.component';

import { AppliedEditEntry } from '../../../models/image-editor.models';
import { imageEditorHistoryEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';

const ENTRY: AppliedEditEntry = { id: 'a1', category: 'adjust', label: 'Brightness 30' };

describe('DotImageEditorHistoryPanelComponent', () => {
    let spectator: Spectator<DotImageEditorHistoryPanelComponent>;
    let dispatcher: Dispatcher;

    const appliedEdits = signal<AppliedEditEntry[]>([]);

    const createComponent = createComponentFactory({
        component: DotImageEditorHistoryPanelComponent,
        providers: [
            provideNoopAnimations(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [Dispatcher, mockProvider(ImageEditorStore, { appliedEdits })]
    });

    beforeEach(() => {
        appliedEdits.set([]);
        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    describe('with applied edits', () => {
        beforeEach(() => {
            appliedEdits.set([ENTRY]);
            spectator.detectChanges();
        });

        it('should render a row per applied edit', () => {
            expect(spectator.queryAll(byTestId('image-editor-history-entry'))).toHaveLength(1);
        });

        it('should dispatch editRemoved with the entry id when removing', () => {
            const removeBtn = spectator.query(byTestId('image-editor-history-remove'));
            spectator.click(removeBtn!.querySelector('button')!);

            const event = dispatchedEvent(imageEditorHistoryEvents.editRemoved.type);
            expect(event).toBeDefined();
            expect(event!.payload).toEqual({ id: 'a1' });
        });

        it('should dispatch resetRequested when resetting all edits', () => {
            const resetBtn = spectator.query(byTestId('image-editor-history-reset'));
            spectator.click(resetBtn!.querySelector('button')!);

            expect(dispatchedEvent(imageEditorHistoryEvents.resetRequested.type)).toBeDefined();
        });

        it('should not render the empty state', () => {
            expect(spectator.query(byTestId('image-editor-history-empty'))).toBeNull();
        });
    });

    describe('with no applied edits', () => {
        it('should render the empty state', () => {
            expect(spectator.query(byTestId('image-editor-history-empty'))).toExist();
        });

        it('should not render any history rows', () => {
            expect(spectator.queryAll(byTestId('image-editor-history-entry'))).toHaveLength(0);
        });
    });

    /**
     * Finds the dispatched event matching the given type. `injectDispatch`
     * forwards a `{ scope: 'self' }` options argument, so the event is read from
     * the first call argument.
     */
    function dispatchedEvent(type: string): { type: string; payload?: unknown } | undefined {
        const call = (dispatcher.dispatch as jest.Mock).mock.calls.find(
            ([dispatched]) => dispatched.type === type
        );

        return call?.[0];
    }
});
