import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import {
    DotAIImageContent,
    DotAIImageOrientation,
    DotAIImageResponse,
    DotCMSContentlet
} from '@dotcms/dotcms-models';

import { DotAiService, API_ENDPOINT_FOR_PUBLISH, API_ENDPOINT } from './dot-ai.service';

describe('DotAiService', () => {
    let spectator: SpectatorHttp<DotAiService>;

    const createHttp = createHttpFactory(DotAiService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('generateContent', () => {
        it('should generate content', () => {
            const mockPrompt = 'Test prompt';
            const mockResponse = 'Test response';
            const mockBodyResponse = {
                choices: [
                    {
                        message: {
                            content: mockResponse
                        }
                    }
                ]
            };

            spectator.service.generateContent(mockPrompt).subscribe((response) => {
                expect(response).toEqual(mockResponse);
            });

            const req = spectator.expectOne('/api/v1/ai/text/generate', HttpMethod.POST);
            req.flush(mockBodyResponse);

            expect(JSON.parse(req.request.body)).toEqual({ prompt: mockPrompt });
        });

        it('should handle errors while generating content', () => {
            const mockPrompt = 'Test prompt';

            spectator.service.generateContent(mockPrompt).subscribe(
                () => fail('Expected an error, but received a response'),
                (error) => {
                    expect(error).toBe('Server Error');
                }
            );

            const req = spectator.expectOne('/api/v1/ai/text/generate', HttpMethod.POST);
            req.flush(null, { status: 500, statusText: 'Server Error' });
        });
    });

    describe('generateAndPublishImage', () => {
        it('should generate and publish an image', () => {
            const mockPrompt = 'Test prompt';
            const size = DotAIImageOrientation.SQUARE;

            const mockGenerateResponse: DotAIImageContent = {
                response: 'temp_file123',
                tempFileName: 'Test Imagae'
            } as unknown as DotAIImageContent;
            const mockContentLet = { attr: 'testContent' } as unknown as DotCMSContentlet;
            const mockPublishResponse = {
                entity: {
                    results: [{ key: { ...mockContentLet } }]
                }
            };
            const expectedPublishRequest = {
                contentlets: [
                    {
                        baseType: 'dotAsset',
                        asset: mockGenerateResponse.response,
                        title: mockGenerateResponse.tempFileName,
                        hostFolder: '',
                        indexPolicy: 'WAIT_FOR'
                    }
                ]
            };

            spectator.service.generateAndPublishImage(mockPrompt, size).subscribe((response) => {
                expect(response).toEqual({
                    ...mockGenerateResponse,
                    contentlet: { ...mockContentLet }
                });
            });

            const generateRequest = spectator.expectOne(
                `${API_ENDPOINT}/image/generate`,
                HttpMethod.POST
            );
            generateRequest.flush(mockGenerateResponse);

            expect(JSON.parse(generateRequest.request.body)).toEqual({ prompt: mockPrompt, size });

            const publishRequest = spectator.expectOne(API_ENDPOINT_FOR_PUBLISH, HttpMethod.POST);
            publishRequest.flush(mockPublishResponse);

            expect(JSON.parse(publishRequest.request.body)).toEqual(expectedPublishRequest);
        });

        it('should handle errors while generating image', () => {
            const mockPrompt = 'Test prompt';

            spectator.service.generateAndPublishImage(mockPrompt).subscribe(
                () => fail('Expected an error, but received a response'),
                (error) => {
                    expect(error).toBe('block-editor.extension.ai-image.api-error.missing-token');
                }
            );

            const req = spectator.expectOne('/api/v1/ai/image/generate', HttpMethod.POST);
            req.flush(null, { status: 500, statusText: 'Server Error' });
        });

        it('should handle errors while creating and publishing contentlet', () => {
            const mockPrompt = 'Test prompt' as unknown as DotAIImageResponse;

            spectator.service.createAndPublishContentlet(mockPrompt).subscribe(
                () => fail('Expected an error, but received a response'),
                (error) => {
                    expect(error).toBe(
                        'block-editor.extension.ai-image.api-error.error-publishing-ai-image'
                    );
                }
            );

            const req = spectator.expectOne(API_ENDPOINT_FOR_PUBLISH, HttpMethod.POST);
            req.flush(null, { status: 500, statusText: 'Test Error' });
        });
    });
});
