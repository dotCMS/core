import { computePosition, flip, shift } from '@floating-ui/dom';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    NgZone,
    afterRenderEffect,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { LinkDialogService } from './link-dialog.service';

@Component({
    selector: 'dot-block-editor-link-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule],
    host: {
        '[attr.aria-label]': 'isEditing() ? "Edit link" : "Insert link"',
        class: 'absolute z-50 w-80 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg',
        '[style.display]': 'service.isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: `
        <div
            class="p-4 flex flex-col gap-3"
            (keydown.escape)="service.close()"
            (keydown.enter)="$event.preventDefault(); onInsert()">
            <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide m-0">
                {{ isEditing() ? 'Edit Link' : 'Insert Link' }}
            </p>

            <div class="flex flex-col gap-1">
                <label for="link-url" class="text-xs font-medium text-gray-700">URL</label>
                <input
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

            <div class="flex justify-end gap-2 pt-1">
                <button
                    type="button"
                    (mousedown)="$event.preventDefault(); service.close()"
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
    `
})
export class LinkDialogComponent {
    protected readonly service = inject(LinkDialogService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly document = inject(DOCUMENT);

    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);
    protected readonly isEditing = computed(() => this.service.initialValues() !== null);

    private previouslyFocused: HTMLElement | null = null;

    readonly form = new FormGroup({
        href: new FormControl<string>('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
        }),
        displayText: new FormControl<string>('', { nonNullable: true })
    });

    constructor() {
        // Pre-populate form when opened in edit mode
        effect(() => {
            const values = this.service.initialValues();
            untracked(() => {
                if (values) {
                    this.form.setValue({ href: values.href, displayText: values.displayText });
                }
            });
        });

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
                    this.form.reset({ href: '', displayText: '' });
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

    onInsert(): void {
        if (this.form.controls.href.invalid) return;
        const { href, displayText } = this.form.getRawValue();
        this.service.insert(href, displayText.trim() || undefined);
    }
}
