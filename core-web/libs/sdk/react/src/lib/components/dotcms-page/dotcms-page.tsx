// "use client";

import { useRef } from 'react';

// import { usePathname } from 'next/navigation';
// import { useRouter } from 'next/navigation';
import { useEventHandlers } from '../../hooks/useEventHandlers';
import PageProvider, {
    PageProviderContext,
} from '../page-provider/page-provider';
import Row from '../row/row';

type DotcmsPageProps = {
    entity: PageProviderContext;
};

export function DotcmsPage({ entity }: DotcmsPageProps) {
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
        <PageProvider entity={entity}>
            {entity.layout.body.rows.map((row, index) => (
                <Row ref={addRowRef} key={index} row={row} />
            ))}
        </PageProvider>
    );
}
