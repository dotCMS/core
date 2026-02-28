import { existsSync } from 'node:fs';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import {
    getBinarySidecarPath,
    resolveBinaryPath,
    getBinaryFields,
    downloadBinary,
    downloadContentBinaries,
    prepareBinaryUpload,
    buildMultipartPayload,
    validateBinaryField
} from '../binary';

import type { ContentTypeSchema, BinaryFieldInfo } from '../../core/types';
import type { $Fetch } from 'ofetch';

/** Create an ArrayBuffer from a string for mocking download responses */
function toArrayBuffer(str: string): ArrayBuffer {
    const encoder = new TextEncoder();
    return encoder.encode(str).buffer as ArrayBuffer;
}

// ─── Test Helpers ───────────────────────────────────────────────────────────

let tempDir: string;

beforeEach(async () => {
    tempDir = await mkdtemp(join(tmpdir(), 'dotcli-binary-test-'));
});

afterEach(async () => {
    if (tempDir && existsSync(tempDir)) {
        await rm(tempDir, { recursive: true, force: true });
    }
});

function createMockSchema(
    fields: Array<{ variable: string; fieldType: string }>
): ContentTypeSchema {
    return {
        variable: 'TestType',
        name: 'Test Type',
        id: 'test-type-id',
        fields: fields.map((f, i) => ({
            variable: f.variable,
            name: f.variable,
            fieldType: f.fieldType,
            dataType: 'TEXT',
            sortOrder: i,
            required: false,
            fixed: false,
            readOnly: false,
            searchable: false,
            listed: false
        }))
    };
}

// ─── getBinarySidecarPath ───────────────────────────────────────────────────

describe('getBinarySidecarPath', () => {
    it('should generate correct sidecar path', () => {
        const result = getBinarySidecarPath('abc123', 'heroImage', 'photo.jpg', '/content/blog');
        expect(result).toBe(join('/content/blog', 'assets', 'abc123', 'heroImage.photo.jpg'));
    });

    it('should handle nested content directories', () => {
        const result = getBinarySidecarPath('xyz789', 'file', 'doc.pdf', '/my/deep/dir');
        expect(result).toBe(join('/my/deep/dir', 'assets', 'xyz789', 'file.doc.pdf'));
    });

    it('should handle filenames with multiple dots', () => {
        const result = getBinarySidecarPath('abc123', 'attachment', 'my.file.v2.pdf', '/content');
        expect(result).toBe(join('/content', 'assets', 'abc123', 'attachment.my.file.v2.pdf'));
    });

    it('should handle filenames with spaces', () => {
        const result = getBinarySidecarPath('abc123', 'image', 'my photo.jpg', '/content');
        expect(result).toBe(join('/content', 'assets', 'abc123', 'image.my photo.jpg'));
    });
});

// ─── resolveBinaryPath ──────────────────────────────────────────────────────

describe('resolveBinaryPath', () => {
    it('should resolve ./relative paths to absolute paths', () => {
        const result = resolveBinaryPath('./images/photo.jpg', '/content/blog/post.md');
        expect(result).toBe(join('/content/blog', 'images', 'photo.jpg'));
    });

    it('should return null for plain filenames', () => {
        expect(resolveBinaryPath('photo.jpg', '/content/blog/post.md')).toBeNull();
    });

    it('should return null for null values', () => {
        expect(resolveBinaryPath(null, '/content/blog/post.md')).toBeNull();
    });

    it('should return null for undefined values', () => {
        expect(resolveBinaryPath(undefined, '/content/blog/post.md')).toBeNull();
    });

    it('should return null for empty string', () => {
        expect(resolveBinaryPath('', '/content/blog/post.md')).toBeNull();
    });

    it('should handle nested relative paths', () => {
        const result = resolveBinaryPath('./assets/images/hero.png', '/project/content/article.md');
        expect(result).toBe(join('/project/content', 'assets', 'images', 'hero.png'));
    });

    it('should handle ./ path pointing to same directory', () => {
        const result = resolveBinaryPath('./photo.jpg', '/content/post.md');
        expect(result).toBe(join('/content', 'photo.jpg'));
    });
});

// ─── getBinaryFields ────────────────────────────────────────────────────────

