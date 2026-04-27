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

import { SlashMenuService } from './slash-menu.service';

@Component({
    selector: 'dot-slash-menu',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [],
    host: {
        role: 'listbox',
        'aria-label': 'Block type menu',
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
    template: `
        <ul class="m-0 list-none p-1" role="presentation">
            @for (item of service.items(); track item.label; let i = $index) {
                <li
                    [id]="'slash-opt-' + i"
                    role="option"
                    [attr.aria-selected]="service.activeIndex() === i"
                    [class]="itemClass(i)"
                    (mousedown)="$event.preventDefault(); service.select(item)"
                    (mousemove)="onMouseMove(i)">
                    @if (item.icon) {
                        <span
                            class="slash-menu-item-icon material-symbols-outlined flex h-10 w-10 shrink-0 items-center justify-center rounded border border-gray-200 bg-white text-gray-700"
                            aria-hidden="true">
                            {{ item.icon }}
                        </span>
                    }
                    <span class="flex min-w-0 flex-col">
                        <span class="text-sm font-medium leading-tight text-gray-900">
                            {{ item.label }}
                        </span>
                        <span class="truncate text-xs text-gray-500">{{ item.description }}</span>
                    </span>
                </li>
            }
            @if (service.isLoading()) {
                <li
                    class="flex items-center gap-3 rounded px-2 py-2 text-sm text-gray-400"
                    role="status">
                    <span
                        class="material-symbols-outlined animate-spin text-base"
                        aria-hidden="true">
                        progress_activity
                    </span>
                    Loading content types…
                </li>
            } @else if (service.items().length === 0) {
                <p role="status" class="px-3 py-4 text-center text-sm text-gray-400">
                    No matching blocks
                </p>
            }
        </ul>
    `
})
export class SlashMenuComponent {
    protected readonly service = inject(SlashMenuService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly document = inject(DOCUMENT);

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
