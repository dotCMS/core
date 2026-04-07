import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotAiService } from '@dotcms/data-access';
import { ComponentStatus, DotAIImageOrientation, PromptType } from '@dotcms/dotcms-models';

import { DotAiImagePromptStore } from './ai-image-prompt.store';

import { MOCK_AI_IMAGE_CONTENT, MOCK_GENERATED_AI_IMAGE } from '../utils/mocks';

describe('DotAiImagePromptStore', () => {
    let store: InstanceType<typeof DotAiImagePromptStore>;
    let dotAiService: SpyObject<DotAiService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotAiImagePromptStore, mockProvider(DotAiService)]
        });

        store = TestBed.inject(DotAiImagePromptStore);
        dotAiService = TestBed.inject(DotAiService) as SpyObject<DotAiService>;
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('Initial State', () => {
        it('should have the correct initial state', () => {
            expect(store.status()).toBe(ComponentStatus.INIT);
            expect(store.images()).toEqual([]);
            expect(store.context()).toBeNull();
            expect(store.galleryActiveIndex()).toBe(0);
            expect(store.error()).toBeNull();
            expect(store.formValue()).toEqual({
                text: '',
                type: PromptType.INPUT,
                size: DotAIImageOrientation.HORIZONTAL
            });
        });
    });

    describe('Computed Properties', () => {
        it('should compute isLoading correctly', () => {
            expect(store.isLoading()).toBe(false);
            patchState(store, { status: ComponentStatus.LOADING });
            expect(store.isLoading()).toBe(true);
        });

        it('should compute hasContext correctly', () => {
            expect(store.hasContext()).toBe(false);
            patchState(store, { context: 'context' });
            expect(store.hasContext()).toBe(true);
        });

        it('should compute currentImage correctly', () => {
            expect(store.currentImage()).toBeUndefined();
            patchState(store, {
                images: [MOCK_GENERATED_AI_IMAGE]
            });
            expect(store.currentImage()).toEqual(MOCK_GENERATED_AI_IMAGE);
        });
    });

    describe('Methods', () => {
        it('should set galleryActiveIndex correctly', () => {
            store.setGalleryActiveIndex(1);
            expect(store.galleryActiveIndex()).toBe(1);
        });

        it('should set context correctly', () => {
            store.setContext('new context');
            expect(store.context()).toBe('new context');
        });

        it('should set formValue correctly', () => {
            const formValue = {
                text: 'new text',
                type: PromptType.INPUT,
                size: DotAIImageOrientation.VERTICAL
            };
            store.setFormValue(formValue);
            expect(store.formValue()).toEqual(formValue);
        });

        it('should handle generateImage correctly on success', () => {
            dotAiService.generateAndPublishImage.mockReturnValue(of(MOCK_AI_IMAGE_CONTENT));

            store.setFormValue({
                text: 'prompt',
                type: PromptType.INPUT,
                size: DotAIImageOrientation.HORIZONTAL
            });
            store.generateImage();

            expect(dotAiService.generateAndPublishImage).toHaveBeenCalledWith(
                'prompt',
                DotAIImageOrientation.HORIZONTAL
            );
            expect(store.status()).toBe(ComponentStatus.IDLE);
            expect(store.images().length).toBe(1);
        });

        it('should handle generateImage correctly on error', () => {
            dotAiService.generateAndPublishImage.mockReturnValue(throwError(() => 'error'));

            store.setFormValue({
                text: 'prompt',
                type: PromptType.INPUT,
                size: DotAIImageOrientation.HORIZONTAL
            });
            store.generateImage();

            expect(dotAiService.generateAndPublishImage).toHaveBeenCalledWith(
                'prompt',
                DotAIImageOrientation.HORIZONTAL
            );
            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.error()).toBe('error');
        });
    });
});
