import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    contentletToThumbnailModel,
    resolveContentletIcon,
    tempFileToThumbnailModel,
    translateIconName
} from './dot-content-thumbnail.utils';

const INODE = 'fe160e65-5cf4-4ef6-9b1d-47c5326fec30';

const createContentlet = (overrides: Record<string, unknown> = {}): DotCMSContentlet =>
    ({
        inode: INODE,
        identifier: 'identifier-123',
        title: 'A file',
        modDate: '1727377876000',
        baseType: 'DOTASSET',
        contentType: 'dotAsset',
        ...overrides
    }) as unknown as DotCMSContentlet;

const createTempFile = (overrides: Partial<DotCMSTempFile> = {}): DotCMSTempFile => ({
    fileName: 'file.pdf',
    folder: 'folder',
    id: 'temp_123',
    image: false,
    length: 100,
    mimeType: 'application/pdf',
    referenceUrl: '/dA/temp_123/tmp/file.pdf',
    thumbnailUrl: '/contentAsset/image/temp_123/tmp/filter/Thumbnail/thumbnail.png',
    ...overrides
});

describe('translateIconName', () => {
    it.each([
        ['pdfIcon', 'insert_drive_file'],
        ['docxIcon', 'insert_drive_file'],
        ['mp4Icon', 'videocam'],
        ['mp3Icon', 'audiotrack'],
        ['jpgIcon', 'image'],
        ['woffIcon', 'font_download'],
        ['pageIcon', 'web'],
        ['gearIcon', 'settings'],
        ['contentIcon', 'library_books'],
        ['formIcon', 'format_list_bulleted'],
        ['personaIcon', 'person'],
        ['folderIcon', 'folder'],
        ['uknIcon', 'insert_drive_file']
    ])('translates legacy name %s to %s', (raw, expected) => {
        expect(translateIconName(raw)).toBe(expected);
    });

    it('passes through direct material icon names', () => {
        expect(translateIconName('event_note')).toBe('event_note');
    });

    it('passes through the literal "Icon" as a direct name', () => {
        expect(translateIconName('Icon')).toBe('Icon');
    });

    it('falls back to insert_drive_file for unknown Icon-suffixed extensions', () => {
        expect(translateIconName('unknownextIcon')).toBe('insert_drive_file');
    });

    it.each([
        ['', 'insert_drive_file'],
        [null, 'insert_drive_file'],
        [undefined, 'insert_drive_file']
    ])('falls back to insert_drive_file for %s', (raw, expected) => {
        expect(translateIconName(raw as string)).toBe(expected);
    });
});

describe('resolveContentletIcon', () => {
    it('prefers __icon__ for FILEASSET base type', () => {
        const contentlet = createContentlet({
            baseType: 'FILEASSET',
            __icon__: 'pdfIcon',
            contentTypeIcon: 'event_note',
            icon: 'other'
        });

        expect(resolveContentletIcon(contentlet)).toBe('insert_drive_file');
    });

    it('prefers contentTypeIcon for non-FILEASSET base types', () => {
        const contentlet = createContentlet({
            __icon__: 'pdfIcon',
            contentTypeIcon: 'event_note',
            icon: 'other'
        });

        expect(resolveContentletIcon(contentlet)).toBe('event_note');
    });

    it('falls back through the precedence chain to icon', () => {
        const contentlet = createContentlet({ icon: 'mp4Icon' });

        expect(resolveContentletIcon(contentlet)).toBe('videocam');
    });

    it('falls back to insert_drive_file when no icon fields exist', () => {
        expect(resolveContentletIcon(createContentlet())).toBe('insert_drive_file');
    });
});

