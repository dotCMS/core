import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { getFileMetadata } from './index';

import { NEW_FILE_MOCK } from '../../../utils/mocks';

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
});
