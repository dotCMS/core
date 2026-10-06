/** Stable 800x800 public PNG URL for import-from-URL tests; override via E2E_IMPORT_URL env. */
export const E2E_IMPORT_URL = process.env['E2E_IMPORT_URL'] ?? 'https://placehold.co/800x800.png';

export const REQUIRED_FIELD_ERROR = 'This field is mandatory';

/** Minimal text file buffer for file/binary upload tests. */
export function createTestTextFile(name = 'e2e-test-file.txt') {
    return {
        name,
        mimeType: 'text/plain',
        buffer: Buffer.from('dotCMS E2E test file content')
    };
}

/** Minimal 1x1 PNG for image upload tests. */
export function createTestPngFile(name = 'e2e-test-image.png') {
    const base64Png =
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
    return {
        name,
        mimeType: 'image/png',
        buffer: Buffer.from(base64Png, 'base64')
    };
}
