/**
 * Content handler — serialize/parse contentlets to/from .md files.
 *
 * Converts between dotCMS contentlet data (GraphQL/REST) and local
 * `.md` files using YAML frontmatter + markdown body.
 */

import matter from 'gray-matter';
import { Document, parse as parseYaml } from 'yaml';
import { z } from 'zod';

import {
    BINARY_FIELD_TYPES,
    BODY_FIELD_TYPES,
    BinaryFieldInfo,
    ContentTypeField,
    ContentTypeSchema,
    ContentletFrontmatter,
    ContentletRecord,
    LanguageMap,
    METADATA_KEYS,
    ParsedContentFile,
    SHORT_ID_LENGTH,
    isSystemField
} from '../core/types';

// ─── Field type sets for quick lookup ────────────────────────────────────────

const BODY_TYPES_SET = new Set<string>(BODY_FIELD_TYPES);
export const BINARY_TYPES_SET = new Set<string>(BINARY_FIELD_TYPES);

/** Field types that should be omitted from frontmatter entirely */
export const OMIT_FIELD_TYPES = new Set([
    'Constant',
    'Hidden',
    'Custom-Field',
    'Tab_divider',
    'Line_divider',
    'Row',
    'Column',
    'Host-Folder'
]);

/** Field types serialized as YAML lists */
const LIST_FIELD_TYPES = new Set(['MultiSelect', 'Checkbox', 'Tag', 'Category', 'Relationship']);

/** Relationship fields — skip during push (server manages relationships separately) */
const RELATIONSHIP_TYPES_SET = new Set(['Relationship']);

/** Field types serialized as YAML maps or nested structures */
const MAP_FIELD_TYPES = new Set(['KeyValue', 'Key-Value']);

/** Field types serialized as nested YAML (JSON-like) */
export const JSON_FIELD_TYPES = new Set(['JSON', 'Story-Block', 'StoryBlock']);

// ─── Schema analysis ────────────────────────────────────────────────────────

/**
 * Return user-editable fields from a content type schema,
 * filtering out system fields.
 */
export function getUserFields(schema: ContentTypeSchema): ContentTypeField[] {
    return schema.fields.filter((f) => !isSystemField(f));
}

/**
 * Find the first WYSIWYG / Textarea / StoryBlock field by sortOrder.
 * This becomes the body of the .md file.
 */
export function getBodyField(schema: ContentTypeSchema): string | null {
    const userFields = getUserFields(schema);
    const candidates = userFields
        .filter((f) => BODY_TYPES_SET.has(f.fieldType))
        .sort((a, b) => a.sortOrder - b.sortOrder);

    return candidates.length > 0 ? candidates[0].variable : null;
}

/**
 * Find all Binary / Image / File fields.
 */
export function getBinaryFields(schema: ContentTypeSchema): ContentTypeField[] {
    return getUserFields(schema).filter((f) => BINARY_TYPES_SET.has(f.fieldType));
}

// ─── GraphQL query building ─────────────────────────────────────────────────

export interface GraphQLQueryOptions {
    site?: string;
    query?: string;
    limit?: number;
    offset?: number;
}

/**
 * Map of dotCMS field types to their GraphQL subfield selections.
 *
 * Binary fields return a flat object with `name` (not `fileName`).
 * Image/File fields return the `DotFileasset` type with `fileName` at top level.
 */
const GRAPHQL_SUBSELECTIONS: Record<string, string> = {
    Binary: '{ name size }',
    Image: '{ fileName sortOrder description }',
    File: '{ fileName sortOrder description }',
    'Story-Block': '{ json }',
    StoryBlock: '{ json }',
    'Key-Value': '{ key value }',
    KeyValue: '{ key value }'
};

/**
 * Build a GraphQL collection query for the given content type schema.
 *
 * Requests user field variables + system identifiers (identifier, inode,
 * modDate). Uses `conLanguage { id languageCode countryCode }` and
 * `host { hostName }` for language/host resolution.
 */
