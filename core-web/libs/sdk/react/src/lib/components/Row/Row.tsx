import { forwardRef, useContext } from 'react';

import styles from './row.module.css';

import { PageContext } from '../../contexts/PageContext';
import { DotCMSPageContext } from '../../models';
import { combineClasses } from '../../utils/utils';
import { Column } from '../Column/Column';

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
}

/**
 * Renders a row
 *
 * @category Components
 * @param {React.ForwardedRef<HTMLDivElement, RowProps>} ref
 * @return {*}
 */
export const Row = forwardRef<HTMLDivElement, RowProps>((props: RowProps, ref) => {
    const { isInsideEditor } = useContext<DotCMSPageContext | null>(
        PageContext
    ) as DotCMSPageContext;

    const { row } = props;

    const combinedClasses = combineClasses([styles.row, row.styleClass]);

    const rowProps = isInsideEditor ? { 'data-dot': 'row', 'data-testid': 'row', ref } : {};

    return (
        <div {...rowProps} className={combinedClasses}>
            {row.columns.map((column, index) => (
                <Column key={index} column={column} />
            ))}
        </div>
    );
});
