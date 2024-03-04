import { Observable } from 'rxjs';

import { AsyncPipe, JsonPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import {
    FormControl,
    FormGroup,
    FormGroupDirective,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';


import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { GalleriaModule } from 'primeng/galleria';
import { ImageModule } from 'primeng/image';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {  DotMessagePipe } from '@dotcms/ui';

import { DotAiImagePromptStore, VmAiImagePrompt } from './ai-image-prompt.store';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';
import { AiImagePromptInputComponent } from './components/ai-image-prompt-input/ai-image-prompt-input.component';

import { AIImagePrompt, DotGeneratedAIImage } from '../../shared/services/dot-ai/dot-ai.models';

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
        SkeletonModule,
        JsonPipe,
        GalleriaModule,
        ImageModule,
        AiImagePromptFormComponent
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
    selectedImage : DotGeneratedAIImage;

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

        // this.store.isOpenDialog$.pipe().subscribe(() => {
        //     this.initForm();
        // });

        // this.store.activeGalleryIndex$.pipe().subscribe((activeGalleryIndex) => {
        //     console.log('activeGalleryIndex', activeGalleryIndex);
        //     this.form.patchValue({text: this.store.getImages$[activeGalleryIndex].aiResponse.originalPrompt})
        // });

        this.store.getImages$.pipe(filter((images) => !!images.length)).subscribe((images) => {
            this.selectedImage = images[images.length - 1];

            // this.form.patchValue({ text: images[images.length - 1].aiResponse.originalPrompt });
            // this.generatedValue = images[images.length - 1].aiResponse;
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
     * @param {AIImagePrompt} formValue - The object prompt used to generate the image.
     * @return {void} - This method does not return any value.
     */
    generateImage(formValue: AIImagePrompt): void {
        this.store.generateImage(formValue);
    }

    insetImage(imageInfo: DotGeneratedAIImage): void {
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
