import { HttpError, ValidationError, type DotCMSRuntime } from '../../../runtime';
import { type Endpoint } from '../../toolkit/endpoints';

/**
 * Every endpoint this module calls. The content operations (`lookup`, `upsert_content`) include
 * these in their own lists, and their specs check every request against them.
 */
export const CONTENT_ENDPOINTS: readonly Endpoint[] = [
    'GET /api/v1/contenttype/id/{idOrVar}',
    'POST /api/content/_search'
];

/** Relationship cardinality, by the ordinal dotCMS stores on the field. */
export type RelationshipCardinality = 'one-to-many' | 'many-to-many' | 'one-to-one' | 'many-to-one';

const CARDINALITY_BY_ORDINAL: Record<number, RelationshipCardinality> = {
    0: 'one-to-many',
    1: 'many-to-many',
    2: 'one-to-one',
    3: 'many-to-one'
};

/** A content type field, reduced to what the content operations need. */
export interface FieldSummary {
    variable: string;
    name: string;
    /** Short field class, e.g. `Text`, `Relationship`, `Row`. */
    type: string;
    required: boolean;
    /** Present for relationship fields only. */
    relationship?: {
        /** Variable of the related content type. */
        target: string;
        cardinality?: RelationshipCardinality;
    };
}

/** A content type as the content operations see it. */
export interface ContentTypeDefinition {
    id: string;
    variable: string;
    name: string;
    /** Data fields only — layout fields (rows, columns, tabs) are left out. */
    fields: FieldSummary[];
}

/** Layout-only field classes: they hold no content and are never part of a contentlet body. */
const LAYOUT_FIELD_TYPES = new Set(['Row', 'Column', 'Tab', 'LineDivider', 'TabDivider']);

function asRecord(value: unknown): Record<string, unknown> | undefined {
    return value && typeof value === 'object' && !Array.isArray(value)
        ? (value as Record<string, unknown>)
        : undefined;
}

function entityOf(response: unknown): unknown {
    const record = asRecord(response);

    return record && 'entity' in record ? record['entity'] : response;
}

function readString(record: Record<string, unknown> | undefined, key: string): string | undefined {
    const value = record?.[key];

    return typeof value === 'string' && value ? value : undefined;
}

/** `com.dotcms.contenttype.model.field.ImmutableTextField` → `Text`. */
function shortFieldType(clazz: string | undefined): string {
    if (!clazz) {
        return 'Unknown';
    }

    const last = clazz.split('.').pop() ?? clazz;

    return last.replace(/^Immutable/, '').replace(/Field$/, '') || last;
}

function toFieldSummary(raw: unknown): FieldSummary | undefined {
    const record = asRecord(raw);
    const variable = readString(record, 'variable');
    if (!record || !variable) {
        return undefined;
    }

    const type = shortFieldType(readString(record, 'clazz'));
    const summary: FieldSummary = {
        variable,
        name: readString(record, 'name') ?? variable,
        type,
        required: record['required'] === true
    };

    if (type === 'Relationship') {
        const relationships = asRecord(record['relationships']);
        // `velocityVar` is the related type ("Author") or, for the second side of a
        // relationship that is already defined, "OwnerType.field". The type is what callers need.
        const velocityVar =
            readString(relationships, 'velocityVar') ?? readString(record, 'relationType') ?? '';
        const ordinal = Number(relationships?.['cardinality'] ?? record['values']);
        summary.relationship = {
            target: velocityVar.split('.')[0],
            cardinality: CARDINALITY_BY_ORDINAL[ordinal]
        };
    }

    return summary;
}

/**
 * Read a content type by variable (or id) — the fields with their real variables, and each
 * relationship field's target and cardinality.
 *
 * A miss is a `ValidationError` naming close matches from the session context, because the
 * usual cause is a wrong-case or misspelled variable, which the caller can fix.
 */
export async function getContentType(
    dotcms: DotCMSRuntime,
    idOrVar: string
): Promise<ContentTypeDefinition> {
    const wanted = idOrVar.trim();
    if (!wanted) {
        throw new ValidationError('contentType must be a non-empty content type variable.');
    }

    let raw: unknown;
    try {
        raw = entityOf(
            await dotcms.request({ path: `/api/v1/contenttype/id/${encodeURIComponent(wanted)}` })
        );
    } catch (error) {
        if (error instanceof HttpError && (error.status === 404 || error.status === 400)) {
            throw new ValidationError(await contentTypeNotFoundMessage(dotcms, wanted));
        }

        throw error;
    }

    const record = asRecord(raw);
    const id = readString(record, 'id');
    const variable = readString(record, 'variable');
    if (!record || !id || !variable) {
        throw new ValidationError(await contentTypeNotFoundMessage(dotcms, wanted));
    }

    const fields = (Array.isArray(record['fields']) ? record['fields'] : [])
        .map(toFieldSummary)
        .filter((field): field is FieldSummary => !!field && !LAYOUT_FIELD_TYPES.has(field.type));

    return { id, variable, name: readString(record, 'name') ?? variable, fields };
}

