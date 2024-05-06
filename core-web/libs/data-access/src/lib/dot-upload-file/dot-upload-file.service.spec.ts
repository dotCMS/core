import { createServiceFactory, SpectatorService } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotUploadFileService } from './dot-upload-file.service';

import { DotUploadService } from '../dot-upload/dot-upload.service';

describe('DotUploadFileService', () => {
    let spectator: SpectatorService<DotUploadFileService>;
    let service: DotUploadFileService;

    const createService = createServiceFactory({
        service: DotUploadFileService,
        imports: [HttpClientTestingModule],
        providers: [DotUploadService]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