export function buildGraphQLQuery(
    schema: ContentTypeSchema,
    options: GraphQLQueryOptions = {}
): string {
    const collectionName = `${schema.variable}Collection`;
    const userFields = getUserFields(schema);

    // Build field selections
    const fieldSelections = userFields
        .filter((f) => !OMIT_FIELD_TYPES.has(f.fieldType))
        .map((f) => {
            const sub = GRAPHQL_SUBSELECTIONS[f.fieldType];
            if (sub) {
                return `${f.variable} ${sub}`;
            }

            return f.variable;
        });

    // System fields — use GraphQL object types for language and host
    const systemFields = [
        'identifier',
        'inode',
        'modDate',
        'conLanguage { id languageCode countryCode }',
        'host { hostName }'
    ];

    const allFields = [...systemFields, ...fieldSelections];

    // Build query arguments
    const args: string[] = [];
    if (options.query) {
        args.push(`query: "${options.query}"`);
    }
    if (options.site) {
        args.push(`site: "${options.site}"`);
    }
    if (options.limit !== undefined) {
        args.push(`limit: ${options.limit}`);
    }
    if (options.offset !== undefined) {
        args.push(`offset: ${options.offset}`);
    }

    const argsStr = args.length > 0 ? `(${args.join(', ')})` : '';

    return `{ ${collectionName}${argsStr} { ${allFields.join(' ')} } }`;
}

// ─── Serialization (API data → .md file) ────────────────────────────────────

export interface SerializeResult {
    filename: string;
    content: string;
    binaries: BinaryFieldInfo[];
}

/**
 * Serialize a contentlet record from the API into a .md file string.
 *
 * Returns the filename, the file content (YAML frontmatter + body), and
 * a list of binary field references for sidecar downloading.
 */
export function serializeContentlet(
    record: ContentletRecord,
    schema: ContentTypeSchema,
    languageMap: LanguageMap
): SerializeResult {
    const identifier = record['identifier'] as string;
    const shortId = identifier.slice(0, SHORT_ID_LENGTH);

    // Extract languageId from nested GraphQL object or flat REST field
    const conLanguage = record['conLanguage'] as Record<string, unknown> | undefined;
    const languageId = conLanguage
        ? String(conLanguage['id'] ?? '1')
        : String(record['languageId'] ?? '1');
    const langCode = conLanguage
        ? buildLangCode(conLanguage)
        : languageMap[languageId] || languageId;

    // Determine if this is the default language (first entry or marked default)
    const isDefault = isDefaultLanguage(languageId, languageMap);
    const suffix = isDefault ? '' : `.${langCode}`;
    const filename = `${shortId}${suffix}.md`;

    const bodyFieldVar = getBodyField(schema);
    const userFields = getUserFields(schema);
    const binaries: BinaryFieldInfo[] = [];

    // Build frontmatter object — keys in insertion order
    const frontmatterObj: Record<string, unknown> = {};

    // System metadata first
    frontmatterObj['contentType'] = schema.variable;
    frontmatterObj['identifier'] = identifier;
    frontmatterObj['language'] = langCode;
    frontmatterObj['inode'] = record['inode'] as string;

    if (bodyFieldVar) {
        frontmatterObj['bodyField'] = bodyFieldVar;
    }

    // User fields (except body field and omitted types)
    for (const field of userFields) {
        if (field.variable === bodyFieldVar) continue;
        if (OMIT_FIELD_TYPES.has(field.fieldType)) continue;

        const value = record[field.variable];
        if (value === undefined || value === null) continue;

        const serialized = serializeFieldValue(value, field, shortId);

        if (BINARY_TYPES_SET.has(field.fieldType) && serialized !== undefined) {
            // Track binary for sidecar download
            const binaryInfo = extractBinaryInfo(value, field, shortId);
            if (binaryInfo) {
                binaries.push(binaryInfo);
            }
        }

        if (serialized !== undefined) {
            frontmatterObj[field.variable] = serialized;
        }
    }

    // Serialize with yaml library — block style, no line wrapping
    const doc = new Document(frontmatterObj);
    const yamlStr = doc.toString({ lineWidth: 0 });

    // Extract body content — needs field-type-aware serialization
    let bodyContent = '';
    if (bodyFieldVar) {
        const bodyField = userFields.find((f) => f.variable === bodyFieldVar);
        const rawBody = record[bodyFieldVar];
        if (rawBody !== undefined && rawBody !== null) {
            if (bodyField && JSON_FIELD_TYPES.has(bodyField.fieldType)) {
                // StoryBlock/JSON body: deserialize GraphQL { json } wrapper, then YAML-serialize
                const deserialized = serializeFieldValue(rawBody, bodyField, shortId);
                if (deserialized !== undefined) {
                    const bodyDoc = new Document(deserialized);
                    bodyContent = bodyDoc.toString({ lineWidth: 0 });
                }
            } else {
                bodyContent = String(rawBody);
            }
        }
    }

    const content = `---\n${yamlStr}---\n${bodyContent}\n`;

    return { filename, content, binaries };
}

