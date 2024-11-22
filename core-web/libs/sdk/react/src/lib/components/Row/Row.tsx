import { forwardRef, useContext } from 'react';

import styles from './Row.module.css';

import { PageContext } from '../../contexts/PageContext';
import { DotCMSPageContext } from '../../models';
import { Column } from '../Column/Column';
import { DotError, DotErrorCodes } from '../DotErrorBoundary/DotError';
import DotErrorBoundary from '../DotErrorBoundary/DotErrorBoundary';

/**
 * Props for the row component
 *
 * @interface RowProps
 *
 */
export interface RowProps {
    /**
     * Row data
     *
     * @type {DotCMSPageContext['layout']['body']['rows'][0]}
     * @memberof RowProps
     */
    row: DotCMSPageContext['pageAsset']['layout']['body']['rows'][0];
    index: number;
}

/**
 * This component renders a row with all it's content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @param {React.ForwardedRef<HTMLDivElement, RowProps>} ref
 * @return {JSX.Element} Rendered rows with columns
 */

// Not sure why we have a forwardRef here
export const Row = forwardRef<HTMLDivElement, RowProps>((props: RowProps, ref) => {
    const { isInsideEditor } = useContext<DotCMSPageContext | null>(
        PageContext
    ) as DotCMSPageContext;

    const { row } = props;

    const rowProps = isInsideEditor ? { 'data-dot': 'row', 'data-testid': 'row', ref } : {};

    // RANDOMLY THROW AN ERROR BUT WE CAN MAKE INTEGRITY CHECKS OF THE ROWS
    if (Math.random() > 0.8)
        throw new DotError(DotErrorCodes.ROW001, {
            row: props.index
        });

    return (
        <div className={row.styleClass}>
            <div className="container">
                <div {...rowProps} className={styles.row}>
                    {row.columns.map((column, index) => (
                        <DotErrorBoundary key={index}>
                            <Column column={column} colIndex={index} rowIndex={props.index} />
                        </DotErrorBoundary>
                    ))}
                </div>
            </div>
        </div>
    );
});
