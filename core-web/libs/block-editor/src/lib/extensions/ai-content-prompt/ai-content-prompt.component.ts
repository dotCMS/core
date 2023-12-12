import { Observable, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { filter, takeUntil } from 'rxjs/operators';

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
export class AIContentPromptComponent implements OnInit, OnDestroy {
    vm$: Observable<AiContentPromptState> = this.aiContentPromptStore.vm$;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    @ViewChild('input') private input: ElementRef;

    form: FormGroup<AIContentForm> = new FormGroup<AIContentForm>({
        textPrompt: new FormControl('', Validators.required)
    });

    constructor(private readonly aiContentPromptStore: AiContentPromptStore) {}

    ngOnInit() {
        this.aiContentPromptStore.status$
            .pipe(
                takeUntil(this.destroy$),
                filter((status) => status === 'open')
            )
            .subscribe(() => {
                this.form.reset();
                this.input.nativeElement.focus();
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
    /**
     *  Handle submit event in the form
     * @return {*}  {void}
     * @memberof AIContentPromptComponent
     */
    onSubmit() {
        const textPrompt = this.form.value.textPrompt;
        if (textPrompt) {
            this.aiContentPromptStore.generateContent(textPrompt);
        }
    }

    /**
     *  Handle scape key in the prompt input
     * @param event
     * @return {*}  {void}
     * @memberof AIContentPromptComponent
     */
    handleScape(event: KeyboardEvent): void {
        this.aiContentPromptStore.setStatus('exit');
        event.stopPropagation();
    }
}
