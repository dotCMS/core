// "use client";

import { useRef } from 'react';

// import { usePathname } from 'next/navigation';
// import { useRouter } from 'next/navigation';
import { useDotcmsPageContext } from '../../hooks/useDotcmsPageContext';
import { useEventHandlers } from '../../hooks/useEventHandlers';
import Row from '../row/row';

export function DotcmsPage() {
    const { layout } = useDotcmsPageContext();
    const rowsRef = useRef<HTMLDivElement[]>([]);
    useEventHandlers(rowsRef);

    // const pathname = usePathname();
    // const pathname = '';
    // const router = useRouter();

    // useEffect(() => {
    //     // const url = pathname.split('/');

    //     window.parent.postMessage(
    //         {
    //             action: 'set-url',
    //             payload: {
    //                 url: pathname,
    //                 // url: url === '/' ? 'index' : url.pop() //TODO: We need to enhance this, this will break for: nested/pages/like/this
    //             },
    //         },
    //         '*'
    //     );
    // }, [pathname]);

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
}
