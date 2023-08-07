import { of } from 'rxjs';

import { Component, ElementRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { catchError } from 'rxjs/operators';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';
import { DynamicControl } from '../bubble-form/model/index';

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

    loading = false;
    form: FormGroup;

    dynamicControls: DynamicControl<unknown>[] = [];

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {}

    onSubmit() {
        const textPrompt = this.form.value.textPrompt;

        if (textPrompt) {
            this.formValues.emit({ textPrompt });

            this.aiContentService
                .getIAContent(textPrompt)
                .pipe(
                    catchError((error) => {
                        console.warn('error', error);

                        return of(null);
                    })
                )
                .subscribe((response) => {
                    console.warn('openai response____', response);
                    this.hide.emit(true);
                });
        }
    }

    setFormValue({ textPrompt = '' }) {
        this.form.patchValue({ textPrompt }, { emitEvent: false });
    }

    focusInput() {
        this.input.nativeElement.focus();
    }

    buildForm(controls: DynamicControl<unknown>[]) {
        this.dynamicControls = controls;
        this.form = this.fb.group({});
        this.dynamicControls.forEach((control) => {
            this.form.addControl(
                control.key as keyof FormValues,
                this.fb.control(control.value || null, control.required ? Validators.required : [])
            );
        });
    }

    cleanForm() {
        this.form = null;
    }
}
