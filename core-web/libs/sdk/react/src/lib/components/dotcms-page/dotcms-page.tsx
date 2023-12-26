// "use client";

import { useCallback, useEffect, useRef, useContext } from 'react';

// import { usePathname } from 'next/navigation';
// import { useRouter } from 'next/navigation';
import { getPageElementBound } from '../../utils/utils';
import {
    PageContext,
    PageProviderContext,
} from '../page-provider/page-provider';
import Row from '../row/row';

const router = {
    refresh: () => {
        window.location.reload();
    },
};

// Main layout component
export const DotcmsPage = () => {
    const rowsRef = useRef<HTMLDivElement[]>([]);

    // const pathname = usePathname();
    const pathname = '';
    // const router = useRouter();

    const { layout } = useContext<PageProviderContext>(PageContext);

    useEffect(() => {
        // const url = pathname.split('/');

        window.parent.postMessage(
            {
                action: 'set-url',
                payload: {
                    url: pathname,
                    // url: url === '/' ? 'index' : url.pop() //TODO: We need to enhance this, this will break for: nested/pages/like/this
                },
            },
            '*'
        );
    }, [pathname]);

    // useCallBack to avoid re-create on every render
    const eventMessageHandler = useCallback((event: MessageEvent) => {
        const positionData = getPageElementBound(rowsRef.current);

        switch (event.data) {
            case 'ema-reload-page':
                router.refresh();
                break;

            case 'ema-request-bounds':
                window.parent.postMessage(
                    {
                        action: 'set-bounds',
                        payload: positionData,
                    },
                    '*'
                );
                break;

            default:
                break;
        }
    }, []);
    // We need to unbound this from the component, with a custom hook maybe?

    const eventScrollHandler = useCallback((_event: Event) => {
        window.parent.postMessage(
            {
                action: 'scroll',
            },
            '*'
        );
    }, []);

    useEffect(() => {
        window.addEventListener('message', eventMessageHandler);
        window.addEventListener('scroll', eventScrollHandler);

        return () => {
            window.removeEventListener('message', eventMessageHandler);
            window.removeEventListener('scroll', eventScrollHandler);
        };
    }, [eventMessageHandler, eventScrollHandler]);

    const addRowRef = (el: HTMLDivElement) => {
        if (el && !rowsRef.current.includes(el)) {
            rowsRef.current.push(el);
        }
    };

    return (
        <>
            {layout.body.rows.map((row, index) => (
                <Row ref={addRowRef} key={index} row={row} />
            ))}
        </>
    );
};
