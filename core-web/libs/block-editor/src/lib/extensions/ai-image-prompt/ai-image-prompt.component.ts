import { Observable } from 'rxjs';

import { AsyncPipe, JsonPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import {
    FormControl,
    FormGroup,
    FormGroupDirective,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { GalleriaModule } from 'primeng/galleria';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotAiImagePromptStore, VmAiImagePrompt } from './ai-image-prompt.store';
import { AiImagePromptInputComponent } from './components/ai-image-prompt-input/ai-image-prompt-input.component';

import { DotAIImageGenerationResponse } from '../../shared/services/dot-ai/dot-ai.models';

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
        AccordionModule,
        SkeletonModule,
        JsonPipe,
        GalleriaModule
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

    form: FormGroup;

    /**
     * Hides the dialog.
     * @return {void}
     */
    hideDialog(): void {
        this.store.hideDialog();
    }

    ngOnInit(): void {
        this.initForm();

        this.store.isLoading$.pipe().subscribe((isLoading) => {
            isLoading ? this.form.disable() : this.form.enable();
        });

        this.store.getImages$.pipe(filter(images => !!images.length)).subscribe((images) => {
            this.form.patchValue({'text': images[images.length - 1].aiResponse.revised_prompt });
        });

        // this.vm$.subscribe((vm) => {
        //     vm.status === ComponentStatus.LOADING ? this.form.disable() : this.form.enable();
        // });
    }

    // /**
    //  * Selects the prompt type
    //  *
    //  * @return {void}
    //  */
    // selectType(promptType: PromptType, current: PromptType): void {
    //     if (current != promptType) {
    //         this.store.setPromptType(promptType);
    //     }
    // }

    /**
     * Generates an image based on the provided prompt.
     *
     * @param {string} prompt - The text prompt used to generate the image.
     * @return {void} - This method does not return any value.
     */
    generateImage(): void {
        this.store.generateImage(this.form.value);
    }

    insetImage(imageInfo: DotAIImageGenerationResponse): void {
        this.store.patchState({ selectedImage: imageInfo });
        //TODO: add image carrousel and notify the store
    }

    /**
     * Clears the error at the store on hiding the confirmation dialog.
     *
     * @return {void}
     */
    onHideConfirm(): void {
        this.store.cleanError();
    }

    promptType = 1;

    private initForm(): void {
        this.form = new FormGroup({
            text: new FormControl('', Validators.required),
            type: new FormControl('input', Validators.required),
            size: new FormControl('1792x1024', Validators.required)
        });
    }
}
