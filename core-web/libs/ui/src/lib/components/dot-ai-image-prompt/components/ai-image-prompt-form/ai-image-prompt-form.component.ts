import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    Input,
    OnChanges,
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
import { TooltipModule } from 'primeng/tooltip';

import { filter, take } from 'rxjs/operators';

import { DotAiService, DotAIImageSizeMapperService, DotMessageService } from '@dotcms/data-access';
import {
    AIImagePrompt,
    DotAIImageOrientation,
    DotAISimpleModel,
    DotGeneratedAIImage,
    PromptType
} from '@dotcms/dotcms-models';

import { DotCopyButtonComponent } from './../../../../components/dot-copy-button/dot-copy-button.component';
import { DotFieldRequiredDirective } from './../../../../dot-field-required/dot-field-required.directive';
import { DotMessagePipe } from './../../../../dot-message/dot-message.pipe';
import { DotValidators } from './../../../../validators/dotValidators';

@Component({
    selector: 'dot-ai-image-prompt-form',
    templateUrl: './ai-image-prompt-form.component.html',
    imports: [
        ButtonModule,
        AccordionModule,
        TooltipModule,
        RadioButtonModule,
        ReactiveFormsModule,
        FormsModule,
        DropdownModule,
        InputTextareaModule,
        DotFieldRequiredDirective,
        DotMessagePipe,
        DotCopyButtonComponent
    ],
    styleUrls: ['./ai-image-prompt-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiImagePromptFormComponent implements OnChanges {
    /**
     * The value of the generated AI image.
     */
    @Input()
    value: DotGeneratedAIImage;

    @Input()
    isLoading = false;

    @Input()
    hasEditorContent = true;

    /**
     * An event that is emitted when the value of form change.
     */
    @Output()
    valueChange = new EventEmitter<AIImagePrompt>();

    /**
     * An event that is emitted when the generate action to create a new image is triggered.
     */
    @Output()
    generate = new EventEmitter<void>();

    form: FormGroup;
    aiProcessedPrompt: string;
    dotMessageService = inject(DotMessageService);
    dotAiService = inject(DotAiService);
    dotAIImageSizeMapper = inject(DotAIImageSizeMapperService);
    promptTextAreaPlaceholder = 'block-editor.extension.ai-image.custom.placeholder';
    promptLabel = 'block-editor.extension.ai-image.prompt';
    submitButtonLabel = 'block-editor.extension.ai-image.generate';
    requiredPrompt = true;
    tooltipText: string = null;
    orientationOptions: SelectItem<DotAIImageOrientation>[] = [
        {
            value: DotAIImageOrientation.SQUARE,
            label: this.dotMessageService.get('block-editor.extension.ai-image.orientation.square')
        },
        {
            value: DotAIImageOrientation.LANDSCAPE,
            label: this.dotMessageService.get(
                'block-editor.extension.ai-image.orientation.landscape'
            )
        },
        {
            value: DotAIImageOrientation.PORTRAIT,
            label: this.dotMessageService.get(
                'block-editor.extension.ai-image.orientation.portrait'
            )
        }
    ];
    private currentImageModel = 'dall-e-3'; // Default fallback
    private isUpdatingValidators = false;
    private destroyRef = inject(DestroyRef);

    constructor() {
        this.initForm();
        this.loadImageModelConfig();
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { value, isLoading } = changes;

        this.updatedFormValues(value, isLoading?.currentValue);

        this.setSubmitButtonLabel(isLoading?.currentValue);

        this.toggleFormState(isLoading?.currentValue && !isLoading.firstChange);
    }

    private initForm(): void {
        // Initialize with default orientation (square) and get its size
        const defaultOrientation = DotAIImageOrientation.SQUARE;
        const defaultSize = this.dotAIImageSizeMapper.getSizeForOrientation(
            this.currentImageModel,
            defaultOrientation
        );

        this.form = new FormGroup({
            text: new FormControl('', [Validators.required, DotValidators.noWhitespace]),
            type: new FormControl(PromptType.INPUT, Validators.required),
            orientation: new FormControl(defaultOrientation, Validators.required),
            size: new FormControl(defaultSize, Validators.required)
        });

        const typeControl = this.form.get('type');
        const orientationControl = this.form.get('orientation');
        const sizeControl = this.form.get('size');

        // When orientation changes, update the size based on current model
        orientationControl.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((orientation: DotAIImageOrientation) => {
                const newSize = this.dotAIImageSizeMapper.getSizeForOrientation(
                    this.currentImageModel,
                    orientation
                );
                sizeControl.setValue(newSize, { emitEvent: false });
            });

        this.form.valueChanges
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter(() => !this.isUpdatingValidators)
            )
            .subscribe((value) => {
                // Emit the prompt with size (pixel dimensions) for the backend
                const prompt: AIImagePrompt = {
                    text: value.text,
                    type: value.type,
                    size: value.size // This is the actual pixel dimension string
                };
                this.valueChange.emit(prompt);
            });

        typeControl.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((type) => this.updatePromptControl(type));
    }

    private updatePromptControl(type: PromptType): void {
        this.isUpdatingValidators = true;
        const promptControl = this.form.get('text');
        const isInputType = type === PromptType.INPUT;
        promptControl.setValidators(isInputType ? Validators.required : null);
        promptControl.updateValueAndValidity();
        this.setPromptLabels(isInputType);
        this.isUpdatingValidators = false;
        this.requiredPrompt = isInputType;
    }

    private setPromptLabels(isInputType: boolean): void {
        if (isInputType) {
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
     * @param {SimpleChange} value - value that includes the request and response.
     * @param {boolean} isLoading - The loading status.
     * @return {void}
     */
    private updatedFormValues(value: SimpleChange, isLoading: boolean): void {
        if (value?.currentValue && !value.firstChange && !isLoading) {
            const updatedValue: DotGeneratedAIImage = value.currentValue;
            this.form.patchValue(updatedValue.request);
            this.form.clearValidators();
            this.form.updateValueAndValidity();

            this.aiProcessedPrompt = updatedValue.response?.revised_prompt;
        }
    }

    private setSubmitButtonLabel(isLoading: boolean): void {
        this.submitButtonLabel = isLoading
            ? 'block-editor.extension.ai-image.generating'
            : this.aiProcessedPrompt || this.value?.error
              ? 'block-editor.extension.ai-image.regenerate'
              : 'block-editor.extension.ai-image.generate';
    }

    /**
     * Loads the current image model configuration from the AI service.
     *
     * Resolution order mirrors the text model approach used in CompletionsResource:
     * 1. Find the model in availableModels where type === 'IMAGE' and current === true
     *    (this is the model at currentModelIndex, which defaults to the first in the list)
     * 2. Fall back to the first name in imageModelNames (comma-separated)
     *
     * After resolving, updates the form's size control so the store receives the
     * correct model-specific pixel dimensions.
     */
    private loadImageModelConfig(): void {
        this.dotAiService
            .getConfig()
            .pipe(take(1))
            .subscribe({
                next: (config) => {
                    const resolvedModel = this.resolveCurrentImageModel(
                        config?.availableModels,
                        config?.imageModelNames
                    );
                    if (resolvedModel) {
                        this.currentImageModel = resolvedModel;
                        const orientationControl = this.form?.get('orientation');
                        const sizeControl = this.form?.get('size');
                        if (orientationControl && sizeControl) {
                            const newSize = this.dotAIImageSizeMapper.getSizeForOrientation(
                                this.currentImageModel,
                                orientationControl.value
                            );
                            sizeControl.setValue(newSize);
                        }
                    }
                },
                error: (error) => {
                    console.error('Error loading AI config:', error);
                    // Keep using the default model (dall-e-3)
                }
            });
    }

    /**
     * Resolves the current image model name from the config response.
     * Prefers the availableModels list (uses the current flag set by the backend),
     * falling back to the first model in the imageModelNames comma-separated string.
     */
    private resolveCurrentImageModel(
        availableModels: DotAISimpleModel[],
        imageModelNames: string
    ): string | null {
        const fromAvailable = availableModels?.find(
            (m) => m.type === 'IMAGE' && m.current
        )?.name;
        if (fromAvailable) {
            return fromAvailable;
        }

        return imageModelNames?.split(',')[0]?.trim() ?? null;
    }
}
