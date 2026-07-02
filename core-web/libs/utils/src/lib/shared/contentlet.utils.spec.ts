import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    getFileMetadata,
    getFileVersion,
    cleanMimeTypes,
    checkMimeType,
    isImageFile
} from './contentlet.utils';

const NEW_FILE_MOCK: { entity: DotCMSContentlet } = {
    entity: {
        assetMetaData: {
            contentType: 'image/jpeg',
            editableAsText: false,
            fileSize: 3878653,
            height: 1536,
            isImage: true,
            length: 3878653,
            modDate: 1727377876393,
            name: 'image 2.jpg',
            sha256: '132597a99d807d12d0b13d9bf3149c6644d9f252e33896d95fc9fd177320da62',
            title: 'image 2.jpg',
            version: 20220201,
            width: 2688
        },
        baseType: 'DOTASSET',
        contentType: 'dotAsset',
        identifier: 'a991ddc5-39dc-4782-bc04-f4c4fa0ccff6',
        inode: 'fe160e65-5cf4-4ef6-9b1d-47c5326fec30',
        languageId: 1,
        live: true,
        title: 'image 2.jpg',
        working: true
    } as unknown as DotCMSContentlet
};

const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'enterprise-angular.pdf',
    folder: '',
    id: 'temp_1e8021f973',
    image: false,
    length: 13909932,
    metadata: {
        contentType: 'application/pdf',
        editableAsText: false,
        fileSize: 13909932,
        isImage: false,
        length: 13909932,
        modDate: 1727375044693,
        name: 'enterprise-angular.pdf',
        sha256: '7f8bc1f6485876ca6d49be77917bd35ae3de99f9a56ff94a42df3217419b30cd',
        title: 'enterprise-angular.pdf',
        version: 20220201
    },
    mimeType: 'application/pdf',
    referenceUrl: '/dA/temp_1e8021f973/tmp/enterprise-angular.pdf',
    thumbnailUrl:
        '/contentAsset/image/temp_1e8021f973/tmp/filter/Thumbnail/thumbnail_w/250/thumbnail_h/250/enterprise-angular.pdf'
};

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

    describe('isImageFile', () => {
        it('returns false for null or undefined metadata', () => {
            expect(isImageFile(null)).toBe(false);
            expect(isImageFile(undefined)).toBe(false);
        });

        it('returns false for empty metadata', () => {
            expect(isImageFile({})).toBe(false);
        });

        it('trusts the authoritative isImage flag', () => {
            expect(isImageFile({ isImage: true })).toBe(true);
            expect(
                isImageFile({ isImage: false, contentType: 'application/pdf', name: 'a.pdf' })
            ).toBe(false);
        });

        it('falls back to an image/* content type when isImage is absent', () => {
            expect(isImageFile({ contentType: 'image/png' })).toBe(true);
            expect(isImageFile({ contentType: 'IMAGE/PNG' })).toBe(true);
            expect(isImageFile({ contentType: 'application/pdf' })).toBe(false);
        });

        it('falls back to a known image extension as a last resort', () => {
            expect(isImageFile({ name: 'photo.PNG' })).toBe(true);
            expect(isImageFile({ name: 'archive.tar.gz' })).toBe(false);
            expect(isImageFile({ name: 'noextension' })).toBe(false);
        });

        it.each(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'ico', 'tiff', 'avif', 'heic'])(
            'detects "%s" as an image extension',
            (ext) => {
                expect(isImageFile({ name: `file.${ext}` })).toBe(true);
            }
        );

        it.each(['pdf', 'docx', 'txt', 'mp4', 'zip', 'json'])(
            'does not detect "%s" as an image extension',
            (ext) => {
                expect(isImageFile({ name: `file.${ext}` })).toBe(false);
            }
        );

        it('detects a referenced dotAsset via its assetMetaData', () => {
            expect(isImageFile(getFileMetadata(NEW_FILE_MOCK.entity))).toBe(true);
        });
    });
});
