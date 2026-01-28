import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormGroupDirective } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotMessagePipe } from './../../dot-message/dot-message.pipe';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';
import { AiImagePromptGalleryComponent } from './components/ai-image-prompt-gallery/ai-image-prompt-gallery.component';
import { DotAiImagePromptStore } from './store/ai-image-prompt.store';

@Component({
    selector: 'dot-ai-image-prompt',
    templateUrl: './ai-image-prompt.component.html',
    styleUrls: ['./ai-image-prompt.component.scss'],
    imports: [
        DotMessagePipe,
        Button,
        ConfirmDialog,
        AiImagePromptFormComponent,
        AiImagePromptGalleryComponent
    ],
    providers: [FormGroupDirective, ConfirmationService, DotAiImagePromptStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAIImagePromptComponent implements OnInit {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly store = inject(DotAiImagePromptStore);

    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig<{ context: string }>);

    ngOnInit(): void {
        const context = this.#dialogConfig?.data?.context || '';
        this.store.setContext(context);
    }

    closeDialog(): void {
        this.#confirmationService.confirm({
            key: 'ai-image-prompt',
            header: this.#dotMessageService.get('block-editor.extension.ai.confirmation.header'),
            message: this.#dotMessageService.get('block-editor.extension.ai.confirmation.message'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.#dotMessageService.get('Discard'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            accept: () => {
                this.#dialogRef.close();
            }
        });
    }

    insertImage(): void {
        const currentImage = this.store.currentImage();
        this.#dialogRef.close(currentImage);
    }
}
