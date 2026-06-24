import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotImageEditorAddressBarComponent } from './dot-image-editor-address-bar.component';

import { ActiveTool } from '../../models/image-editor.models';
import { imageEditorHistoryEvents, imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

const PREVIEW_URL = '/contentAsset/image/inode-1/fileAsset?byInode=true';

const messageServiceMock = new MockDotMessageService({
    'edit.content.image-editor.address.copy.success': 'Copied',
    'edit.content.image-editor.address.copy.error': 'Could not copy',
    'edit.content.image-editor.tool.move': 'Move',
    'edit.content.image-editor.tool.crop': 'Crop'
});

describe('DotImageEditorAddressBarComponent', () => {
    let spectator: Spectator<DotImageEditorAddressBarComponent>;
    let dispatcher: Dispatcher;
    let writeText: jest.Mock;

    const previewUrl = signal(PREVIEW_URL);
    const zoom = signal({ level: 100, fitToScreen: true });
    const canUndo = signal(true);
    const canRedo = signal(true);
    const activeTool = signal<ActiveTool>('move');

    const createComponent = createComponentFactory({
        component: DotImageEditorAddressBarComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        componentProviders: [
            Dispatcher,
            mockProvider(MessageService),
            mockProvider(ImageEditorStore, {
                previewUrl,
                zoom,
                canUndo,
                canRedo,
                activeTool
            })
        ]
    });

    /** Resolves the inner native <button> of a PrimeNG p-button addressed by testid. */
    const button = (testId: string) => spectator.query(byTestId(testId))!.querySelector('button')!;

    beforeEach(() => {
        previewUrl.set(PREVIEW_URL);
        zoom.set({ level: 100, fitToScreen: true });
        canUndo.set(true);
        canRedo.set(true);
        activeTool.set('move');

        writeText = jest.fn().mockResolvedValue(undefined);
        Object.defineProperty(navigator, 'clipboard', {
            value: { writeText },
            configurable: true
        });

        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the preview URL in the address field', () => {
        expect(spectator.query(byTestId('image-editor-address-field'))).toHaveText(PREVIEW_URL);
    });

    it('should render the zoom level on the fit control from the zoomLevel input', () => {
        spectator.setInput('zoomLevel', 125);

        expect(spectator.query(byTestId('image-editor-fit-btn'))).toHaveText('125%');
    });

    it('should copy the full (absolute) preview URL to the clipboard when the copy button is clicked', () => {
        spectator.click(button('image-editor-copy-url-btn'));

        expect(writeText).toHaveBeenCalledWith(document.location.origin + PREVIEW_URL);
    });

    it('should dispatch undoRequested when undo is clicked', () => {
        spectator.click(button('image-editor-undo-btn'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(imageEditorHistoryEvents.undoRequested(), {
            scope: 'self'
        });
    });

    it('should dispatch redoRequested when redo is clicked', () => {
        spectator.click(button('image-editor-redo-btn'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(imageEditorHistoryEvents.redoRequested(), {
            scope: 'self'
        });
    });

    it('should disable undo when there is nothing to undo', () => {
        canUndo.set(false);
        spectator.detectChanges();

        expect(button('image-editor-undo-btn')).toBeDisabled();
    });

    it('should emit zoomIn, zoomOut and fit from the zoom controls', () => {
        const zoomInSpy = jest.fn();
        const zoomOutSpy = jest.fn();
        const fitSpy = jest.fn();
        spectator.output('zoomIn').subscribe(zoomInSpy);
        spectator.output('zoomOut').subscribe(zoomOutSpy);
        spectator.output('fit').subscribe(fitSpy);

        spectator.click(button('image-editor-zoom-in-btn'));
        spectator.click(button('image-editor-zoom-out-btn'));
        spectator.click(byTestId('image-editor-fit-btn'));

        expect(zoomInSpy).toHaveBeenCalledTimes(1);
        expect(zoomOutSpy).toHaveBeenCalledTimes(1);
        expect(fitSpy).toHaveBeenCalledTimes(1);
    });

    describe('canvas tools', () => {
        it('should render the move and crop tool toggles with their testids and aria-labels', () => {
            const move = spectator.query(byTestId('image-editor-tool-move'));
            const crop = spectator.query(byTestId('image-editor-tool-crop'));

            expect(move).toBeTruthy();
            expect(crop).toBeTruthy();
            expect(move).toHaveAttribute('aria-label', 'Move');
            expect(crop).toHaveAttribute('aria-label', 'Crop');
        });

        it('should dispatch toolSelected with the crop tool when crop is clicked', () => {
            spectator.click(button('image-editor-tool-crop'));

            expect(dispatcher.dispatch).toHaveBeenCalledWith(
                imageEditorToolEvents.toolSelected('crop'),
                { scope: 'self' }
            );
        });

        it('should dispatch toolSelected with the move tool when move is clicked', () => {
            spectator.click(button('image-editor-tool-move'));

            expect(dispatcher.dispatch).toHaveBeenCalledWith(
                imageEditorToolEvents.toolSelected('move'),
                { scope: 'self' }
            );
        });

        it('should reflect the store active tool with aria-pressed and the active styleClass', () => {
            activeTool.set('crop');
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-tool-crop'))).toHaveAttribute(
                'aria-pressed',
                'true'
            );
            expect(spectator.query(byTestId('image-editor-tool-move'))).toHaveAttribute(
                'aria-pressed',
                'false'
            );
            expect(button('image-editor-tool-crop')).toHaveClass('address-bar__tool--active');
            expect(button('image-editor-tool-move')).not.toHaveClass('address-bar__tool--active');
        });
    });

    it('should expose the expected testids', () => {
        expect(spectator.query(byTestId('image-editor-address-field'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-copy-url-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-tool-move'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-tool-crop'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-zoom-out-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-zoom-in-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-fit-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-undo-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-redo-btn'))).toBeTruthy();
    });
});
