import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

import {
    cacheContentType,
    cacheLanguages,
    clearCache,
    getCachedContentType,
    getCachedLanguages
} from '../cache';

import type { ContentTypeSchema, LanguageMap } from '../types';

describe('cache', () => {
    let cacheDir: string;

    const mockSchema: ContentTypeSchema = {
        variable: 'Blog',
        name: 'Blog',
        id: '123-456',
        fields: [
            {
                variable: 'title',
                name: 'Title',
                fieldType: 'Text',
                dataType: 'TEXT',
                sortOrder: 1,
                required: true,
                fixed: false,
                readOnly: false,
                searchable: true,
                listed: true
            }
        ]
    };

    const mockLanguages: LanguageMap = {
        '1': 'en-US',
        '2': 'es-ES'
    };

    beforeEach(() => {
        cacheDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-cache-'));
    });

    afterEach(() => {
        fs.rmSync(cacheDir, { recursive: true, force: true });
    });

    describe('getCachedContentType', () => {
        it('should return null when no cache file exists', () => {
            const result = getCachedContentType(cacheDir, 'Blog');
            expect(result).toBeNull();
        });

        it('should return cached schema when fresh', () => {
            cacheContentType(cacheDir, 'Blog', mockSchema);

            const result = getCachedContentType(cacheDir, 'Blog');
            expect(result).not.toBeNull();
            expect(result!.variable).toBe('Blog');
            expect(result!.fields).toHaveLength(1);
            expect(result!.cachedAt).toBeDefined();
        });

        it('should return null when cache is stale', () => {
            // Write a schema with an old timestamp
            const dir = path.join(cacheDir, 'content-types');
            fs.mkdirSync(dir, { recursive: true });
            const staleSchema: ContentTypeSchema = {
                ...mockSchema,
                cachedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString() // 2 hours ago
            };
            fs.writeFileSync(path.join(dir, 'Blog.json'), JSON.stringify(staleSchema), 'utf-8');

            const result = getCachedContentType(cacheDir, 'Blog');
            expect(result).toBeNull();
        });

        it('should respect custom TTL', () => {
            cacheContentType(cacheDir, 'Blog', mockSchema);

            // With a very short TTL (0ms), cache should be stale immediately
            // Use a small delay check — freshly written should pass with normal TTL
            const result = getCachedContentType(cacheDir, 'Blog', 60 * 60 * 1000);
            expect(result).not.toBeNull();

            // Now with 0ms TTL everything is stale
            const stale = getCachedContentType(cacheDir, 'Blog', 0);
            expect(stale).toBeNull();
        });

        it('should return null when cachedAt is missing', () => {
            const dir = path.join(cacheDir, 'content-types');
            fs.mkdirSync(dir, { recursive: true });
            const noCachedAt = { ...mockSchema };
            delete (noCachedAt as Partial<ContentTypeSchema>).cachedAt;
            fs.writeFileSync(path.join(dir, 'Blog.json'), JSON.stringify(noCachedAt), 'utf-8');

            const result = getCachedContentType(cacheDir, 'Blog');
            expect(result).toBeNull();
        });
    });

    describe('cacheContentType', () => {
        it('should create cache directory and write schema', () => {
            cacheContentType(cacheDir, 'Blog', mockSchema);

            const filePath = path.join(cacheDir, 'content-types', 'Blog.json');
            expect(fs.existsSync(filePath)).toBe(true);

            const raw = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
            expect(raw.variable).toBe('Blog');
            expect(raw.cachedAt).toBeDefined();
        });

        it('should overwrite existing cache', () => {
            cacheContentType(cacheDir, 'Blog', mockSchema);

            const updated: ContentTypeSchema = {
                ...mockSchema,
                name: 'Blog Updated'
            };
            cacheContentType(cacheDir, 'Blog', updated);

            const result = getCachedContentType(cacheDir, 'Blog');
            expect(result!.name).toBe('Blog Updated');
        });
    });

    describe('getCachedLanguages', () => {
        it('should return null when no cache file exists', () => {
            const result = getCachedLanguages(cacheDir);
            expect(result).toBeNull();
        });

        it('should return cached languages when fresh', () => {
            cacheLanguages(cacheDir, mockLanguages);

            const result = getCachedLanguages(cacheDir);
            expect(result).toEqual(mockLanguages);
        });

        it('should return null when cache is stale', () => {
            fs.mkdirSync(cacheDir, { recursive: true });
            const staleData = {
                cachedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
                languages: mockLanguages
            };
            fs.writeFileSync(
                path.join(cacheDir, 'languages.json'),
                JSON.stringify(staleData),
                'utf-8'
            );

            const result = getCachedLanguages(cacheDir);
            expect(result).toBeNull();
        });
    });

    describe('cacheLanguages', () => {
        it('should create cache directory and write languages', () => {
            cacheLanguages(cacheDir, mockLanguages);

            const filePath = path.join(cacheDir, 'languages.json');
            expect(fs.existsSync(filePath)).toBe(true);

            const raw = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
            expect(raw.languages).toEqual(mockLanguages);
            expect(raw.cachedAt).toBeDefined();
        });
    });

    describe('clearCache', () => {
        it('should clear a specific content type cache', () => {
            cacheContentType(cacheDir, 'Blog', mockSchema);
            cacheContentType(cacheDir, 'Article', { ...mockSchema, variable: 'Article' });

            clearCache(cacheDir, 'Blog');

            expect(getCachedContentType(cacheDir, 'Blog')).toBeNull();
            expect(getCachedContentType(cacheDir, 'Article')).not.toBeNull();
        });

        it('should clear entire cache directory', () => {
            cacheContentType(cacheDir, 'Blog', mockSchema);
            cacheLanguages(cacheDir, mockLanguages);

            clearCache(cacheDir);

            expect(fs.existsSync(cacheDir)).toBe(false);
        });

        it('should not throw when clearing nonexistent type', () => {
            expect(() => clearCache(cacheDir, 'NonExistent')).not.toThrow();
        });

        it('should not throw when clearing nonexistent directory', () => {
            const nonExistent = path.join(os.tmpdir(), 'nonexistent-cache-dir-12345');
            expect(() => clearCache(nonExistent)).not.toThrow();
        });
    });
});
