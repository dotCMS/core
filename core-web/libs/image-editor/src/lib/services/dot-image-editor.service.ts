import { Observable, forkJoin, of, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { ImageEditorAssetContext } from '../models/image-editor.models';

/** Shape of the save endpoint JSON response used to build a {@link DotCMSTempFile}. */
interface SaveEditedImageResponse {
    id: string;
    fileName: string;
    length: number;
    metadata?: DotCMSTempFile['metadata'];
}

/** Natural pixel dimensions of an image resolved from the browser. */
interface NaturalDimensions {
    naturalWidth: number;
    naturalHeight: number;
}

/** Metadata resolved for an asset before editing begins. */
interface AssetMeta {
    naturalWidth: number;
    naturalHeight: number;
    originalBytes: number | null;
}

/**
 * Data-access service for the image editor: resolves asset metadata, queries
 * file sizes, persists the saved/edited image and the focal point, and triggers
 * client-side downloads. All read-only/metadata calls are non-fatal and never
 * throw; only {@link saveEditedImage} rethrows so the store can keep the modal
 * open on failure.
 */
@Injectable({ providedIn: 'root' })
export class DotImageEditorService {
    readonly #http = inject(HttpClient);

    /**
     * Resolves the byte size of a remote asset via a HEAD request.
     * @param url - The asset URL to inspect
     * @returns The `Content-Length` as a number, or `0` when missing or on error
     */
    getFileSize(url: string): Observable<number> {
        return this.#http.head(url, { observe: 'response', responseType: 'text' }).pipe(
            map((res) => Number(res.headers.get('Content-Length')) || 0),
            catchError(() => of(0))
        );
    }

    /**
     * Loads a preview image as a complete, verified blob and returns a local
     * object URL for it.
     *
     * Binding a remote `/contentAsset/image/...` URL straight to an `<img>` paints
     * progressively, so a partially-generated response (the server renders filters
     * on the fly and the first request can race that generation) shows as a
     * truncated band — and the browser still fires `load` because the file header,
     * which carries the dimensions, arrived intact. Reading the full response body
     * here closes that gap: a stream truncated against `Content-Length` makes the
     * request error outright, an explicit length mismatch or an empty / error
     * (HTML/JSON) body is rejected, and only complete image bytes reach the caller —
     * which renders them from a local object URL that can never paint half an image.
     *
     * The request cancels automatically when the caller unsubscribes (e.g. a newer
     * edit supersedes it). Callers own revoking the returned object URL.
     * @param url - The fully-built filter/preview URL
     * @returns An object URL for the verified image blob; errors on an incomplete
     * or non-image response
     */
    loadPreviewImage(url: string): Observable<string> {
        return this.#http.get(url, { observe: 'response', responseType: 'blob' }).pipe(
            map((res) => {
                const blob = res.body;
                const declared = Number(res.headers.get('Content-Length'));
                const type = blob?.type ?? '';
                // The server can answer a still-generating render with a 200 that
                // carries an HTML/JSON error page instead of image bytes.
                const isErrorBody = type.startsWith('text/') || type.includes('json');

                if (
                    !blob ||
                    blob.size === 0 ||
                    isErrorBody ||
                    (declared > 0 && blob.size !== declared)
                ) {
                    throw new Error('Incomplete or invalid image response');
                }

                return URL.createObjectURL(blob);
            })
        );
    }

    /**
     * Persists the edited image to a temp file by hitting the filter URL with
     * the save tokens appended.
     * @param filterUrl - The fully-built filter/preview URL for the edited image
     * @param variable - The binary field id the saved file should target
     * @returns The resulting temp file
     * @throws Rethrows the original error (without surfacing it) so the caller —
     * the store — is the single place that shows the error and keeps the editor
     * open on failure
     */
    saveEditedImage(filterUrl: string, variable: string): Observable<DotCMSTempFile> {
        const separator = filterUrl.includes('?') ? '&' : '?';
        const url = `${filterUrl}${separator}binaryFieldId=${encodeURIComponent(variable)}&_imageToolSaveFile=true`;

        return this.#http.get<SaveEditedImageResponse>(url).pipe(
            map((res) => this.#toTempFile(res)),
            catchError((error: HttpErrorResponse) => throwError(() => error))
        );
    }

    /**
     * Persists the focal point for an asset so it is honored by future renders.
     * @param originalUrl - The base URL of the unfiltered original asset
     * @param fp - Normalized focal point coordinates
     * @returns Completes with `void`; non-fatal and never throws
     */
    persistFocalPoint(originalUrl: string, fp: { x: number; y: number }): Observable<void> {
        const url = `${originalUrl}/filter/FocalPoint/fp/${fp.x},${fp.y}/?overwrite=${Date.now()}`;

        return this.#http.get(url, { responseType: 'text' }).pipe(
            map(() => void 0),
            catchError(() => of(void 0))
        );
    }

    /**
     * Resolves the metadata needed to seed the editor: natural dimensions and
     * the original byte size. Always emits a safe default on error and never
     * throws.
     * @param ctx - Resolved asset context providing the original URL
     * @returns The natural dimensions and original byte size
     */
    loadAssetMeta(ctx: ImageEditorAssetContext): Observable<AssetMeta> {
        return forkJoin({
            dimensions: this.#resolveNaturalDimensions(ctx.originalUrl),
            originalBytes: this.getFileSize(ctx.originalUrl)
        }).pipe(
            map(({ dimensions, originalBytes }) => ({
                naturalWidth: dimensions.naturalWidth,
                naturalHeight: dimensions.naturalHeight,
                originalBytes
            })),
            catchError(() =>
                of<AssetMeta>({ naturalWidth: 0, naturalHeight: 0, originalBytes: null })
            )
        );
    }

    /**
     * Triggers a browser download of the given URL using a transient anchor.
     * @param url - The URL of the resource to download
     * @param fileName - The suggested file name for the download
     */
    triggerDownload(url: string, fileName: string): void {
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = fileName;
        anchor.rel = 'noopener';
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
    }

    #resolveNaturalDimensions(url: string): Observable<NaturalDimensions> {
        return new Observable<NaturalDimensions>((subscriber) => {
            const image = new Image();

            image.onload = () => {
                subscriber.next({
                    naturalWidth: image.naturalWidth,
                    naturalHeight: image.naturalHeight
                });
                subscriber.complete();
            };

            image.onerror = () => {
                subscriber.next({ naturalWidth: 0, naturalHeight: 0 });
                subscriber.complete();
            };

            image.src = url;
        });
    }

    #toTempFile(res: SaveEditedImageResponse): DotCMSTempFile {
        return {
            id: res.id,
            fileName: res.fileName,
            length: res.length,
            metadata: res.metadata,
            folder: '',
            image: true,
            mimeType: res.metadata?.contentType ?? '',
            referenceUrl: '',
            thumbnailUrl: ''
        };
    }
}
