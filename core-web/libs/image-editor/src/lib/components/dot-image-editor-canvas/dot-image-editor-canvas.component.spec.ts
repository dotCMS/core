import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';
import { MockComponent } from 'ng-mocks';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorCanvasComponent } from './dot-image-editor-canvas.component';

import { PreviewStatus } from '../../models/image-editor.models';
import { ImageEditorStore } from '../../store/image-editor.store';
import { DotImageEditorAddressBarComponent } from '../dot-image-editor-address-bar/dot-image-editor-address-bar.component';
import { DotImageEditorCropOverlayComponent } from '../dot-image-editor-crop-overlay/dot-image-editor-crop-overlay.component';
import { DotImageEditorFocalOverlayComponent } from '../dot-image-editor-focal-overlay/dot-image-editor-focal-overlay.component';
import { DotImageEditorToolRailComponent } from '../dot-image-editor-tool-rail/dot-image-editor-tool-rail.component';

const PREVIEW_URL = '/contentAsset/image/inode-1/fileAsset?byInode=true&r=1';
const NEXT_PREVIEW_URL = '/contentAsset/image/inode-1/fileAsset?byInode=true&r=2';

// jsdom has no ResizeObserver; the canvas observes the displayed image to track
// its rendered rect, so provide a no-op implementation for the suite.
class MockResizeObserver {
    observe = jest.fn();
    unobserve = jest.fn();
    disconnect = jest.fn();
}
Object.defineProperty(window, 'ResizeObserver', {
    writable: true,
    configurable: true,
    value: MockResizeObserver
});

// jsdom implements neither HTMLImageElement.decode() nor real natural dimensions;
// stub them so the canvas's decode()-based completeness check resolves with a
// valid, sized image. Individual tests override decode() to simulate failures.
HTMLImageElement.prototype.decode = jest.fn().mockResolvedValue(undefined);
Object.defineProperty(HTMLImageElement.prototype, 'naturalWidth', {
    configurable: true,
    get: () => 800
});
Object.defineProperty(HTMLImageElement.prototype, 'naturalHeight', {
    configurable: true,
    get: () => 600
});

/** Flushes the microtask queue so a resolved/rejected `decode()` promise settles. */
const flushDecode = () => new Promise<void>((resolve) => setTimeout(resolve));

