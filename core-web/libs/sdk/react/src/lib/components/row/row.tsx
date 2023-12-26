import React, { forwardRef } from 'react';

import styles from './row.module.css';

import { RowModel } from '../../types/page.model';
import Column from '../column/column';

interface RowProps {
    row: RowModel;
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
