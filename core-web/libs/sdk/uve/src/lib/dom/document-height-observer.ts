export interface ObserveDocumentHeightOptions {
    onHeightChange: (height: number) => void;
    documentRef?: Document;
    windowRef?: Window;
    debounceMs?: number;
}

export interface DocumentHeightObserverHandle {
    destroy: () => void;
}

/**
 * Observes rendered document height changes and notifies the caller after layout settles.
 *
 * Uses ResizeObserver on <html> for layout/viewport-driven changes and MutationObserver
 * on <body> to catch DOM additions/removals that may shrink the page without a resize.
 * Measurement reads `body.offsetHeight`, which tracks actual content height and
 * decreases correctly after DOM removals, unaffected by CSS min-height on the html element.
 */
export function observeDocumentHeight({
    onHeightChange,
    documentRef = document,
    windowRef = window,
    debounceMs = 50
}: ObserveDocumentHeightOptions): DocumentHeightObserverHandle {
    const html = documentRef.documentElement;
    const body = documentRef.body;
    let debounceTimer: ReturnType<typeof setTimeout> | null = null;
    let rafOuter: number | null = null;
    let rafInner: number | null = null;
    let lastHeight: number | null = null;
    let destroyed = false;

    const measureAndNotify = () => {
        const height = body.offsetHeight;

        if (!height || height === lastHeight) {
            return;
        }

        lastHeight = height;
        onHeightChange(height);
    };

    const scheduleNotify = () => {
        if (destroyed) {
            return;
        }

        if (debounceTimer !== null) {
            clearTimeout(debounceTimer);
        }

        debounceTimer = setTimeout(() => {
            debounceTimer = null;

            if (destroyed) {
                return;
            }

            rafOuter = windowRef.requestAnimationFrame(() => {
                rafOuter = null;

                if (destroyed) {
                    return;
                }

                rafInner = windowRef.requestAnimationFrame(() => {
                    rafInner = null;

                    if (!destroyed) {
                        measureAndNotify();
                    }
                });
            });
        }, debounceMs);
    };

    const onLoad = () => scheduleNotify();

    if (documentRef.readyState === 'complete' || documentRef.readyState === 'interactive') {
        scheduleNotify();
    } else {
        windowRef.addEventListener('load', onLoad);
    }

    const resizeObserver = new ResizeObserver(() => scheduleNotify());
    resizeObserver.observe(html);

    const mutationObserver = new MutationObserver(() => scheduleNotify());
    mutationObserver.observe(documentRef.body ?? html, { childList: true, subtree: true });

    return {
        destroy: () => {
            destroyed = true;

            if (debounceTimer !== null) {
                clearTimeout(debounceTimer);
                debounceTimer = null;
            }

            if (rafOuter !== null) {
                windowRef.cancelAnimationFrame(rafOuter);
                rafOuter = null;
            }

            if (rafInner !== null) {
                windowRef.cancelAnimationFrame(rafInner);
                rafInner = null;
            }

            lastHeight = null;
            resizeObserver.disconnect();
            mutationObserver.disconnect();
            windowRef.removeEventListener('load', onLoad);
        }
    };
}
