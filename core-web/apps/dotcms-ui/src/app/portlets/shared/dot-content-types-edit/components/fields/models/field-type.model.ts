// Re-exported from the shared models lib so feature code can keep importing `FieldType`
// via `../models` while the canonical definition lives in `@dotcms/dotcms-models`.
// `export type`: `FieldType` is a type, and under `isolatedModules` a plain re-export cannot be
// erased at transpile time without knowing that.
export type { FieldType } from '@dotcms/dotcms-models';
