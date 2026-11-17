import {
    CONTENT_ENDPOINTS,
    getContentType,
    isIdentifierLike,
    languageLabel,
    languageVersionsOf,
    searchContentlets,
    type ContentVersion,
    type FieldSummary
} from './shared/content';
import { loadLanguages, RESOLVE_ENDPOINTS, resolveLanguageId, resolveSite } from './shared/resolve';

import { ValidationError, type DotCMSRuntime } from '../../runtime';
import { CONTEXT_ENDPOINTS, type Endpoint } from '../toolkit/endpoints';

/**
 * Every endpoint `upsertContent` calls. The `upsert_content` tool enforces exactly this list,
 * and `upsert-content.spec.ts` checks every request against it. Add a request, add it here.
 */
export const UPSERT_CONTENT_ENDPOINTS: readonly Endpoint[] = [
    ...CONTEXT_ENDPOINTS,
    ...RESOLVE_ENDPOINTS,
    ...CONTENT_ENDPOINTS,
    'PUT /api/v1/workflow/actions/default/fire/{action}'
];

/** Keys the operation sets itself — passing them inside `fields` is refused. */
const SYSTEM_KEYS = new Set([
    'contentType',
    'stInode',
    'languageId',
    'identifier',
    'inode',
    'contentHost'
]);

/** What a relationship check can report. */
export type RelationshipIssueCode =
    | 'related-content-not-found'
    | 'related-content-missing-language'
    | 'related-content-wrong-type'
    | 'relationship-not-resolved';

export interface RelationshipIssue {
    code: RelationshipIssueCode;
    field: string;
    relatedIdentifier?: string;
    requestedLanguage?: string;
    availableLanguages?: string[];
    detail: string;
}

export interface UpsertContentOptions {
    dotcms: DotCMSRuntime;
    /** Content type variable, e.g. "Blog". */
    contentType: string;
    /**
     * Language to write. Required on purpose: when it is omitted, dotCMS writes to the
     * instance's default language, which is often not the one the caller had in mind.
     */
    languageId: number;
    /** Field values keyed by exact field variable. Relationship values: id, ids, or "id1,id2". */
    fields: Record<string, unknown>;
    /** Existing identifier: update it, or add this language version to it. Omit to create. */
    identifier?: string;
    /** Site (hostname or identifier) for new content. Omit to use the instance default. */
    site?: string;
    /** Publish (default) or only save a working version. */
    publish?: boolean;
    /** What to do when a relationship value is unusable in this language. Default "warn". */
    onRelationshipIssue?: 'warn' | 'fail';
    /** @internal Read-back retries while related content is still being cached. */
    readBackAttempts?: number;
    /** @internal Delay between read-back attempts, in ms. */
    readBackDelayMs?: number;
}

export interface UpsertContentManifest {
    identifier: string;
    inode: string;
    contentType: string;
    languageId: number;
    language: string;
    /** What the call did. */
    outcome: 'created' | 'created-language-version' | 'updated';
    /** The workflow action that was fired. */
    action: 'PUBLISH' | 'NEW' | 'EDIT';
    live: boolean;
    /** Every language this contentlet now exists in. */
    languages: string[];
    /** Each relationship field written, with the ids sent and the ids that resolve. */
    relationships: Array<{ field: string; sent: string[]; resolved: string[] }>;
    /** Problems found before or after the write. Empty when everything resolved. */
    warnings: RelationshipIssue[];
}

/**
 * Create or update one contentlet in one language, and check that its relationships resolve.
 *
 * Absorbs the sharp edges of `PUT /api/v1/workflow/actions/default/fire/{action}`:
 *  - field keys are checked against the type's real field variables (an unknown or wrong-case
 *    key is otherwise dropped silently);
 *  - relationship values may be an id, an array of ids, or objects with `identifier`; the fire
 *    endpoint only accepts a comma-separated string (an array is a 500);
 *  - related ids are checked BEFORE the write: one that does not exist, has no version in the
 *    target language, or belongs to another type is reported — the fire accepts all three and
 *    the field then resolves empty;
 *  - after the write, the relationships are read back and compared with what was sent.
 */
