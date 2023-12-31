import { useRef } from 'react';

import { useEventHandlers } from '../../hooks/useEventHandlers';
import PageProvider, { PageProviderContext } from '../PageProvider/PageProvider';
import Row from '../Row/Row';

export type DotcmsPageProps = {
    entity: PageProviderContext;
};

export function DotcmsPage({ entity }: DotcmsPageProps) {
    const rowsRef = useRef<HTMLDivElement[]>([]);
    useEventHandlers({ rows: rowsRef });

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
