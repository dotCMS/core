import { of } from 'rxjs';

import { Component, ElementRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

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
    @Output() hide = new EventEmitter<boolean>();
    @Output() aiResponse = new EventEmitter<string>();

    loading = false;
    form: FormGroup;

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {
        this.buildForm();
    }

    onSubmit() {
        const textPrompt = this.form.value.textPrompt;

        if (textPrompt) {
            this.formValues.emit({ textPrompt });

            this.hide.emit(true);

            this.aiContentService
                .getIAContent(textPrompt)
                .pipe(catchError(() => of(null)))
                .subscribe((response) => {
                    this.aiResponse.emit(response);
                });
        }
    }

    buildForm() {
        this.form = this.fb.group({
            textPrompt: ['', Validators.required]
        });
    }

    setFormValue({ textPrompt = '' }) {
        this.form.patchValue({ textPrompt }, { emitEvent: false });
    }

    focusInput() {
        this.input.nativeElement.focus();
    }

    cleanForm() {
        this.form.reset();
    }
}
