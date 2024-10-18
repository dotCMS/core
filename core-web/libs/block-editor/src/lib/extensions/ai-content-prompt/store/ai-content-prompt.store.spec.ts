import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotAiService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { initialState, AiContentPromptStore } from './ai-content-prompt.store';

describe('AiContentPromptStore', () => {
    let spectator: SpectatorService<AiContentPromptStore>;
    let store: AiContentPromptStore;
    let dotAiService: SpyObject<DotAiService>;

    const createService = createServiceFactory({
        service: AiContentPromptStore,
        mocks: [DotAiService]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotAiService = spectator.inject(DotAiService);
    });

    it('should set open state', (done) => {
        store.setStatus(ComponentStatus.INIT);
        store.state$.subscribe((state) => {
            expect(state.status).toBe(ComponentStatus.INIT);
            done();
        });
    });

    it('should showDialog and set the initial state', (done) => {
        store.patchState({
            prompt: 'test prompt',
            selectedContent: 'test selected content'
        });

        store.showDialog();

        store.state$.subscribe((state) => {
            expect(JSON.stringify(state)).toBe(
                JSON.stringify({
                    ...initialState,
                    generatedContent: [],
                    showDialog: true
                })
            );
            done();
        });
    });

    it('should hideDialog', (done) => {
        store.patchState({ showDialog: true });
        store.hideDialog();
        store.state$.subscribe((state) => {
            expect(state.showDialog).toEqual(false);
            done();
        });
    });

    it('should handle subscription on selected Content', (done) => {
        store.patchState({ selectedContent: 'test selected content' });

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
        store.generateContent(of(prompt));

        // Check if state is updated correctly
        store.state$.subscribe((state) => {
            expect(state.status).toBe(ComponentStatus.IDLE);
            expect(state.generatedContent[0].content).toBe(content);
            expect(state.generatedContent[0].prompt).toBe(prompt);

            done();
        });
    });
});
