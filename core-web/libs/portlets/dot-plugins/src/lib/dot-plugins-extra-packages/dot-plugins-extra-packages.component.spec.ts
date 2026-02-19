import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsExtraPackagesComponent } from './dot-plugins-extra-packages.component';

describe('DotPluginsExtraPackagesComponent', () => {
    let spectator: Spectator<DotPluginsExtraPackagesComponent>;

    const createComponent = createComponentFactory({
        component: DotPluginsExtraPackagesComponent,
        providers: [
            mockProvider(DotOsgiService, {
                getExtraPackages: jest.fn().mockReturnValue(of({ entity: 'pkg1\npkg2' }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DynamicDialogRef, { close: jest.fn() })
        ],
        shallow: true
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should have textarea and save button', () => {
        expect(spectator.query('[data-testid="plugins-extra-packages-textarea"]')).toBeTruthy();
        expect(spectator.query('[data-testid="plugins-extra-packages-save-btn"]')).toBeTruthy();
    });
});
