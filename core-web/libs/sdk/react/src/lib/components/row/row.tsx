import { forwardRef } from 'react';

import styles from './row.module.css';

import Column from '../Column/Column';
import { PageProviderContext } from '../PageProvider/PageProvider';

interface RowProps {
    row: PageProviderContext['layout']['body']['rows'][0];
}

export const Row = forwardRef<HTMLDivElement, RowProps>(({ row }, ref) => {
    return (
        <div data-testid="row" data-dot="row" ref={ref} className={styles.row}>
            {row.columns.map((column, index) => (
                <Column key={index} column={column} />
            ))}
        </div>
    );
});

export default Row;
