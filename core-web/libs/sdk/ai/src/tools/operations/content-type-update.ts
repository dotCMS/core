import { getContentType } from './shared/content';
import {
    checkRelationshipTargets,
    CONTENT_TYPE_FIELD_ENDPOINTS,
    prepareFields,
    resolveWorkflows,
    unregisteredRelationships,
    type FieldInput
} from './shared/content-type-fields';

import { HttpError, ValidationError, type DotCMSRuntime } from '../../runtime';
import { CONTEXT_ENDPOINTS, type Endpoint } from '../toolkit/endpoints';

/**
 * Every endpoint `updateContentType` calls. The `update_content_type` tool enforces exactly
 * this list, and `content-type-update.spec.ts` checks every request against it.
 */
export const UPDATE_CONTENT_TYPE_ENDPOINTS: readonly Endpoint[] = [
    ...CONTEXT_ENDPOINTS,
    ...CONTENT_TYPE_FIELD_ENDPOINTS,
    'PUT /api/v1/contenttype/id/{idOrVar}'
];

/** Field properties an update may change. A field's type cannot change. */
export interface FieldUpdate {
    variable: string;
    name?: string;
    required?: boolean;
    listed?: boolean;
    indexed?: boolean;
    searchable?: boolean;
    hint?: string;
    defaultValue?: string;
    options?: Array<string | { label: string; value: string }>;
}

export interface UpdateContentTypeOptions {
    dotcms: DotCMSRuntime;
    /** Content type variable or id. */
    contentType: string;
    name?: string;
    description?: string;
    /** Replaces the workflow schemes (names or ids). */
    workflows?: string[];
    urlMapPattern?: string;
    detailPage?: string;
    addFields?: FieldInput[];
    updateFields?: FieldUpdate[];
    /** Field variables to delete — deletes the data stored in them. Needs allowDataLoss. */
    removeFields?: string[];
    /** Required with removeFields. */
    allowDataLoss?: boolean;
    /** Report what would change without changing anything. */
    dryRun?: boolean;
}

export interface UpdateContentTypeManifest {
    dryRun: boolean;
    id: string;
    variable: string;
    changes: {
        type: string[];
        added: string[];
        updated: Array<{ variable: string; properties: string[] }>;
        removed: string[];
    };
    /** Field variables on the type after the update (or as they would be, on a dry run). */
    fields: string[];
    warnings: string[];
}

const UPDATABLE_PROPERTIES = [
    'name',
    'required',
    'listed',
    'indexed',
    'searchable',
    'hint',
    'defaultValue'
] as const;

const OPTION_CLASSES = /(Select|MultiSelect|Radio|Checkbox)Field$/;

function asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' ? (value as Record<string, unknown>) : {};
}

/**
 * Change an existing content type: add, update and remove fields, and change its name,
 * description, workflows or URL map.
 *
 * dotCMS has no endpoint that adds a field to an existing type (POST on the fields endpoints
 * answers 404/405), and `POST /api/v1/contenttype` only creates. The one path is a PUT of the
 * whole type: this operation reads it, applies the changes to that full body — keeping every
 * field's id, so existing content keeps its data — and sends it back, with the workflow list
 * in the shape the PUT expects. New fields go through the same validation as
 * `create_content_type`, and new relationship fields are checked for their relationship record.
 */
