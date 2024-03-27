import { Observable } from 'rxjs';

import { Component, DestroyRef, inject, OnInit, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';

import { filter } from 'rxjs/operators';

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
    private destroyRef = inject(DestroyRef);

    @ViewChild('inputTextarea') private inputTextarea: HTMLTextAreaElement;

    ngOnInit() {
        this.store.showDialog$
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((showDialog) => showDialog)
            )
            .subscribe(() => {
                console.log('focus text area');
                this.inputTextarea.focus();
            });
        this.store.status$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((status) => {
            status === ComponentStatus.LOADING ? this.form.disable() : this.form.enable();
        });
    }

    /**
     *  Handle scape key in the prompt input
     * @param event
     * @return {*}  {void}
     * @memberof AIContentPromptComponent
     */
    handleScape(event: KeyboardEvent): void {
        this.store.setStatus(ComponentStatus.INIT);
        event.stopPropagation();
    }

    /**
     * Clears the error at the store on hiding the confirmation dialog.
     *
     * @return {void}
     */
    onHideConfirm(): void {
        this.store.cleanError();
    }

    onPageChange($event: any) {
        console.log($event);
    }
}
