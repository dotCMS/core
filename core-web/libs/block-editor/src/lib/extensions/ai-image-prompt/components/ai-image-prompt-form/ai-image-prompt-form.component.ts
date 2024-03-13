import { NgIf } from '@angular/common';
import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    OnInit,
    Output,
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

    /**
     * Submits the form and emits the form value.
     * @return {void}
     * @memberof AiImagePromptFormComponent
     */
    submitForm(): void {
        this.value.emit(this.form.value);
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { generatedValue, isLoading } = changes;
        if (
            generatedValue?.currentValue &&
            !generatedValue.firstChange &&
            !isLoading?.currentValue
        ) {
            const updatedValue: DotGeneratedAIImage = generatedValue.currentValue;
            this.form.patchValue(updatedValue.request);
            this.form.clearValidators();
            this.form.updateValueAndValidity();

            this.aiProcessedPrompt = updatedValue.response.revised_prompt;
        }

        if (isLoading && !isLoading.firstChange) {
            isLoading.currentValue ? this.form.disable() : this.form.enable();
        }
    }

    private initForm(): void {
        this.form = new FormGroup({
            text: new FormControl('', Validators.required),
            type: new FormControl('input', Validators.required),
            size: new FormControl('1792x1024', Validators.required)
        });

        const sizeControl = this.form.get('size');
        const typeControl = this.form.get('type');

        sizeControl.valueChanges.pipe(takeUntilDestroyed()).subscribe((size) => {
            this.orientation.emit(size);
        });

        typeControl.valueChanges.pipe(takeUntilDestroyed()).subscribe((type) => {
            const promptControl = this.form.get('text');
            type === 'auto'
                ? promptControl.clearValidators()
                : promptControl.setValidators(Validators.required);

            promptControl.updateValueAndValidity();
        });
    }
}
