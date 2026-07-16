/**
 * Constants defining the different types of relationships that can exist between content types.
 * These represent the cardinality of relationships in dotCMS.
 */
export const RelationshipTypes = {
    ONE_TO_ONE: '1-1',
    ONE_TO_MANY: '1-n',
    MANY_TO_ONE: 'n-1',
    MANY_TO_MANY: 'n-n'
} as const;

/**
 * TypeScript type representing the valid relationship type values.
 * Extracted from the RelationshipTypes constant to ensure type safety.
 */
export type RelationshipType = (typeof RelationshipTypes)[keyof typeof RelationshipTypes];

/**
 * Constants defining the selection modes available for relationship fields.
 * These determine how users can interact with relationship field inputs.
 */
export const SelectionModes = {
    SINGLE: 'single',
    MULTIPLE: 'multiple'
} as const;

/**
 * TypeScript type representing the valid selection mode values.
 * Extracted from the SelectionModes constant to ensure type safety.
 */
export type SelectionMode = (typeof SelectionModes)[keyof typeof SelectionModes];

/**
 * Context from the contentlet being edited, used to pre-populate search filters.
 */
export interface ContentletContext {
    languageId?: number;
    host?: string;
    hostName?: string;
    folder?: string;
    url?: string;
}

/**
 * Resolved filter context for pre-populating search filters.
 * Built from ContentletContext when the dialog opens.
 */
export interface ContentletFilterContext {
    hostId: string | null;
    hostName: string | null;
    languageId: number | null;
    folderId: string | null;
    folderPath: string | null;
}

/**
 * Parameters for initializing the existing content dialog store.
 */
export interface InitLoadParams {
    contentTypeId: string;
    selectionMode: SelectionMode;
    selectedItemsIds: string[];
    showFields?: string[] | null;
    cardinality?: number;
    parentContentTypeId?: string;
    fieldVariable?: string;
    isParentField?: boolean;
    currentContentIdentifier?: string;
    contentletContext?: ContentletContext;
}

/**
 * Interface representing a table column configuration for the relationship field table.
 *
 * @property nameField - The name of the field to display in the column.
 * @property header - The display header for the column.
 * @property type - The type of data in the column ('text', 'title', 'language', 'status', 'image').
 */
export interface TableColumn {
    nameField: string;
    header: string;
    type: 'text' | 'title' | 'language' | 'status' | 'image';
}