export async function upsertContent(options: UpsertContentOptions): Promise<UpsertContentManifest> {
    const { dotcms } = options;
    const type = await getContentType(dotcms, options.contentType);
    const languageId = await resolveLanguageId(dotcms, options.languageId);
    const languages = await loadLanguages(dotcms);
    const language = languageLabel(languageId, languages);

    const fieldsByVariable = new Map(type.fields.map((field) => [field.variable, field]));
    checkFieldKeys(type.variable, options.fields, fieldsByVariable);

    // Relationship values → the comma-separated string the fire endpoint accepts.
    const contentlet: Record<string, unknown> = {};
    const sentRelationships: Array<{ field: FieldSummary; ids: string[] }> = [];
    for (const [key, value] of Object.entries(options.fields)) {
        const field = fieldsByVariable.get(key) as FieldSummary;
        if (field.relationship) {
            const ids = relationshipIds(key, value);
            sentRelationships.push({ field, ids });
            contentlet[key] = ids.join(',');
        } else {
            contentlet[key] = value;
        }
    }

    // Existing content: what languages it has decides between update and new language version.
    const identifier = options.identifier?.trim() || undefined;
    let existing: ContentVersion[] = [];
    if (identifier) {
        if (!isIdentifierLike(identifier)) {
            throw new ValidationError(`"${identifier}" is not a valid identifier.`);
        }

        existing = (await languageVersionsOf(dotcms, [identifier])).get(identifier) ?? [];
        if (existing.length === 0) {
            throw new ValidationError(
                `No content with identifier "${identifier}" exists. Omit identifier to create ` +
                    `new content, or use the lookup tool to find the right one.`
            );
        }

        const otherType = existing.find(
            (version) => version.contentType && version.contentType !== type.variable
        );
        if (otherType) {
            throw new ValidationError(
                `Identifier "${identifier}" belongs to ${otherType.contentType}, not ` +
                    `${type.variable}.`
            );
        }
    }

    const warnings = await checkRelatedContent(
        dotcms,
        sentRelationships,
        languageId,
        language,
        languages
    );
    if (warnings.length > 0 && options.onRelationshipIssue === 'fail') {
        throw new ValidationError(
            'Nothing was written: some relationship values cannot resolve in this language. ' +
                warnings.map((warning) => warning.detail).join(' '),
            warnings
        );
    }

    const outcome: UpsertContentManifest['outcome'] = !identifier
        ? 'created'
        : existing.some((version) => version.languageId === languageId)
          ? 'updated'
          : 'created-language-version';
    const publish = options.publish ?? true;
    const action: UpsertContentManifest['action'] = publish
        ? 'PUBLISH'
        : outcome === 'updated'
          ? 'EDIT'
          : 'NEW';

    contentlet['contentType'] = type.variable;
    contentlet['languageId'] = languageId;
    if (identifier) {
        contentlet['identifier'] = identifier;
    }

    if (options.site) {
        contentlet['contentHost'] = (await resolveSite(dotcms, options.site)).identifier;
    }

    const fired = (await dotcms.request({
        method: 'PUT',
        path: `/api/v1/workflow/actions/default/fire/${action}`,
        query: { indexPolicy: 'WAIT_FOR' },
        body: { contentlet }
    })) as { entity?: Record<string, unknown> };
    const entity = fired?.entity ?? {};
    const savedIdentifier = typeof entity['identifier'] === 'string' ? entity['identifier'] : '';
    if (!savedIdentifier) {
        throw new Error(`The ${action} fire returned no identifier.`);
    }

    const readBack = await readRelationships(dotcms, {
        identifier: savedIdentifier,
        languageId,
        sent: sentRelationships,
        // Ids already reported as unresolvable are not waited for.
        skip: new Set(warnings.map((warning) => warning.relatedIdentifier ?? '')),
        attempts: options.readBackAttempts ?? 3,
        delayMs: options.readBackDelayMs ?? 1500
    });

    for (const relationship of readBack) {
        const missing = relationship.sent.filter((id) => !relationship.resolved.includes(id));
        const explained = new Set(warnings.map((warning) => warning.relatedIdentifier));
        const unexplained = missing.filter((id) => !explained.has(id));
        if (unexplained.length > 0) {
            warnings.push({
                code: 'relationship-not-resolved',
                field: relationship.field,
                detail:
                    `${relationship.field}: ${unexplained.join(', ')} was sent but does not ` +
                    `resolve yet in ${language}. Related content can take a few seconds to ` +
                    `resolve after a change; read the content again before changing anything.`
            });
        }
    }

    const finalVersions =
        (await languageVersionsOf(dotcms, [savedIdentifier])).get(savedIdentifier) ?? [];

    return {
        identifier: savedIdentifier,
        inode: typeof entity['inode'] === 'string' ? entity['inode'] : '',
        contentType: type.variable,
        languageId,
        language,
        outcome,
        action,
        live: entity['live'] === true,
        languages: [...new Set(finalVersions.map((v) => languageLabel(v.languageId, languages)))],
        relationships: readBack.map(({ field, sent, resolved }) => ({ field, sent, resolved })),
        warnings
    };
}

function checkFieldKeys(
    typeVariable: string,
    fields: Record<string, unknown>,
    fieldsByVariable: Map<string, FieldSummary>
): void {
    const problems: string[] = [];

    for (const key of Object.keys(fields)) {
        if (fieldsByVariable.has(key)) {
            continue;
        }

        if (SYSTEM_KEYS.has(key)) {
            problems.push(
                `"${key}" is set by the tool — use the ${key === 'contentHost' ? 'site' : key} ` +
                    'parameter instead.'
            );
            continue;
        }

        const caseMatch = [...fieldsByVariable.keys()].find(
            (variable) => variable.toLowerCase() === key.toLowerCase()
        );
        problems.push(
            caseMatch
                ? `"${key}" is not a field — did you mean "${caseMatch}"? Field variables are ` +
                      'case-sensitive.'
                : `"${key}" is not a field of ${typeVariable}.`
        );
    }

    if (problems.length > 0) {
        throw new ValidationError(
            `${problems.join(' ')} dotCMS drops unknown keys without an error, so they are ` +
                `refused here. Fields of ${typeVariable}: ${[...fieldsByVariable.keys()].join(', ')}.`
        );
    }
}

