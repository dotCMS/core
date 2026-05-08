import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    input,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DOT_IMAGE_NODE_NAME } from '../../extensions/nodes/image.extension';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

/**
 * Caret-anchored dialog that lets the user **edit the properties of an existing
 * `dotImage` node** — URL (`src`), tooltip (`title`), alt text. Opened from the toolbar's
 * "Edit image properties" button when an image is selected; prefilled from
 * `manager.imagePropertiesPayload()`. Insertion is the responsibility of
 * {@link ImageInsertDialogComponent}.
 */
@Component({
    selector: 'dot-image-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, InputTextModule, EditorPopoverComponent, DotMessagePipe],
    templateUrl: './image-popover.component.html'
})
export class ImagePropertiesPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);

    readonly form = new FormGroup({
        src: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
        title: new FormControl<string>('', { nonNullable: true }),
        alt: new FormControl<string>('', { nonNullable: true })
    });

    constructor() {
        // Prefill the form whenever the dialog opens with a fresh payload.
        effect(() => {
            const values = this.manager.imagePropertiesPayload()?.initialValues;
            if (values) {
                untracked(() => this.form.setValue(values));
            }
        });

        // Reset on close so the next open starts clean.
        effect(() => {
            if (!this.manager.isOpen('image-properties')) {
                untracked(() => this.form.reset({ src: '', title: '', alt: '' }));
            }
        });
    }

    onApply(): void {
        if (this.form.controls.src.invalid) return;
        const { src, title, alt } = this.form.getRawValue();
        this.editor()
            .chain()
            .focus()
            .updateAttributes(DOT_IMAGE_NODE_NAME, {
                src,
                title: title || null,
                alt: alt || null
            })
            .run();
        this.manager.close();
    }
}
