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
    @ViewChild('input') input: ElementRef;

    @Output() formValues = new EventEmitter<FormValues>();
    @Output() formSubmission = new EventEmitter<boolean>();

    form = new FormGroup({
        textPrompt: new FormControl('')
    });

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {
        this.buildForm();
    }

    onSubmit() {
        const textPrompt = this.form.value.textPrompt;

        if (textPrompt) {
            this.formValues.emit({ textPrompt });

            this.formSubmission.emit(true);

            this.aiContentService
                .getIAContent(textPrompt)
                .pipe(catchError(() => of(null)))
                .subscribe((response) => {
                    console.warn('AI response', response);
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