// ─── Parsing (.md file → data) ──────────────────────────────────────────────

/**
 * Parse a .md file into frontmatter and body using gray-matter.
 */
export function parseContentFile(filePath: string, fileContent: string): ParsedContentFile {
    const parsed = matter(fileContent);

    return {
        frontmatter: parsed.data as ContentletFrontmatter,
        body: parsed.content.replace(/^\n/, ''), // trim leading newline from gray-matter
        filePath
    };
}

/**
 * Build the contentlet JSON payload for the workflow fire API from parsed data.
 */
export function buildPushPayload(
    parsed: ParsedContentFile,
    schema: ContentTypeSchema
): { contentlet: Record<string, unknown>; binaries: BinaryFieldInfo[] } {
    const { frontmatter, body } = parsed;
    const contentlet: Record<string, unknown> = {};
    const binaries: BinaryFieldInfo[] = [];

    // Copy system fields
    contentlet['contentType'] = frontmatter.contentType;
    if (frontmatter.identifier) {
        contentlet['identifier'] = frontmatter.identifier;
    }
    if (frontmatter.inode) {
        contentlet['inode'] = frontmatter.inode;
    }
    if (frontmatter.language) {
        contentlet['languageId'] = resolveLanguageId(frontmatter.language);
    }

    // Map body content back to the body field
    const bodyFieldVar = frontmatter.bodyField || getBodyField(schema);
    if (bodyFieldVar && body) {
        const bodyField = schema.fields.find((f) => f.variable === bodyFieldVar);
        if (bodyField && JSON_FIELD_TYPES.has(bodyField.fieldType)) {
            // StoryBlock/JSON body: parse YAML back to object, then JSON.stringify for the API
            try {
                let parsed2 = parseYaml(body);
                parsed2 = stripDotContentData(parsed2);
                contentlet[bodyFieldVar] = JSON.stringify(parsed2);
            } catch {
                contentlet[bodyFieldVar] = body;
            }
        } else {
            contentlet[bodyFieldVar] = body;
        }
    }

    // Map user fields from frontmatter
    const metadataSet = new Set<string>(METADATA_KEYS);
    for (const [key, value] of Object.entries(frontmatter)) {
        if (metadataSet.has(key)) continue;
        if (value === undefined || value === null) continue;

        // Look up the field in the schema — skip unknown frontmatter keys
        const field = schema.fields.find((f) => f.variable === key);
        if (!field) {
            continue; // not a known schema field, skip
        }

        if (BINARY_TYPES_SET.has(field.fieldType)) {
            if (typeof value === 'string' && value.startsWith('./')) {
                // Sidecar files are named {fieldVar}.{originalFileName}
                // Strip the field variable prefix to get the original filename
                const sidecarName = value.split('/').pop() || value;
                const prefix = `${key}.`;
                const originalFileName = sidecarName.startsWith(prefix)
                    ? sidecarName.slice(prefix.length)
                    : sidecarName;
                binaries.push({
                    fieldVariable: key,
                    localPath: value,
                    fileName: originalFileName
                });
            }
            continue; // binary fields handled via multipart, not JSON
        }

        // Relationship fields: skip — server manages relationships separately
        if (RELATIONSHIP_TYPES_SET.has(field.fieldType)) {
            continue;
        }

        // JSON/StoryBlock fields: the API expects a JSON string, not an object
        if (JSON_FIELD_TYPES.has(field.fieldType) && typeof value === 'object') {
            contentlet[key] = JSON.stringify(stripDotContentData(value));
        } else if (LIST_FIELD_TYPES.has(field.fieldType) && Array.isArray(value)) {
            // Checkbox, MultiSelect, Tag, Category: API expects comma-separated string
            contentlet[key] = value.join(',');
        } else {
            contentlet[key] = value;
        }
    }

    return { contentlet, binaries };
}

