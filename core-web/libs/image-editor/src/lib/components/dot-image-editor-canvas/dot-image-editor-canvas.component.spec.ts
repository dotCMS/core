import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';
import { MockComponent, MockInstance } from 'ng-mocks';
import { Observable, of, throwError } from 'rxjs';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorCanvasComponent } from './dot-image-editor-canvas.component';

import { PreviewStatus } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { ImageEditorStore } from '../../store/image-editor.store';
import { DotImageEditorAddressBarComponent } from '../dot-image-editor-address-bar/dot-image-editor-address-bar.component';
import { DotImageEditorCropOverlayComponent } from '../dot-image-editor-crop-overlay/dot-image-editor-crop-overlay.component';

const PREVIEW_URL = '/contentAsset/image/inode-1/fileAsset?byInode=true&r=1';
const NEXT_PREVIEW_URL = '/contentAsset/image/inode-1/fileAsset?byInode=true&r=2';

/** The object URL the mocked service returns for a given preview URL. */
const objectUrlFor = (url: string) => `blob:${url}`;

// The preview-load strategy, reset to "succeed" each test and overridden per test.
// Indirecting through this keeps the shared mock fn stable so a per-test override
// can never leak into the initial fetch of the following test's component.
let loadResult: (url: string) => Observable<string>;

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