describe('contentletToThumbnailModel', () => {
    describe('guard', () => {
        it('returns icon model when inode is missing', () => {
            const model = contentletToThumbnailModel(createContentlet({ inode: undefined }));

            expect(model).toEqual({
                type: 'icon',
                src: '',
                icon: 'insert_drive_file',
                alt: 'A file'
            });
        });
    });

    describe('video', () => {
        it('builds a playable video src with fieldVariable', () => {
            const model = contentletToThumbnailModel(createContentlet({ mimeType: 'video/mp4' }), {
                fieldVariable: 'asset',
                playableVideo: true
            });

            expect(model.type).toBe('video');
            expect(model.playable).toBe(true);
            expect(model.src).toBe(`/dA/${INODE}/asset`);
        });

        it('falls back to titleImage for the playable src when no fieldVariable', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ mimeType: 'video/mp4', titleImage: 'fileAsset' }),
                { playableVideo: true }
            );

            expect(model.src).toBe(`/dA/${INODE}/fileAsset`);
        });

        it('builds a first-frame src (#t=0.1) through the fieldVariable when not playable', () => {
            const model = contentletToThumbnailModel(createContentlet({ mimeType: 'video/mp4' }), {
                fieldVariable: 'binaryVideo'
            });

            expect(model.type).toBe('video');
            expect(model.playable).toBe(false);
            expect(model.src).toBe(`/dA/${INODE}/binaryVideo#t=0.1`);
        });

        it('builds a first-frame src without a field segment for standalone assets', () => {
            const model = contentletToThumbnailModel(createContentlet({ mimeType: 'video/mp4' }));

            expect(model.src).toBe(`/dA/${INODE}#t=0.1`);
        });

        it('detects video from metadata contentType', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ metaData: { contentType: 'video/webm' } })
            );

            expect(model.type).toBe('video');
        });

        it('returns icon when showVideoThumbnail is false', () => {
            const model = contentletToThumbnailModel(createContentlet({ mimeType: 'video/mp4' }), {
                showVideoThumbnail: false
            });

            expect(model.type).toBe('icon');
        });
    });

    describe('pdf', () => {
        it('builds the pdf_page URL with fieldVariable', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ mimeType: 'application/pdf' }),
                { fieldVariable: 'asset' }
            );

            expect(model.type).toBe('pdf');
            expect(model.src).toBe(
                `/contentAsset/image/${INODE}/asset/pdf_page/1/resize_w/250/quality_q/45`
            );
        });

        it('falls back to titleImage when no fieldVariable', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ mimeType: 'application/pdf', titleImage: 'fileAsset' })
            );

            expect(model.src).toBe(
                `/contentAsset/image/${INODE}/fileAsset/pdf_page/1/resize_w/250/quality_q/45`
            );
        });
    });

    describe('svg', () => {
        it('resolves the raw vector through the fieldVariable (never the resized /dA path)', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ mimeType: 'image/svg+xml', hasTitleImage: true }),
                { fieldVariable: 'asset' }
            );

            expect(model.type).toBe('svg');
            expect(model.src).toBe(`/contentAsset/image/${INODE}/asset`);
        });

        it('uses the FileAsset binary field for FileAsset SVGs (legacy hardcoded "asset" 404d)', () => {
            const model = contentletToThumbnailModel(
                createContentlet({
                    mimeType: 'image/svg+xml',
                    baseType: 'FILEASSET',
                    contentType: 'FileAsset',
                    titleImage: 'fileAsset'
                }),
                { fieldVariable: 'fileAsset' }
            );

            expect(model.src).toBe(`/contentAsset/image/${INODE}/fileAsset`);
        });

        it('falls back to titleImage and then to "asset" when no fieldVariable is given', () => {
            const withTitleImage = contentletToThumbnailModel(
                createContentlet({ mimeType: 'image/svg+xml', titleImage: 'fileAsset' })
            );
            const bare = contentletToThumbnailModel(
                createContentlet({ mimeType: 'image/svg+xml' })
            );

            expect(withTitleImage.src).toBe(`/contentAsset/image/${INODE}/fileAsset`);
            expect(bare.src).toBe(`/contentAsset/image/${INODE}/asset`);
        });
    });

    describe('image', () => {
        it.each([[true], ['true']])('renders image when hasTitleImage is %p', (hasTitleImage) => {
            const model = contentletToThumbnailModel(
                createContentlet({ mimeType: 'image/jpeg', hasTitleImage })
            );

            expect(model.type).toBe('image');
        });

        it('builds the fieldVariable URL with modDateMilis cache buster', () => {
            const model = contentletToThumbnailModel(
                createContentlet({
                    mimeType: 'image/jpeg',
                    hasTitleImage: true,
                    modDateMilis: 1727377876407
                }),
                { fieldVariable: 'asset' }
            );

            expect(model.src).toBe(`/dA/${INODE}/asset/500w/50q?r=1727377876407`);
        });

        it('falls back to modDate as cache buster when modDateMilis is missing', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ mimeType: 'image/jpeg', hasTitleImage: true }),
                { fieldVariable: 'asset' }
            );

            expect(model.src).toBe(`/dA/${INODE}/asset/500w/50q?r=1727377876000`);
        });

        it('uses the image strategy URL when contentlet has an image property and no fieldVariable', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ image: '/some/image.png' })
            );

            expect(model.type).toBe('image');
            expect(model.src).toBe(`/dA/${INODE}/image/resize_w/250/quality_q/45`);
        });

        it('uses the default URL when hasTitleImage is true and no fieldVariable/image', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ hasTitleImage: true, modDateMilis: 99 })
            );

            expect(model.src).toBe(`/dA/${INODE}/500w/50q?r=99`);
        });

        it('renders image when metadata.isImage is true even without hasTitleImage', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ assetMetaData: { isImage: true } })
            );

            expect(model.type).toBe('image');
        });
    });

    describe('icon fallback', () => {
        it.each([[false], ['false'], [undefined]])(
            'returns icon when hasTitleImage is %p and nothing else matches',
            (hasTitleImage) => {
                const model = contentletToThumbnailModel(
                    createContentlet({ mimeType: 'application/zip', hasTitleImage })
                );

                expect(model.type).toBe('icon');
            }
        );

        it('populates icon and alt on every branch', () => {
            const image = contentletToThumbnailModel(
                createContentlet({ hasTitleImage: true, icon: 'jpgIcon' })
            );
            const video = contentletToThumbnailModel(
                createContentlet({ mimeType: 'video/mp4', icon: 'mp4Icon' })
            );

            expect(image.icon).toBe('image');
            expect(image.alt).toBe('A file');
            expect(video.icon).toBe('videocam');
            expect(video.alt).toBe('A file');
        });

        it('prefers metadata title over contentlet title for alt', () => {
            const model = contentletToThumbnailModel(
                createContentlet({ metaData: { title: 'photo.jpg', isImage: true } })
            );

            expect(model.alt).toBe('photo.jpg');
        });
    });
});

