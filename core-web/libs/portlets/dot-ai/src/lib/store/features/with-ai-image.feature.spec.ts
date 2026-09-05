import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { DotAiContentService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAIImageResponse } from '@dotcms/dotcms-models';

import { withAiImage } from './with-ai-image.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

const generated = (): DotAIImageResponse =>
    ({
        response: 'abc123',
        tempFileName: 'cat.png',
        originalPrompt: 'a cat',
        revised_prompt: 'a photorealistic cat'
    }) as DotAIImageResponse;

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withAiImage()
);

describe('withAiImage', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;
    let service: jest.Mocked<DotAiContentService>;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [mockProvider(DotAiContentService), mockProvider(DotHttpErrorManagerService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotAiContentService) as jest.Mocked<DotAiContentService>;
    });

    describe('generateImage (FR-037)', () => {
        it('should store the result and publish NOTHING', () => {
            service.generateImage = jest.fn().mockReturnValue(of(generated()));
            service.createAndPublishContentlet = jest.fn();

            store.generateImage('a cat');

            expect(store.image()?.response).toBe('abc123');
            expect(store.image()?.published).toBe(false);
            // The whole point of the generateImage extract: generating must not publish.
            expect(service.createAndPublishContentlet).not.toHaveBeenCalled();
        });

        it('should expose the provider rewritten prompt', () => {
            service.generateImage = jest.fn().mockReturnValue(of(generated()));

            store.generateImage('a cat');

            expect(store.image()?.revisedPrompt).toBe('a photorealistic cat');
        });

        it('should ignore an empty prompt', () => {
            service.generateImage = jest.fn();

            store.generateImage('   ');

            expect(service.generateImage).not.toHaveBeenCalled();
        });

        it('should report a failure and leave nothing half-rendered', () => {
            service.generateImage = jest.fn().mockReturnValue(throwError(() => 'boom'));

            store.generateImage('a cat');

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.imageGenerating()).toBe(false);
            expect(store.image()).toBeNull();
        });
    });

    describe('saveImage', () => {
        beforeEach(() => {
            service.generateImage = jest.fn().mockReturnValue(of(generated()));
            store.generateImage('a cat');
        });

        it('should publish once even on a double click (exhaustMap, FR-035)', () => {
            const pending = new Subject();
            service.createAndPublishContentlet = jest.fn().mockReturnValue(pending);

            store.saveImage();
            store.saveImage();

            expect(service.createAndPublishContentlet).toHaveBeenCalledTimes(1);
        });

        it('should mark the image published on success', () => {
            service.createAndPublishContentlet = jest.fn().mockReturnValue(of({ contentlet: {} }));

            store.saveImage();

            expect(store.image()?.published).toBe(true);
        });

        it('should keep the image on screen when publishing fails (FR-040)', () => {
            service.createAndPublishContentlet = jest.fn().mockReturnValue(throwError(() => 'no'));

            store.saveImage();

            expect(store.image()).not.toBeNull();
            expect(store.image()?.published).toBe(false);
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
        });

        it('should do nothing when there is no image', () => {
            service.createAndPublishContentlet = jest.fn();
            store.clearImage();

            store.saveImage();

            expect(service.createAndPublishContentlet).not.toHaveBeenCalled();
        });
    });

    it('should expose the same-origin asset url used for preview and download (FR-038)', () => {
        service.generateImage = jest.fn().mockReturnValue(of(generated()));

        store.generateImage('a cat');

        expect(store.imageUrl()).toBe('/dA/abc123/asset.png');
    });
});
