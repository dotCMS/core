import { Observable } from 'rxjs';

import { ChangeDetectionStrategy, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { AiContentPromptState, AiContentPromptStore } from './store/ai-content-prompt.store';

interface AIContentForm {
    textPrompt: FormControl<string>;
}

@Component({
    selector: 'dot-ai-content-prompt',
    templateUrl: './ai-content-prompt.component.html',
    styleUrls: ['./ai-content-prompt.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AIContentPromptComponent implements OnInit {
    vm$: Observable<AiContentPromptState> = this.aiContentPromptStore.vm$;

    @ViewChild('input') private input: ElementRef;

    form: FormGroup<AIContentForm> = new FormGroup<AIContentForm>({
        textPrompt: new FormControl('', Validators.required)
    });

    constructor(private readonly aiContentPromptStore: AiContentPromptStore) {}

    ngOnInit() {
        this.aiContentPromptStore.open$.subscribe((open) => {
            open ? this.input.nativeElement.focus() : this.form.reset();
        });
    }

    onSubmit() {
        const textPrompt = this.form.value.textPrompt;

        if (textPrompt) {
            this.aiContentPromptStore.generateContent(textPrompt);
        }
    }
}
