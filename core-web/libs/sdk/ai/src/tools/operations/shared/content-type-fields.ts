import { HttpError, ValidationError, type DotCMSRuntime } from '../../../runtime';
import { type Endpoint } from '../../toolkit/endpoints';

import type { RelationshipCardinality } from './content';

/** Every endpoint this module calls. */
export const CONTENT_TYPE_FIELD_ENDPOINTS: readonly Endpoint[] = [
    'GET /api/v1/contenttype/id/{idOrVar}',
    'GET /api/v1/workflow/schemes',
    'GET /api/v1/relationships'
];

/** Field types a caller can ask for, by friendly name. */
export const FIELD_TYPES = [
    'text',
    'integer',
    'decimal',
    'textarea',
    'wysiwyg',
    'story-block',
    'select',
    'multi-select',
    'radio',
    'checkbox',
    'date',
    'time',
    'date-time',
    'binary',
    'image',
    'file',
    'tag',
    'category',
    'key-value',
    'json',
    'host-folder',
    'hidden',
    'constant',
    'relationship'
] as const;

export type FieldType = (typeof FIELD_TYPES)[number];

export const CARDINALITIES = ['one-to-many', 'many-to-many', 'one-to-one', 'many-to-one'] as const;

const FIELD_CLASS_PREFIX = 'com.dotcms.contenttype.model.field.Immutable';

/** Friendly type → field class (and a data type, where the class alone is not enough). */
const FIELD_CLASS: Record<FieldType, { clazz: string; dataType?: string }> = {
    text: { clazz: 'TextField' },
    integer: { clazz: 'TextField', dataType: 'INTEGER' },
    decimal: { clazz: 'TextField', dataType: 'FLOAT' },
    textarea: { clazz: 'TextAreaField' },
    wysiwyg: { clazz: 'WysiwygField' },
    'story-block': { clazz: 'StoryBlockField' },
    select: { clazz: 'SelectField' },
    'multi-select': { clazz: 'MultiSelectField' },
    radio: { clazz: 'RadioField' },
    checkbox: { clazz: 'CheckboxField' },
    date: { clazz: 'DateField' },
    time: { clazz: 'TimeField' },
    'date-time': { clazz: 'DateTimeField' },
    binary: { clazz: 'BinaryField' },
    image: { clazz: 'ImageField' },
    file: { clazz: 'FileField' },
    tag: { clazz: 'TagField' },
    category: { clazz: 'CategoryField' },
    'key-value': { clazz: 'KeyValueField' },
    json: { clazz: 'JSONField' },
    'host-folder': { clazz: 'HostFolderField' },
    hidden: { clazz: 'HiddenField' },
    constant: { clazz: 'ConstantField' },
    relationship: { clazz: 'RelationshipField' }
};

const OPTION_TYPES = new Set<FieldType>(['select', 'multi-select', 'radio', 'checkbox']);

const CARDINALITY_ORDINAL: Record<RelationshipCardinality, string> = {
    'one-to-many': '0',
    'many-to-many': '1',
    'one-to-one': '2',
    'many-to-one': '3'
};

/**
 * Field variables that collide with contentlet system properties. dotCMS keeps its own list
 * (`FieldFactoryImpl.RESERVED_FIELD_VARS`), but applies it only to variables it generates —
 * an explicit variable is saved as given. A field named after one of these is created without
 * an error, and the content type then misbehaves: saving content fails (`[BADTYPE]` for
 * `sortOrder`), or the value is read as the system property (`host`, `identifier`, …).
 * `sortOrder` and `folder` are not on the core list but fail the same way.
 */
export const RESERVED_FIELD_VARIABLES: ReadonlySet<string> = new Set(
    [
        'languageId',
        'locked',
        'live',
        'modDate',
        'identifier',
        'host',
        'class',
        'conFolder',
        'conHost',
        'deleted',
        'file',
        'form',
        'inode',
        'modUser',
        'owner',
        'number',
        'string',
        'ownerCanPublish',
        'ownerCanWrite',
        'ownerCanRead',
        'permissions',
        'type',
        'website',
        'working',
        'stInode',
        'disabledWYSIWYG',
        'archived',
        'baseType',
        'contentType',
        'modUserName',
        'ownerUserName',
        'creationDate',
        'publishUser',
        'publishUserName',
        'sortOrder',
        'folder'
    ].map((variable) => variable.toLowerCase())
);

/** Variables that work, but shadow a system property — worth a warning, not a refusal. */
const SHADOWING_FIELD_VARIABLES: Record<string, string> = {
    url: 'a field named "url" replaces the URL dotCMS reports for each contentlet of this type'
};

const VARIABLE_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/;

