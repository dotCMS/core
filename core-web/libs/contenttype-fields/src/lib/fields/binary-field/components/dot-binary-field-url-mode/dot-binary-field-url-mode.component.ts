import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import {
    FormGroup,
    FormControl,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-dot-binary-field-url-mode',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        DotMessagePipe
    ],
    templateUrl: './dot-binary-field-url-mode.component.html',
    styleUrls: ['./dot-binary-field-url-mode.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUrlModeComponent {
    @Output() accept: EventEmitter<string> = new EventEmitter<string>();
    @Output() cancel: EventEmitter<void> = new EventEmitter<void>();

    private readonly validators = [
        Validators.required,
        Validators.pattern(/^(ftp|http|https):\/\/[^ "]+$/)
    ];
    readonly form = new FormGroup({
        url: new FormControl('', this.validators)
    });

    get isInvalid(): boolean {
        const ngControl = this.form.get('url');

        return ngControl.invalid && (ngControl.dirty || ngControl.touched);
    }

    onSubmit(): void {
        if (this.form.valid) {
            this.accept.emit(this.form.value.url);
        }
    }
}
