import { DotCMSContentType, DotPagination } from '@dotcms/dotcms-models';

/**
 * View display mode for content types
 */
export type ViewOption = 'grid' | 'list';

/**
 * Sort configuration for content types
 */
export interface SortOption {
    /** Field to sort by */
    orderby: 'name' | 'usage';
    /** Sort direction */
    direction: 'ASC' | 'DESC';
}

/**
 * Component state interface for palette list
 */
export interface DotUVEPaletteListState {
    /** List of content types to display */
    contentTypes: DotCMSContentType[];
    /** Pagination configuration (excluding total entries) */
    pagination?: Omit<DotPagination, 'totalEntries'>;
    /** Current sort configuration */
    sort: SortOption;
    /** Search filter text */
    filter?: string;
    /** Total number of entries available */
    totalEntries: number;
    /** Loading state indicator */
    loading: boolean;
}
