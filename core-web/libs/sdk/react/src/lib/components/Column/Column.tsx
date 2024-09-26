import { useContext } from 'react';

import styles from './column.module.css';

import { PageContext } from '../../contexts/PageContext';
import { DotCMSPageContext } from '../../models';
import { combineClasses, getPositionStyleClasses } from '../../utils/utils';
import { Container } from '../Container/Container';

/**
 * Props for Column component to render a column with its containers.
 *
 * @export
 * @interface ColumnProps
 */
export interface ColumnProps {
    readonly column: DotCMSPageContext['pageAsset']['layout']['body']['rows'][0]['columns'][0];
}

/**
 * Renders a Column with its containers using information provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @export
 * @param {ColumnProps} { column }
 * @return {JSX.Element} Rendered column with containers
 */
export function Column({ column }: ColumnProps) {
    const { isInsideEditor } = useContext(PageContext) as DotCMSPageContext;

    const { startClass, endClass } = getPositionStyleClasses(
        column.leftOffset,
        column.width + column.leftOffset
    );

    const combinedClasses = combineClasses([styles[endClass], styles[startClass]]);

    const columnProps = isInsideEditor
        ? {
              'data-dot': 'column',
              'data-testid': 'column'
          }
        : {};

    return (
        <div {...columnProps} className={combinedClasses}>
            <div className={column.styleClass}>
                {column.containers.map((container) => (
                    <Container
                        key={`${container.identifier}-${container.uuid}`}
                        containerRef={container}
                    />
                ))}
            </div>
        </div>
    );
}
