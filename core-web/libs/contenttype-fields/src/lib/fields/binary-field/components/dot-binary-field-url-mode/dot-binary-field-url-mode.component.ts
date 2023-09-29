import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
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
    @Input() isLoading = false;
    @Input() error = '';

    @Output() accept: EventEmitter<string> = new EventEmitter<string>();
    @Output() cancel: EventEmitter<void> = new EventEmitter<void>();

    private readonly validators = [
        Validators.required,
        Validators.pattern(/^(ftp|http|https):\/\/[^ "]+$/)
    ];
    readonly form = new FormGroup({
        url: new FormControl('', this.validators)
    });

    get urlControl(): FormControl {
        return this.form.get('url') as FormControl;
    }

    get isInvalid(): boolean {
        return this.urlControl.invalid && !this.isPristine;
    }

    get isPristine(): boolean {
        return !(this.urlControl.dirty || this.urlControl.touched);
    }

    onSubmit(): void {
        if (this.form.invalid) {
            return;
        }

        this.accept.emit(this.form.value.url);
        this.form.reset({ url: this.urlControl.value }); // Reset touch and dirty state
    }
}
