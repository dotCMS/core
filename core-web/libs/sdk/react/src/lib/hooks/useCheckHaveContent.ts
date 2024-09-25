import { useEffect, useRef, useState } from 'react';

export const useCheckHaveContent = () => {
    const contentletDivRef = useRef<HTMLDivElement | null>(null);
    const [haveContent, setHaveContent] = useState<boolean>(false);

    useEffect(() => {
        if (!contentletDivRef.current) {
            return;
        }

        const childElement = contentletDivRef.current.firstElementChild;

        if (!childElement) {
            return;
        }

        const height = childElement.getBoundingClientRect().height;

        if (height > 0) {
            setHaveContent(true);

            return;
        }

        setHaveContent(false);
    }, [contentletDivRef]);

    return { contentletDivRef, haveContent };
};
