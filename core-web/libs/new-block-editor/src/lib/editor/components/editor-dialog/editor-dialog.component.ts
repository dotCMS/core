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
    input,
    output,
    signal,
    untracked
} from '@angular/core';

import {
    EditorDialogManagerService,
    type DialogId
} from '../../services/editor-dialog-manager.service';

/**
 * Shell wrapper for all floating editor dialogs.
 * Handles absolute positioning via @floating-ui/dom, visibility, Escape key, and click-outside.
 * Dialog content is projected via <ng-content>.
 * The (opened) output fires once after the dialog first becomes visible — use it to auto-focus inputs.
 */
@Component({
    selector: 'editor-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'absolute z-50',
        '[style.display]': 'isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: `<ng-content />`
})
export class EditorDialogComponent {
    readonly dialogId = input.required<DialogId>();

    /** Emits once after the dialog is positioned and visible. Use to auto-focus an input. */
    readonly opened = output<void>();

    private readonly manager = inject(EditorDialogManagerService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly doc = inject(DOCUMENT);

    protected readonly isOpen = computed(() => this.manager.activeDialog()?.id === this.dialogId());
    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);

    constructor() {
        // Position the dialog on every render while it is open.
        // The wasPositioned guard ensures (opened) fires only on the first render after opening.
        afterRenderEffect(() => {
            const dialog = this.manager.activeDialog();
            if (!dialog || dialog.id !== this.dialogId()) {
                untracked(() => this.positioned.set(false));
                return;
            }
            const rect = dialog.clientRectFn();
            if (!rect) return;

            computePosition(
                { getBoundingClientRect: () => rect },
                this.el.nativeElement,
                {
                    placement: 'bottom-start',
                    strategy: 'absolute',
                    middleware: [flip(), shift({ padding: 8 })]
                }
            ).then(({ x, y }) => {
                const wasPositioned = untracked(() => this.positioned());
                this.zone.run(() => {
                    untracked(() => {
                        this.floatX.set(x);
                        this.floatY.set(y);
                        this.positioned.set(true);
                    });
                });
                if (!wasPositioned) {
                    this.opened.emit();
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
                if (!this.el.nativeElement.contains(e.target as Node)) {
                    this.zone.run(() => this.manager.close());
                }
            };
            this.doc.addEventListener('keydown', onKey);
            this.doc.addEventListener('mousedown', onMouse);
            onCleanup(() => {
                this.doc.removeEventListener('keydown', onKey);
                this.doc.removeEventListener('mousedown', onMouse);
            });
        });
    }
}
