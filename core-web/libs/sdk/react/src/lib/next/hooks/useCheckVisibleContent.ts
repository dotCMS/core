import { useEffect, useState, RefObject } from 'react';

export const useCheckVisibleContent = (ref: RefObject<HTMLDivElement>) => {
    const [haveContent, setHaveContent] = useState<boolean>(false);

    useEffect(() => {
        if (!ref.current) {
            setHaveContent(false);

            return;
        }

        const { height } = ref.current.getBoundingClientRect();
        setHaveContent(height > 0);
    }, [ref]);

    return haveContent;
};
