import { useRef } from 'react';

import { useEventHandlers } from '../../hooks/useEventHandlers';
import { PageProvider, PageProviderContext } from '../PageProvider/PageProvider';
import { Row } from '../Row/Row';

/**
 * Props for the dotCMS page
 *
 * @export
 * @interface DotcmsPageProps
 */
export type DotcmsPageProps = {
    /**
     * Response from the dotcms page api
     *
     * @type {PageProviderContext}
     */
    readonly entity: PageProviderContext;
};

/**
 * Renders a dotCMS page body, does not include header and footer
 *
 * @category Components
 * @export
 * @param {DotcmsPageProps} props
 * @return {*}  {JSX.Element}
 */
export function DotcmsLayout(props: DotcmsPageProps): JSX.Element {
    const { entity } = props;
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
