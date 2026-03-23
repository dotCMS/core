import { useState, RefObject, useLayoutEffect } from 'react';

/**
 * @internal
 * A custom React hook that checks whether a referenced HTMLDivElement has visible content based on its height.
 *
 * @param {RefObject<HTMLDivElement>} ref - A React ref object pointing to an HTMLDivElement.
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
export const useCheckVisibleContent = (ref: RefObject<HTMLDivElement>) => {
    const [haveContent, setHaveContent] = useState<boolean>(false);

    useLayoutEffect(() => {
        const element = ref.current;

        if (!element) {
            return;
        }

        // Use ResizeObserver so setState is called inside a callback (not synchronously
        // in the effect body), satisfying the react-hooks/set-state-in-effect rule.
        const observer = new ResizeObserver((entries) => {
            const entry = entries[0];
            setHaveContent(entry.contentRect.height > 0);
        });

        observer.observe(element);

        return () => observer.disconnect();
    }, [ref]);

    return haveContent;
};
