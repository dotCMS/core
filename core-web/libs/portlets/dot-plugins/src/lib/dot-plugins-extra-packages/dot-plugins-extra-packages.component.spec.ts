import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Observable, of } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsExtraPackagesComponent } from './dot-plugins-extra-packages.component';

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

    describe('ngOnInit', () => {
        it('should load and populate extra packages on init', () => {
            expect(osgiService.getExtraPackages).toHaveBeenCalled();
            expect(component.extraPackages()).toBe('pkg1\npkg2');
        });

        it('should default to empty string when entity is null', () => {
            osgiServiceMock.getExtraPackages.mockReturnValue(
                of({ entity: null as unknown as string })
            );
            component.ngOnInit();
            expect(component.extraPackages()).toBe('');
        });

        it('should handle load error', () => {
            const error = new Error('Load failed');
            osgiServiceMock.getExtraPackages.mockReturnValue(
                new Observable((subscriber) => subscriber.error(error))
            );
            component.ngOnInit();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
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
