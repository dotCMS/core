import { getContentType } from './shared/content';
import {
    checkRelationshipTargets,
    CONTENT_TYPE_FIELD_ENDPOINTS,
    prepareFields,
    resolveWorkflows,
    unregisteredRelationships,
    type FieldInput
} from './shared/content-type-fields';
import { RESOLVE_ENDPOINTS, resolveSite } from './shared/resolve';

import { HttpError, ValidationError, type DotCMSRuntime } from '../../runtime';
import { CONTEXT_ENDPOINTS, type Endpoint } from '../toolkit/endpoints';

/**
 * Every endpoint `createContentType` calls. The `create_content_type` tool enforces exactly
 * this list, and `content-type-create.spec.ts` checks every request against it.
 */
export const CREATE_CONTENT_TYPE_ENDPOINTS: readonly Endpoint[] = [
    ...CONTEXT_ENDPOINTS,
    ...RESOLVE_ENDPOINTS,
    ...CONTENT_TYPE_FIELD_ENDPOINTS,
    'POST /api/v1/contenttype'
];

/** The workflow a new type gets when the caller names none. */
export const DEFAULT_WORKFLOW = 'System Workflow';

const SIMPLE_CONTENT_CLASS = 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType';
const VARIABLE_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/;

export interface CreateContentTypeOptions {
    dotcms: DotCMSRuntime;
    name: string;
    /** Content type variable. Omit to let dotCMS derive one from the name. */
    variable?: string;
    description?: string;
    /** Site the type belongs to — hostname or identifier. Omit for a type on all sites. */
    site?: string;
    /** Workflow scheme names or ids. Default: System Workflow. */
    workflows?: string[];
    /** URL map pattern for detail pages, e.g. "/blog/{urlTitle}". */
    urlMapPattern?: string;
    /** Identifier of the detail page used with urlMapPattern. */
    detailPage?: string;
    icon?: string;
    fields: FieldInput[];
    /** Validate and return the request body without creating anything. */
    dryRun?: boolean;
}

export interface CreatedField {
    name: string;
    variable: string;
    type: string;
    required: boolean;
    relationship?: { target: string; cardinality?: string };
}

export interface CreateContentTypeManifest {
    dryRun: boolean;
    id?: string;
    variable?: string;
    name: string;
    site?: string;
    workflows: string[];
    urlMapPattern?: string;
    fields: CreatedField[];
    /** The body sent (dry run) — or that would be sent. */
    body?: Record<string, unknown>;
    warnings: string[];
    /** What to expect next. */
    notes: string[];
}

/**
 * Create a content type from a friendly description of its fields.
 *
 * Everything is validated before anything is written, and every problem is reported at once:
 * field types are mapped to dotCMS field classes (there is no numeric field class — `integer`
 * and `decimal` are text fields with a numeric data type); field variables are checked against
 * the contentlet system properties dotCMS accepts but then fails on; relationship targets must
 * exist; options, cardinality, and the indexed/listed rules of relationship fields are set
 * correctly. After the create, the type is read back and every requested field and
 * relationship record is checked.
 */