/** A field as a caller describes it. */
export interface FieldInput {
    name: string;
    /** Field variable. Omit to let dotCMS derive one from the name. */
    variable?: string;
    type: FieldType;
    required?: boolean;
    listed?: boolean;
    indexed?: boolean;
    searchable?: boolean;
    unique?: boolean;
    hint?: string;
    defaultValue?: string;
    /** select / multi-select / radio / checkbox: values, or label + value pairs. */
    options?: Array<string | { label: string; value: string }>;
    /** constant / hidden: the fixed value. category: the parent category inode. */
    value?: string;
    /** relationship only. `target: "self"` relates the type to itself. */
    relationship?: { target: string; cardinality: RelationshipCardinality };
}

/** One problem found while validating the request; all are collected before failing. */
export interface FieldProblem {
    field: string;
    problem: string;
}

/** A field ready to be sent, plus what the caller should know about it. */
export interface PreparedField {
    input: FieldInput;
    body: Record<string, unknown>;
    /** For relationship fields: the target type variable (self already substituted). */
    target?: string;
}

function lowerFirst(value: string): string {
    return value.charAt(0).toLowerCase() + value.slice(1);
}

function upperFirst(value: string): string {
    return value.charAt(0).toUpperCase() + value.slice(1);
}

/** A non-reserved alternative for a reserved variable, e.g. `sortOrder` → `blogSortOrder`. */
export function suggestVariable(typeVariable: string, variable: string): string {
    return `${lowerFirst(typeVariable)}${upperFirst(variable)}`;
}

/** Validate a field variable; returns the problem, or undefined when it is fine. */
export function variableProblem(typeVariable: string, variable: string): string | undefined {
    if (!VARIABLE_PATTERN.test(variable)) {
        return `variable "${variable}" must start with a letter and contain only letters, digits and "_".`;
    }

    if (RESERVED_FIELD_VARIABLES.has(variable.toLowerCase())) {
        return (
            `variable "${variable}" collides with a contentlet system property; dotCMS would ` +
            `accept it and then fail when content is saved or read. Use e.g. ` +
            `"${suggestVariable(typeVariable, variable)}".`
        );
    }

    return undefined;
}

/**
 * Validate and convert caller fields to field bodies. Collects every problem before failing,
 * so one call reports everything that is wrong.
 *
 * `relationshipTargets` maps each relationship target to whether it exists; "self" is always
 * valid. `takenVariables` are variables already on the type (update), compared
 * case-insensitively.
 */
export function prepareFields(
    typeVariable: string,
    fields: FieldInput[],
    options: {
        relationshipTargets: Map<string, boolean>;
        takenVariables?: string[];
        warnings: string[];
    }
): PreparedField[] {
    const problems: FieldProblem[] = [];
    const taken = new Set((options.takenVariables ?? []).map((v) => v.toLowerCase()));
    const seen = new Set<string>();
    const prepared: PreparedField[] = [];

    for (const input of fields) {
        const label = input.variable ?? input.name;
        const fail = (problem: string) => problems.push({ field: label, problem });

        if (!input.name?.trim()) {
            fail('name is required.');
            continue;
        }

        const mapping = FIELD_CLASS[input.type];
        if (!mapping) {
            fail(`unknown type "${input.type}". Types: ${FIELD_TYPES.join(', ')}.`);
            continue;
        }

        if (input.variable !== undefined) {
            const problem = variableProblem(typeVariable, input.variable);
            if (problem) {
                fail(problem);
            }

            const key = input.variable.toLowerCase();
            if (seen.has(key)) {
                fail(`variable "${input.variable}" is used twice in this request.`);
            }
            if (taken.has(key)) {
                fail(`variable "${input.variable}" already exists on ${typeVariable}.`);
            }
            seen.add(key);

            const shadow = SHADOWING_FIELD_VARIABLES[key];
            if (shadow) {
                options.warnings.push(`${input.variable}: ${shadow}.`);
            }
        }

        const body: Record<string, unknown> = {
            clazz: `${FIELD_CLASS_PREFIX}${mapping.clazz}`,
            name: input.name.trim(),
            required: input.required ?? false,
            indexed: input.indexed ?? false,
            listed: input.listed ?? false,
            searchable: input.searchable ?? false,
            unique: input.unique ?? false
        };
        if (input.variable) body['variable'] = input.variable;
        if (mapping.dataType) body['dataType'] = mapping.dataType;
        if (input.hint) body['hint'] = input.hint;
        if (input.defaultValue !== undefined) body['defaultValue'] = input.defaultValue;

        if (OPTION_TYPES.has(input.type)) {
            if (!input.options?.length) {
                fail(`a ${input.type} field needs options.`);
            } else {
                body['values'] = input.options
                    .map((option) =>
                        typeof option === 'string'
                            ? `${option}|${option}`
                            : `${option.label}|${option.value}`
                    )
                    .join('\r\n');
            }
        } else if (input.options?.length) {
            fail(`options only apply to select, multi-select, radio and checkbox fields.`);
        }

        if (input.type === 'constant' || input.type === 'hidden' || input.type === 'category') {
            if (input.value === undefined || input.value === '') {
                fail(
                    input.type === 'category'
                        ? 'a category field needs value: the parent category inode.'
                        : `a ${input.type} field needs a value.`
                );
            } else {
                body['values'] = input.value;
            }
        }

        if (input.unique && input.type !== 'text') {
            fail('unique applies to text fields only.');
        }

        let target: string | undefined;
        if (input.type === 'relationship') {
            const relationship = input.relationship;
            if (!relationship?.target || !relationship.cardinality) {
                fail('a relationship field needs relationship.target and relationship.cardinality.');
            } else {
                target = relationship.target === 'self' ? typeVariable : relationship.target;
                if (relationship.target !== 'self' && !options.relationshipTargets.get(target)) {
                    fail(`relationship target "${target}" is not a content type on this instance.`);
                }
                body['relationType'] = target;
                body['values'] = CARDINALITY_ORDINAL[relationship.cardinality];
                // dotCMS requires both: a relationship field must be indexed, and cannot be listed.
                if (input.listed) {
                    options.warnings.push(
                        `${label}: relationship fields cannot be listed; listed set to false.`
                    );
                }
                body['indexed'] = true;
                body['listed'] = false;
            }
        } else if (input.relationship) {
            fail('relationship only applies to fields of type "relationship".');
        }

        prepared.push({ input, body, target });
    }

    if (problems.length > 0) {
        throw new ValidationError(
            'Nothing was changed. ' +
                problems.map(({ field, problem }) => `${field}: ${problem}`).join(' '),
            problems
        );
    }

    return prepared;
}

