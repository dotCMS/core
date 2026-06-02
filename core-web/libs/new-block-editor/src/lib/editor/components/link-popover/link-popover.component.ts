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

import { InputTextModule } from 'primeng/inputtext';
import { Select } from 'primeng/select';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { LINK_SELECTION_KEY } from '../../extensions/selection-preserve.extension';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { linkHrefValidator } from '../../utils/url.utils';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

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
    selector: 'dot-link-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, InputTextModule, Select, EditorPopoverComponent, DotMessagePipe],
    templateUrl: './link-popover.component.html'
})
export class LinkPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);

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
            validators: [Validators.required, linkHrefValidator]
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

        // Insert mode (no `linkEl`): once the URL input takes focus the browser stops
        // painting the editor's native selection, leaving the author with no hint of
        // which text will become the link. Paint the exact range with a ProseMirror
        // decoration that survives the blur; clear it when the popover closes.
        effect((onCleanup) => {
            if (!this.manager.isOpen('link')) return;
            if (this.manager.linkPayload()?.linkEl) return;
            const view = this.editor().view;
            view.dispatch(view.state.tr.setMeta(LINK_SELECTION_KEY, { active: true }));
            onCleanup(() =>
                view.dispatch(view.state.tr.setMeta(LINK_SELECTION_KEY, { active: false }))
            );
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
