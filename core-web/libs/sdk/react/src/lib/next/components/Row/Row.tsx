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
    const customRowClass = `${row.styleClass || ''} ${styles.row}`;

    return (
        <div className="dot-row-container">
            <div className={customRowClass} data-dot-object={'row'}>
                {row.columns.map((column, index) => (
                    <Column key={index} column={column} />
                ))}
            </div>
        </div>
    );
};
