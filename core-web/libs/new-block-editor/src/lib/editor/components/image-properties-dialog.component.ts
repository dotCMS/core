import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    input,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { EditorDialogComponent } from './editor-dialog.component';

import { DOT_IMAGE_NODE_NAME } from '../extensions/nodes/image.extension';
import { EditorDialogManagerService } from '../services/editor-dialog.service';

/**
 * Caret-anchored dialog that lets the user **edit the properties of an existing
 * `dotImage` node** — URL (`src`), tooltip (`title`), alt text. Opened from the toolbar's
 * "Edit image properties" button when an image is selected; prefilled from
 * `manager.imagePropertiesPayload()`. Insertion is the responsibility of
 * {@link ImageInsertDialogComponent}.
 */
@Component({
    selector: 'dot-image-properties-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, EditorDialogComponent, DotMessagePipe],
    template: `
        <dot-editor-dialog dialogId="image-properties">
            <div
                [attr.aria-label]="'dot.block.editor.dialog.image-properties.aria-label' | dm"
                class="w-[32rem] max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <div
                    class="p-4 flex flex-col gap-3"
                    (keydown.enter)="$event.preventDefault(); onApply()">
                    <div class="flex flex-col gap-1">
                        <label for="edit-img-url" class="text-sm font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.image-properties.field.url.label' | dm }}
                            <span class="text-red-500" aria-hidden="true">*</span>
                        </label>
                        <input
                            id="edit-img-url"
                            type="text"
                            [formControl]="form.controls.src"
                            [placeholder]="
                                'dot.block.editor.dialog.image-properties.field.url.placeholder'
                                    | dm
                            "
                            class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="edit-img-title" class="text-sm font-medium text-gray-700">
                            {{
                                'dot.block.editor.dialog.image-properties.field.tooltip.label' | dm
                            }}
                        </label>
                        <p id="edit-img-title-hint" class="text-xs text-gray-400 -mt-0.5">
                            {{ 'dot.block.editor.dialog.image-properties.field.tooltip.hint' | dm }}
                        </p>
                        <input
                            id="edit-img-title"
                            type="text"
                            [formControl]="form.controls.title"
                            [placeholder]="
                                'dot.block.editor.dialog.image-properties.field.tooltip.placeholder'
                                    | dm
                            "
                            aria-describedby="edit-img-title-hint"
                            class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="edit-img-alt" class="text-sm font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.image-properties.field.alt.label' | dm }}
                        </label>
                        <p id="edit-img-alt-hint" class="text-xs text-gray-400 -mt-0.5">
                            {{ 'dot.block.editor.dialog.image-properties.field.alt.hint' | dm }}
                        </p>
                        <input
                            id="edit-img-alt"
                            type="text"
                            [formControl]="form.controls.alt"
                            [placeholder]="
                                'dot.block.editor.dialog.image-properties.field.alt.placeholder'
                                    | dm
                            "
                            aria-describedby="edit-img-alt-hint"
                            class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <div class="flex justify-end gap-2">
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); manager.close()"
                            class="rounded border border-gray-300 px-4 py-1.5 text-sm text-gray-600 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-300">
                            {{ 'Cancel' | dm }}
                        </button>
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); onApply()"
                            [disabled]="form.controls.src.invalid"
                            class="rounded bg-indigo-500 px-4 py-1.5 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                            {{ 'Save' | dm }}
                        </button>
                    </div>
                </div>
            </div>
        </dot-editor-dialog>
    `
})
export class ImagePropertiesDialogComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorDialogManagerService);

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
