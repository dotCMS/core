import { TestBed } from '@angular/core/testing';

import { DotMarketingConfigService, INITIAL_STATE } from './dot-marketing-config.service';

describe('DotEditorMarketingService', () => {
    let service: DotMarketingConfigService;

    beforeEach(() => {
        TestBed.configureTestingModule({ teardown: { destroyAfterEach: false } });
        service = TestBed.inject(DotMarketingConfigService);
    });

    test('should be created', () => {
        expect(service).toBeTruthy();
    });

    test('should has initial state', () => {
        expect(service.configObject).toEqual(INITIAL_STATE);
    });

    test('should set a config property', () => {
        const key = 'SHOW_VIDEO_THUMBNAIL';
        const value = false;

        jest.spyOn(service, 'setProperty');

        service.setProperty(key, value);

        expect(service.setProperty).toHaveBeenCalledWith(key, value);
        expect(service.getProperty(key)).toBe(value);
    });

    test('should return the config object', () => {
        expect(service.configObject).toBeDefined();
    });
});
