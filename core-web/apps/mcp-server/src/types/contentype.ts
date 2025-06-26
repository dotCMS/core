import { z } from 'zod';

export const ContentTypeFieldSchema = z.object({
  id: z.string(),
  name: z.string(),
  variable: z.string(),
  type: z.string(),
  required: z.boolean(),
  indexed: z.boolean(),
  listed: z.boolean(),
  unique: z.boolean(),
  searchable: z.boolean(),
  sortOrder: z.number(),
  values: z.string().optional(),
  defaultValue: z.string().optional(),
  hint: z.string().optional(),
  regexCheck: z.string().optional(),
  modDate: z.number().optional(),
  iDate: z.number().optional(),
  fieldType: z.string().optional(),
  fieldTypeLabel: z.string().optional(),
  contentTypeId: z.string().optional(),
  fixed: z.boolean().optional(),
  readOnly: z.boolean().optional(),
  system: z.boolean().optional(),
  dataType: z.string().optional(),
  fieldVariables: z.array(z.any()).optional()
});

export const ContentTypeSchema = z.object({
  baseType: z.enum([
    'CONTENT',
    'WIDGET',
    'FORM',
    'FILEASSET',
    'HTMLPAGE',
    'PERSONA',
    'VANITY_URL',
    'KEY_VALUE',
    'DOTASSET'
  ]),
  clazz: z.string(),
  defaultType: z.boolean(),
  description: z.string(),
  fields: z.array(ContentTypeFieldSchema),
  fixed: z.boolean(),
  folder: z.string(),
  folderPath: z.string(),
  host: z.string(),
  iDate: z.number(),
  icon: z.string(),
  id: z.string(),
  layout: z.array(z.any()),
  metadata: z.record(z.any()),
  modDate: z.number(),
  multilingualable: z.boolean(),
  name: z.string(),
  siteName: z.string(),
  sortOrder: z.number(),
  system: z.boolean(),
  variable: z.string(),
  versionable: z.boolean(),
  workflows: z.array(z.any()),
  systemActionMappings: z.record(z.any()),
  owner: z.string().optional(),
  nEntries: z.number().optional()
});

export type ContentTypeField = z.infer<typeof ContentTypeFieldSchema>;

export type ContentType = z.infer<typeof ContentTypeSchema>;
