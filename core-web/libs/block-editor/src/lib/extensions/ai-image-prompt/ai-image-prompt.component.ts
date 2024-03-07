import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormGroupDirective } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAiImagePromptStore, VmAiImagePrompt } from './ai-image-prompt.store';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';
import { AiImagePromptGalleryComponent } from './components/ai-image-prompt-gallery/ai-image-prompt-gallery.component';

import { AIImagePrompt, DotGeneratedAIImage } from '../../shared/services/dot-ai/dot-ai.models';

@Component({
    selector: 'dot-ai-image-prompt',
    standalone: true,
    templateUrl: './ai-image-prompt.component.html',
    styleUrls: ['./ai-image-prompt.component.scss'],
    imports: [
        NgIf,
        DialogModule,
        AsyncPipe,
        DotMessagePipe,
        ConfirmDialogModule,
        AiImagePromptFormComponent,
        AiImagePromptGalleryComponent
    ],
    providers: [FormGroupDirective],

    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AIImagePromptComponent implements OnInit {
    protected readonly vm$: Observable<VmAiImagePrompt> = inject(DotAiImagePromptStore).vm$;
    protected readonly ComponentStatus = ComponentStatus;
    private confirmationService = inject(ConfirmationService);
    private dotMessageService = inject(DotMessageService);
    private store: DotAiImagePromptStore = inject(DotAiImagePromptStore);

    selectedImage: DotGeneratedAIImage;

    /**
     * Hides the dialog.
     * @return {void}
     */
    hideDialog(): void {
        this.store.hideDialog();
    }

    ngOnInit(): void {
        this.store.getImages$.pipe(filter((images) => !!images.length)).subscribe((images) => {
            this.selectedImage = images[images.length - 1];
        });
    }

    /**
     * Generates an image based on the provided prompt.
     *
     * @param {AIImagePrompt} formValue - The object prompt used to generate the image.
     * @return {void} - This method does not return any value.
     */
    generateImage(formValue: AIImagePrompt): void {
        this.store.generateImage(formValue);
    }

    insetImage(imageInfo: DotGeneratedAIImage): void {
        this.store.patchState({ selectedImage: imageInfo });
    }

    /**
     * Clears the error at the store on hiding the confirmation dialog.
     *
     * @return {void}
     */
    onHideConfirm(): void {
        this.store.cleanError();
    }

    protected readonly console = console;
}
