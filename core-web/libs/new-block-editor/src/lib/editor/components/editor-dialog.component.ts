import { computePosition, flip, shift } from '@floating-ui/dom';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Injector,
    NgZone,
    afterNextRender,
    afterRenderEffect,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';

import { EditorDialogManagerService, type DialogId } from '../services/editor-dialog.service';

/**
 * Shell wrapper for all floating editor dialogs.
 * Handles absolute positioning via @floating-ui/dom, visibility, Escape key, and click-outside.
 * Auto-focuses the first focusable form element in projected content after the dialog is painted.
 * Dialog content is projected via <ng-content>.
 */
@Component({
    selector: 'dot-editor-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'absolute z-50',
        '[style.display]': 'isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: `
        <ng-content />
    `
})
export class EditorDialogComponent {
    readonly dialogId = input.required<DialogId>();

    private readonly manager = inject(EditorDialogManagerService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly doc = inject(DOCUMENT);
    private readonly injector = inject(Injector);

    protected readonly isOpen = computed(() => this.manager.activeDialog()?.id === this.dialogId());
    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);

    constructor() {
        // Position the dialog on every render while it is open.
        // The wasPositioned guard ensures auto-focus runs only on the first render after opening.
        afterRenderEffect(() => {
            const dialog = this.manager.activeDialog();
            if (!dialog || dialog.id !== this.dialogId()) {
                untracked(() => this.positioned.set(false));
                return;
            }
            const rect = dialog.clientRectFn();
            if (!rect) return;

            computePosition({ getBoundingClientRect: () => rect }, this.el.nativeElement, {
                placement: 'bottom-start',
                strategy: 'absolute',
                middleware: [flip(), shift({ padding: 8 })]
            }).then(({ x, y }) => {
                const wasPositioned = untracked(() => this.positioned());
                this.zone.run(() => {
                    untracked(() => {
                        this.floatX.set(x);
                        this.floatY.set(y);
                        this.positioned.set(true);
                    });
                });
                if (!wasPositioned) {
                    // Defer to next render so the visibility binding is painted
                    // before .focus() runs — otherwise it no-ops on a hidden element.
                    afterNextRender(() => this.focusFirstInput(), { injector: this.injector });
                }
            });
        });

        // Close on Escape or click outside.
        effect((onCleanup) => {
            if (!this.isOpen()) return;

            const onKey = (e: KeyboardEvent) => {
                if (e.key === 'Escape') this.zone.run(() => this.manager.close());
            };
            const onMouse = (e: MouseEvent) => {
                const target = e.target as Element | null;
                if (!target) return;
                if (this.el.nativeElement.contains(target)) return;
                // PrimeNG overlay panels (e.g. <p-select> with appendTo="body") render outside
                // this shell's DOM but logically belong to a control inside an open dialog.
                // Treat clicks inside them as inside the dialog so the popover stays open.
                if (target.closest('.p-overlay, .p-select-overlay')) return;
                this.zone.run(() => this.manager.close());
            };
            this.doc.addEventListener('keydown', onKey);
            this.doc.addEventListener('mousedown', onMouse);
            onCleanup(() => {
                this.doc.removeEventListener('keydown', onKey);
                this.doc.removeEventListener('mousedown', onMouse);
            });
        });
    }

    private focusFirstInput(): void {
        const target = this.el.nativeElement.querySelector(
            'input:not([disabled]):not([type="hidden"]), textarea:not([disabled]), select:not([disabled])'
        ) as HTMLElement | null;
        target?.focus();
    }
}
