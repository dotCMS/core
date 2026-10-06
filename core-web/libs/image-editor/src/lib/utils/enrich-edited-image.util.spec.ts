import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { enrichEditedImage } from './enrich-edited-image.util';

import { ImageEditorAssetContext, NormalizedPoint } from '../models/image-editor.models';

const CTX: ImageEditorAssetContext = {
    idOrTempId: 'abc123',
    inode: 'abc123',
    tempId: null,
    variable: 'binary',
    fieldName: 'binary',
    fileName: 'beach.png',
    mimeType: 'image/png',
    isTempFile: false,
    byInode: true,
    naturalWidth: 2752,
    naturalHeight: 1536,
    originalUrl: '/contentAsset/image/abc123/binary'
};

const FOCAL: NormalizedPoint = { x: 0.25, y: 0.75 };

const BASE_TEMP: DotCMSTempFile = {
    fileName: 'edited.png',
    folder: 'shared',
    id: 'temp_1',
    image: false,
    length: 1024,
    mimeType: 'image/png',
    referenceUrl: '/dA/temp_1',
    thumbnailUrl: ''
};

describe('enrichEditedImage', () => {
    it('flags the temp file as an image and seeds the focal point', () => {
        const result = enrichEditedImage(BASE_TEMP, CTX, FOCAL);

        expect(result.image).toBe(true);
        expect(result.metadata?.isImage).toBe(true);
        expect(result.metadata?.focalPoint).toBe('0.25,0.75');
    });

    it('synthesizes metadata from the context when the servlet returns none', () => {
        const tempFile = { ...BASE_TEMP, mimeType: 'unknown', metadata: null } as never;

        const result = enrichEditedImage(tempFile, CTX, FOCAL);

        expect(result.metadata).toEqual({
            contentType: 'image/png', // ctx.mimeType wins over the temp's "unknown"
            fileSize: 1024,
            length: 1024,
            modDate: 0,
            name: 'edited.png',
            sha256: '',
            title: 'edited.png',
            version: 0,
            width: 2752,
            height: 1536,
            isImage: true,
            focalPoint: '0.25,0.75'
        });
    });

    it('preserves real server metadata and only forces isImage + focal point', () => {
        const metadata = {
            contentType: 'image/jpeg',
            fileSize: 5000,
            isImage: true,
            length: 5000,
            modDate: 123,
            name: 'server.jpg',
            sha256: 'deadbeef',
            title: 'Server Title',
            version: 7,
            width: 800,
            height: 600
        };

        const result = enrichEditedImage({ ...BASE_TEMP, metadata }, CTX, FOCAL);

        expect(result.metadata).toEqual({ ...metadata, isImage: true, focalPoint: '0.25,0.75' });
    });

    it('falls back to the context file name when the temp file has none', () => {
        const result = enrichEditedImage(
            { ...BASE_TEMP, fileName: '', metadata: null } as never,
            CTX,
            FOCAL
        );

        expect(result.metadata?.name).toBe('beach.png');
        expect(result.metadata?.title).toBe('beach.png');
    });

    it('omits width/height when the context has no natural dimensions', () => {
        const ctx = { ...CTX, naturalWidth: 0, naturalHeight: 0 };

        const result = enrichEditedImage({ ...BASE_TEMP, metadata: null } as never, ctx, FOCAL);

        expect(result.metadata).not.toHaveProperty('width');
        expect(result.metadata).not.toHaveProperty('height');
    });
});
