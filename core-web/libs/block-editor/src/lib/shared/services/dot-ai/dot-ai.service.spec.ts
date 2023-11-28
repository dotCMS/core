import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotAiService } from './dot-ai.service';

describe('DotAiService', () => {
    let spectator: SpectatorService<DotAiService>;
    let httpTestingController: HttpTestingController;

    const createService = createServiceFactory({
        service: DotAiService,
        imports: [HttpClientTestingModule]
    });

    beforeEach(() => {
        spectator = createService();
        httpTestingController = spectator.inject(HttpTestingController);
    });

    it('should generate content', () => {
        const mockPrompt = 'Test prompt';
        const mockResponse = 'Test response';

        spectator.service.generateContent(mockPrompt).subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpTestingController.expectOne('/api/v1/ai/text/generate');

        expect(req.request.method).toEqual('POST');
        expect(JSON.parse(req.request.body)).toEqual({ prompt: mockPrompt });

        req.flush({ response: mockResponse });
    });

    it('should handle errors while generating content', () => {
        const mockPrompt = 'Test prompt';

        spectator.service.generateContent(mockPrompt).subscribe(
            () => fail('Expected an error, but received a response'),
            (error) => {
                expect(error).toBe('Error fetching AI content');
            }
        );

        const req = httpTestingController.expectOne('/api/v1/ai/text/generate');

        req.flush(null, { status: 500, statusText: 'Server Error' });
    });

    it('should generate image', () => {
        const mockPrompt = 'Test prompt';
        const mockResponse = 'Test response';

        spectator.service.generateImage(mockPrompt).subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpTestingController.expectOne('/api/v1/ai/image/generate');

        expect(req.request.method).toEqual('POST');
        expect(JSON.parse(req.request.body)).toEqual({ prompt: mockPrompt });

        req.flush({ response: mockResponse });
    });

    it('should handle errors while generating image', () => {
        const mockPrompt = 'Test prompt';

        spectator.service.generateImage(mockPrompt).subscribe(
            () => fail('Expected an error, but received a response'),
            (error) => {
                expect(error).toBe('Error fetching AI content');
            }
        );

        const req = httpTestingController.expectOne('/api/v1/ai/image/generate');

        req.flush(null, { status: 500, statusText: 'Server Error' });
    });

    it('should create and publish contentlet', () => {
        const mockFileId = '123';
        const mockResponse = ['contentlet'] as unknown as DotCMSContentlet[];

        spectator.service.createAndPublishContentlet(mockFileId).subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpTestingController.expectOne(
            '/api/v1/workflow/actions/default/fire/PUBLISH'
        );

        expect(req.request.method).toEqual('POST');
        expect(JSON.parse(req.request.body)).toEqual({
            contentlets: [
                {
                    contentType: 'dotAsset',
                    asset: mockFileId,
                    hostFolder: '',
                    indexPolicy: 'WAIT_FOR'
                }
            ]
        });
        req.flush({ entity: { results: mockResponse } });
    });

    it('should handle errors while creating and publishing contentlet', () => {
        const mockFileId = '123';

        spectator.service.createAndPublishContentlet(mockFileId).subscribe(
            () => fail('Expected an error, but received a response'),
            (error) => {
                expect(error).toBe('Test Error');
            }
        );

        const req = httpTestingController.expectOne(
            '/api/v1/workflow/actions/default/fire/PUBLISH'
        );

        req.flush(null, { status: 500, statusText: 'Test Error' });
    });
});
