import { NgIf } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { AIImagePrompt, DotGeneratedAIImage } from '../../../../shared/services/dot-ai/dot-ai.models';


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
        ButtonModule,
        NgIf,
        InputTextareaModule,
        DotFieldRequiredDirective
    ],
    styleUrls: ['./ai-image-prompt-form.component.scss']
})
export class AiImagePromptFormComponent implements OnChanges, OnInit {


    @Input()
    aiProcessedPrompt: string;

    @Input()
    orientationOptions: SelectItem<string>[]

    @Input()
    generatedValue: DotGeneratedAIImage;

    @Input()
    isLoading = false;


    @Output()
    value = new EventEmitter<AIImagePrompt>;



    form: FormGroup;


    ngOnInit(): void {
        this.initForm();

    }

    submitForm(): void {
        this.value.emit(this.form.value);
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { generatedValue, isLoading } = changes;
        if (generatedValue?.currentValue) {
            this.form.patchValue(generatedValue.currentValue.request);
            this.form.clearValidators();
            this.form.updateValueAndValidity();
        }

        if (!isLoading.firstChange) {
            isLoading.currentValue ? this.form.disable() : this.form.enable();
        }
    }


    private initForm(): void {
        this.form = new FormGroup({
            text: new FormControl('', Validators.required),
            type: new FormControl('input', Validators.required),
            size: new FormControl('1792x1024', Validators.required)
        });
    }

}