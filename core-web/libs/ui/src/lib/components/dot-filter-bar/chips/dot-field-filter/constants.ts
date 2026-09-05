/**
 * Field-filter vocabulary, shared by every surface that offers the "More" overflow.
 *
 * Moved here verbatim from the Content Drive portlet: the chips that read these now live in
 * `@dotcms/ui` and serve the AssetPicker too, so one definition has to serve both. The portlet
 * re-exports them rather than redefining, which is what keeps its URL encode/decode pair and its
 * request builder pointing at the same strings the chips write.
 *
 * The string values match the backend field-type contract (edit-content `FIELD_TYPES`).
 */

/**
 * Prefix that marks a filter-bag key as a per-field "user searchable" criterion, e.g. `us.title`.
 *
 * Keeping these entries in the flat filter bag lets them ride the surface's existing persistence —
 * Content Drive's URL encode/decode, the picker's in-memory bag — and be cleared alongside every
 * other filter. The prefix avoids colliding with a known filter key or with a field whose variable
 * happens to be `title`, `workflow`, etc.
 */
export const USER_SEARCHABLE_PREFIX = 'us.';

/** Separator joining multi-select values and date-range `from,to` in the flat filter string. */
export const USER_SEARCHABLE_VALUE_SEPARATOR = ',';

/**
 * Field variable of the content type's title field. It is already covered by the toolbar's keyword
 * search, so it is not offered as a redundant field filter.
 */
export const TITLE_FIELD_VARIABLE = 'title';

/** Text-ish field types filtered with a plain contains term. */
export const FIELD_FILTER_TEXT_TYPES = ['Text', 'Textarea', 'WYSIWYG'] as const;

/** Singular field-type names, matched to their native widget in the filter chip. */
export const FIELD_FILTER_SELECT_TYPE = 'Select';
export const FIELD_FILTER_RADIO_TYPE = 'Radio';
export const FIELD_FILTER_MULTISELECT_TYPE = 'Multi-Select';
export const FIELD_FILTER_CHECKBOX_TYPE = 'Checkbox';

/** Single-value option fields (stored as one string). */
export const FIELD_FILTER_SINGLE_SELECT_TYPES = [
    FIELD_FILTER_SELECT_TYPE,
    FIELD_FILTER_RADIO_TYPE
] as const;

/** Multi-value option fields (stored as a comma-joined list). */
export const FIELD_FILTER_MULTI_SELECT_TYPES = [
    FIELD_FILTER_MULTISELECT_TYPE,
    FIELD_FILTER_CHECKBOX_TYPE
] as const;

/** Complex field types: their own picker, options fetched rather than declared on the field. */
export const FIELD_FILTER_TAG_TYPE = 'Tag';
export const FIELD_FILTER_CATEGORY_TYPE = 'Category';
export const FIELD_FILTER_RELATIONSHIP_TYPE = 'Relationship';

/**
 * Every field type whose value is a list stored comma-joined (multi-select, checkbox, tag,
 * category). Relationship is intentionally excluded — the backend only supports a single related
 * value, so it is stored as one identifier string.
 */
export const FIELD_FILTER_MULTI_VALUE_TYPES: readonly string[] = [
    ...FIELD_FILTER_MULTI_SELECT_TYPES,
    FIELD_FILTER_TAG_TYPE,
    FIELD_FILTER_CATEGORY_TYPE
];

export const FIELD_FILTER_DATE_TYPES = ['Date', 'Date-and-Time', 'Time'] as const;

/** Date field types showing time; `Time` is time-only, `Date-and-Time` shows date + time. */
export const FIELD_FILTER_TIME_ONLY_TYPE = 'Time';
export const FIELD_FILTER_DATE_TIME_TYPE = 'Date-and-Time';

/**
 * Text-backed field types the legacy content search only ever offered via a plain textbox. They
 * render the same `text` control here and filter as a single contains term against the field's
 * indexed value (JSON/Story-Block/Custom = full text, Binary = file name).
 */
export const FIELD_FILTER_JSON_TYPE = 'JSON-Field';
export const FIELD_FILTER_STORY_BLOCK_TYPE = 'Story-Block';
export const FIELD_FILTER_CUSTOM_TYPE = 'Custom-Field';
export const FIELD_FILTER_BINARY_TYPE = 'Binary';
export const FIELD_FILTER_TEXT_FALLBACK_TYPES = [
    FIELD_FILTER_JSON_TYPE,
    FIELD_FILTER_STORY_BLOCK_TYPE,
    FIELD_FILTER_CUSTOM_TYPE,
    FIELD_FILTER_BINARY_TYPE
] as const;

/**
 * Key/Value field. Rendered with a single input plus a `key:value` shorthand; the value is stored
 * as the user typed it and translated to the `key_value` joined term when building the payload.
 */
export const FIELD_FILTER_KEY_VALUE_TYPE = 'Key-Value';

/** Every field type eligible to become a filter (excludes Host-Folder + out-of-scope types). */
export const USER_SEARCHABLE_FIELD_TYPES: readonly string[] = [
    ...FIELD_FILTER_TEXT_TYPES,
    ...FIELD_FILTER_SINGLE_SELECT_TYPES,
    ...FIELD_FILTER_MULTI_SELECT_TYPES,
    ...FIELD_FILTER_DATE_TYPES,
    ...FIELD_FILTER_TEXT_FALLBACK_TYPES,
    FIELD_FILTER_TAG_TYPE,
    FIELD_FILTER_CATEGORY_TYPE,
    FIELD_FILTER_RELATIONSHIP_TYPE,
    FIELD_FILTER_KEY_VALUE_TYPE
];

/**
 * Debounce applied to the store write behind a field control, so typing or spinning a time does
 * not fire a search per keystroke.
 *
 * Prefixed rather than a bare `DEBOUNCE_TIME`: this is a public export of `@dotcms/ui` now, and a
 * name that generic would read as a library-wide default it is not.
 */
export const FIELD_FILTER_DEBOUNCE_TIME = 500;

/** Scroll height of a field-filter panel's option list. */
export const FIELD_FILTER_PANEL_SCROLL_HEIGHT = '25rem';
