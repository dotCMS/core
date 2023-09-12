import { of } from 'rxjs';

import { Component, ElementRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { catchError } from 'rxjs/operators';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';

interface AIContentForm {
    textPrompt: FormControl<string>;
}

@Component({
    selector: 'dot-ai-content-prompt',
    templateUrl: './ai-content-prompt.component.html',
    styleUrls: ['./ai-content-prompt.component.scss']
})
export class AIContentPromptComponent {
    isFormSubmitting = false;

    @ViewChild('input') private input: ElementRef;
    @Output() formSubmission = new EventEmitter<boolean>();

    form: FormGroup<AIContentForm> = new FormGroup<AIContentForm>({
        textPrompt: new FormControl('', Validators.required)
    });

    constructor(private aiContentService: AiContentService) {}

    onSubmit() {
        this.isFormSubmitting = true;
        const textPrompt = this.form.value.textPrompt;

        if (textPrompt) {
            this.aiContentService
                .getIAContent(textPrompt)
                .pipe(catchError(() => of(null)))
                .subscribe(() => {
                    this.isFormSubmitting = false;
                    this.formSubmission.emit(true);
                });
        }
    }

    cleanForm() {
        this.form.reset();
    }

    focusField() {
        this.input.nativeElement.focus();
    }
}
