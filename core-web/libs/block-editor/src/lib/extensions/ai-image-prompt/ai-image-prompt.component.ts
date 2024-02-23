import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
    FormBuilder,
    FormGroupDirective,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { ConfirmationService, SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import {  PanelModule } from 'primeng/panel';
import { RadioButtonModule } from 'primeng/radiobutton';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

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
        AsyncPipe,
        DotMessagePipe,
        ConfirmDialogModule,
        PanelModule,
        DotFieldRequiredDirective,
        FormsModule,
        RadioButtonModule,
        DropdownModule,
        InputTextareaModule,
        AccordionModule
    ],
    providers: [FormGroupDirective],

    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AIImagePromptComponent {
    protected readonly vm$: Observable<VmAiImagePrompt> = inject(DotAiImagePromptStore).vm$;
    protected readonly ComponentStatus = ComponentStatus;
    private confirmationService = inject(ConfirmationService);
    private dotMessageService = inject(DotMessageService);
    private store: DotAiImagePromptStore = inject(DotAiImagePromptStore);


    sizeOptions: SelectItem<number>[] = [
        { value: 1, label: 'Vertical (1024 x 1792)' },
        { value: 2, label: 'Horizontal (1792 x 1024)' },
        { value: 3, label: 'Square (1024 x 1024)' }
    ];

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

    /**
     * Clears the error at the store on hiding the confirmation dialog.
     *
     * @return {void}
     */
    onHideConfirm(): void {
        this.store.cleanError();
    }

    form = inject(FormBuilder).group({
        prompt: ['', Validators.required]
    });
    promptType = 1;

}
