import { Component, inject, input, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { Editor } from '@tiptap/core';

import { EditorModalDirective } from '../../../../directive/editor-modal.directive';

@Component({
    selector: 'dot-image-modal',
    templateUrl: './dot-image-modal.component.html',
    styleUrls: ['./dot-image-modal.component.scss'],
    standalone: true,
    imports: [EditorModalDirective, InputTextModule, ReactiveFormsModule, ButtonModule]
})
export class DotImageModalComponent {
    readonly editor = input.required<Editor>();
    readonly appendTo = input<HTMLElement>();
    @ViewChild('imageModal', { read: EditorModalDirective }) editorModal: EditorModalDirective;

    private readonly fb = inject(FormBuilder);

    form = this.fb.group({
        path: ['', Validators.required],
        alt: [''],
        title: ['']
    });

    onShowFn = this.onShow.bind(this);

    onApply() {
        if (!this.form.valid) {
            return;
        }

        this.editor()
            .chain()
            .focus()
            .updateAttributes('dotImage', {
                src: this.form.value.path,
                alt: this.form.value.alt,
                title: this.form.value.title
            })
            .run();

        this.editorModal?.hide();
    }

    onCancel() {
        this.editorModal?.hide();
    }

    toggle() {
        this.editorModal?.toggle();
    }

    onShow() {
        const { alt, src, title, data } = this.editor().getAttributes('dotImage');
        const { title: dotTitle = '', asset } = data || {};
        this.form.patchValue({
            path: src || asset,
            alt: alt || dotTitle,
            title: title || dotTitle
        });
    }
}
