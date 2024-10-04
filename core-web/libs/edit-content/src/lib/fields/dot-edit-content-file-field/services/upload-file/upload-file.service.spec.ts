import {
    createHttpFactory,
    mockProvider,
    SpectatorHttp,
    SpyObject,
    HttpMethod
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotUploadFileService } from '@dotcms/data-access';

import { DotFileFieldUploadService } from './upload-file.service';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { NEW_FILE_MOCK, NEW_FILE_EDITABLE_MOCK } from '../../../../utils/mocks';

describe('DotFileFieldUploadService', () => {
    let spectator: SpectatorHttp<DotFileFieldUploadService>;
    let dotUploadFileService: SpyObject<DotUploadFileService>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    const createHttp = createHttpFactory({
        service: DotFileFieldUploadService,
        providers: [mockProvider(DotUploadFileService), mockProvider(DotEditContentService)]
    });

    beforeEach(() => {
        spectator = createHttp();
        dotUploadFileService = spectator.inject(DotUploadFileService);
        dotEditContentService = spectator.inject(DotEditContentService);
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('uploadFile', () => {
        it('should upload a file without content', () => {
            dotUploadFileService.uploadDotAsset.mockReturnValue(of(NEW_FILE_MOCK.entity));

            const file = new File([''], 'test.png', {
                type: 'image/png'
            });

            spectator.service.uploadDotAsset(file).subscribe();

            expect(dotUploadFileService.uploadDotAsset).toHaveBeenCalled();
        });

        it('should upload a file with content', () => {
            dotUploadFileService.uploadDotAsset.mockReturnValue(of(NEW_FILE_EDITABLE_MOCK.entity));

            const file = new File(['my content'], 'docker-compose.yml', {
                type: 'text/plain'
            });

            spectator.service.uploadDotAsset(file).subscribe((fileContent) => {
                expect(fileContent.content).toEqual('my content');
            });

            const req = spectator.expectOne(
                NEW_FILE_EDITABLE_MOCK.entity.assetVersion,
                HttpMethod.GET
            );
            req.flush('my content');

            expect(dotUploadFileService.uploadDotAsset).toHaveBeenCalled();
        });
    });

    describe('getContentById', () => {
        it('should get a contentlet without content', () => {
            dotEditContentService.getContentById.mockReturnValue(of(NEW_FILE_MOCK.entity));

            spectator.service.getContentById(NEW_FILE_MOCK.entity.identifier).subscribe();

            expect(dotEditContentService.getContentById).toHaveBeenCalled();
        });

        it('should get a contentlet with content', () => {
            dotEditContentService.getContentById.mockReturnValue(of(NEW_FILE_EDITABLE_MOCK.entity));

            spectator.service
                .getContentById(NEW_FILE_EDITABLE_MOCK.entity.identifier)
                .subscribe((fileContent) => {
                    expect(fileContent.content).toEqual('my content');
                });

            const req = spectator.expectOne(
                NEW_FILE_EDITABLE_MOCK.entity.assetVersion,
                HttpMethod.GET
            );
            req.flush('my content');

            expect(dotEditContentService.getContentById).toHaveBeenCalled();
        });
    });
});
