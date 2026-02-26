import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { getFileMetadata, getFileVersion, cleanMimeTypes, checkMimeType } from './contentlet.utils';

import { NEW_FILE_MOCK, TEMP_FILE_MOCK } from '../../../utils/mocks';

describe('utils', () => {
    describe('getFileMetadata', () => {
        it('should return metaData if present', () => {
            const contentlet: DotCMSContentlet = {
                ...NEW_FILE_MOCK.entity,
                metaData: {
                    title: 'test'
                }
            };

            const result = getFileMetadata(contentlet);
            expect(result).toEqual(contentlet.metaData);
        });

        it('should return assetMetaData if metaData is not present', () => {
            const contentlet: DotCMSContentlet = NEW_FILE_MOCK.entity;

            const result = getFileMetadata(contentlet);
            expect(result).toEqual(contentlet.assetMetaData);
        });

        it('should return an empty object if neither metaData nor assetMetaData is present', () => {
            const contentlet: DotCMSContentlet = {} as DotCMSContentlet;

            const result = getFileMetadata(contentlet) as unknown;
            expect(result).toEqual({});
        });
    });

    describe('getFileVersion', () => {
        it('should return assetVersion if present', () => {
            const contentlet: DotCMSContentlet = {
                ...NEW_FILE_MOCK.entity,
                assetVersion: '1.0'
            };

            const result = getFileVersion(contentlet);
            expect(result).toEqual('1.0');
        });

        it('should return null if assetVersion is not present', () => {
            const contentlet: DotCMSContentlet = {
                ...NEW_FILE_MOCK.entity
            };
            delete contentlet.assetVersion;

            const result = getFileVersion(contentlet);
            expect(result).toBeNull();
        });
    });

    describe('cleanMimeTypes', () => {
        it('should return an empty array for empty input', () => {
            const result = cleanMimeTypes([]);
            expect(result).toEqual([]);
        });

        it('should return an array of cleaned mime types', () => {
            const input = ['image/jpeg', 'application/pdf'];
            const result = cleanMimeTypes(input);
            expect(result).toEqual(['image/jpeg', 'application/pdf']);
        });

        it('should remove asterisks from mime types', () => {
            const input = ['image/*', 'application/*'];
            const expected = ['image/', 'application/'];
            expect(cleanMimeTypes(input)).toEqual(expected);
        });

        it('should convert mime types to lower case', () => {
            const input: string[] = ['IMAGE/JPEG', 'APPLICATION/PDF'];
            const expected: string[] = ['image/jpeg', 'application/pdf'];
            expect(cleanMimeTypes(input)).toEqual(expected);
        });

        it('should filter out empty strings', () => {
            const input: string[] = ['image/jpeg', '', 'application/pdf'];
            const expected: string[] = ['image/jpeg', 'application/pdf'];
            expect(cleanMimeTypes(input)).toEqual(expected);
        });
    });

    describe('checkMimeType', () => {
        it('returns true for empty accepted files array', () => {
            const file = {
                ...TEMP_FILE_MOCK,
                mimeType: 'image/jpeg'
            };
            const acceptedFiles = [];
            expect(checkMimeType(file, acceptedFiles)).toBe(true);
        });

        it('returns true for matching mime type', () => {
            const file = {
                ...TEMP_FILE_MOCK,
                mimeType: 'image/jpeg'
            };
            const acceptedFiles = ['image/jpeg'];
            expect(checkMimeType(file, acceptedFiles)).toBe(true);
        });

        it('returns false for non-matching mime type', () => {
            const file = {
                ...TEMP_FILE_MOCK,
                mimeType: 'image/jpeg'
            };
            const acceptedFiles = ['application/pdf'];
            expect(checkMimeType(file, acceptedFiles)).toBe(false);
        });

        it('returns false for file with no mime type', () => {
            const file = {
                ...TEMP_FILE_MOCK,
                mimeType: null
            };
            const acceptedFiles = ['image/jpeg'];
            expect(checkMimeType(file, acceptedFiles)).toBe(false);
        });

        it('returns true for accepted files array with asterisk (*)', () => {
            const file = {
                ...TEMP_FILE_MOCK,
                mimeType: 'image/jpeg'
            };
            const acceptedFiles = ['image/*'];
            expect(checkMimeType(file, acceptedFiles)).toBe(true);
        });

        it('returns true for accepted files array with multiple mime types', () => {
            const file = {
                ...TEMP_FILE_MOCK,
                mimeType: 'image/jpeg'
            };
            const acceptedFiles = ['image/*', 'application/pdf'];
            expect(checkMimeType(file, acceptedFiles)).toBe(true);
        });
    });
});
