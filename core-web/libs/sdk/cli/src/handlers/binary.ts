/**
 * Binary handler for @dotcms/cli.
 * Handles downloading and uploading binary sidecar files (images, PDFs, etc.)
 * associated with contentlets.
 */
import { existsSync } from 'node:fs';
import { mkdir, writeFile, readFile, stat } from 'node:fs/promises';
import { dirname, join, resolve, extname } from 'node:path';

import {
    BINARY_FIELD_TYPES,
    SHORT_ID_LENGTH,
    type BinaryFieldInfo,
    type BinaryDownloadResult,
    type ContentTypeSchema,
    type ContentletRecord
} from '../core/types';

import type { $Fetch } from 'ofetch';

/** Size threshold for warning (10 MB) */
const SIZE_WARNING_THRESHOLD = 10 * 1024 * 1024;

// ─── Path Helpers ───────────────────────────────────────────────────────────

/**
 * Get the standard sidecar path for a binary field.
 * Pattern: {contentDir}/assets/{shortId}/{fieldVar}.{fileName}
 */
export function getBinarySidecarPath(
    shortId: string,
    fieldVar: string,
    fileName: string,
    contentDir: string
): string {
    return join(contentDir, 'assets', shortId, `${fieldVar}.${fileName}`);
}

/**
 * Determine if a field value is a local file reference.
 * - Starts with `./` → resolve relative to content file directory, return absolute path
 * - Just a filename (e.g., "photo.jpg") → not a local reference, return null
 * - null or missing → return null
 */
export function resolveBinaryPath(
    fieldValue: string | null | undefined,
    contentFilePath: string
): string | null {
    if (!fieldValue) {
        return null;
    }

    if (fieldValue.startsWith('./')) {
        return resolve(dirname(contentFilePath), fieldValue);
    }

    // Plain filename — server keeps existing binary
    return null;
}

// ─── Schema Helpers ─────────────────────────────────────────────────────────

/**
 * Extract binary field variables from a content type schema.
 */
export function getBinaryFields(schema: ContentTypeSchema): string[] {
    return schema.fields
        .filter((f) => (BINARY_FIELD_TYPES as readonly string[]).includes(f.fieldType))
        .map((f) => f.variable);
}

// ─── Download (Pull) ────────────────────────────────────────────────────────

/**
 * Download a single binary field from dotCMS.
 * Uses `/contentAsset/raw-data/{inode}/{fieldVar}` endpoint.
 * Returns null if the binary field has no content (404).
 */
export async function downloadBinary(
    client: $Fetch,
    inode: string,
    fieldVar: string,
    fileName: string,
    destDir: string
): Promise<BinaryDownloadResult | null> {
    const url = `/contentAsset/raw-data/${inode}/${fieldVar}`;
    let data: ArrayBuffer;

    try {
        data = await client(url, {
            method: 'GET',
            responseType: 'arrayBuffer'
        });
    } catch (error: unknown) {
        const status =
            (error as { status?: number }).status ??
            (error as { response?: { status?: number } }).response?.status;
        if (status === 404) {
            return null;
        }
        throw error;
    }

    const savePath = join(destDir, `${fieldVar}.${fileName}`);
    await mkdir(dirname(savePath), { recursive: true });

    const buffer = Buffer.from(data);
    await writeFile(savePath, buffer);

    return {
        fieldVariable: fieldVar,
        fileName,
        savedPath: savePath,
        size: buffer.length
    };
}

/**
 * Download all binary fields for a contentlet.
 * Image/File fields (DotFileasset) return `{ fileName, sortOrder, description }`.
 * Binary fields return a flat object with `{ name, size, ... }`.
 */
export async function downloadContentBinaries(
    client: $Fetch,
    record: ContentletRecord,
    schema: ContentTypeSchema,
    contentDir: string
): Promise<BinaryDownloadResult[]> {
    const binaryFields = getBinaryFields(schema);
    const results: BinaryDownloadResult[] = [];
    const identifier = record['identifier'] as string | undefined;
    const inode = record['inode'] as string | undefined;

    if (!identifier || !inode) {
        return results;
    }

    const shortId = identifier.substring(0, SHORT_ID_LENGTH);
    const assetsDir = join(contentDir, 'assets', shortId);

    for (const fieldVar of binaryFields) {
        const fieldValue = record[fieldVar];
        if (!fieldValue || typeof fieldValue !== 'object') {
            continue;
        }

        const binaryMeta = fieldValue as { fileName?: string; name?: string };
        const binaryName = binaryMeta.fileName || binaryMeta.name;
        if (!binaryName) {
            continue;
        }

        const result = await downloadBinary(client, inode, fieldVar, binaryName, assetsDir);

        if (result) {
            results.push(result);
        }
    }

    return results;
}

// ─── Upload (Push) ──────────────────────────────────────────────────────────

