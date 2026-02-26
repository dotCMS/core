import { DotPageAssetLayoutRow } from '@dotcms/types';
import { combineClasses } from '@dotcms/uve/internal';

import styles from './Row.module.css';

import { Column } from '../Column/Column';

/**
 * @internal
 *
 * Props for the Row component
 * @interface DotCMSRowRendererProps
 * @property {DotPageAssetLayoutRow} row - The row data to be rendered
 */
type DotCMSRowRendererProps = {
    row: DotPageAssetLayoutRow;
};
/**
 * This component renders a row with all it's content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @param {React.ForwardedRef<HTMLDivElement, DotCMS>} ref
 * @return {JSX.Element} Rendered rows with columns
 */
export const Row = ({ row }: DotCMSRowRendererProps) => {
    const customRowClass = combineClasses(['dot-row-container', row.styleClass || '']);

    return (
        <div className={customRowClass}>
            <div className={styles.row} data-dot-object={'row'}>
                {row.columns.map((column, index) => (
                    <Column key={index} column={column} />
                ))}
            </div>
        </div>
    );
};