describe('getBinaryFields', () => {
    it('should extract Binary field variables', () => {
        const schema = createMockSchema([
            { variable: 'title', fieldType: 'Text' },
            { variable: 'heroImage', fieldType: 'Binary' },
            { variable: 'body', fieldType: 'WYSIWYG' }
        ]);
        expect(getBinaryFields(schema)).toEqual(['heroImage']);
    });

    it('should extract Image field variables', () => {
        const schema = createMockSchema([{ variable: 'photo', fieldType: 'Image' }]);
        expect(getBinaryFields(schema)).toEqual(['photo']);
    });

    it('should extract File field variables', () => {
        const schema = createMockSchema([{ variable: 'attachment', fieldType: 'File' }]);
        expect(getBinaryFields(schema)).toEqual(['attachment']);
    });

    it('should extract multiple binary field types', () => {
        const schema = createMockSchema([
            { variable: 'title', fieldType: 'Text' },
            { variable: 'heroImage', fieldType: 'Binary' },
            { variable: 'thumbnail', fieldType: 'Image' },
            { variable: 'attachment', fieldType: 'File' },
            { variable: 'body', fieldType: 'WYSIWYG' }
        ]);
        expect(getBinaryFields(schema)).toEqual(['heroImage', 'thumbnail', 'attachment']);
    });

    it('should return empty array when no binary fields', () => {
        const schema = createMockSchema([
            { variable: 'title', fieldType: 'Text' },
            { variable: 'body', fieldType: 'WYSIWYG' }
        ]);
        expect(getBinaryFields(schema)).toEqual([]);
    });
});

// ─── downloadBinary ─────────────────────────────────────────────────────────

describe('downloadBinary', () => {
    it('should download binary and save to disk', async () => {
        const content = 'fake image content';
        const mockClient = jest.fn().mockResolvedValue(toArrayBuffer(content)) as unknown as $Fetch;

        const result = await downloadBinary(
            mockClient,
            'inode-123',
            'heroImage',
            'photo.jpg',
            join(tempDir, 'assets', 'abc123')
        );

        expect(result).not.toBeNull();
        expect(result!.fieldVariable).toBe('heroImage');
        expect(result!.fileName).toBe('photo.jpg');
        expect(result!.size).toBe(new TextEncoder().encode(content).length);
        expect(existsSync(result!.savedPath)).toBe(true);

        expect(mockClient).toHaveBeenCalledWith('/contentAsset/raw-data/inode-123/heroImage', {
            method: 'GET',
            responseType: 'arrayBuffer'
        });
    });

    it('should return null on 404 (no binary content)', async () => {
        const error = new Error('Not Found');
        (error as unknown as { status: number }).status = 404;
        const mockClient = jest.fn().mockRejectedValue(error) as unknown as $Fetch;

        const result = await downloadBinary(
            mockClient,
            'inode-123',
            'heroImage',
            'photo.jpg',
            join(tempDir, 'assets', 'abc123')
        );

        expect(result).toBeNull();
    });

    it('should return null on 404 via response property', async () => {
        const error = new Error('Not Found');
        (error as unknown as { response: { status: number } }).response = { status: 404 };
        const mockClient = jest.fn().mockRejectedValue(error) as unknown as $Fetch;

        const result = await downloadBinary(
            mockClient,
            'inode-123',
            'heroImage',
            'photo.jpg',
            join(tempDir, 'assets', 'abc123')
        );

        expect(result).toBeNull();
    });

    it('should rethrow non-404 errors', async () => {
        const error = new Error('Server Error');
        (error as unknown as { status: number }).status = 500;
        const mockClient = jest.fn().mockRejectedValue(error) as unknown as $Fetch;

        await expect(
            downloadBinary(mockClient, 'inode-123', 'heroImage', 'photo.jpg', tempDir)
        ).rejects.toThrow('Server Error');
    });

    it('should create directory structure if it does not exist', async () => {
        const mockClient = jest
            .fn()
            .mockResolvedValue(toArrayBuffer('content')) as unknown as $Fetch;

        const deepDir = join(tempDir, 'a', 'b', 'c');
        const result = await downloadBinary(mockClient, 'inode-1', 'file', 'doc.pdf', deepDir);

        expect(result).not.toBeNull();
        expect(existsSync(result!.savedPath)).toBe(true);
    });
});

// ─── downloadContentBinaries ────────────────────────────────────────────────