// ─── Validation ─────────────────────────────────────────────────────────────

const requiredFrontmatterSchema = z.object({
    contentType: z.string().min(1, 'contentType is required'),
    language: z.string().min(1, 'language is required')
});

const updateFrontmatterSchema = requiredFrontmatterSchema.extend({
    identifier: z.string().min(1, 'identifier is required for updates')
});

export interface ValidationResult {
    valid: boolean;
    errors: string[];
}

/**
 * Validate the parsed content file frontmatter against the schema.
 */
export function validateContentFile(
    parsed: ParsedContentFile,
    schema: ContentTypeSchema
): ValidationResult {
    const errors: string[] = [];

    // Check required frontmatter fields
    const baseResult = requiredFrontmatterSchema.safeParse(parsed.frontmatter);
    if (!baseResult.success) {
        for (const issue of baseResult.error.issues) {
            errors.push(`${issue.path.join('.')}: ${issue.message}`);
        }
    }

    // If identifier present, validate it's non-empty
    if (parsed.frontmatter.identifier !== undefined && parsed.frontmatter.identifier !== null) {
        const updateResult = updateFrontmatterSchema.safeParse(parsed.frontmatter);
        if (!updateResult.success) {
            for (const issue of updateResult.error.issues) {
                if (issue.path.includes('identifier')) {
                    errors.push(`${issue.path.join('.')}: ${issue.message}`);
                }
            }
        }
    }

    // Verify contentType matches schema
    if (parsed.frontmatter.contentType && parsed.frontmatter.contentType !== schema.variable) {
        errors.push(
            `contentType mismatch: file has "${parsed.frontmatter.contentType}" but schema is "${schema.variable}"`
        );
    }

    // Validate required user fields are present
    const userFields = getUserFields(schema);
    const bodyFieldVar = parsed.frontmatter.bodyField || getBodyField(schema);

    for (const field of userFields) {
        if (!field.required) continue;
        if (OMIT_FIELD_TYPES.has(field.fieldType)) continue;
        if (BINARY_TYPES_SET.has(field.fieldType)) continue;

        if (field.variable === bodyFieldVar) {
            if (!parsed.body || parsed.body.trim().length === 0) {
                errors.push(`required field "${field.variable}" (body) is empty`);
            }
        } else {
            const value = parsed.frontmatter[field.variable];
            if (value === undefined || value === null || value === '') {
                errors.push(`required field "${field.variable}" is missing`);
            }
        }
    }

    return { valid: errors.length === 0, errors };
}

// ─── Diff normalization ─────────────────────────────────────────────────────

/**
 * Normalize a field value for diff comparison so that semantically
 * identical values from local files and server responses compare equal.
 *
 * Handles:
 * - Binary fields: object → sidecar path string
 * - Date objects (gray-matter auto-parse) → ISO string
 * - Server date strings ("2019-07-12 18:15:00.0") → ISO string
 * - JSON/StoryBlock objects → JSON string
 */
