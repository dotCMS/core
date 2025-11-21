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
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    AIImagePrompt,
    DotAIImageOrientation,
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
        SelectModule,
        TextareaModule,
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
    promptTextAreaPlaceholder = 'block-editor.extension.ai-image.custom.placeholder';
    promptLabel = 'block-editor.extension.ai-image.prompt';
    submitButtonLabel = 'block-editor.extension.ai-image.generate';
    requiredPrompt = true;
    tooltipText: string = null;
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
    private isUpdatingValidators = false;
    private destroyRef = inject(DestroyRef);

    constructor() {
        this.initForm();
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { value, isLoading } = changes;

        this.updatedFormValues(value, isLoading?.currentValue);

        this.setSubmitButtonLabel(isLoading?.currentValue);

        this.toggleFormState(isLoading?.currentValue && !isLoading.firstChange);
    }

    private initForm(): void {
        this.form = new FormGroup({
            text: new FormControl('', [Validators.required, DotValidators.noWhitespace]),
            type: new FormControl(PromptType.INPUT, Validators.required),
            size: new FormControl(DotAIImageOrientation.HORIZONTAL, Validators.required)
        });

        const typeControl = this.form.get('type');

        this.form.valueChanges
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter(() => !this.isUpdatingValidators)
            )
            .subscribe((value: AIImagePrompt) => this.valueChange.emit(value));

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
}
