import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import {
    DotAIImageContent,
    DotAIImageOrientation,
    DotAIImageResponse,
    DotCMSContentlet
} from '@dotcms/dotcms-models';

import { DotAiContentService } from './dot-ai-content.service';
import { AI_API_ENDPOINT, API_ENDPOINT_FOR_PUBLISH } from './dot-ai.constants';

describe('DotAiContentService', () => {
    let spectator: SpectatorHttp<DotAiContentService>;

    const createHttp = createHttpFactory(DotAiContentService);

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
                `${AI_API_ENDPOINT}/image/generate`,
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

        it('should extract message from structured error body { error: { message } }', () => {
            const mockPrompt = 'Test prompt';

            spectator.service.generateAndPublishImage(mockPrompt).subscribe(
                () => fail('Expected an error, but received a response'),
                (error) => {
                    expect(error).toBe("Invalid size '1792x1024' for model");
                }
            );

            const req = spectator.expectOne('/api/v1/ai/image/generate', HttpMethod.POST);
            req.flush(
                { error: { message: "Invalid size '1792x1024' for model" } },
                { status: 400, statusText: 'Bad Request' }
            );
        });

        it('should extract message from error body { error: string }', () => {
            const mockPrompt = 'Test prompt';

            spectator.service.generateAndPublishImage(mockPrompt).subscribe(
                () => fail('Expected an error, but received a response'),
                (error) => {
                    expect(error).toBe('Something went wrong');
                }
            );

            const req = spectator.expectOne('/api/v1/ai/image/generate', HttpMethod.POST);
            req.flush(
                { error: 'Something went wrong' },
                { status: 400, statusText: 'Bad Request' }
            );
        });

        it('should extract message from error body { message: string }', () => {
            const mockPrompt = 'Test prompt';

            spectator.service.generateAndPublishImage(mockPrompt).subscribe(
                () => fail('Expected an error, but received a response'),
                (error) => {
                    expect(error).toBe('Direct error message');
                }
            );

            const req = spectator.expectOne('/api/v1/ai/image/generate', HttpMethod.POST);
            req.flush(
                { message: 'Direct error message' },
                { status: 400, statusText: 'Bad Request' }
            );
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
    describe('generateImage (extracted from generateAndPublishImage)', () => {
        it('should POST the prompt and size and NOT publish anything', () => {
            const mockResponse = { response: 'abc', tempFileName: 'img.png' } as DotAIImageResponse;

            spectator.service.generateImage('a cat', DotAIImageOrientation.SQUARE).subscribe();

            const req = spectator.expectOne(`${AI_API_ENDPOINT}/image/generate`, HttpMethod.POST);
            expect(JSON.parse(req.request.body)).toEqual({
                prompt: 'a cat',
                size: DotAIImageOrientation.SQUARE
            });
            req.flush(mockResponse);

            // FR-037: generating must not publish. Nothing may hit the workflow endpoint.
            spectator.controller.expectNone(API_ENDPOINT_FOR_PUBLISH);
        });

        it('should surface the generate error without attempting a publish', () => {
            let caught: unknown;
            spectator.service.generateImage('a cat').subscribe({ error: (e) => (caught = e) });

            spectator
                .expectOne(`${AI_API_ENDPOINT}/image/generate`, HttpMethod.POST)
                .flush({ error: { message: 'nope' } }, { status: 400, statusText: 'Bad Request' });

            expect(caught).toBe('nope');
            spectator.controller.expectNone(API_ENDPOINT_FOR_PUBLISH);
        });
    });
});
