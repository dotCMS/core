import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorFocalOverlayComponent } from './dot-image-editor-focal-overlay.component';

import { ImageEditorStore } from '../../store/image-editor.store';

const IMAGE_RECT = { x: 0, y: 0, width: 400, height: 300 };

describe('DotImageEditorFocalOverlayComponent', () => {
    let spectator: Spectator<DotImageEditorFocalOverlayComponent>;
    let dispatcher: Dispatcher;

    const createComponent = createComponentFactory({
        component: DotImageEditorFocalOverlayComponent,
        providers: [
            provideNoopAnimations(),
            Dispatcher,
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [
            mockProvider(ImageEditorStore, {
                activeTool: () => 'focal',
                focalPoint: () => ({ x: 0.5, y: 0.5, active: false })
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput('imageRect', IMAGE_RECT);
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
        spectator.detectChanges();
    });

    it('should render the focal marker when the tool is active', () => {
        expect(spectator.query(byTestId('image-editor-focal-marker'))).toExist();
    });

    it('should dispatch focalPointSet with normalized 0..1 coordinates when setFocalPoint is called', () => {
        spectator.component.setFocalPoint();

        // `injectDispatch` forwards a `{ scope: 'self' }` options argument, so the
        // dispatched event is read from the first call argument directly.
        const event = dispatchedEvent('focalPointSet');
        expect(event).toBeDefined();

        const { x, y } = event!.payload as { x: number; y: number };
        // The seeded focal point is the image center.
        expect(event!.payload).toEqual({ x: 0.5, y: 0.5 });
        expect(x).toBeGreaterThanOrEqual(0);
        expect(x).toBeLessThanOrEqual(1);
        expect(y).toBeGreaterThanOrEqual(0);
        expect(y).toBeLessThanOrEqual(1);
    });

    it('should dispatch focalPointCleared when cancelFocalPoint is called', () => {
        spectator.component.cancelFocalPoint();

        expect(dispatchedEvent('focalPointCleared')).toBeDefined();
    });

    it('should stop propagation and dispatch focalPointCleared on Escape', () => {
        const event = new KeyboardEvent('keydown', { key: 'Escape' });
        const stopSpy = jest.spyOn(event, 'stopPropagation');

        spectator.element.dispatchEvent(event);

        expect(stopSpy).toHaveBeenCalled();
        expect(dispatchedEvent('focalPointCleared')).toBeDefined();
    });

    /** Finds the first dispatched event whose type matches the given suffix. */
    function dispatchedEvent(typeSuffix: string): { type: string; payload?: unknown } | undefined {
        const call = (dispatcher.dispatch as jest.Mock).mock.calls.find(([dispatched]) =>
            dispatched.type.includes(typeSuffix)
        );

        return call?.[0];
    }
});
