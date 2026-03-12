import {
    computePosition,
    flip,
    offset,
    shift,
    type Placement,
    type VirtualElement
} from '@floating-ui/dom';

export interface FloatingUIOptions {
    placement?: Placement;
    strategy?: 'absolute' | 'fixed';
    offset?: number | { mainAxis?: number; crossAxis?: number };
    zIndex?: number;
    appendTo?: HTMLElement | (() => HTMLElement);
    onHide?: () => void;
    onShow?: () => void;
    /** Callback when user clicks outside the floating element (receives the click event). */
    onClickOutside?: (event: MouseEvent) => void;
}

export interface FloatingUIInstance {
    show: () => void;
    hide: () => void;
    destroy: () => void;
    updatePosition: () => Promise<void>;
    /** Update the reference rect getter (e.g. when selection changes) */
    setReferenceRect: (getRect: () => DOMRect) => void;
    readonly isVisible: boolean;
}

const DEFAULT_OPTIONS: Required<
    Pick<FloatingUIOptions, 'placement' | 'strategy' | 'offset' | 'zIndex'>
> = {
    placement: 'bottom-start',
    strategy: 'fixed',
    offset: 8,
    zIndex: 10
};

/**
 * Creates a Floating UI–based floating instance that positions `floatingElement`
 * relative to a reference rect (e.g. from getBoundingClientRect or posToDOMRect).
 * Use this instead of Tippy for menu/popover positioning.
 */
export function createFloatingUI(
    getReferenceRect: () => DOMRect,
    floatingElement: HTMLElement,
    options: FloatingUIOptions = {}
): FloatingUIInstance {
    const opts = { ...DEFAULT_OPTIONS, ...options };
    let currentGetRect = getReferenceRect;
    let visible = false;
    let clickOutsideAbort: AbortController | null = null;

    const appendToEl = (): HTMLElement =>
        typeof opts.appendTo === 'function' ? opts.appendTo() : (opts.appendTo ?? document.body);

    async function updatePosition(): Promise<void> {
        const virtualRef: VirtualElement = {
            getBoundingClientRect: () => currentGetRect()
        };
        const { x, y } = await computePosition(virtualRef, floatingElement, {
            placement: opts.placement,
            strategy: opts.strategy,
            middleware: [
                offset(
                    typeof opts.offset === 'number' ? opts.offset : (opts.offset?.mainAxis ?? 8)
                ),
                flip({
                    fallbackPlacements: ['top-start', 'bottom-end', 'top-end', 'bottom-start']
                }),
                shift({ padding: 8 })
            ]
        });
        floatingElement.style.position = opts.strategy;
        floatingElement.style.left = `${x}px`;
        floatingElement.style.top = `${y}px`;
        floatingElement.style.zIndex = String(opts.zIndex);
    }

    function setupClickOutside(): void {
        if (!opts.onClickOutside) return;
        clickOutsideAbort = new AbortController();
        const handler = (e: MouseEvent) => {
            const target = e.target as Node;
            if (floatingElement.contains(target)) return;
            opts.onClickOutside?.(e);
        };
        const ac = clickOutsideAbort;
        setTimeout(() => {
            if (ac) {
                document.addEventListener('click', handler, { signal: ac.signal });
            }
        }, 0);
    }

    function teardownClickOutside(): void {
        clickOutsideAbort?.abort();
        clickOutsideAbort = null;
    }

    function show(): void {
        const parent = appendToEl();
        // Always move into the target parent (e.g. body) so position:fixed is in the right stacking context.
        // Table menu and others are created inside the editor DOM and would never be moved otherwise.
        if (floatingElement.parentNode !== parent) {
            floatingElement.parentNode?.removeChild(floatingElement);
            parent.appendChild(floatingElement);
        }
        floatingElement.style.visibility = 'hidden';
        floatingElement.style.display = '';
        visible = true;
        // Let the browser lay out the element after appending, then position and reveal
        requestAnimationFrame(() => {
            updatePosition().then(() => {
                floatingElement.style.visibility = 'visible';
                opts.onShow?.();
                setupClickOutside();
            });
        });
    }

    function hide(): void {
        visible = false;
        teardownClickOutside();
        floatingElement.style.visibility = 'hidden';
        opts.onHide?.();
    }

    function destroy(): void {
        teardownClickOutside();
        hide();
        floatingElement.remove();
    }

    function setReferenceRect(getRect: () => DOMRect): void {
        currentGetRect = getRect;
    }

    return {
        show,
        hide,
        destroy,
        updatePosition,
        setReferenceRect,
        get isVisible() {
            return visible;
        }
    };
}
