import styles from './Row.module.css';

import { DotPageAssetLayoutRow } from '../../types';
import { Column } from '../Column/Column';

type RowProps = {
    row: DotPageAssetLayoutRow;
};
/**
 * This component renders a row with all it's content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @param {React.ForwardedRef<HTMLDivElement, RowProps>} ref
 * @return {JSX.Element} Rendered rows with columns
 */
export const Row = ({ row }: RowProps) => {
    const rowProps = { 'data-dot': 'row' };

    return (
        <div className={row.styleClass}>
            <div className="dot-container">
                <div {...rowProps} className={styles.row}>
                    {row.columns.map((column, index) => (
                        <Column key={index} column={column} />
                    ))}
                </div>
            </div>
        </div>
    );
};
