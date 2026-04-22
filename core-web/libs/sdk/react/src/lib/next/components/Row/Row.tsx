import { DotPageAssetLayoutRow } from '@dotcms/types';
import { combineClasses, DOT_SECTION_ID_PREFIX } from '@dotcms/uve/internal';

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
    /** 1-based section index used as the `id` anchor for editor scroll-to-section. */
    index: number;
};
/**
 * This component renders a row with all it's content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @param {React.ForwardedRef<HTMLDivElement, DotCMS>} ref
 * @return {JSX.Element} Rendered rows with columns
 */
export const Row = ({ row, index }: DotCMSRowRendererProps) => {
    const customRowClass = combineClasses(['dot-row-container', row.styleClass || '']);

    return (
        <div id={`${DOT_SECTION_ID_PREFIX}${index}`} className={customRowClass}>
            <div className={styles.row} data-dot-object={'row'}>
                {row.columns.map((column, index) => (
                    <Column key={index} column={column} />
                ))}
            </div>
        </div>
    );
};
