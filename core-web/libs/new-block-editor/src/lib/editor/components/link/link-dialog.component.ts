import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    computed,
    effect,
    inject,
    input,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { Editor } from '@tiptap/core';

import { EditorDialogComponent } from '../editor-dialog/editor-dialog.component';
import { EditorDialogManagerService } from '../../services/editor-dialog-manager.service';

@Component({
    selector: 'dot-link-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, EditorDialogComponent],
    template: `
        <editor-dialog dialogId="link" (opened)="focusHref()">
            <div
                [attr.aria-label]="isEditing() ? 'Edit link' : 'Insert link'"
                class="w-80 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <div
                    class="p-4 flex flex-col gap-3"
                    (keydown.enter)="$event.preventDefault(); onInsert()">
                    <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide m-0">
                        {{ isEditing() ? 'Edit Link' : 'Insert Link' }}
                    </p>

                    <div class="flex flex-col gap-1">
                        <label for="link-url" class="text-xs font-medium text-gray-700">URL</label>
                        <input
                            #hrefInput
                            id="link-url"
                            type="url"
                            [formControl]="form.controls.href"
                            placeholder="https://example.com"
                            class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="link-text" class="text-xs font-medium text-gray-700">
                            Link text
                            <span class="text-gray-400 font-normal">(optional)</span>
                        </label>
                        <input
                            id="link-text"
                            type="text"
                            [formControl]="form.controls.displayText"
                            placeholder="What readers will see"
                            class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <label class="flex items-center gap-2 cursor-pointer" for="open-in-new-tab">
                        <input
                            id="open-in-new-tab"
                            type="checkbox"
                            [formControl]="form.controls.openInNewTab"
                            class="h-4 w-4 shrink-0 rounded border border-gray-300 text-indigo-600 focus:ring-2 focus:ring-indigo-400 focus:outline-none" />
                        <span class="text-sm text-gray-700">Open in new tab</span>
                    </label>

                    <div class="flex justify-end gap-2 pt-1">
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); manager.close()"
                            class="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300">
                            Cancel
                        </button>
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); onInsert()"
                            [disabled]="form.controls.href.invalid"
                            class="rounded bg-indigo-500 px-4 py-1.5 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                            {{ isEditing() ? 'Save' : 'Insert' }}
                        </button>
                    </div>
                </div>
            </div>
        </editor-dialog>
    `
})
export class LinkDialogComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorDialogManagerService);
    private readonly el = inject(ElementRef<HTMLElement>);

    protected readonly isEditing = computed(
        () => this.manager.linkPayload()?.initialValues != null
    );

    readonly form = new FormGroup({
        href: new FormControl<string>('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
        }),
        displayText: new FormControl<string>('', { nonNullable: true }),
        openInNewTab: new FormControl<boolean>(false, { nonNullable: true })
    });

    constructor() {
        // Pre-populate the form when opened in edit mode.
        effect(() => {
            const payload = this.manager.linkPayload();
            untracked(() => {
                const values = payload?.initialValues;
                if (values) {
                    this.form.setValue({
                        href: values.href ?? '',
                        displayText: values.displayText ?? '',
                        openInNewTab: values.target === '_blank'
                    });
                }
            });
        });

        // Reset form when dialog closes.
        effect(() => {
            if (!this.manager.isOpen('link')) {
                untracked(() => this.form.reset({ href: '', displayText: '', openInNewTab: false }));
            }
        });

        // Manage the `link-editing` CSS class on the active link element.
        effect((onCleanup) => {
            if (!this.manager.isOpen('link')) return;
            const linkEl = this.manager.linkPayload()?.linkEl;
            if (!linkEl) return;
            linkEl.classList.add('link-editing');
            onCleanup(() => linkEl.classList.remove('link-editing'));
        });
    }

    protected focusHref(): void {
        const input = this.el.nativeElement.querySelector('#link-url') as HTMLElement | null;
        input?.focus();
    }

    onInsert(): void {
        if (this.form.controls.href.invalid) return;
        const { href, displayText, openInNewTab } = this.form.getRawValue();
        const payload = this.manager.linkPayload();
        const editor = this.editor();

        if (payload?.linkEl) {
            // Edit mode — update the link in place using the pre-computed anchor position.
            const anchorPos =
                payload.anchorPos ??
                (() => {
                    try {
                        return editor.view.posAtDOM(payload.linkEl!, 0);
                    } catch {
                        return editor.state.selection.from;
                    }
                })();
            editor
                .chain()
                .focus()
                .setTextSelection(anchorPos)
                .extendMarkRange('link')
                .insertContent({
                    type: 'text',
                    text: displayText.trim() || href,
                    marks: [
                        {
                            type: 'link',
                            attrs: { href, target: openInNewTab ? '_blank' : null }
                        }
                    ]
                })
                .run();
        } else {
            // Insert mode
            editor
                .chain()
                .focus()
                .insertContent({
                    type: 'text',
                    text: displayText.trim() || href,
                    marks: [
                        {
                            type: 'link',
                            attrs: { href, target: openInNewTab ? '_blank' : null }
                        }
                    ]
                })
                .run();
        }

        this.manager.close();
    }
}
