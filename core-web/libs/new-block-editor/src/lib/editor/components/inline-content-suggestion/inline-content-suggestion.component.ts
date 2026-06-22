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

import { InlineContentSuggestionService } from '../../services/inline-content-suggestion.service';

/**
 * Floating result list for the inline contentlet `@`-mention picker. Caret-anchored via
 * `@floating-ui/dom`, driven by {@link InlineContentSuggestionService} signals. Mirrors the
 * positioning / scroll-tracking approach of the slash menu, but lists async contentlet search
 * results (title + content type) and handles the loading / empty states.
 */
@Component({
    selector: 'dot-inline-content-suggestion',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotMessagePipe],
    host: {
        role: 'listbox',
        '[attr.aria-label]': 'menuAriaLabel',
        id: 'inline-content-suggestion-menu',
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
        <div class="max-h-72 overflow-y-auto p-1">
            @if (service.isLoading()) {
                <div class="px-2 py-3 text-sm text-gray-500">
                    {{ 'dot.block.editor.suggestion.inline-content.loading' | dm }}
                </div>
            } @else if (service.hasError()) {
                <div class="px-2 py-3">
                    <p class="text-sm font-medium text-gray-700">
                        {{ 'dot.block.editor.suggestion.inline-content.error.label' | dm }}
                    </p>
                    <p class="text-xs text-gray-500">
                        {{ 'dot.block.editor.suggestion.inline-content.error.description' | dm }}
                    </p>
                </div>
            } @else if (service.results().length === 0) {
                <div class="px-2 py-3">
                    <p class="text-sm font-medium text-gray-700">
                        {{ 'dot.block.editor.suggestion.inline-content.empty.label' | dm }}
                    </p>
                    <p class="text-xs text-gray-500">
                        {{ 'dot.block.editor.suggestion.inline-content.empty.description' | dm }}
                    </p>
                </div>
            } @else {
                @for (item of service.results(); track item.identifier; let i = $index) {
                    <button
                        type="button"
                        [id]="'inline-content-opt-' + i"
                        role="option"
                        [attr.aria-selected]="service.activeIndex() === i"
                        [class]="itemClass(i)"
                        (mousemove)="onMouseMove(i)"
                        (click)="service.select(item)">
                        <span class="material-symbols-outlined text-gray-400" aria-hidden="true">
                            article
                        </span>
                        <span class="flex min-w-0 flex-col text-left">
                            <span class="truncate text-sm text-gray-900">
                                {{ item.title || item.identifier }}
                            </span>
                            <span class="truncate text-xs text-gray-500">
                                {{ item.contentType }}
                            </span>
                        </span>
                    </button>
                }
            }
        </div>
    `
})
export class InlineContentSuggestionComponent {
    protected readonly service = inject(InlineContentSuggestionService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly document = inject(DOCUMENT);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly menuAriaLabel = this.dotMessageService.get(
        'dot.block.editor.suggestion.inline-content.aria-label'
    );

    protected onHostPointerDownCapture(): void {
        this.service.prepareMenuPointerInteraction();
    }

    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    // Starts false on every open; prevents a 0,0 flash before computePosition resolves.
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
                `#inline-content-opt-${i}`
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

    protected itemClass(i: number): string {
        const base =
            'flex w-full cursor-pointer items-center gap-3 rounded px-2 py-1.5 transition-colors';
        return this.service.activeIndex() === i
            ? `${base} bg-blue-50`
            : `${base} hover:bg-gray-100`;
    }

    protected onMouseMove(i: number): void {
        if (i !== this.service.activeIndex()) {
            this.service.activeIndex.set(i);
        }
    }
}
