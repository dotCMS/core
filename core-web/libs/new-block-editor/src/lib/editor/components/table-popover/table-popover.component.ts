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

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

const DEFAULT_ROWS = 3;
const DEFAULT_COLS = 3;
const MAX_VALUE = 20;

@Component({
    selector: 'dot-table-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, EditorPopoverComponent, DotMessagePipe],
    template: `
        <dot-editor-popover popoverId="table">
            <div
                [attr.aria-label]="'dot.block.editor.dialog.table.aria-label' | dm"
                class="w-64 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <form
                    [formGroup]="form"
                    class="flex flex-col gap-3 p-3"
                    (keydown.enter)="$event.preventDefault(); onApply()">
                    <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide m-0">
                        {{ 'dot.block.editor.dialog.table.title' | dm }}
                    </p>

                    <div class="flex gap-3">
                        <div class="flex flex-col gap-1 flex-1">
                            <label for="tbl-rows" class="text-xs font-medium text-gray-700">
                                {{ 'dot.block.editor.dialog.table.field.rows' | dm }}
                            </label>
                            <input
                                id="tbl-rows"
                                type="number"
                                formControlName="rows"
                                min="1"
                                [max]="maxValue"
                                class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>
                        <div class="flex flex-col gap-1 flex-1">
                            <label for="tbl-cols" class="text-xs font-medium text-gray-700">
                                {{ 'dot.block.editor.dialog.table.field.columns' | dm }}
                            </label>
                            <input
                                id="tbl-cols"
                                type="number"
                                formControlName="cols"
                                min="1"
                                [max]="maxValue"
                                class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>
                    </div>

                    <label class="flex items-center gap-2 cursor-pointer" for="tbl-header">
                        <input
                            id="tbl-header"
                            type="checkbox"
                            formControlName="withHeaderRow"
                            class="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                        <span class="text-sm text-gray-700">
                            {{ 'dot.block.editor.dialog.table.field.header-row' | dm }}
                        </span>
                    </label>

                    <div class="flex justify-end gap-2 pt-1">
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); manager.close()"
                            class="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300">
                            {{ 'Cancel' | dm }}
                        </button>
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); onApply()"
                            [disabled]="form.invalid"
                            class="rounded bg-indigo-500 px-3 py-1 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                            {{ 'Insert' | dm }}
                        </button>
                    </div>
                </form>
            </div>
        </dot-editor-popover>
    `
})
export class TablePopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);
    protected readonly maxValue = MAX_VALUE;

    readonly form = new FormGroup({
        rows: new FormControl<number>(DEFAULT_ROWS, {
            nonNullable: true,
            validators: [Validators.required, Validators.min(1), Validators.max(MAX_VALUE)]
        }),
        cols: new FormControl<number>(DEFAULT_COLS, {
            nonNullable: true,
            validators: [Validators.required, Validators.min(1), Validators.max(MAX_VALUE)]
        }),
        withHeaderRow: new FormControl<boolean>(true, { nonNullable: true })
    });

    constructor() {
        effect(() => {
            if (!this.manager.isOpen('table')) {
                untracked(() =>
                    this.form.reset({
                        rows: DEFAULT_ROWS,
                        cols: DEFAULT_COLS,
                        withHeaderRow: true
                    })
                );
            }
        });
    }

    onApply(): void {
        if (this.form.invalid) return;
        this.editor().chain().focus().insertTable(this.form.getRawValue()).run();
        this.manager.close();
    }
}
