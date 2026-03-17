import { Injectable } from '@angular/core';

import { DotAIImageOrientation } from '@dotcms/dotcms-models';

/**
 * Service to map AI image orientations to actual pixel dimensions based on the selected model.
 * Different AI models support different image sizes for the same orientation.
 */
@Injectable({ providedIn: 'root' })
export class DotAIImageSizeMapperService {
    private readonly MODEL_SIZE_MAP: Record<
        string,
        Record<DotAIImageOrientation, string>
    > = {
        'dall-e-3': {
            [DotAIImageOrientation.SQUARE]: '1024x1024',
            [DotAIImageOrientation.LANDSCAPE]: '1792x1024',
            [DotAIImageOrientation.PORTRAIT]: '1024x1792'
        },
        'gpt-image-1': {
            [DotAIImageOrientation.SQUARE]: '1024x1024',
            [DotAIImageOrientation.LANDSCAPE]: '1536x1024',
            [DotAIImageOrientation.PORTRAIT]: '1024x1536'
        },
        'gpt-image-1.5': {
            [DotAIImageOrientation.SQUARE]: '1024x1024',
            [DotAIImageOrientation.LANDSCAPE]: '1536x1024',
            [DotAIImageOrientation.PORTRAIT]: '1024x1536'
        },
        'gpt-image-1-mini': {
            [DotAIImageOrientation.SQUARE]: '1024x1024',
            [DotAIImageOrientation.LANDSCAPE]: '1536x1024',
            [DotAIImageOrientation.PORTRAIT]: '1024x1536'
        }
    };

    /**
     * Gets the image size (pixel dimensions) for a given model and orientation.
     * Falls back to dall-e-3 sizes if the model is not recognized.
     *
     * @param modelName - The name of the AI model (e.g., 'dall-e-3', 'gpt-image-1')
     * @param orientation - The desired image orientation
     * @returns The pixel dimensions as a string (e.g., '1024x1024', '1792x1024')
     */
    getSizeForOrientation(
        modelName: string,
        orientation: DotAIImageOrientation
    ): string {
        const normalizedModel = modelName?.toLowerCase().trim();
        const modelSizes = this.MODEL_SIZE_MAP[normalizedModel];

        if (!modelSizes) {
            console.warn(
                `Unknown image model: ${modelName}, falling back to dall-e-3 sizes`
            );

            return this.MODEL_SIZE_MAP['dall-e-3'][orientation];
        }

        return modelSizes[orientation];
    }

    /**
     * Gets all available orientations (all models support all three orientations).
     *
     * @returns Array of available orientations
     */
    getAvailableOrientations(): DotAIImageOrientation[] {
        return [
            DotAIImageOrientation.SQUARE,
            DotAIImageOrientation.LANDSCAPE,
            DotAIImageOrientation.PORTRAIT
        ];
    }

    /**
     * Checks if a model is supported by this mapper.
     *
     * @param modelName - The name of the AI model
     * @returns true if the model is supported, false otherwise
     */
    isModelSupported(modelName: string): boolean {
        const normalizedModel = modelName?.toLowerCase().trim();

        return normalizedModel in this.MODEL_SIZE_MAP;
    }
}
