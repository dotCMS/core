import { useEffect, useState, RefObject } from 'react';

export const useCheckVisibleContent = (ref: RefObject<HTMLDivElement>) => {
    const [haveContent, setHaveContent] = useState<boolean>(false);

    useEffect(() => {
        if (!ref.current) {
            return;
        }

        const childElement = ref.current.firstElementChild;
        if (childElement) {
            const { height } = childElement.getBoundingClientRect();
            setHaveContent(height > 0);
        } else {
            setHaveContent(false);
        }
    }, [ref]);

    return haveContent;
};
