import { Component, ElementRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';

@Component({
    selector: 'dot-ai-content-prompt',
    templateUrl: './ai-content-prompt.component.html',
    styleUrls: ['./ai-content-prompt.component.scss']
})
export class AIContentPromptComponent {
    @ViewChild('input') input: ElementRef;

    @Output() formValues = new EventEmitter();
    @Output() hide = new EventEmitter<boolean>();

    loading = false;
    form: FormGroup;

    get getPrompt() {
        return this.form.get('textPrompt').value;
    }

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {}

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

    async onSubmit() {
        try {
            this.formValues.emit({ ...this.form.value });

            const textPrompt = this.form.get('textPrompt').value;
            const response = await this.aiContentService.fetchAIContent(textPrompt);

            console.warn('openai response____', response);
            this.hide.emit(true);
        } catch (error) {
            console.warn('error', error);
        }
    }

    /**
     * Set Form values without emit `valueChanges` event.
     *
     * @memberof AIContentPromptComponent
     */
    setFormValue({ textPrompt = '' }) {
        this.form.setValue({ textPrompt }, { emitEvent: false });
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
