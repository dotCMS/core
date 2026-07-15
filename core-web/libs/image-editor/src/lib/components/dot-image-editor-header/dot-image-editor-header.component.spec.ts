import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorHeaderComponent } from './dot-image-editor-header.component';

import { imageEditorViewEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

describe('DotImageEditorHeaderComponent', () => {
    let spectator: Spectator<DotImageEditorHeaderComponent>;
    let dispatcher: Dispatcher;

    const isFullscreen = signal(false);

    const createComponent = createComponentFactory({
        component: DotImageEditorHeaderComponent,
        imports: [ButtonModule, DotMessagePipe],
        componentProviders: [Dispatcher, mockProvider(ImageEditorStore, { isFullscreen })]
    });

    beforeEach(() => {
        isFullscreen.set(false);
        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the title', () => {
        const header = spectator.query(byTestId('image-editor-header'));

        expect(header).toBeTruthy();
        expect(header).toHaveText('edit.content.image-editor.title');
    });

    it('should expose the header, full-screen and close testids', () => {
        expect(spectator.query(byTestId('image-editor-header'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-fullscreen-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-close-btn'))).toBeTruthy();
    });

    it('should emit close when the close button is clicked', () => {
        const closeSpy = jest.spyOn(spectator.component.$close, 'emit');
        const button = spectator.query(byTestId('image-editor-close-btn'))?.querySelector('button');

        spectator.click(button as HTMLElement);

        expect(closeSpy).toHaveBeenCalledTimes(1);
    });

    it('should dispatch fullscreenToggled when the full-screen button is clicked', () => {
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
