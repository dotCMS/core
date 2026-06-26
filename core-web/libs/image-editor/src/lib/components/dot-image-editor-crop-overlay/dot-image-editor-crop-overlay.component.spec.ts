import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorCropOverlayComponent } from './dot-image-editor-crop-overlay.component';

import { ImageEditorStore } from '../../store/image-editor.store';

const IMAGE_RECT = { x: 0, y: 0, width: 400, height: 300 };
// Intrinsic size of the displayed image, handed to the overlay via the `naturalSize`
// input (the space crop boxes are converted into).
const NATURAL = { width: 800, height: 600 };

describe('DotImageEditorCropOverlayComponent', () => {
    let spectator: Spectator<DotImageEditorCropOverlayComponent>;
    let dispatcher: Dispatcher;

    const createComponent = createComponentFactory({
        component: DotImageEditorCropOverlayComponent,
        providers: [
            provideNoopAnimations(),
            Dispatcher,
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [
            mockProvider(ImageEditorStore, {
                activeTool: () => 'crop'
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput('imageRect', IMAGE_RECT);
        spectator.setInput('naturalSize', NATURAL);
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
        spectator.detectChanges();
    });

    it('should render the crop box and eight resize handles', () => {
        expect(spectator.query(byTestId('image-editor-crop-box'))).toExist();

        const handles = ['tl', 't', 'tr', 'r', 'br', 'b', 'bl', 'l'];
        handles.forEach((position) => {
            expect(spectator.query(byTestId(`image-editor-crop-handle-${position}`))).toExist();
        });
    });

    it('should dispatch cropApplied with a natural-pixel rect when applyCrop is called', () => {
        spectator.component.applyCrop();

        // The default selection covers the whole image, so scaling 400x300 CSS px
        // up to 800x600 natural px yields the full natural rectangle.
        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            expect.objectContaining({
                payload: { x: 0, y: 0, w: 800, h: 600, active: true, aspect: null }
            }),
            { scope: 'self' }
        );
    });

    it('should dispatch cropCancelled when cancelCrop is called', () => {
        spectator.component.cancelCrop();

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            expect.objectContaining({ type: expect.stringContaining('cropCancelled') }),
            { scope: 'self' }
        );
    });

    it('renders four solid dim panels aligned to the crop box edges', () => {
        // Shrink the box 100px from the right so it no longer fills the image and the
        // dim is visible; the right panel should then start at the box's right edge.
        const rightHandle = spectator.query<HTMLElement>(byTestId('image-editor-crop-handle-r'));
        rightHandle!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 400, clientY: 150 }));
        window.dispatchEvent(new MouseEvent('pointermove', { clientX: 300, clientY: 150 }));
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 300, clientY: 150 }));
        spectator.detectChanges();

        ['top', 'bottom', 'left', 'right'].forEach((side) => {
            expect(spectator.query(byTestId(`image-editor-crop-mask-${side}`))).toExist();
        });

        // Box now spans x:0..300, so the right dim panel begins at x=300.
        const right = spectator.query<HTMLElement>(byTestId('image-editor-crop-mask-right'));
        expect(right!.style.left).toBe('300px');
    });

    it('should nudge the crop rect by 1px on ArrowRight from the focused box', () => {
        const box = spectator.query<HTMLElement>(byTestId('image-editor-crop-box'));

        // The box is seeded to the full image width, so it has no room to move
        // right. Shrink it from the right edge first by dragging the `r` handle
        // 50px to the left, leaving 50px of horizontal slack. jsdom does not
        // expose a global PointerEvent constructor, so a MouseEvent stands in:
        // the handlers only read clientX/clientY and the window listeners are
        // keyed by the pointer event-type strings.
        const rightHandle = spectator.query<HTMLElement>(byTestId('image-editor-crop-handle-r'));
        rightHandle!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 400, clientY: 150 }));
        window.dispatchEvent(new MouseEvent('pointermove', { clientX: 350, clientY: 150 }));
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 350, clientY: 150 }));
        spectator.detectChanges();

        // The box now starts flush against the left edge of the image.
        expect(box!.style.left).toEqual('0px');

        spectator.dispatchKeyboardEvent(box!, 'keydown', 'ArrowRight');
        spectator.detectChanges();

        expect(box!.style.left).toEqual('1px');
    });

    it('should lock the crop to its aspect ratio when dragging a corner with Shift', () => {
        const box = spectator.query<HTMLElement>(byTestId('image-editor-crop-box'));
        const brHandle = spectator.query<HTMLElement>(byTestId('image-editor-crop-handle-br'));

        // The box starts at the full 400x300 image (4:3). Drag the bottom-right
        // corner inward with Shift held; the locked resize must keep the 4:3 ratio
        // and stay anchored at the opposite (top-left) corner.
        brHandle!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 400, clientY: 300 }));
        window.dispatchEvent(
            new MouseEvent('pointermove', { clientX: 300, clientY: 200, shiftKey: true })
        );
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 300, clientY: 200 }));
        spectator.detectChanges();

        expect(parseFloat(box!.style.width) / parseFloat(box!.style.height)).toBeCloseTo(
            400 / 300,
            5
        );
        expect(box!.style.left).toEqual('0px');
        expect(box!.style.top).toEqual('0px');
    });

    it('should resize a corner freely (ignoring aspect) without Shift', () => {
        const box = spectator.query<HTMLElement>(byTestId('image-editor-crop-box'));
        const brHandle = spectator.query<HTMLElement>(byTestId('image-editor-crop-handle-br'));

        // The same inward corner drag without Shift is free-form: width and height
        // follow the pointer independently (400→300, 300→200), breaking the ratio.
        brHandle!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 400, clientY: 300 }));
        window.dispatchEvent(new MouseEvent('pointermove', { clientX: 300, clientY: 200 }));
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 300, clientY: 200 }));
        spectator.detectChanges();

        expect(box!.style.width).toEqual('300px');
        expect(box!.style.height).toEqual('200px');
    });

    it('should reshape the box to the locked aspect, centered and fit to the image', () => {
        const box = spectator.query<HTMLElement>(byTestId('image-editor-crop-box'));

        // The 400x300 image is 4:3 (1.33). A 1:1 lock fits the full height (300) and
        // a matching 300 width, centered horizontally → x=(400-300)/2=50, y=0.
        spectator.setInput('aspect', 1);
        spectator.detectChanges();

        expect(parseFloat(box!.style.width) / parseFloat(box!.style.height)).toBeCloseTo(1, 5);
        expect(box!.style.width).toEqual('300px');
        expect(box!.style.height).toEqual('300px');
        expect(box!.style.left).toEqual('50px');
        expect(box!.style.top).toEqual('0px');
    });

    it('should preserve the locked aspect when resizing a corner without Shift', () => {
        const box = spectator.query<HTMLElement>(byTestId('image-editor-crop-box'));

        // Lock to 1:1 first (box becomes 300x300 at x=50), then drag the bottom-right
        // corner inward WITHOUT Shift: the locked ratio must still hold.
        spectator.setInput('aspect', 1);
        spectator.detectChanges();

        const brHandle = spectator.query<HTMLElement>(byTestId('image-editor-crop-handle-br'));
        brHandle!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 350, clientY: 300 }));
        window.dispatchEvent(new MouseEvent('pointermove', { clientX: 250, clientY: 200 }));
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 250, clientY: 200 }));
        spectator.detectChanges();

        expect(parseFloat(box!.style.width) / parseFloat(box!.style.height)).toBeCloseTo(1, 5);
    });

    it('should expose the crop box size in natural image pixels', () => {
        // The default selection covers the whole 400x300 rendered image; the asset is
        // 800x600 natural, so the box reports the full 800x600 natural size.
        expect(spectator.component.naturalCropSize()).toEqual({ width: 800, height: 600 });
    });

    it('should track the natural size as the box is resized on the canvas', () => {
        // Shrink the box from the right edge by 50 CSS px (400→350). At the 2x scale
        // the reported natural width follows (700) while the height is unchanged.
        const rightHandle = spectator.query<HTMLElement>(byTestId('image-editor-crop-handle-r'));
        rightHandle!.dispatchEvent(new MouseEvent('pointerdown', { clientX: 400, clientY: 150 }));
        window.dispatchEvent(new MouseEvent('pointermove', { clientX: 350, clientY: 150 }));
        window.dispatchEvent(new MouseEvent('pointerup', { clientX: 350, clientY: 150 }));
        spectator.detectChanges();

        expect(spectator.component.naturalCropSize()).toEqual({ width: 700, height: 600 });
    });

    it('should resize the box to a given natural size, centered on its current center', () => {
        const box = spectator.query<HTMLElement>(byTestId('image-editor-crop-box'));

        // Request a 400x300 natural box. At the 2x scale that is 200x150 CSS px,
        // centered in the 400x300 image → x=(400-200)/2=100, y=(300-150)/2=75.
        spectator.component.setNaturalCropSize(400, 300);
        spectator.detectChanges();

        expect(box!.style.width).toEqual('200px');
        expect(box!.style.height).toEqual('150px');
        expect(box!.style.left).toEqual('100px');
        expect(box!.style.top).toEqual('75px');
        expect(spectator.component.naturalCropSize()).toEqual({ width: 400, height: 300 });
    });

    it('should clamp a requested natural size to the rendered image bounds', () => {
        const box = spectator.query<HTMLElement>(byTestId('image-editor-crop-box'));

        // Request a natural size larger than the source (1600x1200 vs 800x600); the
        // CSS-px box is clamped to the full rendered image (400x300).
        spectator.component.setNaturalCropSize(1600, 1200);
        spectator.detectChanges();

        expect(box!.style.width).toEqual('400px');
        expect(box!.style.height).toEqual('300px');
    });

    it('should stop propagation and dispatch cropCancelled on Escape', () => {
        const event = new KeyboardEvent('keydown', { key: 'Escape' });
        const stopSpy = jest.spyOn(event, 'stopPropagation');

        spectator.element.dispatchEvent(event);

        expect(stopSpy).toHaveBeenCalled();
        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            expect.objectContaining({ type: expect.stringContaining('cropCancelled') }),
            { scope: 'self' }
        );
    });
});
