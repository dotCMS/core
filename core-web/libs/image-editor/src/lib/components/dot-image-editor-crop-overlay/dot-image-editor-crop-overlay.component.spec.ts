import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorCropOverlayComponent } from './dot-image-editor-crop-overlay.component';

import { ImageEditorStore } from '../../store/image-editor.store';

const IMAGE_RECT = { x: 0, y: 0, width: 400, height: 300 };
const NATURAL = { naturalWidth: 800, naturalHeight: 600 };

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
                activeTool: () => 'crop',
                assetContext: () => NATURAL
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