/** Which of the relationship targets exist, by variable. "self" is skipped. */
export async function checkRelationshipTargets(
    dotcms: DotCMSRuntime,
    fields: FieldInput[]
): Promise<Map<string, boolean>> {
    const targets = [
        ...new Set(
            fields
                .map((field) => field.relationship?.target)
                .filter((target): target is string => !!target && target !== 'self')
        )
    ];
    const result = new Map<string, boolean>();

    for (const target of targets) {
        try {
            await dotcms.request({ path: `/api/v1/contenttype/id/${encodeURIComponent(target)}` });
            result.set(target, true);
        } catch (error) {
            if (error instanceof HttpError && (error.status === 404 || error.status === 400)) {
                result.set(target, false);
            } else {
                throw error;
            }
        }
    }

    return result;
}

/** Resolve workflow scheme names or ids to ids. Archived schemes are not offered. */
export async function resolveWorkflows(
    dotcms: DotCMSRuntime,
    wanted: string[]
): Promise<Array<{ id: string; name: string }>> {
    const response = (await dotcms.request({ path: '/api/v1/workflow/schemes' })) as {
        entity?: unknown;
    };
    const schemes = (Array.isArray(response?.entity) ? response.entity : [])
        .map((scheme) => scheme as Record<string, unknown>)
        .filter((scheme) => scheme['archived'] !== true)
        .map((scheme) => ({ id: String(scheme['id'] ?? ''), name: String(scheme['name'] ?? '') }))
        .filter((scheme) => scheme.id);

    return wanted.map((value) => {
        const match = schemes.find(
            (scheme) => scheme.id === value || scheme.name.toLowerCase() === value.toLowerCase()
        );
        if (!match) {
            throw new ValidationError(
                `Workflow "${value}" was not found. Available: ${schemes.map((s) => s.name).join(', ')}.`
            );
        }

        return match;
    });
}

/**
 * Relationship fields that have no relationship record, as "Type.field". A relationship field
 * without its record looks right on the content type but relates nothing.
 */
export async function unregisteredRelationships(
    dotcms: DotCMSRuntime,
    typeId: string,
    typeVariable: string,
    fields: Array<{ variable: string; targetId: string }>
): Promise<string[]> {
    const missing: string[] = [];
    const recordsByTarget = new Map<string, Set<string>>();

    for (const { variable, targetId } of fields) {
        if (!recordsByTarget.has(targetId)) {
            const values = new Set<string>();
            for (const id of new Set([targetId, typeId])) {
                try {
                    const response = (await dotcms.request({
                        path: '/api/v1/relationships',
                        query: { contentTypeId: id }
                    })) as { entity?: unknown };
                    for (const entry of Array.isArray(response?.entity) ? response.entity : []) {
                        const value = (entry as Record<string, unknown>)['relationTypeValue'];
                        if (typeof value === 'string') values.add(value);
                    }
                } catch {
                    // Verification is best effort; an unreadable list is not evidence of a problem.
                    values.add(`${typeVariable}.${variable}`);
                }
            }
            recordsByTarget.set(targetId, values);
        }

        if (!recordsByTarget.get(targetId)?.has(`${typeVariable}.${variable}`)) {
            missing.push(`${typeVariable}.${variable}`);
        }
    }

    return missing;
}
