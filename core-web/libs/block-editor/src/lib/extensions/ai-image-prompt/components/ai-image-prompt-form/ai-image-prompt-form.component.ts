import { NgIf } from '@angular/common';
import {
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChange,
    SimpleChanges
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotMessageService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import {
    AIImagePrompt,
    DotAIImageOrientation,
    DotGeneratedAIImage
} from '../../../../shared/services/dot-ai/dot-ai.models';
import { PromptType } from '../../ai-image-prompt.models';

@Component({
    selector: 'dot-ai-image-prompt-form',
    standalone: true,
    templateUrl: './ai-image-prompt-form.component.html',
    imports: [
        ButtonModule,
        AccordionModule,
        RadioButtonModule,
        ReactiveFormsModule,
        FormsModule,
        DropdownModule,
        NgIf,
        InputTextareaModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    styleUrls: ['./ai-image-prompt-form.component.scss']
})
export class AiImagePromptFormComponent implements OnChanges, OnInit {
    @Input()
    generatedValue: DotGeneratedAIImage;

    @Input()
    isLoading = false;

    @Output()
    value = new EventEmitter<AIImagePrompt>();

    @Output()
    orientation = new EventEmitter<DotAIImageOrientation>();

    form: FormGroup;
    aiProcessedPrompt: string;
    dotMessageService = inject(DotMessageService);
    destroyRef = inject(DestroyRef);
    promptTextAreaPlaceholder = 'block-editor.extension.ai-image.custom.placeholder';
    promptLabel = 'block-editor.extension.ai-image.prompt';
    submitButtonLabel = 'block-editor.extension.ai-image.generate';
    requiredPrompt = true;

    orientationOptions: SelectItem<DotAIImageOrientation>[] = [
        {
            value: DotAIImageOrientation.HORIZONTAL,
            label: this.dotMessageService.get(
                'block-editor.extension.ai-image.orientation.horizontal'
            )
        },
        {
            value: DotAIImageOrientation.SQUARE,
            label: this.dotMessageService.get('block-editor.extension.ai-image.orientation.square')
        },
        {
            value: DotAIImageOrientation.VERTICAL,
            label: this.dotMessageService.get(
                'block-editor.extension.ai-image.orientation.vertical'
            )
        }
    ];

    ngOnInit(): void {
        this.initForm();
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { generatedValue, isLoading } = changes;

        this.updatedFormValues(generatedValue, isLoading?.currentValue);

        this.setSubmitButtonLabel(isLoading?.currentValue);

        this.toggleFormState(isLoading?.currentValue && !isLoading.firstChange);
    }

    private initForm(): void {
        this.form = new FormGroup({
            text: new FormControl('', Validators.required),
            type: new FormControl(PromptType.INPUT, Validators.required),
            size: new FormControl(DotAIImageOrientation.HORIZONTAL, Validators.required)
        });

        const sizeControl = this.form.get('size');
        const typeControl = this.form.get('type');

        sizeControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((size) => {
            this.orientation.emit(size);
        });

        typeControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((type) => {
            const promptControl = this.form.get('text');
            type === PromptType.AUTO
                ? promptControl.clearValidators()
                : promptControl.setValidators(Validators.required);

            promptControl.updateValueAndValidity();

            this.setTypeLabels(type);
            this.requiredPrompt = type === PromptType.INPUT;
        });
    }

    private setTypeLabels(type: PromptType): void {
        if (type === PromptType.INPUT) {
            this.promptLabel = 'block-editor.extension.ai-image.prompt';
            this.promptTextAreaPlaceholder = 'block-editor.extension.ai-image.custom.placeholder';
        } else {
            this.promptLabel = 'block-editor.extension.ai-image.custom.props';
            this.promptTextAreaPlaceholder = 'block-editor.extension.ai-image.placeholder';
        }
    }

    private toggleFormState(isLoading: boolean): void {
        isLoading ? this.form?.disable() : this.form?.enable();
    }

    /**
     * Updates the form values based on the generated content that comes from
     * the endpoint.
     *
     * @param {SimpleChange} generatedValue - The generated value.
     * @param {boolean} isLoading - The loading status.
     * @return {void}
     */
    private updatedFormValues(generatedValue: SimpleChange, isLoading: boolean): void {
        if (generatedValue?.currentValue && !generatedValue.firstChange && !isLoading) {
            const updatedValue: DotGeneratedAIImage = generatedValue.currentValue;
            this.form.patchValue(updatedValue.request);
            this.form.clearValidators();
            this.form.updateValueAndValidity();

            this.aiProcessedPrompt = updatedValue.response.revised_prompt;
        }
    }

    private setSubmitButtonLabel(isLoading: boolean): void {
        this.submitButtonLabel = isLoading
            ? 'block-editor.extension.ai-image.generating'
            : this.aiProcessedPrompt
            ? 'block-editor.extension.ai-image.regenerate'
            : 'block-editor.extension.ai-image.generate';
    }
}
