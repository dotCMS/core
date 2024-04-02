import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DEFAULT_IMAGE_URL_PATTERN, formatDotImageNode, replaceURLPattern } from './editor.utils';

describe('Editor Utils', () => {
    describe('formatDotImageNode', () => {
        it('should return formatted image node', () => {
            const asset: DotCMSContentlet = {
                ...EMPTY_CONTENTLET,
                baseType: 'DOTASSET',
                assetVersion: 'version',
                asset: 'asset',
                title: 'title',
                titleImage: 'titleImage',
                inode: 'inode',
                identifier: 'identifier'
            };

            const pattern = `/dA/{shortyId}/{name}?language_id={languageId}`;
            const src = replaceURLPattern(pattern, asset);
            const result = formatDotImageNode(pattern, asset);

            expect(result).toBe(
                `<img src="${src}"\n` +
                    `alt="${asset.title}"\n` +
                    `data-field-name="${asset.titleImage}"\n` +
                    `data-inode="${asset.inode}"\n` +
                    `data-identifier="${asset.identifier}"\n` +
                    `data-saveas="${asset.title}" />`
            );
        });
    });

    describe('replaceURLPattern', () => {
        const ASSET: DotCMSContentlet = {
            ...EMPTY_CONTENTLET,
            fileName: 'file',
            name: 'name',
            path: '/path',
            extension: 'ext',
            languageId: 1,
            hostName: 'hostname',
            inode: 'inode-123',
            host: 'hostId',
            identifier: 'identifier-123'
        };

        describe('FILEASSET', () => {
            const PATTERN =
                '/{fileName}/{path}/{extension}/{languageId}/{hostname}/{inode}/{hostId}/{identifier}/{shortyInode}/{shortyId}';

            it('should replace placeholders in the pattern with asset properties', () => {
                const result = replaceURLPattern(PATTERN, ASSET);
                const expected =
                    '/file//path/ext/1/hostname/inode-123/hostId/identifier-123/inode123/identifier';

                expect(result).toEqual(expected);
            });
        });

        describe('CONTENTASSET', () => {
            const PATTERN =
                '/{name}/{path}/{extension}/{languageId}/{hostname}/{inode}/{hostId}/{identifier}/{shortyInode}/{shortyId}';

            it('should replace placeholders in the pattern with asset properties and return the pattern', () => {
                const CONTENT_ASSET = {
                    ...ASSET,
                    fileName: '',
                    name: 'name',
                    baseType: 'CONTENT'
                };
                const result = replaceURLPattern(PATTERN, CONTENT_ASSET);
                const expected =
                    '/name//path/ext/1/hostname/inode-123/hostId/identifier-123/inode123/identifier';

                expect(result).toEqual(expected);
            });
        });
    });

    describe('CONSTANTS', () => {
        it('should have a default image url pattern', () => {
            expect(DEFAULT_IMAGE_URL_PATTERN).toBe(
                '/dA/{shortyId}/{name}?language_id={languageId}'
            );
        });
    });
});
