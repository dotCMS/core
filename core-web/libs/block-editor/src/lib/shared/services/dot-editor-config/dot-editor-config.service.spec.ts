import { TestBed } from '@angular/core/testing';

import { DotEditorConfigService, INITIAL_STATE } from './dot-editor-config.service';

describe('DotEditorConfigService', () => {
    let service: DotEditorConfigService;

    beforeEach(() => {
        TestBed.configureTestingModule({ teardown: { destroyAfterEach: false } });
        service = TestBed.inject(DotEditorConfigService);
    });

    test('should be created', () => {
        expect(service).toBeTruthy();
    });

    test('should has initial state', () => {
        expect(service.getConfigObject()).toEqual(INITIAL_STATE);
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
        expect(service.getConfigObject()).toBeDefined();
    });
});
