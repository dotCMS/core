import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormGroupDirective } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotMessagePipe } from './../../dot-message/dot-message.pipe';
import { DotAiImagePromptStore, VmAiImagePrompt } from './ai-image-prompt.store';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';
import { AiImagePromptGalleryComponent } from './components/ai-image-prompt-gallery/ai-image-prompt-gallery.component';

@Component({
    selector: 'dot-ai-image-prompt',
    standalone: true,
    templateUrl: './ai-image-prompt.component.html',
    styleUrls: ['./ai-image-prompt.component.scss'],
    imports: [
        DialogModule,
        AsyncPipe,
        DotMessagePipe,
        ButtonModule,
        ConfirmDialogModule,
        AiImagePromptFormComponent,
        AiImagePromptGalleryComponent
    ],
    providers: [FormGroupDirective, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAIImagePromptComponent {
    protected readonly vm$: Observable<VmAiImagePrompt> = inject(DotAiImagePromptStore).vm$;
    protected readonly ComponentStatus = ComponentStatus;
    private dotMessageService = inject(DotMessageService);
    private confirmationService = inject(ConfirmationService);
    store: DotAiImagePromptStore = inject(DotAiImagePromptStore);

    closeDialog(): void {
        this.confirmationService.confirm({
            key: 'ai-image-prompt',
            header: this.dotMessageService.get('block-editor.extension.ai.confirmation.header'),
            message: this.dotMessageService.get('block-editor.extension.ai.confirmation.message'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessageService.get('Discard'),
            rejectLabel: this.dotMessageService.get('Cancel'),
            accept: () => {
                this.store.hideDialog();
            }
        });
    }
}
