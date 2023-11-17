import { Observable, Subject } from 'rxjs';

import { Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { AiContentPromptState, AiContentPromptStore } from './store/ai-content-prompt.store';

interface AIContentForm {
    textPrompt: FormControl<string>;
}

@Component({
    selector: 'dot-ai-content-prompt',
    templateUrl: './ai-content-prompt.component.html',
    styleUrls: ['./ai-content-prompt.component.scss']
})
export class AIContentPromptComponent implements OnDestroy {
    vm$: Observable<AiContentPromptState> = this.aiContentPromptStore.vm$;

    @ViewChild('input') private input: ElementRef;

    private destroy$ = new Subject<boolean>();

    form: FormGroup<AIContentForm> = new FormGroup<AIContentForm>({
        textPrompt: new FormControl('', Validators.required)
    });

    constructor(private readonly aiContentPromptStore: AiContentPromptStore) {}

    ngOnDestroy() {
        this.destroy$.next(true);
    }

    get store() {
        return this.aiContentPromptStore;
    }

    onSubmit() {
        const textPrompt = this.form.value.textPrompt;

        if (textPrompt) {
            this.aiContentPromptStore.generateContent(textPrompt);
        }
    }

    cleanForm() {
        this.form.reset();
    }

    focusField() {
        this.input.nativeElement.focus();
    }
}
