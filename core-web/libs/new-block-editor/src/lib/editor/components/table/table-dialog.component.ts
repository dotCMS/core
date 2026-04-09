import { computePosition, flip, shift } from '@floating-ui/dom';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    NgZone,
    afterRenderEffect,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { TableDialogService } from './table-dialog.service';

const DEFAULT_ROWS = 3;
const DEFAULT_COLS = 3;
const MAX_VALUE = 20;

@Component({
    selector: 'dot-block-editor-table-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule],
    host: {
        'aria-label': 'Insert table',
        class: 'absolute z-50 w-64 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg',
        '[style.display]': 'service.isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: `
        <form
            [formGroup]="form"
            class="flex flex-col gap-3 p-3"
            (keydown.escape)="service.close()"
            (keydown.enter)="$event.preventDefault(); onApply()">
            <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide m-0">
                Insert Table
            </p>

            <div class="flex gap-3">
                <div class="flex flex-col gap-1 flex-1">
                    <label for="tbl-rows" class="text-xs font-medium text-gray-700">Rows</label>
                    <input
                        id="tbl-rows"
                        type="number"
                        formControlName="rows"
                        min="1"
                        [max]="maxValue"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none" />
                </div>
                <div class="flex flex-col gap-1 flex-1">
                    <label for="tbl-cols" class="text-xs font-medium text-gray-700">Columns</label>
                    <input
                        id="tbl-cols"
                        type="number"
                        formControlName="cols"
                        min="1"
                        [max]="maxValue"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none" />
                </div>
            </div>

            <label class="flex items-center gap-2 cursor-pointer">
                <input
                    id="tbl-header"
                    type="checkbox"
                    formControlName="withHeaderRow"
                    class="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                <span class="text-sm text-gray-700">Include header row</span>
            </label>

            <div class="flex justify-end gap-2 pt-1">
                <button
                    type="button"
                    (mousedown)="$event.preventDefault(); service.close()"
                    class="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300">
                    Cancel
                </button>
                <button
                    type="button"
                    (mousedown)="$event.preventDefault(); onApply()"
                    [disabled]="form.invalid"
                    class="rounded bg-indigo-500 px-3 py-1 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                    Insert
                </button>
            </div>
        </form>
    `
})
export class TableDialogComponent {
    protected readonly service = inject(TableDialogService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly document = inject(DOCUMENT);

    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);
    protected readonly maxValue = MAX_VALUE;

    private previouslyFocused: HTMLElement | null = null;

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
        effect((onCleanup) => {
            if (!this.service.isOpen()) return;

            this.previouslyFocused = this.document.activeElement as HTMLElement | null;

            const handleKeyDown = (event: KeyboardEvent) => {
                if (event.key === 'Escape') this.zone.run(() => this.service.close());
            };
            const handleMouseDown = (event: MouseEvent) => {
                if (!this.el.nativeElement.contains(event.target as Node)) {
                    this.zone.run(() => this.service.close());
                }
            };

            this.document.addEventListener('keydown', handleKeyDown);
            this.document.addEventListener('mousedown', handleMouseDown);
            onCleanup(() => {
                this.document.removeEventListener('keydown', handleKeyDown);
                this.document.removeEventListener('mousedown', handleMouseDown);
                this.previouslyFocused?.focus({ preventScroll: true });
                this.previouslyFocused = null;
            });
        });

        afterRenderEffect(() => {
            const isOpen = this.service.isOpen();
            const clientRectFn = this.service.clientRectFn();

            if (!isOpen || !clientRectFn) {
                untracked(() => {
                    this.positioned.set(false);
                    this.form.reset({
                        rows: DEFAULT_ROWS,
                        cols: DEFAULT_COLS,
                        withHeaderRow: true
                    });
                });
                return;
            }

            const virtualRef = {
                getBoundingClientRect: () => clientRectFn() ?? new DOMRect()
            };

            computePosition(virtualRef, this.el.nativeElement, {
                placement: 'bottom-start',
                strategy: 'absolute',
                middleware: [flip(), shift({ padding: 8 })]
            }).then(({ x, y }) => {
                this.zone.run(() => {
                    untracked(() => {
                        this.floatX.set(x);
                        this.floatY.set(y);
                        this.positioned.set(true);
                    });
                });
                setTimeout(() => {
                    const firstInput = this.el.nativeElement.querySelector(
                        'input:not([type="file"]):not([type="checkbox"])'
                    ) as HTMLElement | null;
                    firstInput?.focus();
                }, 0);
            });
        });
    }

    onApply(): void {
        if (this.form.invalid) return;
        this.service.insert(this.form.getRawValue());
    }
}
