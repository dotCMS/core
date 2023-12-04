import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TooltipModule } from 'primeng/tooltip';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { PromptType } from './ai-image-prompt.models';
import { DotAiImagePromptStore, VmAiImagePrompt } from './ai-image-prompt.store';
import { AiImagePromptInputComponent } from './components/ai-image-prompt-input/ai-image-prompt-input.component';

@Component({
    selector: 'dot-ai-image-prompt',
    standalone: true,
    templateUrl: './ai-image-prompt.component.html',
    styleUrls: ['./ai-image-prompt.component.scss'],
    imports: [
        ButtonModule,
        TooltipModule,
        ReactiveFormsModule,
        OverlayPanelModule,
        NgIf,
        DialogModule,
        AiImagePromptInputComponent,
        AiImagePromptInputComponent,
        AsyncPipe,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AIImagePromptComponent {
    protected readonly vm$: Observable<VmAiImagePrompt> = inject(DotAiImagePromptStore).vm$;

    protected readonly ComponentStatus = ComponentStatus;
    private store = inject(DotAiImagePromptStore);

    /**
     * Hides the dialog.
     * @return {void}
     */
    hideDialog(): void {
        this.store.hideDialog();
    }

    /**
     * Selects the prompt type
     *
     * @return {void}
     */
    selectType(promptType: PromptType, current: PromptType): void {
        if (current != promptType) {
            this.store.setPromptType(promptType);
        }
    }

    /**
     * Generates an image based on the provided prompt.
     *
     * @param {string} prompt - The text prompt used to generate the image.
     * @return {void} - This method does not return any value.
     */
    generateImage(prompt: string): void {
        this.store.generateImage(prompt);
    }
}