describe('downloadContentBinaries', () => {
    it('should download all binary fields for a contentlet', async () => {
        const schema = createMockSchema([
            { variable: 'title', fieldType: 'Text' },
            { variable: 'heroImage', fieldType: 'Binary' },
            { variable: 'thumbnail', fieldType: 'Image' }
        ]);

        const record = {
            identifier: 'abcdef123456',
            inode: 'inode-abc',
            title: 'Test Post',
            heroImage: { idPath: '/data/abc/heroImage', name: 'hero.jpg' },
            thumbnail: { idPath: '/data/abc/thumbnail', name: 'thumb.png' }
        };

        const mockClient = jest
            .fn()
            .mockResolvedValue(toArrayBuffer('binary data')) as unknown as $Fetch;

        const results = await downloadContentBinaries(mockClient, record, schema, tempDir);

        expect(results).toHaveLength(2);
        expect(results[0].fieldVariable).toBe('heroImage');
        expect(results[0].fileName).toBe('hero.jpg');
        expect(results[1].fieldVariable).toBe('thumbnail');
        expect(results[1].fileName).toBe('thumb.png');
    });

    it('should skip fields with no binary data', async () => {
        const schema = createMockSchema([
            { variable: 'heroImage', fieldType: 'Binary' },
            { variable: 'optionalFile', fieldType: 'File' }
        ]);

        const record = {
            identifier: 'abcdef123456',
            inode: 'inode-abc',
            heroImage: { idPath: '/data/abc/heroImage', name: 'hero.jpg' },
            optionalFile: null
        };

        const mockClient = jest.fn().mockResolvedValue(toArrayBuffer('data')) as unknown as $Fetch;

        const results = await downloadContentBinaries(mockClient, record, schema, tempDir);

        expect(results).toHaveLength(1);
        expect(results[0].fieldVariable).toBe('heroImage');
    });

    it('should return empty array if record has no identifier', async () => {
        const schema = createMockSchema([{ variable: 'file', fieldType: 'Binary' }]);
        const mockClient = jest.fn() as unknown as $Fetch;

        const results = await downloadContentBinaries(mockClient, {}, schema, tempDir);
        expect(results).toEqual([]);
        expect(mockClient).not.toHaveBeenCalled();
    });

    it('should return empty array if record has no inode', async () => {
        const schema = createMockSchema([{ variable: 'file', fieldType: 'Binary' }]);
        const mockClient = jest.fn() as unknown as $Fetch;

        const results = await downloadContentBinaries(
            mockClient,
            { identifier: 'abc123' },
            schema,
            tempDir
        );
        expect(results).toEqual([]);
    });

    it('should skip binary fields where name is missing', async () => {
        const schema = createMockSchema([{ variable: 'heroImage', fieldType: 'Binary' }]);
        const record = {
            identifier: 'abcdef123456',
            inode: 'inode-abc',
            heroImage: { idPath: '/some/path' } // no name
        };

        const mockClient = jest.fn() as unknown as $Fetch;
        const results = await downloadContentBinaries(mockClient, record, schema, tempDir);

        expect(results).toEqual([]);
        expect(mockClient).not.toHaveBeenCalled();
    });
});

// ─── prepareBinaryUpload ────────────────────────────────────────────────────

describe('prepareBinaryUpload', () => {
    it('should resolve and validate existing file', async () => {
        const filePath = join(tempDir, 'photo.jpg');
        await writeFile(filePath, 'fake image');

        const binaryInfo: BinaryFieldInfo = {
            fieldVariable: 'heroImage',
            localPath: './photo.jpg',
            fileName: 'photo.jpg'
        };

        const contentFile = join(tempDir, 'content.md');
        const result = await prepareBinaryUpload(binaryInfo, contentFile);

        expect(result.fieldVariable).toBe('heroImage');
        expect(result.filePath).toBe(filePath);
        expect(result.fileName).toBe('photo.jpg');
        expect(result.mimeType).toBe('image/jpeg');
    });

    it('should throw when file does not exist', async () => {
        const binaryInfo: BinaryFieldInfo = {
            fieldVariable: 'heroImage',
            localPath: './missing.jpg',
            fileName: 'missing.jpg'
        };

        const contentFile = join(tempDir, 'content.md');

        await expect(prepareBinaryUpload(binaryInfo, contentFile)).rejects.toThrow(
            /Binary file not found.*referenced by field 'heroImage'/
        );
    });

    it('should warn when file exceeds 10MB', async () => {
        const filePath = join(tempDir, 'large.pdf');
        // Create a file > 10MB by writing sparse content
        const largeBuffer = Buffer.alloc(11 * 1024 * 1024, 0);
        await writeFile(filePath, largeBuffer);

        const warnSpy = jest.spyOn(console, 'warn').mockImplementation();

        const binaryInfo: BinaryFieldInfo = {
            fieldVariable: 'document',
            localPath: './large.pdf',
            fileName: 'large.pdf'
        };

        await prepareBinaryUpload(binaryInfo, join(tempDir, 'content.md'));

        expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('> 10MB'));
        warnSpy.mockRestore();
    });

    it('should detect correct mime types', async () => {
        const testCases = [
            { ext: '.png', expected: 'image/png' },
            { ext: '.pdf', expected: 'application/pdf' },
            { ext: '.gif', expected: 'image/gif' },
            { ext: '.unknown', expected: 'application/octet-stream' }
        ];

        for (const tc of testCases) {
            const filePath = join(tempDir, `test${tc.ext}`);
            await writeFile(filePath, 'data');

            const binaryInfo: BinaryFieldInfo = {
                fieldVariable: 'file',
                localPath: `./test${tc.ext}`,
                fileName: `test${tc.ext}`
            };

            const result = await prepareBinaryUpload(binaryInfo, join(tempDir, 'content.md'));
            expect(result.mimeType).toBe(tc.expected);
        }
    });
});

