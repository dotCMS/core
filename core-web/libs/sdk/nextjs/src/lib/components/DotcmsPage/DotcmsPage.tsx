import { useRouter, usePathname } from 'next/navigation';
import { useEffect, useRef } from 'react';

import { CUSTOMER_ACTIONS } from '@dotcms/client';
import { DotcmsPageProps, PageProvider, Row, useEventHandlers } from '@dotcms/react';

export function DotcmsPage({ entity }: DotcmsPageProps) {
    const router = useRouter();
    const pathname = usePathname();

    useEffect(() => {
        window.parent.postMessage(
            {
                action: CUSTOMER_ACTIONS.SET_URL,
                payload: {
                    url: pathname === '/' ? 'index' : pathname.replace('/', '')
                }
            },
            '*'
        );
    }, [pathname]);

    const rowsRef = useRef<HTMLDivElement[]>([]);
    useEventHandlers({
        rows: rowsRef,
        reload: router.refresh
    });

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
