import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { AiImagePromptInputComponent } from './ai-image-prompt-input.component';

describe('AiImagePromptInputComponent', () => {
    let spectator: Spectator<AiImagePromptInputComponent>;
    const createComponent = createComponentFactory(AiImagePromptInputComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
