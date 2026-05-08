import { firstValueFrom } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { DotUploadFileService } from '@dotcms/data-access';

import { type DotImageData } from '../extensions/nodes/image.extension';
import { type DotVideoData } from '../extensions/nodes/video.extension';

/** Resolves API asset paths to a browser-usable URL on the current origin. */
function sameOriginAssetUrl(asset: string): string {
    if (asset.startsWith('http://') || asset.startsWith('https://')) {
        return asset;
    }
    return asset.startsWith('/') ? asset : `/${asset}`;
}

export interface UploadedImage {
    src: string;
    data: DotImageData;
}

export interface UploadedVideo {
    src: string;
    data: DotVideoData;
}

/**
 * Editor-facing adapter around `@dotcms/data-access`'s {@link DotUploadFileService}.
 *
 * Why this wrapper exists: the drop-handler in {@link handleMediaDrop} consumes Promises
 * (`async/await`), while {@link DotUploadFileService.publishContent} returns an Observable
 * stream of `DotCMSContentlet[]`. This service:
 *  1. Bridges Promise/Observable so the drop flow stays linear and readable.
 *  2. Unwraps the publish endpoint's `Record<contentTypeKey, contentlet>` shape — the
 *     workflow PUBLISH endpoint nests the contentlet under a content-type key, mirroring
 *     `dotAssets[0][Object.keys(dotAssets[0])[0]]` in the legacy block-editor.
 *  3. Narrows the generic {@link DotCMSContentlet} response into the editor's
 *     {@link DotImageData} / {@link DotVideoData} shapes.
 *
 * The two-step temp upload + workflow publish chain itself lives in `DotUploadFileService`,
 * not here.
 */
@Injectable({ providedIn: 'root' })
export class DotUploadService {
    private readonly uploadFileService = inject(DotUploadFileService);

    async uploadImage(file: File): Promise<UploadedImage> {
        const contentlet = await this.publishAsset(file);
        return {
            src: sameOriginAssetUrl(contentlet.asset),
            data: toAssetData(contentlet) satisfies DotImageData
        };
    }

    async uploadVideo(file: File): Promise<UploadedVideo> {
        const contentlet = await this.publishAsset(file);
        return {
            src: sameOriginAssetUrl(contentlet.asset),
            data: toAssetData(contentlet) satisfies DotVideoData
        };
    }

    private async publishAsset(file: File): Promise<PublishedAsset> {
        const dotAssets = await firstValueFrom(
            this.uploadFileService.publishContent({ data: file })
        );
        const wrapped = dotAssets?.[0] as Record<string, PublishedAsset> | undefined;
        if (!wrapped) {
            throw new Error('Publish: missing results');
        }
        // Workflow PUBLISH wraps the contentlet under the content-type variable key.
        const contentlet = Object.values(wrapped)[0];
        if (!contentlet?.asset) {
            throw new Error('Publish: missing asset path');
        }
        return contentlet;
    }
}

/**
 * Subset of {@link DotCMSContentlet} the editor needs after a publish — `asset` is required
 * (it's the storage path for the uploaded file). Other fields default safely for narrowing.
 */
interface PublishedAsset {
    asset: string;
    identifier: string;
    inode: string;
    languageId: number;
    title: string;
}

function toAssetData(contentlet: PublishedAsset): DotImageData {
    return {
        identifier: contentlet.identifier,
        inode: contentlet.inode,
        languageId: contentlet.languageId,
        title: contentlet.title ?? '',
        asset: contentlet.asset
    };
}
