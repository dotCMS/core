import { of } from 'rxjs';

import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';

import { catchError, switchMap } from 'rxjs/operators';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';

@Component({
    selector: 'dot-ai-image-prompt',
    templateUrl: './ai-image-prompt.component.html',
    styleUrls: ['./ai-image-prompt.component.scss']
})
export class AIImagePromptComponent {
    form = this.fb.group({
        firstInputField: ['', Validators.required],
        secondInputField: ['', Validators.required]
    });

    isFormSubmitting = false;
    showForm1 = false;
    showForm2 = false;

    @Output() formSubmission = new EventEmitter<boolean>();
    @Output() aiResponse = new EventEmitter<string>();

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {}

    onSubmit() {
        this.isFormSubmitting = true;
        const prompt = this.form.value.firstInputField;

        if (prompt) {
            this.aiContentService
                .getAIImage(prompt)
                .pipe(
                    catchError(() => of(null)),
                    switchMap((imageId) => {
                        console.warn('image_imageId', imageId);

                        if (!imageId) {
                            return of(null);
                        }

                        return this.aiContentService.createAndPublishContentlet(imageId);
                    })
                )
                .subscribe((contentlet) => {
                    this.isFormSubmitting = false;
                    this.formSubmission.emit(true);
                    this.aiResponse.emit(contentlet);
                });
        }
    }

    onSecondSubmit() {
        this.isFormSubmitting = true;
        const prompt = this.form.value.secondInputField;

        if (prompt) {
            this.aiContentService.getAIImage(prompt);
        }
    }

    toggleForm1() {
        this.showForm1 = !this.showForm1;
        if (this.showForm1) {
            this.showForm2 = false;
        }
    }

    toggleForm2() {
        this.showForm2 = !this.showForm2;
        if (this.showForm2) {
            this.showForm1 = false;
        }
    }

    cleanForm() {
        this.form.reset();
        this.showForm1 = false;
        this.showForm2 = false;
    }
}
