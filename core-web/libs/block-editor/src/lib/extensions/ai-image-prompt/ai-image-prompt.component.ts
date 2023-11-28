import { of } from 'rxjs';

import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';

import { catchError, switchMap } from 'rxjs/operators';

import { DotAiService } from '../../shared/services/dot-ai/dot-ai.service';

@Component({
    selector: 'dot-ai-image-prompt',
    templateUrl: './ai-image-prompt.component.html',
    styleUrls: ['./ai-image-prompt.component.scss']
})
export class AIImagePromptComponent {
    form = this.fb.group({
        promptGenerate: ['', Validators.required],
        promptAutoGenerate: ['', Validators.required]
    });

    isFormSubmitting = false;
    showFormOne = true;
    showFormTwo = false;

    @Output() formSubmission = new EventEmitter<boolean>();
    @Output() aiResponse = new EventEmitter<string>();

    constructor(private fb: FormBuilder, private aiContentService: DotAiService) {}

    onSubmit() {
        this.isFormSubmitting = true;
        const promptGenerate = this.form.value.promptGenerate;
        const promptAutoGenerate = this.form.value.promptAutoGenerate;
        const combinedPrompt = `${promptGenerate} ${promptAutoGenerate}`;

        this.isFormSubmitting = false;
        this.formSubmission.emit(true);

        if (prompt) {
            this.aiContentService
                .generateImage(combinedPrompt)
                .pipe(
                    catchError(() => of(null)),
                    switchMap((imageId) => {
                        if (!imageId) {
                            return of(null);
                        }

                        return this.aiContentService.createAndPublishContentlet(imageId);
                    })
                )
                .subscribe((contentlet) => {
                    this.aiResponse.emit(contentlet);
                });
        }
    }

    openFormOne() {
        this.showFormOne = true;
        this.showFormTwo = false;
    }

    openFormTwo() {
        this.showFormTwo = true;
        this.showFormOne = false;
    }

    cleanForm() {
        this.form.reset();
        this.showFormOne = true;
        this.showFormTwo = false;
    }
}
