
import { RelationshipTypes, TableColumn } from './models/relationship.models';

/**
 * Maps cardinality numbers to RelationshipTypes enum values.
 * Used to determine the type of relationship between content types.
 *
 * @constant
 * @type {Record<number, RelationshipTypes>}
 *
 * @property {RelationshipTypes} 0 - One-to-Many relationship type
 * @property {RelationshipTypes} 1 - Many-to-Many relationship type
 * @property {RelationshipTypes} 2 - One-to-One relationship type
 * @property {RelationshipTypes} 3 - Many-to-One relationship type
 */
export const RELATIONSHIP_OPTIONS = {
    0: RelationshipTypes.ONE_TO_MANY,
    1: RelationshipTypes.MANY_TO_MANY,
    2: RelationshipTypes.ONE_TO_ONE,
    3: RelationshipTypes.MANY_TO_ONE
};

/**
 * Key for the showFields variable in field variables
 */
export const SHOW_FIELDS_VARIABLE_KEY = 'showFields';

/**
 * Interface for table column configuration
 */


/**
 * Fixed reorder column definition
 */
export const REORDER_COLUMN: TableColumn = {
    field: 'reorder',
    header: '',
    width: '2rem',
    type: 'string'
};

/**
 * Fixed actions column definition
 */
export const ACTION_COLUMN: TableColumn = {
    field: 'actions',
    header: '',
    width: '3rem',
    frozen: true,
    alignFrozen: 'right',
    type: 'string'
};

/**
 * Default relationship columns when no showFields is specified
 */
export const DEFAULT_RELATIONSHIP_COLUMNS = {
    TITLE: { field: 'title', header: 'Title', type: 'string' as const },
    LANGUAGE: { field: 'language', header: 'Language', type: 'string' as const },
    STATUS: { field: 'status', header: 'Status', type: 'string' as const }
};

/**
 * Default field widths for common relationship fields
 */
export const FIELD_WIDTHS: Record<string, string> = {
    'title': '12rem',
    'language': '8rem',
    'status': '8rem',
    'firstName': '10rem',
    'lastName': '10rem',
    'profilePhoto': '6rem',
    'email': '12rem',
    'phone': '10rem',
    'dateCreated': '10rem',
    'dateModified': '10rem'
};

/**
 * Common image field name patterns
 */
export const IMAGE_FIELD_PATTERNS = [
    'profilePhoto', 'avatar', 'image', 'photo', 'picture', 'thumbnail',
    'banner', 'logo', 'icon', 'cover', 'background'
];

/**
 * Common image file extensions
 */
export const IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'];

