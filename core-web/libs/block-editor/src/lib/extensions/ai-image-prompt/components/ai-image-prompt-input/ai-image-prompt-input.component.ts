import { AsyncPipe, NgIf, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotAutofocusDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { PromptType } from '../../ai-image-prompt.models';

//TODO: make this component more flexible is we need more PromptType
//TODO: disable in auto if you dont have a prompt and content in BlockEditor
@Component({
    selector: 'dot-ai-image-prompt-input',
    standalone: true,
    templateUrl: './ai-image-prompt-input.component.html',
    styleUrls: ['./ai-image-prompt-input.component.scss'],
    imports: [
        ButtonModule,
        NgIf,
        ReactiveFormsModule,
        TooltipModule,
        NgTemplateOutlet,
        InputTextareaModule,
        DotMessagePipe,
        AsyncPipe,
        DotAutofocusDirective,
        AutoFocusModule,
        DotFieldValidationMessageComponent
    ],

    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiImagePromptInputComponent implements OnChanges {
    isSelected = false;

    @Input()
    placeholder: string;

    @Input()
    isLoading: boolean;

    @Input()
    type: PromptType;

    @Output()
    promptChanged = new EventEmitter<string>();

    form = inject(FormBuilder).group({
        prompt: ['', Validators.required]
    });

    @Input()
    set selected(isSelected: boolean) {
        this.isSelected = isSelected;
        this.resetForm();
    }

    get promptControl(): FormControl {
        return this.form.get('prompt') as FormControl;
    }

    /**
     * Emit the prompt
     *
     * @return {void}
     */
    generateImage(): void {
        if (this.form.valid) {
            this.promptChanged.emit(this.promptControl.value);
            this.disableForm();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { type } = changes;
        if (type && type.currentValue === 'auto') {
            this.promptControl.clearValidators();
            this.form.updateValueAndValidity();
        }
    }

    private resetForm() {
        this.form.enable();
        this.form.reset();
    }

    private disableForm() {
        this.form.disable();
    }
}
