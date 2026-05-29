import { autoUpdate, computePosition, flip, shift } from '@floating-ui/dom';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Injector,
    NgZone,
    afterNextRender,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';

import { EditorPopoverService, type PopoverId } from '../../services/editor-popover.service';

/**
 * Shell wrapper for all caret-anchored editor popovers (link, table, image-properties, emoji).
 * Handles absolute positioning via @floating-ui/dom, visibility, Escape key, and click-outside.
 * Auto-focuses the first focusable form element in projected content after the popover is painted.
 * Popover content is projected via <ng-content>.
 */
@Component({
    selector: 'dot-editor-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // `position: fixed` + floating-ui `strategy: 'fixed'` — same pair the slash menu uses.
    // `absolute` would anchor the element to its offset parent in document space, causing it
    // to drift visually whenever the editor container scrolls. `fixed` pins to the viewport,
    // and floating-ui's `strategy: 'fixed'` already returns viewport-relative coords so the
    // two coordinate spaces match.
    host: {
        class: 'fixed z-50',
        '[style.display]': 'isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: `
        <ng-content />
    `
})
export class EditorPopoverComponent {
    /**
     * Accepts a single popover id or an array of ids. The shell is "open" when the
     * service's active popover matches any of the listed ids — used by the unified
     * `dot-table-handle-popover` to host the column / row / selection variants under
     * a single shell.
     */
    readonly popoverId = input.required<PopoverId | readonly PopoverId[]>();

    readonly #manager = inject(EditorPopoverService);
    readonly #el = inject(ElementRef<HTMLElement>);
    readonly #zone = inject(NgZone);
    readonly #doc = inject(DOCUMENT);
    readonly #injector = inject(Injector);

    /** True when the service's active id matches this shell's id (or any of them). */
    #matchesActive(activeId: PopoverId | undefined): boolean {
        if (activeId == null) return false;
        const ids = this.popoverId();
        return Array.isArray(ids) ? ids.includes(activeId) : ids === activeId;
    }

    protected readonly isOpen = computed(() =>
        this.#matchesActive(this.#manager.activePopover()?.id)
    );
    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);

    constructor() {
        // Track the active popover and reposition the shell while it's open. floating-ui's
        // `autoUpdate` registers scroll/resize/layout-change listeners and calls our updater,
        // so the popover stays anchored to its trigger rect (e.g. the cursor line) as the
        // user scrolls instead of getting stranded at its initial viewport spot.
        effect((onCleanup) => {
            const active = this.#manager.activePopover();
            if (!active || !this.#matchesActive(active.id)) {
                untracked(() => this.positioned.set(false));
                return;
            }

            // Virtual reference whose `getBoundingClientRect` calls the manager's
            // clientRectFn each time — so each reposition reads the current cursor rect.
            const virtualEl = {
                getBoundingClientRect: () => active.clientRectFn() ?? new DOMRect()
            };

            const update = () => {
                computePosition(virtualEl, this.#el.nativeElement, {
                    placement: 'bottom-start',
                    strategy: 'fixed',
                    middleware: [flip(), shift({ padding: 8 })]
                }).then(({ x, y }) => {
                    const wasPositioned = untracked(() => this.positioned());
                    this.#zone.run(() => {
                        untracked(() => {
                            this.floatX.set(x);
                            this.floatY.set(y);
                            this.positioned.set(true);
                        });
                    });
                    if (!wasPositioned) {
                        // Defer to next render so the visibility binding is painted
                        // before .focus() runs — otherwise it no-ops on a hidden element.
                        afterNextRender(() => this.#focusFirstInput(), {
                            injector: this.#injector
                        });
                    }
                });
            };

            // autoUpdate handles resize/layout shifts, but it walks the real DOM to register
            // scroll listeners. Virtual elements have no contextElement, so ancestorScroll
            // never fires. Add a manual capture-phase scroll listener — same pattern the
            // slash menu uses — so the popover re-positions when the editor container scrolls.
            const onScroll = () => update();
            this.#doc.addEventListener('scroll', onScroll, { passive: true, capture: true });

            const cleanup = autoUpdate(virtualEl, this.#el.nativeElement, update);
            onCleanup(() => {
                cleanup();
                this.#doc.removeEventListener('scroll', onScroll, { capture: true });
            });
        });

        // Close on Escape or click outside.
        effect((onCleanup) => {
            if (!this.isOpen()) return;

            const onKey = (e: KeyboardEvent) => {
                if (e.key === 'Escape') this.#zone.run(() => this.#manager.close());
            };
            const onMouse = (e: MouseEvent) => {
                const target = e.target as Element | null;
                if (!target) return;
                if (this.#el.nativeElement.contains(target)) return;
                // PrimeNG overlay panels (e.g. <p-select> with appendTo="body") render outside
                // this shell's DOM but logically belong to a control inside an open popover.
                // Treat clicks inside them as inside the popover so the popover stays open.
                if (target.closest('.p-overlay, .p-select-overlay')) return;
                this.#zone.run(() => this.#manager.close());
            };
            this.#doc.addEventListener('keydown', onKey);
            this.#doc.addEventListener('mousedown', onMouse);
            onCleanup(() => {
                this.#doc.removeEventListener('keydown', onKey);
                this.#doc.removeEventListener('mousedown', onMouse);
            });
        });
    }

    #focusFirstInput(): void {
        const target = this.#el.nativeElement.querySelector(
            'input:not([disabled]):not([type="hidden"]), textarea:not([disabled]), select:not([disabled])'
        ) as HTMLElement | null;
        target?.focus();
    }
}