// jsdom has no object-URL API; the canvas revokes object URLs it no longer shows.
URL.createObjectURL = jest.fn(
    (blob: Blob) => `blob:${(blob as unknown as { name?: string }).name ?? 'mock'}`
);
URL.revokeObjectURL = jest.fn();

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
    // The crop overlay is mocked with MockComponent, which stubs only its
    // inputs/outputs — not its public `naturalCropSize` computed nor its
    // `setNaturalCropSize` method, which the canvas footer reads/calls. Stub both
    // on the mock so the footer can wire to them: `naturalCropSize` is a signal the
    // test can drive, `setNaturalCropSize` a spy to assert the canvas forwards edits.
    MockInstance.scope();
    const overlayCropSize = signal({ width: 0, height: 0 });
    const setNaturalCropSize = jest.fn();

    let spectator: Spectator<DotImageEditorCanvasComponent>;
    let dispatcher: Dispatcher;
    let service: jest.Mocked<DotImageEditorService>;

    const previewUrl = signal(PREVIEW_URL);
    const previewStatus = signal<PreviewStatus>('idle');
    const zoom = signal({ level: 100, fitToScreen: true });
    const activeTool = signal<'move' | 'crop'>('move');
    const assetContext = signal({ naturalWidth: 800, naturalHeight: 600 });

    const createComponent = createComponentFactory({
        component: DotImageEditorCanvasComponent,
        providers: [
            provideNoopAnimations(),
            Dispatcher,
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            mockProvider(DotImageEditorService, {
                // Stable fn that delegates to the per-test strategy (see `loadResult`).
                loadPreviewImage: jest.fn((url: string) => loadResult(url))
            })
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
                            DotImageEditorCropOverlayComponent
                        ]
                    },
                    add: {
                        imports: [
                            MockComponent(DotImageEditorAddressBarComponent),
                            MockComponent(DotImageEditorCropOverlayComponent)
                        ]
                    }
                }
            ]
        ]
    });

    /**
     * Pumps change detection so the `toObservable(pendingUrl)` effect emits (the
     * queued preview is fetched, mocked) and a second pass mounts its pending
     * `<img>` layer. Two passes because the effect sets `pendingSrc` during the
     * first pass, after the template was already evaluated.
     */
    const settlePending = () => {
        spectator.detectChanges();
        spectator.detectChanges();
    };

    beforeEach(() => {
        // Expose the overlay's public size read/write on the mock for this test.
        overlayCropSize.set({ width: 0, height: 0 });
        setNaturalCropSize.mockClear();
        MockInstance(DotImageEditorCropOverlayComponent, 'naturalCropSize', overlayCropSize);
        MockInstance(DotImageEditorCropOverlayComponent, 'setNaturalCropSize', setNaturalCropSize);

        previewUrl.set(PREVIEW_URL);
        previewStatus.set('idle');
        zoom.set({ level: 100, fitToScreen: true });
        activeTool.set('move');
        (HTMLImageElement.prototype.decode as jest.Mock).mockResolvedValue(undefined);
        // Default strategy: every preview resolves to a complete object URL. Set
        // before createComponent so the component's initial fetch uses it.
        loadResult = (url: string) => of(objectUrlFor(url));

        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        service = spectator.inject(
            DotImageEditorService,
            true
        ) as jest.Mocked<DotImageEditorService>;
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the canvas stage and child components', () => {
        expect(spectator.query(byTestId('image-editor-canvas'))).toExist();
        expect(spectator.query('dot-image-editor-address-bar')).toExist();
        expect(spectator.query('dot-image-editor-crop-overlay')).toExist();
    });

    it('should no longer render the floating tool rail', () => {
        expect(spectator.query('dot-image-editor-tool-rail')).not.toExist();
    });

    it('should show the skeleton and no spinner when idle', () => {
        expect(spectator.query(byTestId('image-editor-skeleton'))).toExist();
        expect(spectator.query(byTestId('image-editor-loading'))).not.toExist();
    });

    it('should fetch the queued preview as a verified blob before rendering it', () => {
        settlePending();

        expect(service.loadPreviewImage).toHaveBeenCalledWith(PREVIEW_URL);
        const pending = spectator.query<HTMLImageElement>(byTestId('image-editor-pending-img'));
        expect(pending?.getAttribute('src')).toBe(objectUrlFor(PREVIEW_URL));
    });

    it('should keep the displayed image visible while loading (crossfade invariant)', async () => {
        // Promote a first frame, then begin loading the next preview.
        settlePending();
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();

        previewStatus.set('loading');
        previewUrl.set(NEXT_PREVIEW_URL);
        settlePending();

        expect(spectator.query(byTestId('image-editor-loading'))).toExist();
        expect(spectator.query(byTestId('image-editor-display-img'))).toExist();
        expect(
            spectator
                .query<HTMLImageElement>(byTestId('image-editor-display-img'))
                ?.hasAttribute('hidden')
        ).toBe(false);
    });

    it('should promote the pending image and dispatch previewLoaded on load', async () => {
        settlePending();
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();

        expect(dispatchedEvent('previewLoaded')).toBeDefined();
        // Once promoted, the displayed layer shows the verified blob and no pending layer remains.
        spectator.detectChanges();
        const displayed = spectator.query<HTMLImageElement>(byTestId('image-editor-display-img'));
        expect(displayed?.getAttribute('src')).toBe(objectUrlFor(PREVIEW_URL));
        expect(spectator.query(byTestId('image-editor-pending-img'))).not.toExist();
    });

    it('should abandon a superseded preview and promote only the latest under rapid edits', async () => {
        // Queue the first preview, then advance the store before it is promoted.
        settlePending();
        previewUrl.set(NEXT_PREVIEW_URL);
        settlePending();

        // The pending layer now targets the newest preview; the stale fetch was dropped.
        const pending = spectator.query<HTMLImageElement>(byTestId('image-editor-pending-img'));
        expect(pending?.getAttribute('src')).toBe(objectUrlFor(NEXT_PREVIEW_URL));

        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();
        spectator.detectChanges();

        const displayed = spectator.query<HTMLImageElement>(byTestId('image-editor-display-img'));
        expect(displayed?.getAttribute('src')).toBe(objectUrlFor(NEXT_PREVIEW_URL));
        expect(dispatchedEvent('previewLoaded')).toBeDefined();
    });

    it('should report previewErrored when the preview blob fails to load (incomplete response)', () => {
        // The service rejects a truncated / incomplete server response.
        loadResult = () => throwError(() => new Error('Incomplete or invalid image response'));
        previewUrl.set(NEXT_PREVIEW_URL);
        settlePending();

        expect(dispatchedEvent('previewErrored')).toBeDefined();
        // No pending layer is mounted for a failed fetch.
        expect(spectator.query(byTestId('image-editor-pending-img'))).not.toExist();
    });

    it('should report a failed pending image load to the store (which owns the retry policy)', () => {
        settlePending();
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'error');

        expect(dispatchedEvent('previewErrored')).toBeDefined();
    });

    it('should report previewErrored when the loaded image fails to decode', async () => {
        (HTMLImageElement.prototype.decode as jest.Mock).mockRejectedValueOnce(
            new Error('decode failed')
        );

        settlePending();
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();

        expect(dispatchedEvent('previewErrored')).toBeDefined();
        expect(dispatchedEvent('previewLoaded')).toBeUndefined();
    });

    it('should render the pending image only when the preview URL differs from the displayed one', async () => {
        // No displayed frame yet, so the current preview is pending.
        settlePending();
        expect(spectator.query(byTestId('image-editor-pending-img'))).toExist();

        // Promote the current preview: the pending layer is released.
        spectator.dispatchFakeEvent(byTestId('image-editor-pending-img'), 'load');
        await flushDecode();
        spectator.detectChanges();
        expect(spectator.query(byTestId('image-editor-pending-img'))).not.toExist();

        // Advancing the store re-mounts the pending layer for the new URL.
        previewUrl.set(NEXT_PREVIEW_URL);
        settlePending();
        const pending = spectator.query<HTMLImageElement>(byTestId('image-editor-pending-img'));
        expect(pending?.getAttribute('src')).toBe(objectUrlFor(NEXT_PREVIEW_URL));
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

    describe('tool actions bar', () => {
        it('should not render the action bar when the move tool is active', () => {
            activeTool.set('move');
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-canvas-footer'))).not.toExist();
            expect(spectator.query(byTestId('image-editor-crop-apply-btn'))).not.toExist();
            expect(spectator.query(byTestId('image-editor-crop-cancel-btn'))).not.toExist();
        });

        it('should render the action bar with crop actions when the crop tool is active', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-canvas-footer'))).toExist();
            expect(spectator.query(byTestId('image-editor-crop-apply-btn'))).toExist();
            expect(spectator.query(byTestId('image-editor-crop-cancel-btn'))).toExist();
        });

        it('should render the aspect presets including Free in the crop bar', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-aspect-free'))).toExist();
            expect(spectator.query(byTestId('image-editor-aspect-square'))).toExist();
            expect(spectator.query(byTestId('image-editor-aspect-standard'))).toExist();
            expect(spectator.query(byTestId('image-editor-aspect-wide'))).toExist();
        });

        it('should lock the crop aspect when a preset pill is clicked and clear it on Free', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            // The component owns cropAspect, which it binds to the crop overlay's
            // `aspect` input ([aspect]="cropAspect()").
            const cropAspect = () =>
                (
                    spectator.component as unknown as { cropAspect: () => number | null }
                ).cropAspect();

            spectator.click(spectator.query(byTestId('image-editor-aspect-wide'))!);
            spectator.detectChanges();

            // The selected pill is highlighted and the locked ratio is stored.
            const wide = spectator.query(byTestId('image-editor-aspect-wide'));
            expect(wide).toHaveClass('canvas__aspect--active');
            expect(cropAspect()).toBeCloseTo(16 / 9, 5);

            // Free clears the lock back to free-form (null) and highlights instead.
            spectator.click(spectator.query(byTestId('image-editor-aspect-free'))!);
            spectator.detectChanges();
            expect(cropAspect()).toBeNull();
            expect(spectator.query(byTestId('image-editor-aspect-free'))).toHaveClass(
                'canvas__aspect--active'
            );
            expect(wide).not.toHaveClass('canvas__aspect--active');
        });

        it('should invoke the crop overlay apply/cancel from the action bar when cropping', () => {
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

        it('should render the width/height size inputs when cropping', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-crop-width-input'))).toExist();
            expect(spectator.query(byTestId('image-editor-crop-height-input'))).toExist();
        });

        it('should disable the size inputs in Free mode and enable them under a preset', async () => {
            activeTool.set('crop');
            spectator.detectChanges();
            // NgModel applies disabled-state changes in a microtask, so flush it
            // before reading the inner input's disabled state.
            await Promise.resolve();
            spectator.detectChanges();

            // The inner PrimeNG <input> carries the disabled state. Free (null) is a
            // pure readout, so both fields are disabled.
            const widthInput = () =>
                spectator.query(byTestId('image-editor-crop-width-input'))!.querySelector('input')!;
            const heightInput = () =>
                spectator
                    .query(byTestId('image-editor-crop-height-input'))!
                    .querySelector('input')!;

            expect(widthInput().disabled).toBe(true);
            expect(heightInput().disabled).toBe(true);

            // Selecting a preset locks a ratio and makes the fields editable.
            spectator.click(spectator.query(byTestId('image-editor-aspect-square'))!);
            spectator.detectChanges();
            await Promise.resolve();
            spectator.detectChanges();

            expect(widthInput().disabled).toBe(false);
            expect(heightInput().disabled).toBe(false);
        });

        it('should drive the crop overlay setter when the width is edited under a preset', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            // Lock to 1:1 so the height follows the typed width.
            spectator.click(spectator.query(byTestId('image-editor-aspect-square'))!);
            spectator.detectChanges();

            // Editing the width to 320 px drives the overlay setter with the locked
            // ratio's matching height (320 / 1 = 320).
            (
                spectator.component as unknown as {
                    onCropWidthChange: (value: number | null) => void;
                }
            ).onCropWidthChange(320);

            expect(setNaturalCropSize).toHaveBeenCalledWith(320, 320);
        });

        it('should ignore size edits while in Free mode (pure readout)', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            // Free is the default; an edit must not reach the overlay setter.
            (
                spectator.component as unknown as {
                    onCropWidthChange: (value: number | null) => void;
                }
            ).onCropWidthChange(320);

            expect(setNaturalCropSize).not.toHaveBeenCalled();
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
