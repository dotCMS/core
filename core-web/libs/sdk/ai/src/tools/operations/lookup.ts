import {
    CONTENT_ENDPOINTS,
    escapeLucenePhrase,
    getContentType,
    groupByIdentifier,
    languageLabel,
    searchContentlets,
    toVersions,
    type RelationshipCardinality
} from './shared/content';
import { loadLanguages, RESOLVE_ENDPOINTS } from './shared/resolve';

import { HttpError, ValidationError, type DotCMSRuntime } from '../../runtime';
import { CONTEXT_ENDPOINTS, type Endpoint } from '../toolkit/endpoints';

/**
 * Every endpoint `lookupContent` calls — all reads. The `lookup` tool enforces exactly this
 * list, and `lookup.spec.ts` checks every request against it. Add a request, add it here.
 */
export const LOOKUP_ENDPOINTS: readonly Endpoint[] = [
    ...CONTEXT_ENDPOINTS,
    ...RESOLVE_ENDPOINTS,
    ...CONTENT_ENDPOINTS,
    'GET /api/v1/relationships'
];

/** Results per call when the caller does not say. */
export const DEFAULT_LOOKUP_LIMIT = 20;
/** Upper bound on results per call. */
export const MAX_LOOKUP_LIMIT = 100;
/** Upper bound on the language versions requested from the index in one search. */
const MAX_VERSIONS_PER_SEARCH = 500;

export interface LookupOptions {
    dotcms: DotCMSRuntime;
    /** Content type variable to look in, e.g. "Author". */
    contentType: string;
    /** Phrase match on one field (case-insensitive). `field` defaults to `title` or `name`. */
    match?: { field?: string; value: string };
    /** Extra Lucene clauses appended to the query, e.g. "+Author.role:editor". */
    query?: string;
    /** Include archived content. Default false. */
    includeArchived?: boolean;
    /** Maximum distinct contentlets to return. Default 20, max 100. */
    limit?: number;
}

/** One language version of a result. */
export interface LookupVersion {
    languageId: number;
    language: string;
    live: boolean;
    working: boolean;
    archived: boolean;
    inode: string;
}

/** One contentlet, with every language version that exists for it. */
export interface LookupResult {
    identifier: string;
    title: string;
    /** True when at least one language version is published. */
    live: boolean;
    versions: LookupVersion[];
    /** Language codes this contentlet exists in. */
    languages: string[];
    /** Instance languages this contentlet does NOT exist in. */
    missingLanguages: string[];
}

export interface LookupManifest {
    contentType: { variable: string; name: string; id: string };
    /** The Lucene query that was run. */
    query: string;
    /** Distinct contentlets returned. */
    count: number;
    /** True when more matches exist than `limit` allowed. */
    truncated: boolean;
    results: LookupResult[];
    /** This type's own relationship fields and what they point to. */
    relationshipFields: Array<{
        field: string;
        target: string;
        cardinality?: RelationshipCardinality;
        required: boolean;
    }>;
    /** Relationship fields on OTHER types that point to this type ("Blog.author"). */
    referencedBy: string[];
    /** Every language on the instance. */
    instanceLanguages: string[];
    /** What to do with this answer. */
    guidance: string[];
}

function clampLimit(limit: number | undefined): number {
    if (limit === undefined) {
        return DEFAULT_LOOKUP_LIMIT;
    }

    if (!Number.isInteger(limit) || limit < 1) {
        throw new ValidationError('limit must be a positive integer.');
    }

    return Math.min(limit, MAX_LOOKUP_LIMIT);
}

/**
 * Find what already exists for a content type before creating or relating content.
 *
 * Built for the question "does this already exist, and in which languages?":
 *  - unpublished content is included (the search runs in preview mode for a back-end token);
 *  - results are grouped per contentlet, with every language version it has and the instance
 *    languages it is missing — a relationship only resolves in a language the related
 *    content exists in;
 *  - the type's relationship fields, and the fields on other types that point to it, are
 *    listed so the caller knows which content is shared and should be reused, not duplicated.
 *
 * Read-only.
 */
