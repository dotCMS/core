import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';

interface NodeProps {
    textPrompt: string;
    textPromptResponse?: string;
}

@Component({
    selector: 'dot-ai-content-prompt',
    templateUrl: './ai-content-prompt.component.html',
    styleUrls: ['./ai-content-prompt.component.css']
})
export class AIContentPromptComponent implements OnInit {
    @ViewChild('input') input: ElementRef;

    @Output() hide: EventEmitter<boolean> = new EventEmitter(false);

    @Input() initialValues: NodeProps = {
        textPrompt: ''
    };

    loading = false;
    form: FormGroup;

    constructor(private fb: FormBuilder, private aiContentService: AiContentService) {}

    ngOnInit() {
        this.form = this.fb.group({
            textPrompt: ''
        });
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

    async submitForm(event?: Event) {
        try {
            if (event) {
                event.preventDefault();
            }

            if (this.form) {
                const textPrompt = this.form.get('textPrompt').value;
                const response = await this.aiContentService.fetchAIContent(textPrompt);

                console.warn('openai response____', response);
                this.hide.emit(true);
            } else {
                console.warn('form is null');
            }
        } catch (error) {
            console.error('error______', error);
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
        this.setFormValue({ ...this.initialValues });
    }
}
