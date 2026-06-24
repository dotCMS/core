import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotImageEditorToolRailComponent } from './dot-image-editor-tool-rail.component';

import { ActiveTool } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

const messageServiceMock = new MockDotMessageService({
    'edit.content.image-editor.tool.move': 'Move',
    'edit.content.image-editor.tool.crop': 'Crop'
});

describe('DotImageEditorToolRailComponent', () => {
    let spectator: Spectator<DotImageEditorToolRailComponent>;
    let dispatcher: Dispatcher;

    const activeTool = signal<ActiveTool>('move');

    const createComponent = createComponentFactory({
        component: DotImageEditorToolRailComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        componentProviders: [
            Dispatcher,
            mockProvider(ImageEditorStore, {
                activeTool
            })
        ]
    });

    beforeEach(() => {
        activeTool.set('move');
        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the two tools with testids and aria-labels', () => {
        const move = spectator.query(byTestId('image-editor-tool-move'));
        const crop = spectator.query(byTestId('image-editor-tool-crop'));

        expect(move).toBeTruthy();
        expect(crop).toBeTruthy();

        expect(move).toHaveAttribute('aria-label', 'Move');
        expect(crop).toHaveAttribute('aria-label', 'Crop');
    });

    it('should expose a vertical toolbar role on the host', () => {
        expect(spectator.element).toHaveAttribute('role', 'toolbar');
        expect(spectator.element).toHaveAttribute('aria-orientation', 'vertical');
    });

    it('should dispatch toolSelected with the crop tool when crop is clicked', () => {
        spectator.click(byTestId('image-editor-tool-crop'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            imageEditorToolEvents.toolSelected('crop'),
            { scope: 'self' }
        );
    });

    it('should mark the active tool with aria-pressed true and others false', () => {
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
    });
});
