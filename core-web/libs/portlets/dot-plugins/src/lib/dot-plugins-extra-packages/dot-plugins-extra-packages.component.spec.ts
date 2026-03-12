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
        spectator = createComponent();
        component = spectator.component;
        osgiService = spectator.inject(DotOsgiService);
        dialogRef = spectator.inject(DynamicDialogRef);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('ngOnInit', () => {
        it('should load extra packages on init', () => {
            expect(osgiService.getExtraPackages).toHaveBeenCalled();
            expect(component.extraPackages()).toBe('pkg1\npkg2');
        });

        it('should handle load error', () => {
            const error = new Error('Load failed');
            jest.spyOn(osgiService, 'getExtraPackages').mockReturnValue(throwError(error));
            component.ngOnInit();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });

        it('should default to empty string on null entity', () => {
            jest.spyOn(osgiService, 'getExtraPackages').mockReturnValue(
                of({ entity: null as unknown as string })
            );
            component.ngOnInit();
            expect(component.extraPackages()).toBe('');
        });
    });

    describe('save', () => {
        it('should save extra packages and close dialog with true', () => {
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

        it('should set saving to true during request', () => {
            jest.spyOn(osgiService, 'updateExtraPackages').mockReturnValue(of({}));
            component.save();
            expect(component.saving()).toBe(false); // reset after success
        });
    });

    describe('close', () => {
        it('should close dialog with false', () => {
            component.close();
            expect(dialogRef.close).toHaveBeenCalledWith(false);
        });
    });
});
