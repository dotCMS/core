import { forwardRef } from 'react';

import styles from './row.module.css';

import { Column } from '../Column/Column';
import { PageProviderContext } from '../PageProvider/PageProvider';

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
     * @type {PageProviderContext['layout']['body']['rows'][0]}
     * @memberof RowProps
     */
    row: PageProviderContext['layout']['body']['rows'][0];
}

/**
 * Renders a row
 *
 * @category Components
 * @param {React.ForwardedRef<HTMLDivElement, RowProps>} ref
 * @return {*}
 */
export const Row = forwardRef<HTMLDivElement, RowProps>((props: RowProps, ref) => {
    const { row } = props;

    const combinedClasses = [styles.row, row.styleClass].filter(Boolean).join(' ');

    return (
        <div data-testid="row" data-dot="row" ref={ref} className={combinedClasses}>
            {row.columns.map((column, index) => (
                <Column key={index} column={column} />
            ))}
        </div>
    );
});
