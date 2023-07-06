import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';

import { DotDeviceSelectorSeoComponent } from './dot-device-selector-seo.component';

describe('DotDeviceSelectorSeoComponent', () => {
    let spectator: Spectator<DotDeviceSelectorSeoComponent>;
    let mockDotDevicesService: Partial<DotDevicesService>;
    let mockDotMessageService: Partial<DotMessageService>;

    const createComponent = createComponentFactory({
        component: DotDeviceSelectorSeoComponent,
        declarations: [],
        providers: [
            { provide: DotDevicesService, useValue: mockDotDevicesService },
            { provide: DotMessageService, useValue: mockDotMessageService }
        ]
    });

    beforeEach(() => {
        mockDotDevicesService = {
            get: jest.fn().mockReturnValue(of([]))
        };

        mockDotMessageService = {
            get: jest.fn().mockReturnValue('Message')
        };

        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
