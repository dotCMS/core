import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsExtraPackagesComponent } from './dot-plugins-extra-packages.component';

describe('DotPluginsExtraPackagesComponent', () => {
    let spectator: Spectator<DotPluginsExtraPackagesComponent>;
    let component: DotPluginsExtraPackagesComponent;
    let osgiService: DotOsgiService;
    let dialogRef: DynamicDialogRef;
    let httpErrorManager: DotHttpErrorManagerService;

    const createComponent = createComponentFactory({
        component: DotPluginsExtraPackagesComponent,
        providers: [
            mockProvider(DotOsgiService, {
                getExtraPackages: jest.fn().mockReturnValue(of({ entity: 'pkg1\npkg2' })),
                updateExtraPackages: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DynamicDialogRef, { close: jest.fn() })
        ],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createComponent();
        component = spectator.component;
        osgiService = spectator.inject(DotOsgiService);
        dialogRef = spectator.inject(DynamicDialogRef);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
    });

    describe('ngOnInit', () => {
        it('should load and populate extra packages on init', () => {
            expect(osgiService.getExtraPackages).toHaveBeenCalled();
            expect(component.extraPackages()).toBe('pkg1\npkg2');
        });

        it('should default to empty string when entity is null', () => {
            jest.spyOn(osgiService, 'getExtraPackages').mockReturnValue(
                of({ entity: null as unknown as string })
            );
            component.ngOnInit();
            expect(component.extraPackages()).toBe('');
        });

        it('should handle load error', () => {
            const error = new Error('Load failed');
            jest.spyOn(osgiService, 'getExtraPackages').mockReturnValue(throwError(error));
            component.ngOnInit();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
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
            jest.spyOn(osgiService, 'updateExtraPackages').mockReturnValue(throwError(error));
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
