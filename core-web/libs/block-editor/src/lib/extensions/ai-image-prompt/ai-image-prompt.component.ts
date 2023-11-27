import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TooltipModule } from 'primeng/tooltip';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { PromptType } from './ai-image-prompt.models';
import { DotAiImagePromptStore } from './ai-image-prompt.store';
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
        AsyncPipe
    ],
    providers: [DotAiImagePromptStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AIImagePromptComponent {
    vm$ = inject(DotAiImagePromptStore).vm$;

    protected readonly ComponentStatus = ComponentStatus;
    private store = inject(DotAiImagePromptStore);

    /**
     * Show the dialog.
     * Called from the plugin extension
     *
     * @memberof AIImagePromptComponent
     * @returns {void}
     */
    showDialog(editorContent: string): void {
        this.store.showDialog(editorContent);
    }

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
     * @param promptType
     * @return {void} - This method does not return any value.
     */
    generateImage(prompt: string, promptType: PromptType): void {
        if (promptType === 'input') {
            this.store.generateImage(prompt);
        } else if (promptType === 'auto') {
            this.store.generateImageUsingBlockEditorContent(prompt);
        }
    }
}
