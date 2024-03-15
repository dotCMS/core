import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormGroupDirective } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAiImagePromptStore, VmAiImagePrompt } from './ai-image-prompt.store';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';
import { AiImagePromptGalleryComponent } from './components/ai-image-prompt-gallery/ai-image-prompt-gallery.component';

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
export class AIImagePromptComponent {
    protected readonly vm$: Observable<VmAiImagePrompt> = inject(DotAiImagePromptStore).vm$;
    protected readonly ComponentStatus = ComponentStatus;
    private confirmationService = inject(ConfirmationService);
    private dotMessageService = inject(DotMessageService);
    store: DotAiImagePromptStore = inject(DotAiImagePromptStore);

    /**
     * Clears the error at the store on hiding the confirmation dialog.
     *
     * @return {void}
     */
    onHideConfirm(): void {
        this.store.cleanError();
    }
}
