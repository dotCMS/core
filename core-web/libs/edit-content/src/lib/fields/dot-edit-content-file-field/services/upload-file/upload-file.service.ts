import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { getFileMetadata, getFileVersion } from '../../utils';

@Injectable()
export class DotFileFieldUploadService {
    readonly #fileService = inject(DotUploadFileService);
    readonly #contentService = inject(DotEditContentService);
    readonly #httpClient = inject(HttpClient);

    /**
     * Uploads a file and returns a contentlet with the file metadata and id.
     * @param file the file to be uploaded
     * @returns a contentlet with the file metadata and id
     */
    uploadDotAsset(file: File) {
        return this.#fileService
            .uploadDotAsset(file)
            .pipe(switchMap((contentlet) => this.#addContent(contentlet)));
    }

    /**
     * Returns a contentlet by its identifier and adds the content if it's a editable as text file.
     * @param identifier the identifier of the contentlet
     * @returns a contentlet with the content if it's a editable as text file
     */
    getContentById(identifier: string) {
        return this.#contentService
            .getContentById(identifier)
            .pipe(switchMap((contentlet) => this.#addContent(contentlet)));
    }

    /**
     * Adds the content of a contentlet if it's a editable as text file.
     * @param contentlet the contentlet to be processed
     * @returns a contentlet with the content if it's a editable as text file, otherwise the original contentlet
     */
    #addContent(contentlet: DotCMSContentlet) {
        const { editableAsText } = getFileMetadata(contentlet);
        const contentURL = getFileVersion(contentlet);

        if (editableAsText && contentURL) {
            return this.#getContentFile(contentURL).pipe(
                map((content) => ({ ...contentlet, content }))
            );
        }

        return of(contentlet);
    }

    /**
     * Downloads the content of a file by its URL.
     * @param contentURL the URL of the file content
     * @returns an observable of the file content
     */
    #getContentFile(contentURL: string) {
        return this.#httpClient.get(contentURL, { responseType: 'text' });
    }
}
