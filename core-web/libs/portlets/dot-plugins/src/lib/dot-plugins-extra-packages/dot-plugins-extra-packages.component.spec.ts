import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { Observable, of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService, DotOsgiService } from '@dotcms/data-access';

import {
    DotPluginsExtraPackagesComponent,
    SEARCH_DEBOUNCE_MS
} from './dot-plugins-extra-packages.component';

describe('DotPluginsExtraPackagesComponent', () => {
    let spectator: Spectator<DotPluginsExtraPackagesComponent>;
    let component: DotPluginsExtraPackagesComponent;
    let osgiService: DotOsgiService;
    let dialogRef: DynamicDialogRef;
    let httpErrorManager: DotHttpErrorManagerService;
    let confirmationConfirmSpy: jest.SpyInstance;

    const osgiServiceMock = {
        getExtraPackages: jest.fn().mockReturnValue(of({ entity: 'pkg1\npkg2' })),
        updateExtraPackages: jest.fn().mockReturnValue(of({}))
    };

    const createComponent = createComponentFactory({
        component: DotPluginsExtraPackagesComponent,
        providers: [
            { provide: DotOsgiService, useValue: osgiServiceMock },
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DotMessageService, {
                get: jest.fn((key: string, ..._args: string[]) => key)
            }),
            mockProvider(DynamicDialogRef, { close: jest.fn() })
        ],
        shallow: false
    });

    function clickPButton(testId: string): void {
        const host = spectator.query(byTestId(testId));
        expect(host).toExist();
        const nativeButton = host.querySelector('button');
        expect(nativeButton).toBeTruthy();
        spectator.click(nativeButton!);
    }

    function nativeTextarea(): HTMLTextAreaElement | null {
        const host = spectator.query(byTestId('plugins-extra-packages-textarea'));
        if (!host) return null;
        return host instanceof HTMLTextAreaElement
            ? host
            : host.querySelector<HTMLTextAreaElement>('textarea');
    }

    beforeEach(() => {
        osgiServiceMock.getExtraPackages.mockReset();
        osgiServiceMock.getExtraPackages.mockReturnValue(of({ entity: 'pkg1\npkg2' }));
        osgiServiceMock.updateExtraPackages.mockReset();
        osgiServiceMock.updateExtraPackages.mockReturnValue(of({}));

        spectator = createComponent();
        component = spectator.component;
        osgiService = spectator.inject(DotOsgiService);
        dialogRef = spectator.inject(DynamicDialogRef);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
        const confirmationService = spectator.debugElement.injector.get(ConfirmationService);
        confirmationConfirmSpy = jest.spyOn(confirmationService, 'confirm');
        jest.mocked(dialogRef.close).mockClear();
        jest.mocked(httpErrorManager.handle).mockClear();
    });

    describe('initialization', () => {
        it('should load and populate extra packages on init', () => {
            expect(osgiService.getExtraPackages).toHaveBeenCalled();
            expect(component.extraPackages()).toBe('pkg1\npkg2');
        });

        it('should default to empty string when entity is null', () => {
            osgiServiceMock.getExtraPackages.mockReturnValue(
                of({ entity: null as unknown as string })
            );
            const s = createComponent();
            expect(s.component.extraPackages()).toBe('');
        });

        it('should handle load error', () => {
            const error = new Error('Load failed');
            osgiServiceMock.getExtraPackages.mockReturnValue(
                new Observable((subscriber) => subscriber.error(error))
            );
            const s = createComponent();
            expect(s.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        });
    });

    describe('template', () => {
        it('should render instructions copy from i18n key', () => {
            const instructions = spectator.query('p.m-0');
            expect(instructions).toExist();
            expect(instructions).toHaveText('plugins.extra-packages.instructions');
        });

        it('should bind textarea to loaded packages and expose aria-label key', () => {
            expect(component.extraPackages()).toBe('pkg1\npkg2');
            spectator.detectChanges();

            const textarea = nativeTextarea();
            expect(textarea).toBeTruthy();
            expect(textarea!.value).toBe('pkg1\npkg2');
            expect(textarea!.getAttribute('aria-label')).toBe('plugins.extra-packages.title');
        });

        it('should render cancel, reset, and save controls', () => {
            expect(spectator.query(byTestId('plugins-extra-packages-cancel-btn'))).toExist();
            expect(spectator.query(byTestId('plugins-extra-packages-reset-btn'))).toExist();
            expect(spectator.query(byTestId('plugins-extra-packages-save-btn'))).toExist();
        });

        it('should update extraPackages when the user edits the textarea', () => {
            const textarea = nativeTextarea();
            expect(textarea).toBeTruthy();
            spectator.typeInElement('edited\ncontent', textarea!);
            expect(component.extraPackages()).toBe('edited\ncontent');
        });

        it('should close with false when cancel is activated from the template', () => {
            clickPButton('plugins-extra-packages-cancel-btn');
            expect(osgiService.updateExtraPackages).not.toHaveBeenCalled();
            expect(dialogRef.close).toHaveBeenCalledWith(false);
        });

        it('should save and close with true when save is activated from the template', () => {
            const textarea = nativeTextarea();
            expect(textarea).toBeTruthy();
            spectator.typeInElement('from-ui', textarea!);
            clickPButton('plugins-extra-packages-save-btn');
            expect(osgiService.updateExtraPackages).toHaveBeenCalledWith('from-ui');
            expect(dialogRef.close).toHaveBeenCalledWith(true);
        });

        it('should open reset confirmation when reset is activated from the template', () => {
            clickPButton('plugins-extra-packages-reset-btn');
            expect(confirmationConfirmSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    header: 'plugins.extra-packages.reset',
                    message: 'plugins.extra-packages.reset.confirm.message',
                    acceptLabel: 'Ok',
                    rejectLabel: 'Cancel'
                })
            );
        });

        it('should render confirm dialog host for reset flow', () => {
            expect(spectator.query('p-confirmdialog')).toExist();
        });
    });

    describe('search', () => {
        const PACKAGES_TEXT = 'com.example.foo\ncom.example.bar\norg.example.baz\ncom.example.qux';

        function searchInput(): HTMLInputElement | null {
            const host = spectator.query(byTestId('plugins-extra-packages-search'));
            if (!host) return null;
            return host instanceof HTMLInputElement
                ? host
                : host.querySelector<HTMLInputElement>('input');
        }

        beforeEach(() => {
            component.extraPackages.set(PACKAGES_TEXT);
            spectator.detectChanges();
        });

        it('should keep focus on the search input after typing (debounced)', fakeAsync(() => {
            const input = searchInput()!;
            const textarea = nativeTextarea()!;
            expect(input).toBeTruthy();
            expect(textarea).toBeTruthy();

            input.focus();
            expect(document.activeElement).toBe(input);

            spectator.typeInElement('com', input);
            tick(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(component.matchCount()).toBe(3);
            expect(document.activeElement).toBe(input);
        }));

        it('should pre-select the first match on the textarea while typing without focusing it', fakeAsync(() => {
            const input = searchInput()!;
            const textarea = nativeTextarea()!;

            input.focus();
            spectator.typeInElement('com', input);
            tick(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(document.activeElement).toBe(input);
            const firstStart = component.matchPositions()[0];
            expect(textarea.selectionStart).toBe(firstStart);
            expect(textarea.selectionEnd).toBe(firstStart + 'com'.length);
        }));

        it('should move focus to the textarea on next-match navigation', fakeAsync(() => {
            const input = searchInput()!;
            const textarea = nativeTextarea()!;

            input.focus();
            spectator.typeInElement('com', input);
            tick(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            component.nextMatch();

            expect(document.activeElement).toBe(textarea);
            expect(component.currentMatchIndex()).toBe(1);
            const secondStart = component.matchPositions()[1];
            expect(textarea.selectionStart).toBe(secondStart);
            expect(textarea.selectionEnd).toBe(secondStart + 'com'.length);
        }));

        it('should move focus to the textarea on prev-match navigation', fakeAsync(() => {
            const input = searchInput()!;
            const textarea = nativeTextarea()!;

            input.focus();
            spectator.typeInElement('com', input);
            tick(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            component.prevMatch();

            expect(document.activeElement).toBe(textarea);
            expect(component.currentMatchIndex()).toBe(component.matchCount() - 1);
        }));

        it('should leave focus untouched when the query has no matches', fakeAsync(() => {
            const input = searchInput()!;

            input.focus();
            spectator.typeInElement('does-not-exist', input);
            tick(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(component.matchCount()).toBe(0);
            expect(document.activeElement).toBe(input);
        }));
    });

    describe('save', () => {
        it('should save the current packages and close the dialog with true', () => {
            component.extraPackages.set('pkg1\npkg2\npkg3');
            component.save();
            expect(osgiService.updateExtraPackages).toHaveBeenCalledWith('pkg1\npkg2\npkg3');
            expect(dialogRef.close).toHaveBeenCalledWith(true);
        });

        it('should handle save error and reset saving state', () => {
            const error = new Error('Save failed');
            osgiServiceMock.updateExtraPackages.mockReturnValue(
                new Observable((subscriber) => subscriber.error(error))
            );
            component.save();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(component.saving()).toBe(false);
        });
    });

    describe('close', () => {
        it('should close dialog with false without saving', () => {
            component.close();
            expect(osgiService.updateExtraPackages).not.toHaveBeenCalled();
            expect(dialogRef.close).toHaveBeenCalledWith(false);
        });
    });
});
