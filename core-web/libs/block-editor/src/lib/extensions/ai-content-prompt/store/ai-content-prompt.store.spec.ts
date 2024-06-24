import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotAiService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { AiContentPromptState, AiContentPromptStore } from './ai-content-prompt.store';

describe('AiContentPromptStore', () => {
    let spectator: SpectatorService<AiContentPromptStore>;
    let store: AiContentPromptStore;
    let dotAiService: SpyObject<DotAiService>;

    const createStoreService = createServiceFactory({
        service: AiContentPromptStore,
        mocks: [DotAiService]
    });

    beforeEach(() => {
        spectator = createStoreService();
        store = spectator.service;
        dotAiService = spectator.inject(DotAiService);
    });

    it('should set open state', (done) => {
        spectator.service.setStatus(ComponentStatus.INIT);
        store.state$.subscribe((state) => {
            expect(state.status).toBe(ComponentStatus.INIT);
            done();
        });
    });

    it('should showDialog and set the initial state', (done) => {
        const initialState: AiContentPromptState = {
            prompt: '',
            generatedContent: [],
            selectedContent: '',
            activeIndex: null,
            status: ComponentStatus.INIT,
            showDialog: false,
            submitLabel: 'block-editor.extension.ai-image.generate'
        };

        //dirty state
        spectator.service.patchState({
            prompt: 'test prompt',
            selectedContent: 'test selected content'
        });

        spectator.service.showDialog();
        store.state$.subscribe((state) => {
            expect(state.showDialog).toEqual(true);
            expect(state).toEqual(initialState);
            done();
        });
    });

    it('should hideDialog', (done) => {
        spectator.service.patchState({ showDialog: true });
        spectator.service.hideDialog();
        store.state$.subscribe((state) => {
            expect(state.showDialog).toEqual(false);
            done();
        });
    });

    it('should handle subscription on selected Content', (done) => {
        spectator.service.patchState({ selectedContent: 'test selected content' });

        store.selectedContent$.subscribe((selectedContent) => {
            expect(selectedContent).toBe('test selected content');
            done();
        });
    });

    it('should call dotAiService.generateContent and update state', (done) => {
        const prompt = 'test prompt';
        const content = 'generated content';

        // Mock dotAiService.generateContent to return a known observable
        dotAiService.generateContent.mockReturnValue(of(content));

        // Trigger the effect
        spectator.service.generateContent(of(prompt));

        // Check if state is updated correctly
        store.state$.subscribe((state) => {
            expect(state.status).toBe(ComponentStatus.IDLE);
            expect(state.generatedContent).toBe([{ content, prompt }]);
            done();
        });
    });
});
