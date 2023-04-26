import { TestBed } from '@angular/core/testing';

import { DotUploadFileService } from './dot-upload-file.service';

describe('DotUploadFileService', () => {
    let service: DotUploadFileService;

    beforeEach(() => {
        TestBed.configureTestingModule({ teardown: { destroyAfterEach: false } });
        service = TestBed.inject(DotUploadFileService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
