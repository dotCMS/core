import React, { forwardRef } from 'react';

import styles from './row.module.css';

import Column from '../column/column';
import { PageProviderContext } from '../page-provider/page-provider';

interface RowProps {
    row: PageProviderContext['layout']['body']['rows'][0];
}

export function Row({ row }: RowProps, ref: React.Ref<HTMLDivElement>) {
    return (
        <div data-dot="row" ref={ref} className={styles.row}>
            {row.columns.map((column, index) => (
                <Column key={index} column={column} />
            ))}
        </div>
    );
}

export default forwardRef(Row);
