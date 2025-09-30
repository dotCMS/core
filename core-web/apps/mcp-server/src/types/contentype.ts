import { z } from 'zod';

// Enum for actual content type baseType property (no 'ANY')
export const ContentTypeBaseTypeEnum = z.enum([
    'CONTENT',
    'WIDGET',
    'FORM',
    'FILEASSET',
    'HTMLPAGE',
    'PERSONA',
    'VANITY_URL',
    'KEY_VALUE',
    'DOTASSET'
]);

export type ContentTypeBaseType = z.infer<typeof ContentTypeBaseTypeEnum>;

export const WorkflowSchema = z.object({
    archived: z.boolean(),
    creationDate: z.number(),
    defaultScheme: z.boolean(),
    description: z.string(),
    entryActionId: z.string().nullable(),
    id: z.string(),
    mandatory: z.boolean(),
    modDate: z.number(),
    name: z.string(),
    system: z.boolean(),
    variableName: z.string()
});

const RowClazzEnum = z.enum(['com.dotcms.contenttype.model.field.ImmutableRowField']);
const ColumnClazzEnum = z.enum(['com.dotcms.contenttype.model.field.ImmutableColumnField']);
const TabDividerClazzEnum = z.enum(['com.dotcms.contenttype.model.field.ImmutableTabDividerField']);
const LineDividerClazzEnum = z.enum([
    'com.dotcms.contenttype.model.field.ImmutableLineDividerField'
]);
// Known field clazz types - using union to allow for unknown/deprecated types
const KnownFieldClazzEnum = z.enum([
    'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    'com.dotcms.contenttype.model.field.ImmutableCategoryField',
    'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
    'com.dotcms.contenttype.model.field.ImmutableConstantField',
    'com.dotcms.contenttype.model.field.ImmutableCustomField',
    'com.dotcms.contenttype.model.field.ImmutableDateField',
    'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
    'com.dotcms.contenttype.model.field.ImmutableFileField',
    'com.dotcms.contenttype.model.field.ImmutableHiddenField',
    'com.dotcms.contenttype.model.field.ImmutableImageField',
    'com.dotcms.contenttype.model.field.ImmutableJSONField',
    'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
    'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
    'com.dotcms.contenttype.model.field.ImmutableRadioField',
    'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
    'com.dotcms.contenttype.model.field.ImmutableSelectField',
    'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
    'com.dotcms.contenttype.model.field.ImmutableTagField',
    'com.dotcms.contenttype.model.field.ImmutableTextField',
    'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
    'com.dotcms.contenttype.model.field.ImmutableTimeField',
    'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
    'com.dotcms.contenttype.model.field.ImmutableLineDividerField'
]);

// Flexible field clazz that accepts known types or any string matching the dotCMS field pattern
const FieldClazzEnum = z.union([
    KnownFieldClazzEnum,
    z.string().refine(
        (val) => /^com\.dotcms\.contenttype\.model\.field\.Immutable\w+Field$/.test(val),
        { message: 'Field clazz must be a valid dotCMS field type' }
    )
]);

const DividerSchema = z.object({
    clazz: RowClazzEnum.or(TabDividerClazzEnum).or(LineDividerClazzEnum),
    contentTypeId: z.string(),
    dataType: z.string(),
    fieldContentTypeProperties: z.array(z.any()).optional(),
    fieldType: z.string(),
    fieldTypeLabel: z.string(),
    fieldVariables: z.array(z.record(z.any())),
    fixed: z.boolean(),
    forceIncludeInApi: z.boolean(),
    iDate: z.number(),
    id: z.string().optional(),
    indexed: z.boolean(),
    listed: z.boolean(),
    modDate: z.number(),
    name: z.string(),
    readOnly: z.boolean(),
    required: z.boolean(),
    searchable: z.boolean(),
    sortOrder: z.number(),
    unique: z.boolean(),
    variable: z.string().optional()
});

const ColumnDividerSchema = z.object({
    clazz: ColumnClazzEnum,
    contentTypeId: z.string(),
    dataType: z.string(),
    fieldContentTypeProperties: z.array(z.any()).optional(),
    fieldType: z.string(),
    fieldTypeLabel: z.string(),
    fieldVariables: z.array(z.record(z.any())),
    fixed: z.boolean(),
    forceIncludeInApi: z.boolean(),
    iDate: z.number(),
    id: z.string().optional(),
    indexed: z.boolean(),
    listed: z.boolean(),
    modDate: z.number(),
    name: z.string(),
    readOnly: z.boolean(),
    required: z.boolean(),
    searchable: z.boolean(),
    sortOrder: z.number(),
    unique: z.boolean(),
    variable: z.string().optional()
});

const ContentTypeFieldSchema = z.object({
    clazz: FieldClazzEnum,
    contentTypeId: z.string(),
    dataType: z.string(),
    fieldType: z.string(),
    fieldTypeLabel: z.string(),
    fieldVariables: z.array(z.record(z.any())),
    fixed: z.boolean(),
    forceIncludeInApi: z.boolean(),
    hint: z.string().optional(),
    iDate: z.number(),
    id: z.string(),
    indexed: z.boolean(),
    listed: z.boolean(),
    modDate: z.number(),
    name: z.string(),
    readOnly: z.boolean(),
    required: z.boolean(),
    searchable: z.boolean(),
    sortOrder: z.number(),
    system: z.boolean().optional(),
    unique: z.boolean(),
    variable: z.string(),
    values: z.string().optional(),
    defaultValue: z.string().optional(),
});

const ColumnSchema = z.object({
    columnDivider: ColumnDividerSchema,
    fields: z.array(ContentTypeFieldSchema)
});

export const LayoutSchema = z.object({
    divider: DividerSchema,
    columns: z.array(ColumnSchema)
});

export const ContentTypeSchema = z.object({
    baseType: ContentTypeBaseTypeEnum,
    clazz: z.string(),
    defaultType: z.boolean(),
    description: z.string().optional(),
    fields: z.array(ContentTypeFieldSchema).optional(),
    fixed: z.boolean(),
    folder: z.string(),
    folderPath: z.string(),
    host: z.string(),
    iDate: z.number(),
    icon: z.string().optional(),
    id: z.string(),
    layout: z.array(LayoutSchema),
    metadata: z.record(z.any()),
    modDate: z.number(),
    multilingualable: z.boolean(),
    name: z.string(),
    siteName: z.string().optional(),
    sortOrder: z.number(),
    system: z.boolean(),
    variable: z.string(),
    versionable: z.boolean(),
    workflows: z.array(WorkflowSchema).optional(),
    systemActionMappings: z.union([z.record(z.any()), z.array(z.any())]).optional(),
    owner: z.string().optional(),
    nEntries: z.number().optional(),
    detailPage: z.string().optional(),
    publishDateVar: z.string().optional(),
    urlMapPattern: z.string().optional()
});

export type ContentTypeField = z.infer<typeof ContentTypeFieldSchema>;

export type Workflow = z.infer<typeof WorkflowSchema>;

export type Layout = z.infer<typeof LayoutSchema>;

export type ContentType = z.infer<typeof ContentTypeSchema>;

export type FieldClazz = z.infer<typeof FieldClazzEnum>;
