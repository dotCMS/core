import { Observable } from 'rxjs';

import { Component, DestroyRef, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';

import { delay, filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotValidators } from '@dotcms/ui';

import { AiContentPromptState, AiContentPromptStore } from './store/ai-content-prompt.store';

interface AIContentForm {
    textPrompt: FormControl<string>;
}

@Component({
    selector: 'dot-ai-content-prompt',

    templateUrl: './ai-content-prompt.component.html',
    styleUrls: ['./ai-content-prompt.component.scss']
})
export class AIContentPromptComponent implements OnInit {
    store: AiContentPromptStore = inject(AiContentPromptStore);
    vm$: Observable<AiContentPromptState> = this.store.vm$;
    readonly ComponentStatus = ComponentStatus;
    form: FormGroup<AIContentForm> = new FormGroup<AIContentForm>({
        textPrompt: new FormControl('', [Validators.required, DotValidators.noWhitespace])
    });
    confirmationService = inject(ConfirmationService);
    dotMessageService = inject(DotMessageService);
    submitButtonLabel = `block-editor.extension.ai-image.generate`;
    private destroyRef = inject(DestroyRef);

    @ViewChild('inputTextarea') private inputTextarea: ElementRef<HTMLTextAreaElement>;

    ngOnInit() {
        this.setSubscriptions();
    }

    /**
     * Clears the error at the store on hiding the confirmation dialog.
     *
     * @return {void}
     */
    onHideConfirm(): void {
        this.store.cleanError();
    }

    private setSubscriptions(): void {
        // Focus on the input when the dialog is shown and clean form.
        this.store.showDialog$
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((showDialog) => showDialog),
                delay(10) // wait dialog to be visible
            )
            .subscribe(() => {
                this.form.reset();
                this.inputTextarea.nativeElement.focus();
            });

        // Disable the form and change the submit button label when the status is loading
        this.vm$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((state) => {
            if (state.status === ComponentStatus.LOADING) {
                this.form.disable();
                this.submitButtonLabel = 'block-editor.extension.ai-image.generating';
            } else {
                this.form.enable();
                this.submitButtonLabel = state.content
                    ? 'block-editor.extension.ai-image.regenerate'
                    : 'block-editor.extension.ai-image.generate';
            }
        });
    }
}
