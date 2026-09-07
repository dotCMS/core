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
// It also doesn't expose `MarkerSeverity`, which #hasErrorSeverityMarker relies on to
// tell a real syntax error apart from an informational hint/warning marker.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = {
    ...monacoMock,
    languages: { ...monacoMock.languages, getLanguages: () => [] },
    MarkerSeverity: { Hint: 1, Info: 2, Warning: 4, Error: 8 }
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

    describe('Content validation (Monaco markers)', () => {
        // Builds a single Monaco marker of the given severity. Only `severity`/`message`
        // matter to the gate; the position fields just satisfy the marker shape.
        const marker = (severity: number, message = 'diagnostic') => ({
            severity,
            message,
            startLineNumber: 1,
            startColumn: 1,
            endLineNumber: 1,
            endColumn: 1,
            owner: 'javascript',
            resource: null
        });

        // Drives the editor to a given set of markers AND mirrors what ngx-monaco-editor's
        // own Validator would then set on the control (any marker -> a single `monaco` error).
        // The real TS language service that produces these markers can't run in jsdom, so we
        // inject them; this exercises our gate, not Monaco's marker generation.
        const setMarkers = (markers: ReturnType<typeof marker>[]) => {
            jest.spyOn(monaco.editor, 'getModelMarkers').mockReturnValue(markers);
            spectator.component.contentField.setErrors(
                markers.length ? { monaco: { value: markers.map((m) => m.message) } } : null
            );
        };

        const spyUpload = () =>
            jest.spyOn(spectator.component.store, 'uploadFile').mockImplementation(() => undefined);

        beforeEach(() => {
            spectator.component.ngOnInit();
            spectator.component.form.controls.name.setValue('script.js');

            const editor = monaco.editor.create();
            spectator.component.onEditorInit(
                editor as unknown as monaco.editor.IStandaloneCodeEditor
            );
        });

        it('should save when the content has no markers and the name is valid', () => {
            setMarkers([]);
            const uploadFileSpy = spyUpload();

            spectator.component.onSubmit();

            expect(uploadFileSpy).toHaveBeenCalled();
            expect(spectator.component.$hasSyntaxError()).toBe(false);
        });

        it('should save when the content only has Hint-severity markers (e.g. unused-variable diagnostics)', () => {
            setMarkers(
                [monaco.MarkerSeverity.Hint, monaco.MarkerSeverity.Hint].map((s) =>
                    marker(s, "'foo' is declared but its value is never read.")
                )
            );
            const uploadFileSpy = spyUpload();

            spectator.component.onSubmit();

            expect(uploadFileSpy).toHaveBeenCalled();
            expect(spectator.component.$hasSyntaxError()).toBe(false);
        });

        it('should save when the content only has Warning-severity markers', () => {
            setMarkers([marker(monaco.MarkerSeverity.Warning, 'Unreachable code detected.')]);
            const uploadFileSpy = spyUpload();

            spectator.component.onSubmit();

            expect(uploadFileSpy).toHaveBeenCalled();
            expect(spectator.component.$hasSyntaxError()).toBe(false);
        });

        it('should block saving and flag $hasSyntaxError when the content has an Error-severity marker', () => {
            setMarkers([marker(monaco.MarkerSeverity.Error, "'}' expected.")]);
            const uploadFileSpy = spyUpload();

            spectator.component.onSubmit();

            expect(uploadFileSpy).not.toHaveBeenCalled();
            expect(spectator.component.$hasSyntaxError()).toBe(true);
        });

        it('should block saving when markers mix hints/warnings with at least one Error', () => {
            setMarkers([
                marker(
                    monaco.MarkerSeverity.Hint,
                    "'foo' is declared but its value is never read."
                ),
                marker(monaco.MarkerSeverity.Warning, 'Unreachable code detected.'),
                marker(monaco.MarkerSeverity.Error, "'}' expected.")
            ]);
            const uploadFileSpy = spyUpload();

            spectator.component.onSubmit();

            expect(uploadFileSpy).not.toHaveBeenCalled();
            expect(spectator.component.$hasSyntaxError()).toBe(true);
        });

        it('should clear $hasSyntaxError and allow saving once the error markers are resolved', () => {
            // Start with a real syntax error.
            setMarkers([marker(monaco.MarkerSeverity.Error, "'}' expected.")]);
            expect(spectator.component.$hasSyntaxError()).toBe(true);

            // User fixes it: markers clear and ngx-monaco-editor drops the `monaco` error.
            setMarkers([]);
            expect(spectator.component.$hasSyntaxError()).toBe(false);

            const uploadFileSpy = spyUpload();
            spectator.component.onSubmit();

            expect(uploadFileSpy).toHaveBeenCalled();
        });

        it('should still block saving when the name field is invalid, regardless of content markers', () => {
            spectator.component.form.controls.name.setValue('nodotextension');
            setMarkers([]);
            const uploadFileSpy = spyUpload();

            spectator.component.onSubmit();

            expect(uploadFileSpy).not.toHaveBeenCalled();
        });
    });
});
