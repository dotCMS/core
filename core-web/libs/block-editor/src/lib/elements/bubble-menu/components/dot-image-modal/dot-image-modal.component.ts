import { Component, input } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { EditorModalDirective } from '../../../../directive/editor-modal.directive';

@Component({
    selector: 'dot-image-modal',
    templateUrl: './dot-image-modal.component.html',
    styleUrls: ['./dot-image-modal.component.scss'],
    standalone: true,
    hostDirectives: [
        {
            directive: EditorModalDirective,
            inputs: ['editor', 'appendTo']
        }
    ],
    imports: [InputTextModule, ReactiveFormsModule, ButtonModule]
})
export class DotImageModalComponent {
    readonly appendTo = input<HTMLElement>();

    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            path: ['', Validators.required],
            alt: [''],
            caption: ['']
        });
    }

    onApply() {
        if (this.form.valid) {
            // TODO: handle apply logic
            // e.g., emit event or call service
        }
    }

    onCancel() {
        // TODO: handle cancel logic
        // e.g., close modal or reset form
        this.form.reset();
    }
}
