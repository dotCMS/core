import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { Component, DestroyRef, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SkeletonModule } from 'primeng/skeleton';

import { delay, filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotValidators,
    PrincipalConfiguration
} from '@dotcms/ui';

import { AiContentPromptState, AiContentPromptStore } from './store/ai-content-prompt.store';

interface AIContentForm {
    textPrompt: FormControl<string>;
    generatedText: FormControl<string>;
}

@Component({
    selector: 'dot-ai-content-prompt',
    standalone: true,
    templateUrl: './ai-content-prompt.component.html',
    imports: [
        DialogModule,
        ReactiveFormsModule,
        InputTextareaModule,
        DotMessagePipe,
        ButtonModule,
        SkeletonModule,
        NgIf,
        AsyncPipe,
        DotEmptyContainerComponent,
        ConfirmDialogModule
    ],
    styleUrls: ['./ai-content-prompt.component.scss']
})
export class AIContentPromptComponent implements OnInit {
    store: AiContentPromptStore = inject(AiContentPromptStore);
    vm$: Observable<AiContentPromptState> = this.store.vm$;
    readonly ComponentStatus = ComponentStatus;
    form: FormGroup<AIContentForm> = new FormGroup<AIContentForm>({
        textPrompt: new FormControl('', [Validators.required, DotValidators.noWhitespace]),
        generatedText: new FormControl('')
    });
    confirmationService = inject(ConfirmationService);
    dotMessageService = inject(DotMessageService);
    submitButtonLabel: string;
    private destroyRef = inject(DestroyRef);
    emptyConfiguration: PrincipalConfiguration = {
        title: this.dotMessageService.get('block-editor.extension.ai-content.error'),
        icon: 'pi-exclamation-triangle'
    };

    @ViewChild('inputTextarea') private inputTextarea: ElementRef<HTMLTextAreaElement>;

    ngOnInit() {
        this.setSubscriptions();
    }

    /**
     * Confirmation to close the dialog.
     *
     * @return {void}
     */
    closeDialog(): void {
        this.confirmationService.confirm({
            key: 'ai-image-prompt',
            header: this.dotMessageService.get('block-editor.extension.ai.confirmation.header'),
            message: this.dotMessageService.get('block-editor.extension.ai.confirmation.message'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessageService.get('Discard'),
            rejectLabel: this.dotMessageService.get('Cancel'),
            accept: () => {
                this.store.hideDialog();
            }
        });
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

        // Disable form and set the submit button label based on the status.
        this.store.status$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((status) => {
            this.form[status === ComponentStatus.LOADING ? 'disable' : 'enable']();
        });

        // Set the form content based on the active index
        this.store.activeContent$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
            this.form.patchValue({ textPrompt: data?.prompt, generatedText: data?.content });
        });

        // Set the submit button label
        this.store.submitLabel$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((label) => {
            this.submitButtonLabel = label;
        });
    }
}
