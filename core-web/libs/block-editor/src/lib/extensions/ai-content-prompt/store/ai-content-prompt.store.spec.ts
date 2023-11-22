import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { AiContentPromptStore } from './ai-content-prompt.store';

import { DotAiService } from '../../../shared';

describe('AiContentPromptStore', () => {
    let spectator: SpectatorService<AiContentPromptStore>;
    let store: AiContentPromptStore;
    let dotAiService: jest.Mocked<DotAiService>;

    const createStoreService = createServiceFactory({
        service: AiContentPromptStore,
        mocks: [DotAiService]
    });

    beforeEach(() => {
        spectator = createStoreService();
        store = spectator.service;
        dotAiService = spectator.inject(DotAiService) as jest.Mocked<DotAiService>;
    });

    it('should set open state', (done) => {
        spectator.service.setOpen(true);
        store.state$.subscribe((state) => {
            expect(state.open).toBe(true);
            done();
        });
    });

    it('should set acceptContent state', (done) => {
        spectator.service.setAcceptContent(true);
        store.state$.subscribe((state) => {
            expect(state.acceptContent).toBe(true);
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
            expect(state.loading).toBe(false);
            expect(state.content).toBe(content);
            done();
        });
    });

    it('should reGenerateContent using the last prompt', (done) => {
        const state = spectator.service['state'];
        const lastPrompt = 'last prompt';
        const content = 'generated content';

        // Set the prompt to a known value
        spectator.service.setState({ ...state, prompt: lastPrompt });

        // Mock the generateContent method of DotAiService
        dotAiService.generateContent.mockReturnValue(of(content));

        // Trigger the reGenerateContent method
        store.reGenerateContent();

        store.state$.subscribe((state) => {
            expect(state.content).toBe(content);
            expect(dotAiService.generateContent).toHaveBeenCalledWith(lastPrompt);
            done();
        });
    });
});