export async function updateContentType(
    options: UpdateContentTypeOptions
): Promise<UpdateContentTypeManifest> {
    const { dotcms } = options;
    const current = await getContentType(dotcms, options.contentType);
    const raw = asRecord(
        asRecord(
            await dotcms.request({
                path: `/api/v1/contenttype/id/${encodeURIComponent(current.id)}`
            })
        )['entity']
    );
    const rawFields = (Array.isArray(raw['fields']) ? raw['fields'] : []).map(asRecord);
    const variables = rawFields.map((field) => String(field['variable'] ?? ''));

    const warnings: string[] = [];
    const changes: UpdateContentTypeManifest['changes'] = {
        type: [],
        added: [],
        updated: [],
        removed: []
    };
    const problems: string[] = [];

    // ---- removals ------------------------------------------------------------------
    const remove = new Set(options.removeFields ?? []);
    for (const variable of remove) {
        if (!variables.includes(variable)) {
            problems.push(`removeFields: "${variable}" is not a field of ${current.variable}.`);
        }
    }
    if (remove.size > 0 && !options.allowDataLoss && !options.dryRun) {
        problems.push(
            `removeFields deletes the data stored in ${[...remove].join(', ')} for every ` +
                `contentlet of ${current.variable}. Pass allowDataLoss: true to confirm.`
        );
    }

    // ---- updates -------------------------------------------------------------------
    const updatesByVariable = new Map((options.updateFields ?? []).map((u) => [u.variable, u]));
    for (const [variable, update] of updatesByVariable) {
        const field = rawFields.find((f) => f['variable'] === variable);
        if (!field) {
            problems.push(`updateFields: "${variable}" is not a field of ${current.variable}.`);
            continue;
        }
        if (remove.has(variable)) {
            problems.push(`"${variable}" is both updated and removed.`);
        }
        if (update.options && !OPTION_CLASSES.test(String(field['clazz'] ?? ''))) {
            problems.push(`updateFields: "${variable}" does not take options.`);
        }
        if (String(field['clazz'] ?? '').endsWith('RelationshipField') && update.listed) {
            problems.push(
                `updateFields: "${variable}" is a relationship field and cannot be listed.`
            );
        }
    }

    if (problems.length > 0) {
        throw new ValidationError(`Nothing was changed. ${problems.join(' ')}`);
    }

    // ---- additions (validated like a create) ---------------------------------------
    const addFields = options.addFields ?? [];
    const prepared = addFields.length
        ? prepareFields(current.variable, addFields, {
              relationshipTargets: await checkRelationshipTargets(dotcms, addFields),
              takenVariables: variables.filter((variable) => !remove.has(variable)),
              warnings
          })
        : [];

    // ---- build the full body ---------------------------------------------------------
    const fields: Record<string, unknown>[] = [];
    for (const field of rawFields) {
        const variable = String(field['variable'] ?? '');
        if (remove.has(variable)) {
            changes.removed.push(variable);
            continue;
        }

        const update = updatesByVariable.get(variable);
        if (!update) {
            fields.push(field);
            continue;
        }

        const next = { ...field };
        const changed: string[] = [];
        for (const property of UPDATABLE_PROPERTIES) {
            const value = update[property];
            if (value !== undefined && next[property] !== value) {
                next[property] = value;
                changed.push(property);
            }
        }
        if (update.options) {
            const values = update.options
                .map((option) =>
                    typeof option === 'string'
                        ? `${option}|${option}`
                        : `${option.label}|${option.value}`
                )
                .join('\r\n');
            if (next['values'] !== values) {
                next['values'] = values;
                changed.push('options');
            }
        }
        fields.push(next);
        changes.updated.push({ variable, properties: changed });
    }

    for (const { body, input } of prepared) {
        fields.push({ ...body, contentTypeId: current.id });
        changes.added.push(input.variable ?? input.name);
    }

    const next: Record<string, unknown> = { ...raw, fields };
    // The GET returns `workflows` (objects); the PUT reads `workflow` (ids).
    const currentWorkflows = (Array.isArray(raw['workflows']) ? raw['workflows'] : [])
        .map((workflow) => String(asRecord(workflow)['id'] ?? ''))
        .filter(Boolean);
    delete next['workflows'];
    next['workflow'] = currentWorkflows;
    if (options.workflows?.length) {
        const resolved = await resolveWorkflows(dotcms, options.workflows);
        next['workflow'] = resolved.map((workflow) => workflow.id);
        changes.type.push(`workflows → ${resolved.map((workflow) => workflow.name).join(', ')}`);
    }

    for (const property of ['name', 'description', 'urlMapPattern', 'detailPage'] as const) {
        const value = options[property];
        if (value !== undefined && raw[property] !== value) {
            next[property] = value;
            changes.type.push(property);
        }
    }

    const nothing =
        changes.type.length + changes.added.length + changes.removed.length === 0 &&
        changes.updated.every((update) => update.properties.length === 0);
    if (nothing) {
        warnings.push('Nothing to change: the request matches the current content type.');
    }

    if (options.dryRun || nothing) {
        return {
            dryRun: !!options.dryRun,
            id: current.id,
            variable: current.variable,
            changes,
            fields: fields
                .map((field) => String(field['variable'] ?? field['name'] ?? ''))
                .filter(Boolean),
            warnings
        };
    }

    try {
        await dotcms.request({
            method: 'PUT',
            path: `/api/v1/contenttype/id/${encodeURIComponent(current.id)}`,
            body: next
        });
    } catch (error) {
        if (error instanceof HttpError && error.status === 404) {
            throw new HttpError(
                error.status,
                error.statusText,
                `${error.body} — the content type PUT answers 404 for an invalid body as well as ` +
                    'for a missing type; the type was found before the update.'
            );
        }

        throw error;
    }

    // Read back and verify.
    const saved = await getContentType(dotcms, current.id);
    const savedVariables = saved.fields.map((field) => field.variable);
    for (const variable of changes.removed) {
        if (savedVariables.includes(variable)) {
            warnings.push(`${variable}: still present after the update.`);
        }
    }

    const relationshipFields: Array<{ variable: string; targetId: string }> = [];
    for (const { input, target } of prepared) {
        if (!target) continue;
        const savedField = saved.fields.find(
            (field) => field.variable === input.variable || field.name === input.name
        );
        if (!savedField) {
            warnings.push(
                `${input.variable ?? input.name}: requested but missing after the update.`
            );
            continue;
        }
        const targetId =
            target === saved.variable ? saved.id : (await getContentType(dotcms, target)).id;
        relationshipFields.push({ variable: savedField.variable, targetId });
    }
    for (const missing of await unregisteredRelationships(
        dotcms,
        saved.id,
        saved.variable,
        relationshipFields
    )) {
        warnings.push(`${missing}: the field exists but no relationship record was created.`);
    }

    for (const { input } of prepared) {
        if (
            !saved.fields.some(
                (field) => field.variable === input.variable || field.name === input.name
            )
        ) {
            if (input.type !== 'relationship') {
                warnings.push(
                    `${input.variable ?? input.name}: requested but missing after the update.`
                );
            }
        }
    }

    return {
        dryRun: false,
        id: saved.id,
        variable: saved.variable,
        changes,
        fields: savedVariables,
        warnings
    };
}
