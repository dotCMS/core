import { of } from 'rxjs';

import { Component, ElementRef, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';

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
export class AIContentPromptComponent implements OnInit {
    @ViewChild('textPrompt') input: ElementRef;

    @Output() formValues = new EventEmitter<FormValues>();
    @Output() hide = new EventEmitter<boolean>();

    loading = false;
    form: FormGroup<{
        textPrompt: FormControl<string | null>;
    }>;

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {}

    ngOnInit() {
        this.buildForm();
    }

    /**
     * Build FormGroup
     * @memberof AIContentPromptComponent
     * @param {NodeProps} [props]
     */
    buildForm() {
        this.form = this.fb.group({
            textPrompt: ['', Validators.required]
        });
    }

    onSubmit() {
        const textPrompt = this.form.get('textPrompt').value;

        console.info('this.form.get()', this.form.get('textPrompt'));

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

    /**
     * Set Form values without emit `valueChanges` event.
     *
     * @memberof AIContentPromptComponent
     */
    setFormValue({ textPrompt = '' }) {
        this.form.patchValue({ textPrompt }, { emitEvent: false });
    }

    /**
     * Set Focus prompt Input
     *
     * @memberof AIContentPromptComponent
     */
    focusInput() {
        this.input.nativeElement.focus();
    }

    /**
     * Listen Key events on search input
     *
     * @param {KeyboardEvent} e
     * @return {*}
     * @memberof AIContentPromptComponent
     */
    onKeyDownEvent(e: KeyboardEvent) {
        e.stopImmediatePropagation();

        if (e.key === 'Escape') {
            this.hide.emit(true);

            return true;
        }
    }

    /**
     * Reset form value to initials
     *
     * @memberof AIContentPromptComponent
     */
    resetForm() {
        this.setFormValue({ textPrompt: '' });
    }

    cleanForm() {
        this.form = null;
    }
}