/**
 * Validate and prepare a binary field for upload.
 * Resolves `./` paths relative to the content file's directory.
 * Throws if the referenced file doesn't exist.
 */
export async function prepareBinaryUpload(
    binaryInfo: BinaryFieldInfo,
    contentFilePath: string
): Promise<{ fieldVariable: string; filePath: string; fileName: string; mimeType: string }> {
    const absolutePath = resolve(dirname(contentFilePath), binaryInfo.localPath);

    if (!existsSync(absolutePath)) {
        throw new Error(
            `Binary file not found: ${absolutePath} (referenced by field '${binaryInfo.fieldVariable}')`
        );
    }

    const fileStat = await stat(absolutePath);
    if (fileStat.size > SIZE_WARNING_THRESHOLD) {
        console.warn(
            `Warning: Binary file ${binaryInfo.fileName} is ${(fileStat.size / (1024 * 1024)).toFixed(1)}MB (> 10MB)`
        );
    }

    const mimeType = getMimeType(binaryInfo.fileName);

    return {
        fieldVariable: binaryInfo.fieldVariable,
        filePath: absolutePath,
        fileName: binaryInfo.fileName,
        mimeType
    };
}

/**
 * Build a FormData multipart payload for the workflow fire endpoint.
 * Adds the contentlet JSON as "json" part and each binary file as "file" part.
 *
 * The dotCMS workflow fire API expects:
 * - "json" part: `{"contentlet": {...}}` wrapped JSON
 * - "file" part: binary file data (one per binary field)
 */
export async function buildMultipartPayload(
    contentletJson: Record<string, unknown>,
    binaries: BinaryFieldInfo[],
    contentFilePath: string
): Promise<FormData> {
    const formData = new FormData();

    // Add contentlet JSON as a part — must be wrapped in { contentlet: ... }
    // Include binaryFields array so the server knows which fields receive uploaded files
    const binaryFieldNames = binaries.map((b) => b.fieldVariable);
    const wrappedJson = JSON.stringify({
        contentlet: contentletJson,
        binaryFields: binaryFieldNames
    });
    formData.append('json', new Blob([wrappedJson], { type: 'application/json' }));

    // Add each binary file — dotCMS expects "file" as the multipart field name
    for (const binaryInfo of binaries) {
        const prepared = await prepareBinaryUpload(binaryInfo, contentFilePath);
        const fileBuffer = await readFile(prepared.filePath);
        const arrayBuffer = fileBuffer.buffer.slice(
            fileBuffer.byteOffset,
            fileBuffer.byteOffset + fileBuffer.byteLength
        ) as ArrayBuffer;
        const blob = new Blob([arrayBuffer], { type: prepared.mimeType });
        formData.append('file', blob, prepared.fileName);
    }

    return formData;
}

// ─── Validation ─────────────────────────────────────────────────────────────

/**
 * Validate a binary field value.
 * - If `./` path: check file exists, check size, warn if > 10MB
 * - If `null`: field will be cleared on server
 * - If string without `./`: no-op (server keeps existing binary)
 */
export async function validateBinaryField(
    fieldValue: unknown,
    fieldVar: string,
    contentFilePath: string
): Promise<void> {
    if (fieldValue === null || fieldValue === undefined) {
        // Field will be cleared on server
        return;
    }

    if (typeof fieldValue !== 'string') {
        return;
    }

    if (!fieldValue.startsWith('./')) {
        // Plain filename — server keeps existing binary, nothing to validate
        return;
    }

    const absolutePath = resolve(dirname(contentFilePath), fieldValue);

    if (!existsSync(absolutePath)) {
        throw new Error(
            `Binary file not found: ${absolutePath} (referenced by field '${fieldVar}')`
        );
    }

    const fileStat = await stat(absolutePath);
    if (fileStat.size > SIZE_WARNING_THRESHOLD) {
        console.warn(
            `Warning: Binary file for field '${fieldVar}' is ${(fileStat.size / (1024 * 1024)).toFixed(1)}MB (> 10MB)`
        );
    }
}

// ─── Utilities ──────────────────────────────────────────────────────────────

/**
 * Simple mime type lookup based on file extension.
 */
function getMimeType(fileName: string): string {
    const ext = extname(fileName).toLowerCase();
    const mimeTypes: Record<string, string> = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.gif': 'image/gif',
        '.webp': 'image/webp',
        '.svg': 'image/svg+xml',
        '.pdf': 'application/pdf',
        '.doc': 'application/msword',
        '.docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        '.xls': 'application/vnd.ms-excel',
        '.xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        '.zip': 'application/zip',
        '.mp4': 'video/mp4',
        '.mp3': 'audio/mpeg',
        '.txt': 'text/plain',
        '.csv': 'text/csv',
        '.json': 'application/json',
        '.xml': 'application/xml'
    };

    return mimeTypes[ext] || 'application/octet-stream';
}