// ─── buildMultipartPayload ──────────────────────────────────────────────────

describe('buildMultipartPayload', () => {
    it('should build FormData with JSON and binary parts', async () => {
        const filePath = join(tempDir, 'photo.jpg');
        await writeFile(filePath, 'fake image data');

        const contentletJson = {
            contentType: 'Blog',
            title: 'Test',
            identifier: 'abc123'
        };

        const binaries: BinaryFieldInfo[] = [
            {
                fieldVariable: 'heroImage',
                localPath: './photo.jpg',
                fileName: 'photo.jpg'
            }
        ];

        const contentFile = join(tempDir, 'content.md');

        const formData = await buildMultipartPayload(contentletJson, binaries, contentFile);

        expect(formData).toBeInstanceOf(FormData);
        expect(formData.get('json')).toBeInstanceOf(Blob);
        expect(formData.get('heroImage')).toBeInstanceOf(Blob);
    });

    it('should include multiple binary parts', async () => {
        await writeFile(join(tempDir, 'image.jpg'), 'image data');
        await writeFile(join(tempDir, 'doc.pdf'), 'pdf data');

        const binaries: BinaryFieldInfo[] = [
            { fieldVariable: 'heroImage', localPath: './image.jpg', fileName: 'image.jpg' },
            { fieldVariable: 'attachment', localPath: './doc.pdf', fileName: 'doc.pdf' }
        ];

        const formData = await buildMultipartPayload(
            { contentType: 'Blog' },
            binaries,
            join(tempDir, 'content.md')
        );

        expect(formData.get('json')).toBeInstanceOf(Blob);
        expect(formData.get('heroImage')).toBeInstanceOf(Blob);
        expect(formData.get('attachment')).toBeInstanceOf(Blob);
    });

    it('should include only JSON when no binaries provided', async () => {
        const formData = await buildMultipartPayload(
            { contentType: 'Blog' },
            [],
            join(tempDir, 'content.md')
        );

        expect(formData.get('json')).toBeInstanceOf(Blob);
    });

    it('should throw if a binary file is missing', async () => {
        const binaries: BinaryFieldInfo[] = [
            {
                fieldVariable: 'heroImage',
                localPath: './nonexistent.jpg',
                fileName: 'nonexistent.jpg'
            }
        ];

        await expect(
            buildMultipartPayload({ contentType: 'Blog' }, binaries, join(tempDir, 'content.md'))
        ).rejects.toThrow(/Binary file not found/);
    });
});

// ─── validateBinaryField ────────────────────────────────────────────────────

describe('validateBinaryField', () => {
    it('should pass for null value (clear field)', async () => {
        await expect(
            validateBinaryField(null, 'heroImage', join(tempDir, 'content.md'))
        ).resolves.toBeUndefined();
    });

    it('should pass for undefined value', async () => {
        await expect(
            validateBinaryField(undefined, 'heroImage', join(tempDir, 'content.md'))
        ).resolves.toBeUndefined();
    });

    it('should pass for plain filename (no ./)', async () => {
        await expect(
            validateBinaryField('photo.jpg', 'heroImage', join(tempDir, 'content.md'))
        ).resolves.toBeUndefined();
    });

    it('should pass for existing ./ referenced file', async () => {
        await writeFile(join(tempDir, 'photo.jpg'), 'image data');

        await expect(
            validateBinaryField('./photo.jpg', 'heroImage', join(tempDir, 'content.md'))
        ).resolves.toBeUndefined();
    });

    it('should throw for missing ./ referenced file', async () => {
        await expect(
            validateBinaryField('./missing.jpg', 'heroImage', join(tempDir, 'content.md'))
        ).rejects.toThrow(/Binary file not found.*referenced by field 'heroImage'/);
    });

    it('should warn for large ./ referenced file', async () => {
        const filePath = join(tempDir, 'large.bin');
        const largeBuffer = Buffer.alloc(11 * 1024 * 1024, 0);
        await writeFile(filePath, largeBuffer);

        const warnSpy = jest.spyOn(console, 'warn').mockImplementation();

        await validateBinaryField('./large.bin', 'document', join(tempDir, 'content.md'));

        expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('> 10MB'));
        warnSpy.mockRestore();
    });

    it('should pass for non-string values', async () => {
        await expect(
            validateBinaryField(42, 'heroImage', join(tempDir, 'content.md'))
        ).resolves.toBeUndefined();
    });
});
