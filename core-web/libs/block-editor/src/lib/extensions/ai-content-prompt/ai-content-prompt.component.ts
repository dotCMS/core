import { of } from 'rxjs';

import { Component, ElementRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';

import { catchError } from 'rxjs/operators';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';

interface FormValues {
    textPrompt: string;
}

@Component({
    selector: 'dot-ai-content-prompt',
    templateUrl: './ai-content-prompt.component.html',
    styleUrls: ['./ai-content-prompt.component.scss']
})
export class AIContentPromptComponent {
    isFormSubmitting = false;

    @ViewChild('input') input: ElementRef;

    @Output() formValues = new EventEmitter<FormValues>();
    @Output() formSubmission = new EventEmitter<boolean>();
    @Output() aiResponse = new EventEmitter<string>();

    form = new FormGroup({
        textPrompt: new FormControl('')
    });

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {
        this.buildForm();
    }

    onSubmit() {
        this.isFormSubmitting = true;
        const textPrompt = this.form.value.textPrompt;

        if (textPrompt) {
            this.formValues.emit({ textPrompt });

            this.aiContentService
                .getIAContent(textPrompt)
                .pipe(catchError(() => of(null)))
                .subscribe((response) => {
                    this.isFormSubmitting = false;
                    this.formSubmission.emit(true);
                    this.aiResponse.emit(response);
                });
        }
    }

    cleanForm() {
        this.form.reset();
    }

    focusField() {
        this.input.nativeElement.focus();
    }

    private buildForm() {
        this.form = this.fb.group({
            textPrompt: ['', Validators.required]
        });
    }
}
