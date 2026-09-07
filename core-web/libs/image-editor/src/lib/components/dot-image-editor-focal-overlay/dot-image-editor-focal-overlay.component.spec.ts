import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

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

    it('should commit the focal point live when the marker is placed and released', () => {
        // jsdom has no global PointerEvent constructor; a MouseEvent stands in —
        // the handler only reads clientX/clientY and keys window listeners by type.
        const surface = spectator.query<HTMLElement>(byTestId('image-editor-focal-surface'));
        surface!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 100, clientY: 150 }));
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 100, clientY: 150 }));

        // 100/400 = 0.25 horizontally, 150/300 = 0.5 vertically.
        const event = dispatchedEvent('focalPointSet');
        expect(event).toBeDefined();
        expect(event!.payload).toEqual({ x: 0.25, y: 0.5 });
    });

    it('maps the click through the zoom scale so the point lands under the cursor when zoomed', () => {
        // Zoomed out to 50%: the painted click offset is half the logical px, so the
        // handler must divide by the scale. Before the fix this mapped to 0.25/0.2.
        spectator.setInput('scale', 0.5);
        spectator.detectChanges();

        const surface = spectator.query<HTMLElement>(byTestId('image-editor-focal-surface'));
        surface!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 100, clientY: 60 }));
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 100, clientY: 60 }));

        // (100 / 0.5) / 400 = 0.5 ; (60 / 0.5) / 300 = 0.4
        const event = dispatchedEvent('focalPointSet');
        expect(event).toBeDefined();
        expect(event!.payload).toEqual({ x: 0.5, y: 0.4 });
    });

    it('should leave the focal tool (toolSelected move) when done is called', () => {
        spectator.component.done();

        const event = dispatchedEvent('toolSelected');
        expect(event).toBeDefined();
        expect(event!.payload).toBe('move');
    });

    it('should stop propagation and leave the focal tool on Escape', () => {
        const event = new KeyboardEvent('keydown', { key: 'Escape' });
        const stopSpy = jest.spyOn(event, 'stopPropagation');

        spectator.element.dispatchEvent(event);

        expect(stopSpy).toHaveBeenCalled();
        expect(dispatchedEvent('toolSelected')).toBeDefined();
    });

    /** Finds the first dispatched event whose type matches the given suffix. */
    function dispatchedEvent(typeSuffix: string): { type: string; payload?: unknown } | undefined {
        const call = (dispatcher.dispatch as jest.Mock).mock.calls.find(([dispatched]) =>
            dispatched.type.includes(typeSuffix)
        );

        return call?.[0];
    }
});
