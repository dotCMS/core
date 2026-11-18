import { useState, RefObject, useLayoutEffect } from 'react';

/**
 * @internal
 * A custom React hook that checks whether a referenced HTMLDivElement has visible content based on its height.
 *
 * The measurement is only used to reserve space for the editor's empty-contentlet placeholder,
 * so it is gated behind `enabled`. `getBoundingClientRect()` forces a synchronous layout, and
 * running it from a layout effect on every contentlet of a page is expensive — in production
 * the result was measured and then discarded.
 *
 * @param {RefObject<HTMLDivElement>} ref - A React ref object pointing to an HTMLDivElement.
 * @param {boolean} [enabled=true] - When false the element is never measured and the hook returns false.
 * @returns {boolean} - Returns true if the element's height is greater than zero (indicating visible content), otherwise false.
 *
 * @example
 * import { useRef } from 'react';
 * import { useCheckVisibleContent } from 'src/lib/next/hooks/useCheckVisibleContent';
 *
 * function MyComponent() {
 *   const contentRef = useRef<HTMLDivElement>(null);
 *   const isContentVisible = useCheckVisibleContent(contentRef);
 *
 *   return (
 *     <div ref={contentRef}>
 *       {isContentVisible ? 'Content is visible' : 'Content is not visible'}
 *     </div>
 *   );
 * }
 */
export const useCheckVisibleContent = (ref: RefObject<HTMLDivElement>, enabled = true) => {
    const [haveContent, setHaveContent] = useState<boolean>(false);

    useLayoutEffect(() => {
        if (!enabled) {
            return;
        }

        if (!ref.current) {
            setHaveContent(false);

            return;
        }

        const { height } = ref.current.getBoundingClientRect();
        setHaveContent(height > 0);
    }, [ref, enabled]);

    return haveContent;
};