async function contentTypeNotFoundMessage(dotcms: DotCMSRuntime, wanted: string) {
    let candidates: string[] = [];
    try {
        const { contentTypes } = await dotcms.loadContext();
        const needle = wanted.toLowerCase();
        candidates = contentTypes
            .map((type) => type.variable)
            .filter(
                (variable) =>
                    variable.toLowerCase().includes(needle) ||
                    needle.includes(variable.toLowerCase())
            )
            .slice(0, 10);
    } catch {
        // Suggestions are a convenience; the message stands without them.
    }

    const hint = candidates.length
        ? ` Similar content types: ${candidates.join(', ')}.`
        : ' Content type variables are case-sensitive.';

    return `Content type "${wanted}" was not found.${hint}`;
}

/** One language version of a contentlet, as the search index reports it. */
export interface ContentVersion {
    identifier: string;
    inode: string;
    contentType: string;
    languageId: number;
    title: string;
    live: boolean;
    working: boolean;
    archived: boolean;
}

function toVersion(raw: unknown): ContentVersion | undefined {
    const record = asRecord(raw);
    const identifier = readString(record, 'identifier');
    if (!record || !identifier) {
        return undefined;
    }

    return {
        identifier,
        inode: readString(record, 'inode') ?? '',
        contentType: readString(record, 'contentType') ?? '',
        languageId: Number(record['languageId']) || 0,
        title: readString(record, 'title') ?? '',
        live: record['live'] === true,
        working: record['working'] === true,
        archived: record['archived'] === true
    };
}

/**
 * Run a Lucene query through `POST /api/content/_search` and return the raw contentlets.
 *
 * With an API token from a back-end user this endpoint answers in preview mode: each match is
 * its latest WORKING version — unpublished content included — one entry per language version.
 */
export async function searchContentlets(
    dotcms: DotCMSRuntime,
    query: string,
    options: { limit?: number; depth?: number; sort?: string } = {}
): Promise<Record<string, unknown>[]> {
    const body: Record<string, unknown> = { query, limit: options.limit ?? 50 };
    if (options.depth !== undefined) {
        body['depth'] = options.depth;
    }
    if (options.sort) {
        body['sort'] = options.sort;
    }

    const response = asRecord(
        entityOf(await dotcms.request({ method: 'POST', path: '/api/content/_search', body }))
    );
    const view = asRecord(response?.['jsonObjectView']);
    const contentlets = view?.['contentlets'] ?? response?.['contentlets'];

    return (Array.isArray(contentlets) ? contentlets : [])
        .map(asRecord)
        .filter((record): record is Record<string, unknown> => !!record);
}

/** Parse search results into language versions. */
export function toVersions(contentlets: unknown[]): ContentVersion[] {
    return contentlets.map(toVersion).filter((version): version is ContentVersion => !!version);
}

/** Group language versions by identifier, keeping first-seen order. */
export function groupByIdentifier(versions: ContentVersion[]): Map<string, ContentVersion[]> {
    const groups = new Map<string, ContentVersion[]>();
    for (const version of versions) {
        const group = groups.get(version.identifier) ?? [];
        group.push(version);
        groups.set(version.identifier, group);
    }

    return groups;
}

/** dotCMS identifiers are hex (optionally dashed); anything else cannot go into a query. */
const IDENTIFIER_PATTERN = /^[0-9a-zA-Z-]{8,64}$/;

export function isIdentifierLike(value: string): boolean {
    return IDENTIFIER_PATTERN.test(value);
}

/**
 * Every language version of the given identifiers, keyed by identifier. An identifier with no
 * entry does not exist (or is not visible to the token).
 */
export async function languageVersionsOf(
    dotcms: DotCMSRuntime,
    identifiers: string[]
): Promise<Map<string, ContentVersion[]>> {
    const unique = [...new Set(identifiers)].filter(isIdentifierLike);
    if (unique.length === 0) {
        return new Map();
    }

    const contentlets = await searchContentlets(dotcms, `+identifier:(${unique.join(' ')})`, {
        limit: Math.min(unique.length * 20, 500)
    });

    return groupByIdentifier(toVersions(contentlets));
}

/** Escape a value for use inside a quoted Lucene phrase. */
export function escapeLucenePhrase(value: string): string {
    return value.replace(/[\\"]/g, '\\$&');
}

/** `{ id, isoCode }` → `"en-us (1)"` style label, falling back to the bare id. */
export function languageLabel(
    languageId: number,
    languages: Array<{ id: number; isoCode: string }>
): string {
    const language = languages.find((entry) => entry.id === languageId);

    return language?.isoCode ? language.isoCode : String(languageId);
}
