import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorFullscreenToggleComponent } from './dot-image-editor-fullscreen-toggle.component';

import { imageEditorViewEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

describe('DotImageEditorFullscreenToggleComponent', () => {
    let spectator: Spectator<DotImageEditorFullscreenToggleComponent>;
    let dispatcher: Dispatcher;

    const isFullscreen = signal(false);

    const createComponent = createComponentFactory({
        component: DotImageEditorFullscreenToggleComponent,
        imports: [ButtonModule, DotMessagePipe],
        componentProviders: [Dispatcher, mockProvider(ImageEditorStore, { isFullscreen })]
    });

    beforeEach(() => {
        isFullscreen.set(false);
        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should dispatch fullscreenToggled when clicked', () => {
        const button = spectator
            .query(byTestId('image-editor-fullscreen-btn'))
            ?.querySelector('button');

        spectator.click(button as HTMLElement);

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            imageEditorViewEvents.fullscreenToggled(),
            {
                scope: 'self'
            }
        );
    });

    it('should show the "enter full screen" icon while windowed', () => {
        const icon = spectator
            .query(byTestId('image-editor-fullscreen-btn'))
            ?.querySelector('.material-symbols-outlined');

        expect(icon?.textContent?.trim()).toBe('open_in_full');
    });

    it('should swap the full-screen icon to "exit full screen" while full-screen', () => {
        isFullscreen.set(true);
        spectator.detectChanges();

        const icon = spectator
            .query(byTestId('image-editor-fullscreen-btn'))
            ?.querySelector('.material-symbols-outlined');

        expect(icon?.textContent?.trim()).toBe('close_fullscreen');
    });
});
