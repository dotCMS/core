import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { Dialog } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { monacoMock } from '@dotcms/utils-testing';

import { DotFormFileEditorComponent } from './dot-form-file-editor.component';

import { DotFileFieldUploadService } from '../../services/upload-file/upload-file.service';

// monacoMock doesn't expose `getLanguages`, which getInfoByLang / the velocity
// registration call. Provide a no-op so the component's Monaco hooks don't throw.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = {
    ...monacoMock,
    languages: { ...monacoMock.languages, getLanguages: () => [] }
};

describe('DotFormFileEditorComponent', () => {
    let spectator: Spectator<DotFormFileEditorComponent>;
    let dialogContainer: HTMLElement;

    const createComponent = createComponentFactory({
        component: DotFormFileEditorComponent,
        imports: [DotMessagePipe],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, { get: jest.fn(() => 'msg') }),
            mockProvider(DotFileFieldUploadService),
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        header: 'Edit File',
                        uploadedFile: null,
                        allowFileNameEdit: true,
                        uploadType: 'temp',
                        acceptedFiles: []
                    }
                }
            },
            // The component resizes this host `.p-dialog` element on full-screen toggle.
            { provide: Dialog, useValue: { container: () => dialogContainer } }
        ],
        // No initial render: keeps the lazy `ngx-monaco-editor` (and its loader) out of
        // the way; the full-screen logic is signal/effect driven and needs no template.
        detectChanges: false
    });

    beforeEach(() => {
        // jsdom has no matchMedia; #prefersReducedMotion reads it during #applyFullscreen.
        window.matchMedia =
            window.matchMedia ??
            (jest.fn().mockReturnValue({ matches: false }) as unknown as typeof window.matchMedia);
        dialogContainer = document.createElement('div');
        spectator = createComponent();
    });

    describe('Full screen', () => {
        it('should start windowed', () => {
            expect(spectator.component.$isFullscreen()).toBe(false);
            expect(spectator.component.$fullscreenIcon()).toBe('open_in_full');
        });

        it('should flip state and icon on toggle', () => {
            spectator.component.toggleFullscreen();

            expect(spectator.component.$isFullscreen()).toBe(true);
            expect(spectator.component.$fullscreenIcon()).toBe('close_fullscreen');

            spectator.component.toggleFullscreen();

            expect(spectator.component.$isFullscreen()).toBe(false);
            expect(spectator.component.$fullscreenIcon()).toBe('open_in_full');
        });

        it('should expand the dialog to the viewport when entering full screen', () => {
            spectator.flushEffects(); // initial windowed run

            spectator.component.toggleFullscreen();
            spectator.flushEffects();

            expect(dialogContainer.style.width).toBe('100vw');
            expect(dialogContainer.style.height).toBe('100vh');
            expect(dialogContainer.style.maxWidth).toBe('100vw');
            expect(dialogContainer.style.maxHeight).toBe('100vh');
        });

        it('should restore the windowed size when leaving full screen', () => {
            // Seed the windowed size DialogService would have set inline.
            dialogContainer.style.width = '90%';
            dialogContainer.style.height = '90%';
            spectator.flushEffects();

            spectator.component.toggleFullscreen(); // enter
            spectator.flushEffects();
            spectator.component.toggleFullscreen(); // exit
            spectator.flushEffects();

            expect(dialogContainer.style.width).toBe('90%');
            expect(dialogContainer.style.height).toBe('90%');
        });

        it('should render the dialog title from the config header', () => {
            spectator.component.ngOnInit();

            expect(spectator.component.$header()).toBe('Edit File');
        });
    });
});
