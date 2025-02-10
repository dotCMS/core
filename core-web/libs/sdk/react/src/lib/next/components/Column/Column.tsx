import styles from './Column.module.css';

import { DotPageAssetLayoutColumn } from '../../types';
import { combineClasses, getColumnPositionClasses } from '../../utils';
import { Container } from '../Container/Container';

/**
 * Props for Column component that represents a single column in the grid layout system.
 *
 * @export
 * @interface ColumnProps
 * @property {DotPageAssetLayoutColumn} column - Column configuration from dotCMS Page API including
 * width, leftOffset, styleClass, and containers
 */
export interface ColumnProps {
    readonly column: DotPageAssetLayoutColumn;
}

/**
 * Renders a Column component that represents a single column in a 12-column grid system.
 * The column's position and width are determined by the leftOffset and width properties
 * from the dotCMS Page API. Uses CSS Grid classes for positioning.
 *
 * @example
 * ```tsx
 * <Column column={{
 *   leftOffset: 0,
 *   width: 6,
 *   styleClass: "custom-class",
 *   containers: []
 * }} />
 * ```
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @export
 * @param {ColumnProps} { column } - Column configuration object
 * @return {JSX.Element} Rendered column with its containers positioned in the grid
 */
export function Column({ column }: ColumnProps) {
    const { startClass, endClass } = getColumnPositionClasses(column);
    const combinedClasses = combineClasses([styles[endClass], styles[startClass]]);

    return (
        <div data-dot="column" className={combinedClasses}>
            <div className={column.styleClass}>
                {column.containers.map((container) => (
                    <Container
                        key={`${container.identifier}-${container.uuid}`}
                        container={container}
                    />
                ))}
            </div>
        </div>
    );
}
