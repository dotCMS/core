import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsUploadComponent } from './dot-plugins-upload.component';

describe('DotPluginsUploadComponent', () => {
    let spectator: Spectator<DotPluginsUploadComponent>;

    const createComponent = createComponentFactory({
        component: DotPluginsUploadComponent,
        providers: [
            mockProvider(DotOsgiService, {
                uploadBundles: jest.fn().mockReturnValue(of({ entity: {} }))
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

    it('should have file upload and submit button', () => {
        expect(spectator.query('[data-testid="plugins-upload-files"]')).toBeTruthy();
        expect(spectator.query('[data-testid="plugins-upload-submit-btn"]')).toBeTruthy();
    });
});
