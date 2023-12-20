import { forwardRef } from 'react';

import Column from './column';

const Row = forwardRef(({ row }, ref) => {
    return (
        <div data-dot="row" ref={ref} className="grid grid-cols-12 gap-4">
            {row.columns.map((column, index) => (
                <Column key={index} column={column} />
            ))}
        </div>
    );
});

export default Row;
