import { computePosition, flip, offset, shift } from '@floating-ui/dom';

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

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { SlashMenuService } from './slash-menu.service';

@Component({
    selector: 'dot-slash-menu',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotMessagePipe],
    host: {
        role: 'listbox',
        '[attr.aria-label]': 'menuAriaLabel',
        id: 'slash-command-menu',
        'aria-live': 'polite',
        tabindex: '-1',
        class: 'fixed z-50 w-72 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg',
        '[style.display]': 'service.isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()',
        '(pointerdown.capture)': 'onHostPointerDownCapture()'
    },
    templateUrl: './slash-menu.component.html'
})
export class SlashMenuComponent {
    protected readonly service = inject(SlashMenuService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly document = inject(DOCUMENT);
    private readonly dotMessageService = inject(DotMessageService);

    /** Resolved at construction so the host's `[attr.aria-label]` binds to a static string. */
    protected readonly menuAriaLabel = this.dotMessageService.get(
        'dot.block.editor.slash-menu.aria-label'
    );

    protected onHostPointerDownCapture(): void {
        this.service.prepareMenuPointerInteraction();
    }

    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    // Starts false on every open; prevents a 0,0 flash before computePosition resolves
    protected readonly positioned = signal(false);
    private readonly scrollTick = signal(0);

    constructor() {
        effect((onCleanup) => {
            if (!this.service.isOpen()) return;

            const onScroll = () => this.scrollTick.update((n) => n + 1);
            this.document.addEventListener('scroll', onScroll, { passive: true, capture: true });
            onCleanup(() => {
                this.document.removeEventListener('scroll', onScroll, { capture: true });
            });
        });

        // Keep the active option visible when arrow keys move the selection.
        afterRenderEffect(() => {
            if (!this.service.isOpen()) return;
            const i = this.service.activeIndex();
            const target = this.el.nativeElement.querySelector(
                `#slash-opt-${i}`
            ) as HTMLElement | null;
            target?.scrollIntoView({ block: 'nearest' });
        });

        afterRenderEffect(() => {
            this.scrollTick();
            const isOpen = this.service.isOpen();
            const clientRectFn = this.service.clientRectFn();

            if (!isOpen || !clientRectFn) {
                untracked(() => this.positioned.set(false));
                return;
            }

            const virtualRef = {
                getBoundingClientRect: () => clientRectFn() ?? new DOMRect()
            };

            // Host uses `position: fixed` (Tailwind `fixed`). Floating UI must use the same
            // strategy or `left`/`top` are interpreted in the wrong space (large offset vs `/`).
            computePosition(virtualRef, this.el.nativeElement, {
                placement: 'bottom-start',
                strategy: 'fixed',
                middleware: [offset(4), flip(), shift({ padding: 8 })]
            }).then(({ x, y }) => {
                this.zone.run(() => {
                    untracked(() => {
                        this.floatX.set(x);
                        this.floatY.set(y);
                        this.positioned.set(true);
                    });
                });
            });
        });
    }

    itemClass(i: number): string {
        const base =
            'flex w-full cursor-pointer items-center gap-3 rounded px-2 py-1.5 transition-colors';
        return this.service.activeIndex() === i
            ? `${base} bg-blue-50`
            : `${base} hover:bg-gray-100`;
    }

    onMouseMove(i: number): void {
        if (i !== this.service.activeIndex()) {
            this.service.activeIndex.set(i);
        }
    }
}
