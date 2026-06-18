import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotImageEditorAddressBarComponent } from './dot-image-editor-address-bar.component';

import { imageEditorHistoryEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

const PREVIEW_URL = '/contentAsset/image/inode-1/fileAsset?byInode=true';

const messageServiceMock = new MockDotMessageService({
    'edit.content.image-editor.address.copy.success': 'Copied',
    'edit.content.image-editor.address.copy.error': 'Could not copy'
});

describe('DotImageEditorAddressBarComponent', () => {
    let spectator: Spectator<DotImageEditorAddressBarComponent>;
    let dispatcher: Dispatcher;
    let writeText: jest.Mock;

    const previewUrl = signal(PREVIEW_URL);
    const zoom = signal({ level: 100, fitToScreen: true });
    const canUndo = signal(true);
    const canRedo = signal(true);

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
                canRedo
            })
        ]
    });

    beforeEach(() => {
        previewUrl.set(PREVIEW_URL);
        zoom.set({ level: 100, fitToScreen: true });
        canUndo.set(true);
        canRedo.set(true);

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
        const field = spectator.query<HTMLInputElement>(byTestId('image-editor-address-field'));

        expect(field?.value).toBe(PREVIEW_URL);
    });

    it('should render the zoom level from the zoomLevel input', () => {
        spectator.setInput('zoomLevel', 125);

        expect(spectator.query(byTestId('image-editor-zoom-value'))).toHaveText('125%');
    });

    it('should copy the preview URL to the clipboard when the copy button is clicked', () => {
        spectator.click(byTestId('image-editor-copy-url-btn'));

        expect(writeText).toHaveBeenCalledWith(PREVIEW_URL);
    });

    it('should dispatch undoRequested when undo is clicked', () => {
        spectator.click(byTestId('image-editor-undo-btn'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(imageEditorHistoryEvents.undoRequested(), {
            scope: 'self'
        });
    });

    it('should dispatch redoRequested when redo is clicked', () => {
        spectator.click(byTestId('image-editor-redo-btn'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(imageEditorHistoryEvents.redoRequested(), {
            scope: 'self'
        });
    });

    it('should disable undo when there is nothing to undo', () => {
        canUndo.set(false);
        spectator.detectChanges();

        expect(spectator.query(byTestId('image-editor-undo-btn'))).toHaveAttribute('disabled');
    });

    it('should emit zoomIn, zoomOut and fit from the zoom controls', () => {
        const zoomInSpy = jest.fn();
        const zoomOutSpy = jest.fn();
        const fitSpy = jest.fn();
        spectator.output('zoomIn').subscribe(zoomInSpy);
        spectator.output('zoomOut').subscribe(zoomOutSpy);
        spectator.output('fit').subscribe(fitSpy);

        spectator.click(byTestId('image-editor-zoom-in-btn'));
        spectator.click(byTestId('image-editor-zoom-out-btn'));
        spectator.click(byTestId('image-editor-fit-btn'));

        expect(zoomInSpy).toHaveBeenCalledTimes(1);
        expect(zoomOutSpy).toHaveBeenCalledTimes(1);
        expect(fitSpy).toHaveBeenCalledTimes(1);
    });

    it('should expose the expected testids', () => {
        expect(spectator.query(byTestId('image-editor-address-field'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-copy-url-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-zoom-out-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-zoom-in-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-fit-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-undo-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-redo-btn'))).toBeTruthy();
    });
});
