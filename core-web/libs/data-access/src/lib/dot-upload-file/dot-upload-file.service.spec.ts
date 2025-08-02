import { createHttpFactory, mockProvider, SpectatorHttp, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotUploadFileService } from './dot-upload-file.service';

import { DotWorkflowActionsFireService } from '../dot-workflow-actions-fire/dot-workflow-actions-fire.service';

describe('DotUploadFileService', () => {
    let spectator: SpectatorHttp<DotUploadFileService>;
    let dotWorkflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;

    const createHttp = createHttpFactory({
        service: DotUploadFileService,
        providers: [DotUploadFileService, mockProvider(DotWorkflowActionsFireService)]
    });

    beforeEach(() => {
        spectator = createHttp();

        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('uploadDotAsset', () => {
        it('should upload a file as a dotAsset', () => {
            dotWorkflowActionsFireService.newContentlet.mockReturnValueOnce(
                of({ entity: { identifier: 'test' } })
            );

            const file = new File([''], 'test.png', {
                type: 'image/png'
            });

            spectator.service.uploadDotAsset(file).subscribe();

            expect(dotWorkflowActionsFireService.newContentlet).toHaveBeenCalled();
        });
    });
});