export async function lookupContent(options: LookupOptions): Promise<LookupManifest> {
    const { dotcms } = options;
    const limit = clampLimit(options.limit);
    const type = await getContentType(dotcms, options.contentType);

    const clauses = [`+contentType:${type.variable}`];
    if (!options.includeArchived) {
        clauses.push('+deleted:false');
    }

    if (options.match) {
        clauses.push(matchClause(type.variable, type.fields, options.match));
    }

    if (options.query?.trim()) {
        clauses.push(options.query.trim());
    }

    const query = clauses.join(' ');
    const languages = await loadLanguages(dotcms);
    const versionsWanted = Math.min(
        (limit + 1) * Math.max(languages.length, 1),
        MAX_VERSIONS_PER_SEARCH
    );

    const groups = groupByIdentifier(
        toVersions(
            await searchContentlets(dotcms, query, {
                limit: versionsWanted,
                sort: 'modDate desc'
            })
        )
    );

    const identifiers = [...groups.keys()];
    const truncated = identifiers.length > limit;
    const instanceLanguages = languages.map((language) => language.isoCode).filter(Boolean);

    const results: LookupResult[] = identifiers.slice(0, limit).map((identifier) => {
        const versions = groups.get(identifier) ?? [];
        const present = new Set(versions.map((version) => version.languageId));

        return {
            identifier,
            title: versions.find((version) => version.title)?.title ?? '',
            live: versions.some((version) => version.live),
            versions: versions.map((version) => ({
                languageId: version.languageId,
                language: languageLabel(version.languageId, languages),
                live: version.live,
                working: version.working,
                archived: version.archived,
                inode: version.inode
            })),
            languages: [...present].map((id) => languageLabel(id, languages)),
            missingLanguages: languages
                .filter((language) => !present.has(language.id))
                .map((language) => language.isoCode)
        };
    });

    const relationshipFields = type.fields
        .filter((field) => field.relationship)
        .map((field) => ({
            field: field.variable,
            target: field.relationship?.target ?? '',
            cardinality: field.relationship?.cardinality,
            required: field.required
        }));

    const referencedBy = await referencingFields(dotcms, type.id, type.variable);

    return {
        contentType: { variable: type.variable, name: type.name, id: type.id },
        query,
        count: results.length,
        truncated,
        results,
        relationshipFields,
        referencedBy,
        instanceLanguages,
        guidance: guidanceFor(results, referencedBy, options.match !== undefined)
    };
}

function matchClause(
    typeVariable: string,
    fields: Array<{ variable: string; type: string }>,
    match: { field?: string; value: string }
): string {
    const value = match.value.trim();
    if (!value) {
        throw new ValidationError('match.value must be a non-empty string.');
    }

    const fieldName =
        match.field?.trim() ||
        ['title', 'name'].find((candidate) => fields.some((f) => f.variable === candidate));
    const field = fields.find((f) => f.variable === fieldName);

    if (!field) {
        const available = fields
            .filter((f) => f.type !== 'Relationship')
            .map((f) => f.variable)
            .join(', ');
        throw new ValidationError(
            match.field
                ? `Field "${match.field}" is not a field of ${typeVariable} (field variables are ` +
                      `case-sensitive). Fields: ${available}.`
                : `${typeVariable} has no "title" or "name" field — name the field to match in ` +
                      `match.field. Fields: ${available}.`
        );
    }

    if (field.type === 'Relationship') {
        throw new ValidationError(
            `"${field.variable}" is a relationship field and cannot be phrase-matched. Look up ` +
                `the related type instead.`
        );
    }

    // Field terms in the index are prefixed with the type variable; an unprefixed field name
    // matches nothing for most custom fields.
    return `+${typeVariable}.${field.variable}:"${escapeLucenePhrase(value)}"`;
}

/**
 * Relationship fields declared on other types that point to `typeId`, as "OwnerType.field".
 * Best effort: a failure here leaves the list empty rather than failing the lookup.
 */
async function referencingFields(
    dotcms: DotCMSRuntime,
    typeId: string,
    typeVariable: string
): Promise<string[]> {
    try {
        const response = (await dotcms.request({
            path: '/api/v1/relationships',
            query: { contentTypeId: typeId }
        })) as { entity?: unknown };
        const entries = Array.isArray(response?.entity) ? response.entity : [];
        const values = entries
            .map((entry) =>
                entry && typeof entry === 'object'
                    ? (entry as Record<string, unknown>)['relationTypeValue']
                    : undefined
            )
            .filter((value): value is string => typeof value === 'string' && value.includes('.'))
            .filter((value) => value.split('.')[0] !== typeVariable);

        return [...new Set(values)];
    } catch (error) {
        if (error instanceof HttpError) {
            return [];
        }

        throw error;
    }
}

function guidanceFor(results: LookupResult[], referencedBy: string[], matched: boolean): string[] {
    const guidance: string[] = [];

    if (results.length === 0) {
        guidance.push(
            matched
                ? 'No match, published or not. Before creating one, consider a broader match ' +
                      '(another field, or no match at all) — titles are often written differently.'
                : 'This content type has no content yet (published or not).'
        );
    } else {
        guidance.push(
            'Results include unpublished (working) content. Reuse an existing identifier ' +
                'instead of creating a duplicate.'
        );
    }

    if (results.some((result) => result.missingLanguages.length > 0)) {
        guidance.push(
            'A relationship resolves only in a language the related content exists in. Before ' +
                'linking an item from content in one of its missingLanguages, create that ' +
                'language version (same identifier, that languageId) — or the field will resolve ' +
                'empty in that language.'
        );
    }

    if (referencedBy.length > 0) {
        guidance.push(
            `This type is shared: it is referenced by ${referencedBy.join(', ')}. Prefer ` +
                'reusing existing items over creating new ones.'
        );
    }

    return guidance;
}