/** Normalize a relationship value to a list of identifiers. Empty clears the relationship. */
function relationshipIds(field: string, value: unknown): string[] {
    const items: unknown[] =
        value === null || value === undefined || value === ''
            ? []
            : Array.isArray(value)
              ? value
              : typeof value === 'string'
                ? value.split(',')
                : [value];

    return [
        ...new Set(
            items.map((item) => {
                const id =
                    typeof item === 'string'
                        ? item.trim()
                        : item && typeof item === 'object'
                          ? String((item as Record<string, unknown>)['identifier'] ?? '').trim()
                          : '';
                if (!id || !isIdentifierLike(id)) {
                    throw new ValidationError(
                        `Relationship field "${field}" takes content identifiers (a string, an ` +
                            `array of strings, or objects with "identifier"); got ` +
                            `${JSON.stringify(item)}.`
                    );
                }

                return id;
            })
        )
    ];
}

async function checkRelatedContent(
    dotcms: DotCMSRuntime,
    sent: Array<{ field: FieldSummary; ids: string[] }>,
    languageId: number,
    language: string,
    languages: Array<{ id: number; isoCode: string }>
): Promise<RelationshipIssue[]> {
    const all = sent.flatMap((relationship) => relationship.ids);
    if (all.length === 0) {
        return [];
    }

    const versions = await languageVersionsOf(dotcms, all);
    const issues: RelationshipIssue[] = [];

    for (const { field, ids } of sent) {
        for (const id of ids) {
            const found = versions.get(id) ?? [];
            if (found.length === 0) {
                issues.push({
                    code: 'related-content-not-found',
                    field: field.variable,
                    relatedIdentifier: id,
                    detail: `${field.variable}: no content with identifier ${id} exists.`
                });
                continue;
            }

            const target = field.relationship?.target;
            const actualType = found[0].contentType;
            if (target && actualType && actualType !== target) {
                issues.push({
                    code: 'related-content-wrong-type',
                    field: field.variable,
                    relatedIdentifier: id,
                    detail: `${field.variable}: ${id} is a ${actualType}, but the field relates to ${target}.`
                });
                continue;
            }

            if (!found.some((version) => version.languageId === languageId)) {
                const available = [
                    ...new Set(found.map((v) => languageLabel(v.languageId, languages)))
                ];
                issues.push({
                    code: 'related-content-missing-language',
                    field: field.variable,
                    relatedIdentifier: id,
                    requestedLanguage: language,
                    availableLanguages: available,
                    detail:
                        `${field.variable}: ${id} exists only in ${available.join(', ')}, so it ` +
                        `will resolve empty in ${language}. Create its ${language} version ` +
                        `(same identifier) first.`
                });
            }
        }
    }

    return issues;
}

function idsOf(value: unknown): string[] {
    const items = Array.isArray(value) ? value : value ? [value] : [];

    return items
        .map((item) =>
            item && typeof item === 'object'
                ? (item as Record<string, unknown>)['identifier']
                : typeof item === 'string'
                  ? item
                  : undefined
        )
        .filter((id): id is string => typeof id === 'string');
}

async function readRelationships(
    dotcms: DotCMSRuntime,
    args: {
        identifier: string;
        languageId: number;
        sent: Array<{ field: FieldSummary; ids: string[] }>;
        skip: Set<string>;
        attempts: number;
        delayMs: number;
    }
): Promise<Array<{ field: string; sent: string[]; resolved: string[] }>> {
    if (args.sent.length === 0) {
        return [];
    }

    let result: Array<{ field: string; sent: string[]; resolved: string[] }> = [];
    const attempts = Math.max(1, args.attempts);

    for (let attempt = 1; attempt <= attempts; attempt++) {
        const [contentlet] = await searchContentlets(
            dotcms,
            `+identifier:${args.identifier} +languageId:${args.languageId}`,
            { limit: 1, depth: 1 }
        );
        result = args.sent.map(({ field, ids }) => ({
            field: field.variable,
            sent: ids,
            resolved: idsOf(contentlet?.[field.variable])
        }));

        const complete = result.every(({ sent, resolved }) =>
            sent.every((id) => args.skip.has(id) || resolved.includes(id))
        );
        if (complete || attempt === attempts) {
            break;
        }

        await new Promise((resolve) => setTimeout(resolve, args.delayMs));
    }

    return result;
}