describe('tempFileToThumbnailModel', () => {
    it('returns pdf for a pdf temp file using thumbnailUrl', () => {
        const tempFile = createTempFile({
            metadata: {
                contentType: 'application/pdf',
                fileSize: 100,
                isImage: false,
                length: 100,
                modDate: 0,
                name: 'file.pdf',
                sha256: '',
                title: 'file.pdf',
                version: 1
            }
        });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model.type).toBe('pdf');
        expect(model.src).toBe(tempFile.thumbnailUrl);
        expect(model.icon).toBe('insert_drive_file');
    });

    it('returns image when metadata.isImage is true', () => {
        const tempFile = createTempFile({
            fileName: 'photo.jpg',
            metadata: {
                contentType: 'image/jpeg',
                fileSize: 100,
                isImage: true,
                length: 100,
                modDate: 0,
                name: 'photo.jpg',
                sha256: '',
                title: 'photo.jpg',
                version: 1
            }
        });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model).toEqual({
            type: 'image',
            src: tempFile.thumbnailUrl,
            icon: 'image',
            alt: 'photo.jpg'
        });
    });

    it('falls back to referenceUrl when thumbnailUrl is empty', () => {
        const tempFile = createTempFile({ thumbnailUrl: '' });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model.src).toBe(tempFile.referenceUrl);
    });

    it('returns svg with the raw referenceUrl (not the rasterized thumbnail)', () => {
        const tempFile = createTempFile({
            fileName: 'logo.svg',
            metadata: {
                contentType: 'image/svg+xml',
                fileSize: 100,
                isImage: true,
                length: 100,
                modDate: 0,
                name: 'logo.svg',
                sha256: '',
                title: 'logo.svg',
                version: 1
            }
        });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model.type).toBe('svg');
        expect(model.src).toBe(tempFile.referenceUrl);
    });

    it('returns a first-frame video preview for video content types', () => {
        const tempFile = createTempFile({
            fileName: 'clip.mp4',
            metadata: {
                contentType: 'video/mp4',
                fileSize: 100,
                isImage: false,
                length: 100,
                modDate: 0,
                name: 'clip.mp4',
                sha256: '',
                title: 'clip.mp4',
                version: 1
            }
        });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model.type).toBe('video');
        expect(model.playable).toBe(false);
        expect(model.src).toBe(`${tempFile.thumbnailUrl}#t=0.1`);
        expect(model.icon).toBe('videocam');
    });

    it('synthesizes fallback metadata when the temp file has none', () => {
        const tempFile = createTempFile({
            mimeType: 'image/png',
            image: true,
            metadata: undefined
        });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model.type).toBe('image');
        expect(model.alt).toBe('file.pdf');
    });

    it('detects image from the content type when isImage is absent (image-editor save)', () => {
        const tempFile = createTempFile({
            fileName: 'f1.png',
            mimeType: 'image/png',
            image: false,
            metadata: undefined
        });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model.type).toBe('image');
        expect(model.src).toBe(tempFile.thumbnailUrl);
    });

    it('returns icon when there is no thumbnailUrl nor referenceUrl', () => {
        const model = tempFileToThumbnailModel(
            createTempFile({ thumbnailUrl: '', referenceUrl: '' })
        );

        expect(model.type).toBe('icon');
        expect(model.src).toBe('');
    });

    it('returns icon for unknown content types with the extension glyph', () => {
        const tempFile = createTempFile({
            fileName: 'song.mp3',
            mimeType: 'application/octet-stream',
            metadata: {
                contentType: 'application/octet-stream',
                fileSize: 100,
                isImage: false,
                length: 100,
                modDate: 0,
                name: 'song.mp3',
                sha256: '',
                title: 'song.mp3',
                version: 1
            }
        });

        const model = tempFileToThumbnailModel(tempFile);

        expect(model.type).toBe('icon');
        expect(model.icon).toBe('audiotrack');
    });
});