describe('DotImageEditorCanvasComponent', () => {
    let spectator: Spectator<DotImageEditorCanvasComponent>;
    let dispatcher: Dispatcher;

    const previewUrl = signal(PREVIEW_URL);
    const previewStatus = signal<PreviewStatus>('idle');
    const zoom = signal({ level: 100, fitToScreen: true });
    const activeTool = signal<'move' | 'crop' | 'focal'>('move');
    const assetContext = signal({ naturalWidth: 800, naturalHeight: 600 });

    const createComponent = createComponentFactory({
        component: DotImageEditorCanvasComponent,
        providers: [
            provideNoopAnimations(),
            Dispatcher,
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [
            mockProvider(ImageEditorStore, {
                previewUrl,
                previewStatus,
                zoom,
                activeTool,
                assetContext
            })
        ],
        // Isolate the canvas from the children's own store/dispatch wiring.
        overrideComponents: [
            [
                DotImageEditorCanvasComponent,
                {
                    remove: {
                        imports: [
                            DotImageEditorAddressBarComponent,
                            DotImageEditorToolRailComponent,
                            DotImageEditorCropOverlayComponent,
                            DotImageEditorFocalOverlayComponent
                        ]
                    },
                    add: {
                        imports: [
                            MockComponent(DotImageEditorAddressBarComponent),
                            MockComponent(DotImageEditorToolRailComponent),
                            MockComponent(DotImageEditorCropOverlayComponent),
                            MockComponent(DotImageEditorFocalOverlayComponent)
                        ]
                    }
                }
            ]
        ]
    });

    beforeEach(() => {
        previewUrl.set(PREVIEW_URL);
        previewStatus.set('idle');
        zoom.set({ level: 100, fitToScreen: true });
        activeTool.set('move');
        (HTMLImageElement.prototype.decode as jest.Mock).mockResolvedValue(undefined);

        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the canvas stage and child components', () => {
        expect(spectator.query(byTestId('image-editor-canvas'))).toExist();
        expect(spectator.query('dot-image-editor-address-bar')).toExist();
        expect(spectator.query('dot-image-editor-tool-rail')).toExist();
        expect(spectator.query('dot-image-editor-crop-overlay')).toExist();
        expect(spectator.query('dot-image-editor-focal-overlay')).toExist();
    });

    it('should show the skeleton and no spinner when idle', () => {
        expect(spectator.query(byTestId('image-editor-skeleton'))).toExist();
        expect(spectator.query(byTestId('image-editor-loading'))).not.toExist();
    });

    it('should keep the displayed image visible while loading (crossfade invariant)', () => {
        // Seed a displayed frame, then begin loading the next preview.
        previewUrl.set(PREVIEW_URL);
        spectator.detectChanges();
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        previewStatus.set('loading');
        spectator.detectChanges();

        expect(spectator.query(byTestId('image-editor-loading'))).toExist();
        expect(spectator.query(byTestId('image-editor-display-img'))).toExist();
    });

    it('should promote the pending image and dispatch previewLoaded on load', async () => {
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();

        expect(dispatchedEvent('previewLoaded')).toBeDefined();
        // Once promoted, the displayed URL matches the preview so no pending layer remains.
        spectator.detectChanges();
        expect(spectator.query(byTestId('image-editor-pending-img'))).not.toExist();
    });

    it('should promote the URL that loaded, not the live store URL, under rapid edits', async () => {
        // The store has already advanced to a newer preview by the time the
        // earlier pending image fires its load event.
        previewUrl.set(NEXT_PREVIEW_URL);
        const img = document.createElement('img');
        spectator.component['onPendingLoaded'](PREVIEW_URL, { target: img } as unknown as Event);
        await flushDecode();
        spectator.detectChanges();

        // Layer A promotes the URL that actually loaded.
        const displayed = spectator.query<HTMLImageElement>(byTestId('image-editor-display-img'));
        expect(displayed?.getAttribute('src')).toBe(PREVIEW_URL);

        // Layer A stays visible (crossfade invariant holds).
        expect(displayed?.hasAttribute('hidden')).toBe(false);

        // Layer B remains mounted for the newer URL the store advanced to.
        const pending = spectator.query<HTMLImageElement>(byTestId('image-editor-pending-img'));
        expect(pending?.getAttribute('src')).toBe(NEXT_PREVIEW_URL);

        expect(dispatchedEvent('previewLoaded')).toBeDefined();
    });

    it('should report a failed pending load to the store (which owns the retry policy)', () => {
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'error');

        expect(dispatchedEvent('previewErrored')).toBeDefined();
    });

    it('should report previewErrored when the loaded image fails to decode', async () => {
        (HTMLImageElement.prototype.decode as jest.Mock).mockRejectedValueOnce(
            new Error('decode failed')
        );

        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();

        expect(dispatchedEvent('previewErrored')).toBeDefined();
        expect(dispatchedEvent('previewLoaded')).toBeUndefined();
    });

    it('should render the pending image only when the preview URL differs from the displayed one', async () => {
        // No displayed frame yet, so the current preview is pending.
        expect(spectator.query(byTestId('image-editor-pending-img'))).toExist();

        // Promote the current preview, then advance the store to a new URL.
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();
        previewUrl.set(NEXT_PREVIEW_URL);
        spectator.detectChanges();

        const pending = spectator.query<HTMLImageElement>(byTestId('image-editor-pending-img'));
        expect(pending?.getAttribute('src')).toBe(NEXT_PREVIEW_URL);
    });

    it('should show the error overlay and retry button when errored', () => {
        previewStatus.set('error');
        spectator.detectChanges();

        expect(spectator.query(byTestId('image-editor-error'))).toExist();
        expect(spectator.query(byTestId('image-editor-retry-btn'))).toExist();
    });

    it('should dispatch retryRequested when retry is clicked', () => {
        previewStatus.set('error');
        spectator.detectChanges();

        const retryBtn = spectator.query(byTestId('image-editor-retry-btn'));
        spectator.click(retryBtn!.querySelector('button')!);

        expect(dispatchedEvent('retryRequested')).toBeDefined();
    });

    describe('footer band', () => {
        it('should render no action buttons when the move tool is active', () => {
            activeTool.set('move');
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-canvas-footer'))).toExist();
            expect(spectator.query(byTestId('image-editor-crop-apply-btn'))).not.toExist();
            expect(spectator.query(byTestId('image-editor-crop-cancel-btn'))).not.toExist();
            expect(spectator.query(byTestId('image-editor-focal-crop-btn'))).not.toExist();
        });

        it('should invoke the crop overlay apply/cancel from the footer when cropping', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            const cropOverlay = spectator.query(DotImageEditorCropOverlayComponent)!;
            const applySpy = jest.spyOn(cropOverlay, 'applyCrop');
            const cancelSpy = jest.spyOn(cropOverlay, 'cancelCrop');

            const applyBtn = spectator.query(byTestId('image-editor-crop-apply-btn'));
            spectator.click(applyBtn!.querySelector('button')!);
            expect(applySpy).toHaveBeenCalled();

            const cancelBtn = spectator.query(byTestId('image-editor-crop-cancel-btn'));
            spectator.click(cancelBtn!.querySelector('button')!);
            expect(cancelSpy).toHaveBeenCalled();
        });

        it('should crop to the selected aspect from the focal bar', () => {
            activeTool.set('focal');
            spectator.detectChanges();

            // Select 16:9, then crop.
            spectator.click(spectator.query(byTestId('image-editor-aspect-wide'))!);
            const cropBtn = spectator.query(byTestId('image-editor-focal-crop-btn'));
            spectator.click(cropBtn!.querySelector('button')!);

            const event = dispatchedEvent('aspectCropApplied');
            expect(event).toBeDefined();
            expect(event!.payload).toEqual({ aspect: 16 / 9, label: '16:9' });
        });
    });

    /** Finds the first dispatched event whose type matches the given suffix. */
    function dispatchedEvent(typeSuffix: string): { type: string; payload?: unknown } | undefined {
        const call = (dispatcher.dispatch as jest.Mock).mock.calls.find(([dispatched]) =>
            dispatched.type.includes(typeSuffix)
        );

        return call?.[0];
    }
});
