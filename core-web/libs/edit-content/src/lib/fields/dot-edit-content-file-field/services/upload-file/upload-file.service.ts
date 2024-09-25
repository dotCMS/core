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

    uploadDotAsset(file: File) {
        return this.#fileService
            .uploadDotAsset(file)
            .pipe(switchMap((contentlet) => this.#addContent(contentlet)));
    }

    getContentById(identifier: string) {
        return this.#contentService
            .getContentById(identifier)
            .pipe(switchMap((contentlet) => this.#addContent(contentlet)));
    }

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

    #getContentFile(contentURL: string) {
        return this.#httpClient.get(contentURL, { responseType: 'text' });
    }
}
