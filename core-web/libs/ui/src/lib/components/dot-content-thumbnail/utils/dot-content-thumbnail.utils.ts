import { DotCMSContentlet, DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';
import { getFileMetadata } from '@dotcms/utils';

import {
    DotContentThumbnail,
    DotContentletThumbnailOptions
} from '../models/dot-content-thumbnail.model';

const DEFAULT_ICON = 'insert_drive_file';

const AUDIO_ICON = 'audiotrack';
const DOC_ICON = DEFAULT_ICON;
const IMAGE_ICON = 'image';
const VIDEO_ICON = 'videocam';
const FONT_ICON = 'font_download';

/**
 * Legacy extension → Material Symbols glyph map, ported verbatim from the
 * Stencil `dot-contentlet-icon` element so icon parity is preserved.
 */
const EXTENSION_ICON_MAP: Record<string, string> = {
    // Misc
    page: 'web',
    gear: 'settings',
    content: 'library_books',
    form: 'format_list_bulleted',
    persona: 'person',
    ukn: DOC_ICON,
    folder: 'folder',

    // Text
    doc: DOC_ICON,
    docx: DOC_ICON,
    odt: DOC_ICON,
    ott: DOC_ICON,
    odm: DOC_ICON,

    // Spreadsheet
    csv: DOC_ICON,
    numbers: DOC_ICON,
    wks: DOC_ICON,
    xls: DOC_ICON,
    xlsx: DOC_ICON,
    ods: DOC_ICON,
    ots: DOC_ICON,

    // Presentation
    keynote: DOC_ICON,
    ppt: DOC_ICON,
    pptx: DOC_ICON,
    odp: DOC_ICON,
    otp: DOC_ICON,

    // PDF
    pdf: DOC_ICON,

    // Video
    asf: VIDEO_ICON,
    avi: VIDEO_ICON,
    mov: VIDEO_ICON,
    mp4: VIDEO_ICON,
    mpg: VIDEO_ICON,
    ogg: VIDEO_ICON,
    ogv: VIDEO_ICON,
    rm: VIDEO_ICON,
    vob: VIDEO_ICON,

    // Image
    bmp: IMAGE_ICON,
    image: IMAGE_ICON,
    jpeg: IMAGE_ICON,
    jpg: IMAGE_ICON,
    pct: IMAGE_ICON,
    png: IMAGE_ICON,
    gif: IMAGE_ICON,
    webp: IMAGE_ICON,
    svg: IMAGE_ICON,
    ico: IMAGE_ICON,

    // Audio
    aac: AUDIO_ICON,
    aif: AUDIO_ICON,
    iff: AUDIO_ICON,
    m3u: AUDIO_ICON,
    mid: AUDIO_ICON,
    mp3: AUDIO_ICON,
    mpa: AUDIO_ICON,
    ra: AUDIO_ICON,
    wav: AUDIO_ICON,
    wma: AUDIO_ICON,

    // Code
    vtl: DOC_ICON,
    js: DOC_ICON,
    jsx: DOC_ICON,
    esm: DOC_ICON,
    ts: DOC_ICON,
    tsx: DOC_ICON,
    html: DOC_ICON,
    scss: DOC_ICON,
    sass: DOC_ICON,
    less: DOC_ICON,
    css: DOC_ICON,

    // Font
    otf: FONT_ICON,
    ttf: FONT_ICON,
    ttc: FONT_ICON,
    fnt: FONT_ICON,
    woff: FONT_ICON,
    woff2: FONT_ICON,
    eot: FONT_ICON
};

const SVG_MIME_TYPE = 'image/svg+xml';
const PDF_MIME_TYPE = 'application/pdf';

/**
 * Translates a dotCMS icon field value into a Material Symbols glyph name.
 *
 * Legacy values carry an `Icon` suffix (e.g. `pdfIcon`) where the prefix is a
 * file extension resolved through {@link EXTENSION_ICON_MAP}; any other
 * non-empty value is assumed to already be a Material icon name and passes
 * through unchanged (same heuristic as the Stencil `dot-contentlet-icon`).
 *
 * @param raw icon field value from a contentlet (`__icon__`, `contentTypeIcon` or `icon`)
 * @returns a Material Symbols glyph name, `insert_drive_file` when unresolvable
 */
export function translateIconName(raw: string | undefined | null): string {
    if (!raw) {
        return DEFAULT_ICON;
    }

    if (raw.includes('Icon') && raw !== 'Icon') {
        const extension = raw.replace('Icon', '');

        return EXTENSION_ICON_MAP[extension] || DEFAULT_ICON;
    }

    return raw;
}

/**
 * Resolves the icon field of a contentlet following the legacy Stencil
 * precedence and translates it to a Material Symbols glyph.
 *
 * @param contentlet the contentlet to resolve the icon for
 * @returns a Material Symbols glyph name
 */
export function resolveContentletIcon(contentlet: DotCMSContentlet): string {
    const raw =
        contentlet.baseType === 'FILEASSET'
            ? (contentlet['__icon__'] ?? contentlet['contentTypeIcon'] ?? contentlet['icon'])
            : (contentlet['contentTypeIcon'] ?? contentlet['__icon__'] ?? contentlet['icon']);

    return translateIconName(raw);
}

/**
 * Maps a saved contentlet (FileAsset, dotAsset or contentlet with binary
 * fields) to a render-ready {@link DotContentThumbnail}.
 *
 * URL strategies replicate the legacy Stencil `dot-contentlet-thumbnail`
 * (`getImageURL`), with one deliberate fix: SVG resolves to the raw
 * `/contentAsset/image/{inode}/asset` BEFORE the fieldVariable strategy, so
 * vector images are never rasterized/cropped.
 *
 * Not covered (legacy modes with no Angular consumer): `backgroundImage`
 * rendering and the ≥96px icon extension label.
 *
 * @param contentlet the contentlet to build the thumbnail for
 * @param options see {@link DotContentletThumbnailOptions}
 * @returns a resolved thumbnail model; `icon` is populated on every branch
 */
export function contentletToThumbnailModel(
    contentlet: DotCMSContentlet,
    options: DotContentletThumbnailOptions = {}
): DotContentThumbnail {
    const { fieldVariable, playableVideo = false, showVideoThumbnail = true } = options;

    const metadata: Partial<DotFileMetadata> = getFileMetadata(contentlet);
    const mimeType = metadata.contentType || contentlet.mimeType || '';
    const alt = metadata.title || contentlet.title || '';
    const icon = resolveContentletIcon(contentlet);
    const iconModel: DotContentThumbnail = { type: 'icon', src: '', icon, alt };

    const { inode } = contentlet;

    if (!inode) {
        return iconModel;
    }

    const variable = fieldVariable || contentlet.titleImage;
    const cacheBuster = contentlet['modDateMilis'] || contentlet.modDate;

    if (mimeType.includes('video')) {
        if (!showVideoThumbnail) {
            return iconModel;
        }

        return playableVideo
            ? { type: 'video', src: `/dA/${inode}/${variable}`, icon, alt, playable: true }
            : { type: 'video', src: `/dA/${inode}#t=0.1`, icon, alt, playable: false };
    }

    if (mimeType === PDF_MIME_TYPE) {
        return {
            type: 'pdf',
            src: `/contentAsset/image/${inode}/${variable}/pdf_page/1/resize_w/250/quality_q/45`,
            icon,
            alt
        };
    }

    if (mimeType === SVG_MIME_TYPE) {
        // Serve the raw vector through the binary field that actually holds it:
        // 'fileAsset' for FileAssets, 'asset' for dotAssets (the legacy Stencil
        // hardcoded 'asset' and 404'd -> icon for FileAsset SVGs)
        return {
            type: 'svg',
            src: `/contentAsset/image/${inode}/${variable || 'asset'}`,
            icon,
            alt
        };
    }

    // hasTitleImage is typed boolean but some endpoints return the string 'true'
    const hasTitleImage = contentlet.hasTitleImage as boolean | string;
    const renderImage =
        hasTitleImage === true ||
        hasTitleImage === 'true' ||
        !!contentlet['image'] ||
        metadata.isImage === true;

    if (renderImage) {
        if (fieldVariable) {
            return {
                type: 'image',
                src: `/dA/${inode}/${fieldVariable}/500w/50q?r=${cacheBuster}`,
                icon,
                alt
            };
        }

        if (contentlet['image']) {
            return {
                type: 'image',
                src: `/dA/${inode}/image/resize_w/250/quality_q/45`,
                icon,
                alt
            };
        }

        return { type: 'image', src: `/dA/${inode}/500w/50q?r=${cacheBuster}`, icon, alt };
    }

    return iconModel;
}

/**
 * Maps a just-uploaded temp file (`/api/v1/temp` response) to a render-ready
 * {@link DotContentThumbnail}. Temp files carry their own preview URLs
 * (`thumbnailUrl` / `referenceUrl`), so no `/dA` building is needed.
 *
 * @param tempFile the temp file to build the thumbnail for
 * @returns a resolved thumbnail model; `icon` is populated on every branch
 */
export function tempFileToThumbnailModel(tempFile: DotCMSTempFile): DotContentThumbnail {
    const metadata = tempFile.metadata ?? buildFallbackTempFileMetadata(tempFile);
    const src = tempFile.thumbnailUrl || tempFile.referenceUrl;
    const alt = metadata.name || tempFile.fileName || '';
    const extension = (metadata.name || '').split('.').pop()?.toLowerCase() ?? '';
    const icon = EXTENSION_ICON_MAP[extension] || DEFAULT_ICON;
    const iconModel: DotContentThumbnail = { type: 'icon', src: '', icon, alt };

    if (!src) {
        return iconModel;
    }

    const fileType = metadata.contentType?.split('/')[0];

    if (metadata.contentType === SVG_MIME_TYPE) {
        // Prefer the raw file over the rasterized thumbnail so SVGs render uncropped
        return { type: 'svg', src: tempFile.referenceUrl || src, icon, alt };
    }

    // isImage may be absent on synthesized metadata (e.g. image-editor saves),
    // so an image/* content type also qualifies
    if (metadata.isImage || fileType === 'image') {
        return { type: 'image', src, icon, alt };
    }

    if (extension === 'pdf') {
        return { type: 'pdf', src, icon, alt };
    }

    if (fileType === 'video') {
        return { type: 'video', src, icon, alt, playable: true };
    }

    return iconModel;
}

/**
 * Synthesizes a {@link DotFileMetadata} from the flat fields of a temp file.
 * Temp files freshly returned by `/api/v1/temp` (e.g. after an image-editor
 * save) may lack the `metadata` block — use this so downstream consumers can
 * rely on a non-null metadata shape.
 *
 * @param tempFile the temp file missing its metadata block
 * @returns metadata derived from mimeType/fileName/image/length
 */
export function buildFallbackTempFileMetadata(tempFile: DotCMSTempFile): DotFileMetadata {
    return {
        contentType: tempFile?.mimeType || '',
        fileSize: tempFile?.length || 0,
        isImage: tempFile?.image || false,
        length: tempFile?.length || 0,
        modDate: 0,
        name: tempFile?.fileName || '',
        sha256: '',
        title: tempFile?.fileName || '',
        version: 0
    };
}