export function normalizeForDiff(value: unknown, field: ContentTypeField, shortId: string): string {
    if (value === undefined || value === null) return '';

    // Binary fields: convert object to sidecar path
    if (BINARY_TYPES_SET.has(field.fieldType)) {
        if (typeof value === 'object') {
            const obj = value as Record<string, unknown>;
            const fileName = (obj['fileName'] || obj['name']) as string;
            if (fileName) return `./assets/${shortId}/${field.variable}.${fileName}`;
            return '';
        }
        return String(value);
    }

    // Date objects (gray-matter auto-parse) → ISO string
    if (value instanceof Date) {
        return value.toISOString();
    }

    // Server date strings like "2019-07-12 18:15:00.0" → try ISO normalization
    if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}[\sT]/.test(value)) {
        const parsed = new Date(value);
        if (!isNaN(parsed.getTime())) return parsed.toISOString();
    }

    // JSON/StoryBlock fields: unwrap GraphQL { json: ... } envelope, then stringify
    if (JSON_FIELD_TYPES.has(field.fieldType)) {
        let inner: unknown = value;

        // Server returns StoryBlock as { json: ... } — unwrap
        if (
            typeof inner === 'object' &&
            inner !== null &&
            'json' in (inner as Record<string, unknown>)
        ) {
            const jsonVal = (inner as Record<string, unknown>)['json'];
            if (typeof jsonVal === 'string') {
                try {
                    inner = JSON.parse(jsonVal);
                } catch {
                    inner = jsonVal;
                }
            } else {
                inner = jsonVal;
            }
        }

        // JSON string → parse to object for consistent stringify
        if (typeof inner === 'string') {
            try {
                inner = JSON.parse(inner);
            } catch {
                /* keep as string */
            }
        }

        return typeof inner === 'object' ? JSON.stringify(inner) : String(inner ?? '');
    }

    return String(value);
}

// ─── Internal helpers ───────────────────────────────────────────────────────

/**
 * Strip expanded dotContent nodes in StoryBlock JSON down to just
 * `{ identifier, languageId }`.
 *
 * When content is pulled via GraphQL, dotContent nodes embed the full
 * contentlet data (including relationship arrays). The server expects
 * only `{ identifier, languageId }` — sending full objects causes a
 * ClassCastException (LinkedHashMap → Contentlet).
 */
function stripDotContentData(value: unknown): unknown {
    if (value === null || value === undefined || typeof value !== 'object') {
        return value;
    }

    if (Array.isArray(value)) {
        return value.map((item) => stripDotContentData(item));
    }

    const obj = value as Record<string, unknown>;

    // dotContent node: strip attrs.data to { identifier, languageId }
    if (obj['type'] === 'dotContent' && typeof obj['attrs'] === 'object' && obj['attrs'] !== null) {
        const attrs = obj['attrs'] as Record<string, unknown>;
        if (typeof attrs['data'] === 'object' && attrs['data'] !== null) {
            const data = attrs['data'] as Record<string, unknown>;
            return {
                ...obj,
                attrs: {
                    ...attrs,
                    data: {
                        identifier: data['identifier'],
                        languageId: data['languageId']
                    }
                }
            };
        }
    }

    // Recurse into child nodes (e.g. content arrays)
    const result: Record<string, unknown> = {};
    for (const [key, val] of Object.entries(obj)) {
        result[key] = stripDotContentData(val);
    }

    return result;
}

/**
 * Serialize a field value for YAML frontmatter based on its field type.
 */
