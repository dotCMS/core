import { TestBed } from '@angular/core/testing';

import { DotImageService } from './dot-image.service';

describe('DotAssetService', () => {
    let service: DotImageService;

    beforeEach(() => {
        TestBed.configureTestingModule({ teardown: { destroyAfterEach: false } });
        service = TestBed.inject(DotImageService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
