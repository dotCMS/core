import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotAIImageContent, DotAIImageOrientation, DotAIImageResponse } from './dot-ai.models';
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

    it('should generate and publish an image', () => {
        const mockPrompt = 'Test prompt';
        const mockGenerateResponse: DotAIImageResponse = {
            response: 'temp_file123',
            tempFileName: 'Test Imagae'
        } as unknown as DotAIImageContent;
        const mockPublishResponse = [{ '123': 'testContent' }] as unknown as DotCMSContentlet[];
        const mockPublishRequest = [
            {
                baseType: 'dotAsset',
                asset: mockGenerateResponse.response,
                title: mockGenerateResponse.tempFileName,
                hostFolder: '',
                indexPolicy: 'WAIT_FOR'
            }
        ];
        const size = DotAIImageOrientation.SQUARE;

        spectator.service.generateAndPublishImage(mockPrompt, size).subscribe((response) => {
            expect(response).toEqual({
                contentlet: Object.values(mockPublishResponse[0])[0],
                ...mockGenerateResponse
            });
        });

        const generateRequest = httpTestingController.expectOne('/api/v1/ai/image/generate');
        const publishRequest = httpTestingController.expectOne(
            '/api/v1/workflow/actions/default/fire/PUBLISH'
        );

        expect(generateRequest.request.method).toEqual('POST');
        expect(JSON.parse(generateRequest.request.body)).toEqual({ prompt: mockPrompt, size });

        expect(publishRequest.request.method).toEqual('POST');
        expect(JSON.parse(generateRequest.request.body)).toEqual({ mockPublishRequest });

        generateRequest.flush({ response: mockGenerateResponse });
        publishRequest.flush({ response: mockPublishResponse });
    });

    it('should handle errors while generating image', () => {
        const mockPrompt = 'Test prompt';

        spectator.service.generateAndPublishImage(mockPrompt).subscribe(
            () => fail('Expected an error, but received a response'),
            (error) => {
                expect(error).toBe('Error fetching AI content');
            }
        );

        const req = httpTestingController.expectOne('/api/v1/ai/image/generate');

        req.flush(null, { status: 500, statusText: 'Server Error' });
    });

    it('should handle errors while creating and publishing contentlet', () => {
        const mockPrompt = 'Test prompt' as unknown as DotAIImageResponse;

        spectator.service.createAndPublishContentlet(mockPrompt).subscribe(
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
