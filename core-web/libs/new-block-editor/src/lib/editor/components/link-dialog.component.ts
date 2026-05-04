import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { Select } from 'primeng/select';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { EditorDialogComponent } from './editor-dialog.component';

import { EditorDialogManagerService } from '../services/editor-dialog.service';

/** Rel-attribute values exposed in the Advanced section's dropdown. */
const REL_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
    { value: 'noopener noreferrer', label: 'noopener noreferrer' },
    { value: 'noopener', label: 'noopener' },
    { value: 'noreferrer', label: 'noreferrer' },
    { value: 'nofollow', label: 'nofollow' },
    { value: 'sponsored', label: 'sponsored' },
    { value: 'ugc', label: 'ugc' }
];

@Component({
    selector: 'dot-link-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, Select, EditorDialogComponent, DotMessagePipe],
    template: `
        <dot-editor-dialog dialogId="link">
            <div
                [attr.aria-label]="
                    (isEditing()
                        ? 'dot.block.editor.dialog.link.aria-label.edit'
                        : 'dot.block.editor.dialog.link.aria-label.insert'
                    ) | dm
                "
                class="w-80 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <div
                    class="p-4 flex flex-col gap-3"
                    (keydown.enter)="$event.preventDefault(); onInsert()">
                    <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide m-0">
                        {{
                            (isEditing()
                                ? 'dot.block.editor.dialog.link.title.edit'
                                : 'dot.block.editor.dialog.link.title.insert'
                            ) | dm
                        }}
                    </p>

                    <div class="flex flex-col gap-1">
                        <label for="link-url" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.link.field.url.label' | dm }}
                        </label>
                        <input
                            #hrefInput
                            id="link-url"
                            type="url"
                            [formControl]="form.controls.href"
                            [placeholder]="
                                'dot.block.editor.dialog.link.field.url.placeholder' | dm
                            "
                            class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="link-text" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.link.field.text.label' | dm }}
                            <span class="text-gray-400 font-normal">
                                {{ 'dot.block.editor.dialog.link.field.text.optional' | dm }}
                            </span>
                        </label>
                        <input
                            id="link-text"
                            type="text"
                            [formControl]="form.controls.displayText"
                            [placeholder]="
                                'dot.block.editor.dialog.link.field.text.placeholder' | dm
                            "
                            class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <label class="flex items-center gap-2 cursor-pointer" for="open-in-new-tab">
                        <input
                            id="open-in-new-tab"
                            type="checkbox"
                            [formControl]="form.controls.openInNewTab"
                            class="h-4 w-4 shrink-0 rounded border border-gray-300 text-indigo-600 focus:ring-2 focus:ring-indigo-400 focus:outline-none" />
                        <span class="text-sm text-gray-700">
                            {{ 'dot.block.editor.dialog.link.field.new-tab' | dm }}
                        </span>
                    </label>

                    <button
                        type="button"
                        [attr.aria-expanded]="advancedOpen()"
                        aria-controls="link-advanced-section"
                        data-testid="link-advanced-toggle"
                        (mousedown)="$event.preventDefault(); toggleAdvanced()"
                        class="flex items-center gap-1 self-start rounded text-xs font-medium text-gray-600 hover:text-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-300">
                        <span>{{ 'dot.block.editor.dialog.link.advanced' | dm }}</span>
                        <span aria-hidden="true" class="material-symbols-outlined text-base">
                            {{ advancedOpen() ? 'expand_less' : 'expand_more' }}
                        </span>
                    </button>

                    @if (advancedOpen()) {
                        <div id="link-advanced-section" class="flex flex-col gap-3">
                            <div class="flex flex-col gap-1">
                                <label for="link-title" class="text-xs font-medium text-gray-700">
                                    {{ 'dot.block.editor.dialog.link.field.title.label' | dm }}
                                </label>
                                <input
                                    id="link-title"
                                    type="text"
                                    [formControl]="form.controls.title"
                                    [placeholder]="
                                        'dot.block.editor.dialog.link.field.title.placeholder' | dm
                                    "
                                    class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                            </div>

                            <div class="flex flex-col gap-1">
                                <label
                                    for="link-aria-label"
                                    class="text-xs font-medium text-gray-700">
                                    {{ 'dot.block.editor.dialog.link.field.aria-label.label' | dm }}
                                </label>
                                <input
                                    id="link-aria-label"
                                    type="text"
                                    [formControl]="form.controls.ariaLabel"
                                    [placeholder]="
                                        'dot.block.editor.dialog.link.field.aria-label.placeholder'
                                            | dm
                                    "
                                    class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                            </div>

                            <div class="flex flex-col gap-1">
                                <label for="link-rel" class="text-xs font-medium text-gray-700">
                                    {{ 'dot.block.editor.dialog.link.field.rel.label' | dm }}
                                </label>
                                <p-select
                                    inputId="link-rel"
                                    appendTo="body"
                                    [size]="'small'"
                                    [showClear]="true"
                                    [options]="relOptions"
                                    optionLabel="label"
                                    optionValue="value"
                                    [placeholder]="
                                        'dot.block.editor.dialog.link.field.rel.placeholder' | dm
                                    "
                                    [formControl]="form.controls.rel"
                                    [pt]="selectPt" />
                            </div>
                        </div>
                    }

                    <div class="flex justify-end gap-2 pt-1">
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); manager.close()"
                            class="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300">
                            {{ 'Cancel' | dm }}
                        </button>
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); onInsert()"
                            [disabled]="form.controls.href.invalid"
                            class="rounded bg-indigo-500 px-4 py-1.5 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                            {{ (isEditing() ? 'Save' : 'Insert') | dm }}
                        </button>
                    </div>
                </div>
            </div>
        </dot-editor-dialog>
    `
})
export class LinkDialogComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorDialogManagerService);

    protected readonly relOptions = REL_OPTIONS;

    /**
     * PrimeNG passthrough config for the rel `<p-select>`. Mirrors the toolbar's block-type
     * select so the two dropdowns stay visually consistent. Extract to a shared util when
     * a third caller appears; not now (premature abstraction).
     */
    protected readonly selectPt = {
        root: 'bg-white border border-indigo-200 rounded-md text-sm text-indigo-900 hover:border-indigo-300 transition-colors',
        label: '!text-indigo-900',
        dropdown: 'w-7 text-indigo-500',
        panel: 'bg-white border border-indigo-200 rounded-md shadow-lg mt-1',
        list: 'p-1',
        item: 'px-3 py-1.5 text-sm text-slate-700 rounded hover:bg-indigo-50 hover:text-indigo-700 aria-selected:bg-indigo-600 aria-selected:text-white'
    };

    protected readonly isEditing = computed(
        () => this.manager.linkPayload()?.initialValues != null
    );

    /**
     * Tracks whether the Advanced (Title / Aria Label / Rel) section is visible.
     * Auto-expands when an existing link's payload carries any of those values, so the
     * user immediately sees what they've set without an extra click.
     */
    protected readonly advancedOpen = signal(false);

    readonly form = new FormGroup({
        href: new FormControl<string>('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
        }),
        displayText: new FormControl<string>('', { nonNullable: true }),
        openInNewTab: new FormControl<boolean>(false, { nonNullable: true }),
        title: new FormControl<string>('', { nonNullable: true }),
        ariaLabel: new FormControl<string>('', { nonNullable: true }),
        // Nullable: PrimeNG <p-select> with [showClear] resets the value to null on clear.
        rel: new FormControl<string | null>('')
    });

    protected toggleAdvanced(): void {
        this.advancedOpen.update((v) => !v);
    }

    constructor() {
        // Pre-populate the form when opened in edit mode.
        effect(() => {
            const payload = this.manager.linkPayload();
            untracked(() => {
                const values = payload?.initialValues;
                if (values) {
                    const title = values.title ?? '';
                    const ariaLabel = values.ariaLabel ?? '';
                    const rel = values.rel ?? '';
                    this.form.setValue({
                        href: values.href ?? '',
                        displayText: values.displayText ?? '',
                        openInNewTab: values.target === '_blank',
                        title,
                        ariaLabel,
                        rel
                    });
                    // If any advanced field is populated, surface the section so the user
                    // can see what they previously set without hunting for the toggle.
                    this.advancedOpen.set(!!(title || ariaLabel || rel));
                }
            });
        });

        // Reset form when dialog closes.
        effect(() => {
            if (!this.manager.isOpen('link')) {
                untracked(() => {
                    this.form.reset({
                        href: '',
                        displayText: '',
                        openInNewTab: false,
                        title: '',
                        ariaLabel: '',
                        rel: ''
                    });
                    this.advancedOpen.set(false);
                });
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

    onInsert(): void {
        if (this.form.controls.href.invalid) return;
        const { href, displayText, openInNewTab, title, ariaLabel, rel } = this.form.getRawValue();
        const payload = this.manager.linkPayload();
        const editor = this.editor();

        // Empty strings → null so renderHTML on the link mark omits the attribute and the
        // global `Link.HTMLAttributes.rel` default applies for `rel`.
        const linkAttrs = {
            href,
            target: openInNewTab ? '_blank' : null,
            title: title.trim() || null,
            'aria-label': ariaLabel.trim() || null,
            rel: (rel ?? '').trim() || null
        };

        if (payload?.linkEl) {
            // Edit mode — update the link in place using the pre-computed anchor position.
            const linkEl = payload.linkEl;
            const anchorPos =
                payload.anchorPos ??
                (() => {
                    try {
                        return editor.view.posAtDOM(linkEl, 0);
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
                    marks: [{ type: 'link', attrs: linkAttrs }]
                })
                .run();
        } else {
            editor
                .chain()
                .focus()
                .insertContent({
                    type: 'text',
                    text: displayText.trim() || href,
                    marks: [{ type: 'link', attrs: linkAttrs }]
                })
                .run();
        }

        this.manager.close();
    }
}