function serializeFieldValue(value: unknown, field: ContentTypeField, shortId: string): unknown {
    const { fieldType } = field;

    if (BINARY_TYPES_SET.has(fieldType)) {
        return serializeBinaryField(value, field, shortId);
    }

    if (LIST_FIELD_TYPES.has(fieldType)) {
        return serializeListField(value);
    }

    if (MAP_FIELD_TYPES.has(fieldType)) {
        // GraphQL returns Key-Value as [{key, value}, ...] — convert to map
        if (Array.isArray(value)) {
            const map: Record<string, unknown> = {};
            for (const item of value) {
                if (typeof item === 'object' && item !== null && 'key' in item) {
                    map[(item as Record<string, string>)['key']] = (item as Record<string, string>)[
                        'value'
                    ];
                }
            }
            return Object.keys(map).length > 0 ? map : value;
        }

        if (typeof value === 'string') {
            try {
                return JSON.parse(value);
            } catch {
                return value;
            }
        }

        return value;
    }

    if (JSON_FIELD_TYPES.has(fieldType)) {
        // StoryBlock from GraphQL comes as { json: ... }
        if (
            typeof value === 'object' &&
            value !== null &&
            'json' in (value as Record<string, unknown>)
        ) {
            const jsonVal = (value as Record<string, unknown>)['json'];
            if (typeof jsonVal === 'string') {
                try {
                    return JSON.parse(jsonVal);
                } catch {
                    return jsonVal;
                }
            }
            return jsonVal;
        }

        if (typeof value === 'string') {
            try {
                return JSON.parse(value);
            } catch {
                return value;
            }
        }

        return value;
    }

    // WYSIWYG fields that are NOT the body field → multiline string
    if (fieldType === 'WYSIWYG') {
        return String(value);
    }

    return value;
}

function serializeBinaryField(
    value: unknown,
    field: ContentTypeField,
    shortId: string
): string | undefined {
    if (!value) return undefined;

    if (typeof value === 'object' && value !== null) {
        const obj = value as Record<string, unknown>;
        // Image/File (DotFileasset) → { fileName }, Binary (flat) → { name }
        const fileName = (obj['fileName'] || obj['name']) as string;
        if (fileName) {
            return `./assets/${shortId}/${field.variable}.${fileName}`;
        }
        // No filename found in object — skip
        return undefined;
    }

    if (typeof value === 'string') {
        return value;
    }

    return undefined;
}

function serializeListField(value: unknown): unknown[] {
    if (Array.isArray(value)) {
        return value;
    }

    if (typeof value === 'string') {
        return value
            .split(',')
            .map((s) => s.trim())
            .filter((s) => s.length > 0);
    }

    return [value];
}

function extractBinaryInfo(
    value: unknown,
    field: ContentTypeField,
    shortId: string
): BinaryFieldInfo | null {
    if (typeof value === 'object' && value !== null) {
        const obj = value as Record<string, unknown>;
        const fileName = (obj['fileName'] || obj['name']) as string;
        if (fileName) {
            const localPath = `./assets/${shortId}/${field.variable}.${fileName}`;

            return {
                fieldVariable: field.variable,
                localPath,
                fileName
            };
        }
    }

    return null;
}

/**
 * Build a language code like "en-US" from a GraphQL conLanguage object.
 */
function buildLangCode(conLanguage: Record<string, unknown>): string {
    const langCode = (conLanguage['languageCode'] as string) || 'en';
    const countryCode = conLanguage['countryCode'] as string;
    return countryCode ? `${langCode}-${countryCode}` : langCode;
}

/**
 * Determine if a languageId is the default language.
 * Convention: languageId "1" is always default in dotCMS.
 */
function isDefaultLanguage(languageId: string, languageMap: LanguageMap): boolean {
    // Language ID 1 is the default in dotCMS
    if (languageId === '1') return true;

    // If there's only one language in the map, it's the default
    const keys = Object.keys(languageMap);
    if (keys.length === 1) return keys[0] === languageId;

    return false;
}

/** Common language code → ID mapping. dotCMS language 1 is always the default. */
const LANG_CODE_TO_ID: Record<string, string> = {
    'en-US': '1',
    en: '1'
};

/**
 * Resolve a language code like "en-US" back to a languageId.
 * Falls back to "1" (default) if unknown.
 */
function resolveLanguageId(langCode: string): string {
    return LANG_CODE_TO_ID[langCode] || '1';
}
