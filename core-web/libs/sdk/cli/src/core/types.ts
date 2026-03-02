/**
 * Shared type definitions for @dotcms/cli
 * All modules code against these interfaces.
 */

// ─── Config ──────────────────────────────────────────────────────────────────

export interface DotCliConfig {
    default: string;
    instances: Record<string, InstanceConfig>;
    pull?: PullConfigEntry[];
    concurrency?: number;
    requestDelay?: number;
}

export interface InstanceConfig {
    url: string;
}

export interface PullConfigEntry {
    type: string;
    site?: string;
    query?: string;
    language?: string;
    limit?: number;
    withBinaries?: boolean;
}

// ─── Auth ────────────────────────────────────────────────────────────────────

export interface AuthStore {
    [instanceName: string]: AuthEntry;
}

export type AuthEntry = TokenAuth | CredentialsAuth;

export interface TokenAuth {
    type: 'token';
    token: string;
}

export interface CredentialsAuth {
    type: 'credentials';
    token: string;
    expiresAt: string;
}

// ─── Content Type Schema ─────────────────────────────────────────────────────

export interface ContentTypeSchema {
    variable: string;
    name: string;
    id: string;
    fields: ContentTypeField[];
    /** Timestamp when this schema was cached */
    cachedAt?: string;
}

export interface ContentTypeField {
    variable: string;
    name: string;
    fieldType: string;
    dataType: string;
    sortOrder: number;
    required: boolean;
    fixed: boolean;
    readOnly: boolean;
    searchable: boolean;
    listed: boolean;
    /** For Select/Radio/MultiSelect: available values */
    values?: string;
    /** Default value if any */
    defaultValue?: string;
    /** For relationships: related content type */
    relationType?: string;
}

/** Field types that can become the body of a .md file */
export const BODY_FIELD_TYPES = ['WYSIWYG', 'Textarea', 'Story-Block'] as const;

/** Field types that represent binary data (sidecar files) */
export const BINARY_FIELD_TYPES = ['Binary', 'Image', 'File'] as const;

/** Field types that use dataType=SYSTEM but are user-editable */
const USER_EDITABLE_SYSTEM_TYPES = new Set(['Binary', 'Image', 'File']);

/**
 * Determine if a content type field is a system field (not user-editable).
 * A field is system/non-editable if any of these are true:
 * - `fixed === true` — immutable system field
 * - `readOnly === true` — read-only field
 * - `dataType === 'SYSTEM'` — system data type (dividers, tabs, constants)
 *   EXCEPT for Binary/Image/File which use SYSTEM dataType but are user-editable
 */
export function isSystemField(field: ContentTypeField): boolean {
    if (field.fixed === true || field.readOnly === true) {
        return true;
    }

    if (field.dataType === 'SYSTEM' && !USER_EDITABLE_SYSTEM_TYPES.has(field.fieldType)) {
        return true;
    }

    return false;
}

// ─── Snapshot ────────────────────────────────────────────────────────────────

export interface SnapshotStore {
    [identifier: string]: SnapshotEntry;
}

export interface SnapshotEntry {
    file: string; // filename only (e.g., "e5e92e.md")
    title: string; // contentlet title for list display
    hash: string; // content hash for change detection
    pulledAt: string; // ISO timestamp of last pull
    inode: string; // remote version for conflict detection
    source: string; // instance name where this was pulled/pushed from
    modDate: string; // server's last modified date (auto-generated, for audit)
}

export type FileState = 'unchanged' | 'modified' | 'new' | 'deleted';

// ─── Contentlet (serialized form) ────────────────────────────────────────────

/** YAML frontmatter metadata for a contentlet .md file */
export interface ContentletFrontmatter {
    contentType: string;
    identifier: string;
    language: string;
    inode: string;
    bodyField?: string;
    [fieldVariable: string]: unknown;
}

/** Parsed contentlet file: frontmatter + body content */
export interface ParsedContentFile {
    frontmatter: ContentletFrontmatter;
    body: string;
    filePath: string;
}

// ─── HTTP / API ──────────────────────────────────────────────────────────────

export interface HttpClientOptions {
    baseURL: string;
    token: string;
    timeout?: number;
}

export interface GraphQLResponse<T = Record<string, unknown>> {
    data: T;
    errors?: Array<{
        message: string;
        locations?: Array<{ line: number; column: number }>;
        path?: string[];
    }>;
}

export interface ContentletCollectionResponse {
    [collectionName: string]: ContentletRecord[];
}

export type ContentletRecord = Record<string, unknown>;

// ─── Push ────────────────────────────────────────────────────────────────────

export interface PushResult {
    file: string;
    status: 'created' | 'updated' | 'skipped' | 'error';
    identifier?: string;
    error?: string;
}

export interface PushErrorLog {
    timestamp: string;
    failed: Array<{
        file: string;
        error: string;
        identifier?: string;
    }>;
}

// ─── Binary ──────────────────────────────────────────────────────────────────

export interface BinaryFieldInfo {
    fieldVariable: string;
    localPath: string;
    fileName: string;
}

export interface BinaryDownloadResult {
    fieldVariable: string;
    fileName: string;
    savedPath: string;
    size: number;
}

// ─── Cache ───────────────────────────────────────────────────────────────────

export interface CacheOptions {
    /** Cache directory path (default: .dotcli/cache) */
    cacheDir: string;
    /** TTL in milliseconds (default: 1 hour) */
    ttl?: number;
}

// ─── Language ────────────────────────────────────────────────────────────────

export interface LanguageMap {
    /** languageId → language code (e.g., 1 → "en-US") */
    [languageId: string]: string;
}

export interface LanguageEntry {
    id: number;
    languageCode: string;
    countryCode: string;
    language: string;
    country: string;
    defaultLanguage: boolean;
}

// ─── Constants ───────────────────────────────────────────────────────────────

export const DOTCLI_DIR = '.dotcli';
export const CONFIG_FILE = 'config.yml';
export const AUTH_FILE = '.auth.json';
export const SNAPSHOT_FILE = '.snapshot.json';
export const CACHE_DIR = 'cache';
export const CONTENT_TYPES_CACHE_DIR = 'content-types';
export const PUSH_ERRORS_FILE = 'last-push-errors.json';

export const DEFAULT_CONCURRENCY = 5;
export const DEFAULT_CACHE_TTL = 60 * 60 * 1000; // 1 hour in ms

/** System/metadata keys that are NOT user content fields */
export const METADATA_KEYS = [
    'contentType',
    'identifier',
    'language',
    'inode',
    'bodyField'
] as const;

/** Short ID length (first N chars of identifier for filename) */
export const SHORT_ID_LENGTH = 6;
