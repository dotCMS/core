import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    input,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

const EMPTY_VALUES = {
    caption: '',
    ariaLabel: '',
    ariaLabelledby: ''
};

/**
 * Toolbar-anchored a11y popover for the active table. Edits:
 *
 *   - **Caption** — sets the table's `caption` attribute. Always editable; a non-empty value
 *     stores the caption, an empty value clears it. NOTE: the value is persisted on the table
 *     node but is not yet rendered as a visible `<caption>` element in the editor (the table uses
 *     prosemirror's resizable `TableView`, which would need to emit the caption). Tracked
 *     separately — see issue #35980 follow-up.
 *   - **aria-label** — accessible name for the `<table>`.
 *   - **aria-labelledby** — references an `id` of an external label.
 *
 * Phase 3: stripped down from the prior `TableActionsPopover` — merge / split / delete-table
 * were dropped as out-of-scope for the a11y ticket. Opened from the new `table_edit` toolbar
 * button only when the cursor is inside a table.
 */
@Component({
    selector: 'dot-table-properties-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, InputTextModule, EditorPopoverComponent, DotMessagePipe],
    template: `
        <dot-editor-popover popoverId="table-properties">
            <div
                [attr.aria-label]="'dot.block.editor.dialog.table-properties.title' | dm"
                class="w-80 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <form
                    [formGroup]="form"
                    class="flex flex-col gap-3 p-3"
                    (keydown.enter)="$event.preventDefault(); onApply()">
                    <p class="m-0 text-xs font-semibold tracking-wide text-gray-500 uppercase">
                        {{ 'dot.block.editor.dialog.table-properties.title' | dm }}
                    </p>

                    <div class="flex flex-col gap-1">
                        <label for="tbl-caption" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.table-properties.caption' | dm }}
                        </label>
                        <input
                            pInputText
                            pSize="small"
                            id="tbl-caption"
                            type="text"
                            formControlName="caption"
                            data-testid="tbl-caption"
                            [placeholder]="
                                'dot.block.editor.dialog.table-properties.caption.placeholder' | dm
                            "
                            class="w-full" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="tbl-aria-label" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.table-properties.aria-label' | dm }}
                        </label>
                        <input
                            pInputText
                            pSize="small"
                            id="tbl-aria-label"
                            type="text"
                            formControlName="ariaLabel"
                            data-testid="tbl-aria-label"
                            [placeholder]="
                                'dot.block.editor.dialog.table-properties.aria-label.placeholder'
                                    | dm
                            "
                            class="w-full" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="tbl-aria-labelledby" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.table-properties.aria-labelledby' | dm }}
                        </label>
                        <input
                            pInputText
                            pSize="small"
                            id="tbl-aria-labelledby"
                            type="text"
                            formControlName="ariaLabelledby"
                            data-testid="tbl-aria-labelledby"
                            [placeholder]="
                                'dot.block.editor.dialog.table-properties.aria-labelledby.placeholder'
                                    | dm
                            "
                            class="w-full" />
                    </div>

                    <div class="flex justify-end gap-2 pt-1">
                        <button
                            type="button"
                            data-testid="tbl-cancel"
                            (mousedown)="$event.preventDefault(); manager.close()"
                            class="cursor-pointer rounded-sm px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 focus:ring-2 focus:ring-gray-300 focus:outline-none">
                            {{ 'dot.common.cancel' | dm }}
                        </button>
                        <button
                            type="button"
                            data-testid="tbl-apply"
                            (mousedown)="$event.preventDefault(); onApply()"
                            class="cursor-pointer rounded-sm bg-indigo-500 px-3 py-1 text-sm text-white hover:bg-indigo-600 focus:ring-2 focus:ring-indigo-400 focus:outline-none">
                            {{ 'dot.common.apply' | dm }}
                        </button>
                    </div>
                </form>
            </div>
        </dot-editor-popover>
    `
})
export class TablePropertiesPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);

    readonly form = new FormGroup({
        caption: new FormControl<string>('', { nonNullable: true }),
        ariaLabel: new FormControl<string>('', { nonNullable: true }),
        ariaLabelledby: new FormControl<string>('', { nonNullable: true })
    });

    constructor() {
        effect(() => {
            const payload = this.manager.tablePropertiesPayload();
            const open = this.manager.isOpen('table-properties');

            untracked(() => {
                if (open && payload) {
                    this.form.reset(payload.initialValues);
                } else if (!open) {
                    this.form.reset(EMPTY_VALUES);
                }
            });
        });
    }

    onApply(): void {
        const { caption, ariaLabel, ariaLabelledby } = this.form.getRawValue();

        // All three a11y fields are stored as table attributes — set them in one shot.
        // A non-empty caption stores the `caption` attribute; an empty value clears it.
        this.editor()
            .chain()
            .focus()
            .updateAttributes('table', {
                caption: caption?.trim() ? caption.trim() : null,
                ariaLabel: ariaLabel?.trim() ? ariaLabel.trim() : null,
                ariaLabelledby: ariaLabelledby?.trim() ? ariaLabelledby.trim() : null
            })
            .run();

        this.manager.close();
    }
}