export async function createContentType(
    options: CreateContentTypeOptions
): Promise<CreateContentTypeManifest> {
    const { dotcms } = options;
    const name = options.name?.trim();
    if (!name) {
        throw new ValidationError('name is required.');
    }

    const variable = options.variable?.trim();
    if (variable !== undefined && !VARIABLE_PATTERN.test(variable)) {
        throw new ValidationError(
            `variable "${variable}" must start with a letter and contain only letters, digits and "_".`
        );
    }

    if (!options.fields?.length) {
        throw new ValidationError('fields must list at least one field.');
    }

    if (variable && (await typeExists(dotcms, variable))) {
        throw new ValidationError(
            `A content type with variable "${variable}" already exists. Use update_content_type ` +
                `to change it.`
        );
    }

    const warnings: string[] = [];
    const typeVariable = variable ?? name.replace(/[^A-Za-z0-9]/g, '');
    const prepared = prepareFields(typeVariable, options.fields, {
        relationshipTargets: await checkRelationshipTargets(dotcms, options.fields),
        warnings
    });

    const workflows = await resolveWorkflows(
        dotcms,
        options.workflows?.length ? options.workflows : [DEFAULT_WORKFLOW]
    );
    const site = options.site ? await resolveSite(dotcms, options.site) : undefined;

    const body: Record<string, unknown> = {
        clazz: SIMPLE_CONTENT_CLASS,
        name,
        workflow: workflows.map((workflow) => workflow.id),
        fields: prepared.map((field) => field.body)
    };
    if (variable) body['variable'] = variable;
    if (options.description) body['description'] = options.description;
    if (site) body['host'] = site.identifier;
    if (options.urlMapPattern) body['urlMapPattern'] = options.urlMapPattern;
    if (options.detailPage) body['detailPage'] = options.detailPage;
    if (options.icon) body['icon'] = options.icon;

    const requested: CreatedField[] = prepared.map(({ input, target }) => ({
        name: input.name,
        variable: input.variable ?? '',
        type: input.type,
        required: input.required ?? false,
        ...(target
            ? { relationship: { target, cardinality: input.relationship?.cardinality } }
            : {})
    }));

    if (options.dryRun) {
        return {
            dryRun: true,
            variable,
            name,
            site: site?.hostname,
            workflows: workflows.map((workflow) => workflow.name),
            urlMapPattern: options.urlMapPattern,
            fields: requested,
            body,
            warnings,
            notes: ['Dry run: nothing was created.']
        };
    }

    let created: Record<string, unknown>;
    try {
        const response = (await dotcms.request({
            method: 'POST',
            path: '/api/v1/contenttype',
            body
        })) as { entity?: unknown };
        const entity = Array.isArray(response?.entity) ? response.entity[0] : response?.entity;
        created = (entity ?? {}) as Record<string, unknown>;
    } catch (error) {
        if (
            error instanceof HttpError &&
            error.status === 400 &&
            /Invalid content type variable/i.test(error.body)
        ) {
            throw new ValidationError(
                `The variable "${variable ?? typeVariable}" is not available. Either a content ` +
                    `type uses it, or one that used it was deleted recently and is still being ` +
                    `removed (deletion completes in the background). Choose another variable, or ` +
                    `wait and retry.`
            );
        }

        throw error;
    }

    const id = typeof created['id'] === 'string' ? created['id'] : '';
    if (!id) {
        throw new Error('The content type create returned no id.');
    }

    // Read back and check every requested field and relationship.
    const saved = await getContentType(dotcms, id);
    const savedByVariable = new Map(saved.fields.map((field) => [field.variable, field]));
    const savedByName = new Map(saved.fields.map((field) => [field.name, field]));
    const fields: CreatedField[] = requested.map((field) => {
        const match =
            (field.variable && savedByVariable.get(field.variable)) || savedByName.get(field.name);
        if (!match) {
            warnings.push(`${field.variable || field.name}: requested but missing after create.`);
            return field;
        }

        return { ...field, variable: match.variable };
    });

    const relationshipFields: Array<{ variable: string; targetId: string }> = [];
    for (const field of fields) {
        if (field.relationship && field.variable) {
            const targetId =
                field.relationship.target === saved.variable
                    ? saved.id
                    : (await getContentType(dotcms, field.relationship.target)).id;
            relationshipFields.push({ variable: field.variable, targetId });
        }
    }
    for (const missing of await unregisteredRelationships(
        dotcms,
        saved.id,
        saved.variable,
        relationshipFields
    )) {
        warnings.push(
            `${missing}: the field exists but no relationship record was created, so it relates ` +
                `nothing. Re-save the field (update_content_type with updateFields) to register it.`
        );
    }

    const savedUrlMap =
        typeof created['urlMapPattern'] === 'string' ? created['urlMapPattern'] : undefined;
    if (options.urlMapPattern && savedUrlMap !== options.urlMapPattern) {
        warnings.push(
            `urlMapPattern was not saved as requested (saved: ${savedUrlMap ?? 'none'}).`
        );
    }

    return {
        dryRun: false,
        id: saved.id,
        variable: saved.variable,
        name: saved.name,
        site: site?.hostname,
        workflows: workflows.map((workflow) => workflow.name),
        urlMapPattern: savedUrlMap,
        fields,
        warnings,
        notes: [
            'The GraphQL schema picks up a new content type after a short delay; a query for ' +
                `${saved.variable}Collection can fail with "undefined" for a few seconds.`
        ]
    };
}

async function typeExists(dotcms: DotCMSRuntime, variable: string): Promise<boolean> {
    try {
        await dotcms.request({ path: `/api/v1/contenttype/id/${encodeURIComponent(variable)}` });
        return true;
    } catch (error) {
        if (error instanceof HttpError && (error.status === 404 || error.status === 400)) {
            return false;
        }

        throw error;
    }
}
