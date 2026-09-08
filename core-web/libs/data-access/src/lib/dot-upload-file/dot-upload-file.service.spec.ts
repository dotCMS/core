import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { DotBulkUploadForm, DotBulkUploadSubmitResponse } from '@dotcms/dotcms-models';

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

    describe('uploadFilesByBaseType — a batch, job-backed', () => {
        const SUBMIT_URL = '/api/v1/assets/_bulkupload';

        const file = (name: string, bytes = 4) =>
            new File([new Uint8Array(bytes)], name, { type: 'image/png' });

        const form: DotBulkUploadForm = {
            baseType: 'DOTASSET',
            folderId: 'folder-1',
            totalSizeBytes: 8
        };

        const bodyOf = (request: { body: FormData }) => request.body;

        // `Blob.text()` does not exist in this jsdom, so the part is read the long way rather than
        // the assertion being weakened to just its declared type.
        const readPart = (blob: Blob): Promise<string> =>
            new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(String(reader.result));
                reader.onerror = () => reject(reader.error);
                reader.readAsText(blob);
            });

        const formPartOf = async (sent: FormData) =>
            JSON.parse(await readPart(sent.get('form') as Blob));

        it('should post to the bulk upload endpoint', () => {
            spectator.service.uploadFilesByBaseType([file('a.png')], form).subscribe();

            spectator.expectOne(SUBMIT_URL, HttpMethod.POST);
        });

        it('should send one files part per file, in the order they were chosen', () => {
            // The order is the contract's: it is preserved in the outcome, which is how a per-file
            // result is matched back to the file the author picked.
            const chosen = [file('first.png'), file('second.png'), file('third.png')];

            spectator.service.uploadFilesByBaseType(chosen, form).subscribe();

            const sent = bodyOf(spectator.expectOne(SUBMIT_URL, HttpMethod.POST).request);

            expect((sent.getAll('files') as File[]).map((part) => part.name)).toEqual([
                'first.png',
                'second.png',
                'third.png'
            ]);
        });

        it('should send every chosen file, not just the first', () => {
            // This is the reported defect, at the layer that would recreate it.
            spectator.service
                .uploadFilesByBaseType([file('a.png'), file('b.png'), file('c.png')], form)
                .subscribe();

            const sent = bodyOf(spectator.expectOne(SUBMIT_URL, HttpMethod.POST).request);

            expect(sent.getAll('files')).toHaveLength(3);
        });

        it('should send the batch parameters as one json form part', async () => {
            spectator.service.uploadFilesByBaseType([file('a.png')], form).subscribe();

            const sent = bodyOf(spectator.expectOne(SUBMIT_URL, HttpMethod.POST).request);
            const part = sent.get('form') as Blob;

            // Declared as json, not as text: the server reads this part with Jackson.
            expect(part.type).toBe('application/json');
            expect(await formPartOf(sent)).toEqual(form);
        });

        it('should declare the batch total so an oversized batch is refused before it uploads', async () => {
            const chosen = [file('a.png', 10), file('b.png', 15)];

            spectator.service
                .uploadFilesByBaseType(chosen, { baseType: 'DOTASSET', folderId: 'folder-1' })
                .subscribe();

            const sent = bodyOf(spectator.expectOne(SUBMIT_URL, HttpMethod.POST).request);
            const parsed = await formPartOf(sent);

            // Summed by the client, because the early refusal exists only where the caller declares a
            // total. Without it the only enforcement left aborts mid-read, after the author has waited.
            expect(parsed.totalSizeBytes).toBe(25);
        });

        it('should target a site instead of a folder at a site root', async () => {
            spectator.service
                .uploadFilesByBaseType([file('a.png')], { baseType: 'FILEASSET', siteId: 'site-1' })
                .subscribe();

            const sent = bodyOf(spectator.expectOne(SUBMIT_URL, HttpMethod.POST).request);
            const parsed = await formPartOf(sent);

            expect(parsed.siteId).toBe('site-1');
            expect(parsed.folderId).toBeUndefined();
        });

        it('should emit the accepted handle', (done) => {
            const accepted: DotBulkUploadSubmitResponse = {
                jobId: 'e6d9bae8-657b-4e2f-8524-c0222db66355',
                statusUrl: '/api/v1/jobs/e6d9bae8-657b-4e2f-8524-c0222db66355/status'
            };

            spectator.service.uploadFilesByBaseType([file('a.png')], form).subscribe((result) => {
                expect(result).toEqual(accepted);
                done();
            });

            spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({ entity: accepted });
        });

        it('should not submit an empty batch', () => {
            spectator.service.uploadFilesByBaseType([], form).subscribe({ error: () => undefined });

            spectator.controller.expectNone(SUBMIT_URL);
        });
    });
});
