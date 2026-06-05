import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    input,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

const EMPTY_VALUES = {
    caption: '',
    hasCaption: false,
    ariaLabel: '',
    ariaLabelledby: ''
};

/**
 * Toolbar-anchored a11y popover for the active table. Edits:
 *
 *   - **Caption** — sets the table's `caption` attribute (rendered as a `<caption>` child).
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
    imports: [ReactiveFormsModule, EditorPopoverComponent, DotMessagePipe],
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

                    <label class="flex cursor-pointer items-center gap-2" for="tbl-has-caption">
                        <input
                            id="tbl-has-caption"
                            type="checkbox"
                            formControlName="hasCaption"
                            data-testid="tbl-has-caption"
                            class="size-4 rounded-sm border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                        <span class="text-sm text-gray-700">
                            {{ 'dot.block.editor.dialog.table-properties.add-caption' | dm }}
                        </span>
                    </label>

                    <div class="flex flex-col gap-1">
                        <label for="tbl-caption" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.table-properties.caption' | dm }}
                        </label>
                        <input
                            id="tbl-caption"
                            type="text"
                            formControlName="caption"
                            data-testid="tbl-caption"
                            [attr.disabled]="form.controls.hasCaption.value ? null : true"
                            class="w-full rounded-sm border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none disabled:bg-gray-100 disabled:text-gray-400" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="tbl-aria-label" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.table-properties.aria-label' | dm }}
                        </label>
                        <input
                            id="tbl-aria-label"
                            type="text"
                            formControlName="ariaLabel"
                            data-testid="tbl-aria-label"
                            class="w-full rounded-sm border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <div class="flex flex-col gap-1">
                        <label for="tbl-aria-labelledby" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.table-properties.aria-labelledby' | dm }}
                        </label>
                        <input
                            id="tbl-aria-labelledby"
                            type="text"
                            formControlName="ariaLabelledby"
                            data-testid="tbl-aria-labelledby"
                            class="w-full rounded-sm border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none" />
                    </div>

                    <div class="flex justify-end gap-2 pt-1">
                        <button
                            type="button"
                            data-testid="tbl-cancel"
                            (mousedown)="$event.preventDefault(); manager.close()"
                            class="rounded-sm px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 focus:ring-2 focus:ring-gray-300 focus:outline-none">
                            {{ 'dot.common.cancel' | dm }}
                        </button>
                        <button
                            type="button"
                            data-testid="tbl-apply"
                            (mousedown)="$event.preventDefault(); onApply()"
                            class="rounded-sm bg-indigo-500 px-3 py-1 text-sm text-white hover:bg-indigo-600 focus:ring-2 focus:ring-indigo-400 focus:outline-none">
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
        hasCaption: new FormControl<boolean>(false, { nonNullable: true }),
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
        const { hasCaption, caption, ariaLabel, ariaLabelledby } = this.form.getRawValue();

        // All three a11y fields are stored as table attributes — set them in one shot.
        this.editor()
            .chain()
            .focus()
            .updateAttributes('table', {
                caption: hasCaption && caption.trim() ? caption.trim() : null,
                ariaLabel: ariaLabel?.trim() ? ariaLabel.trim() : null,
                ariaLabelledby: ariaLabelledby?.trim() ? ariaLabelledby.trim() : null
            })
            .run();

        this.manager.close();
    }
}
