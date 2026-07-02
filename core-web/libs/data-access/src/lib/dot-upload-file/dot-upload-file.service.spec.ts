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

        it('should upload a file as a dotAsset with extra data', () => {
            dotWorkflowActionsFireService.newContentlet.mockReturnValueOnce(
                of({ entity: { identifier: 'test' } })
            );

            const file = new File([''], 'test.png', {
                type: 'image/png'
            });

            spectator.service.uploadDotAsset(file, { title: 'test' }).subscribe();

            expect(dotWorkflowActionsFireService.newContentlet).toHaveBeenCalled();
        });

        it('should default to the dotAsset content type', () => {
            dotWorkflowActionsFireService.newContentlet.mockReturnValueOnce(
                of({ entity: { identifier: 'test' } })
            );

            const file = new File([''], 'test.png', { type: 'image/png' });

            spectator.service.uploadDotAsset(file).subscribe();

            expect(dotWorkflowActionsFireService.newContentlet).toHaveBeenCalledWith(
                'dotAsset',
                expect.anything(),
                expect.anything()
            );
        });
    });

    describe('uploadFileByBaseType', () => {
        it('should upload a file resolving the content type from the given base type', () => {
            dotWorkflowActionsFireService.newContentletByBaseType.mockReturnValueOnce(
                of({ entity: { identifier: 'test' } })
            );

            const file = new File([''], 'test.png', { type: 'image/png' });

            spectator.service.uploadFileByBaseType(file, 'FILEASSET').subscribe();

            expect(dotWorkflowActionsFireService.newContentletByBaseType).toHaveBeenCalledWith(
                'FILEASSET',
                expect.anything(),
                expect.anything()
            );
        });

        it('should pass the base type and extra data through to the fire service', () => {
            dotWorkflowActionsFireService.newContentletByBaseType.mockReturnValueOnce(
                of({ entity: { identifier: 'test' } })
            );

            const file = new File([''], 'test.png', { type: 'image/png' });

            spectator.service
                .uploadFileByBaseType(file, 'DOTASSET', { hostFolder: '123' })
                .subscribe();

            expect(dotWorkflowActionsFireService.newContentletByBaseType).toHaveBeenCalledWith(
                'DOTASSET',
                expect.objectContaining({ hostFolder: '123' }),
                expect.anything()
            );
        });

        it('should not pass a contentType to the fire service', () => {
            dotWorkflowActionsFireService.newContentletByBaseType.mockReturnValueOnce(
                of({ entity: { identifier: 'test' } })
            );

            const file = new File([''], 'test.png', { type: 'image/png' });

            spectator.service.uploadFileByBaseType(file, 'FILEASSET').subscribe();

            expect(dotWorkflowActionsFireService.newContentlet).not.toHaveBeenCalled();
        });
    });
});
