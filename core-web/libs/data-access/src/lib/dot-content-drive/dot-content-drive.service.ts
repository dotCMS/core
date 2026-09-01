import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotContentDriveSearchRequest, DotContentDriveSearchResponse } from '@dotcms/dotcms-models';

@Injectable({
    providedIn: 'root'
})
export class DotContentDriveService {
    readonly #http = inject(HttpClient);

    search(request: DotContentDriveSearchRequest): Observable<DotContentDriveSearchResponse> {
        return this.#http
            .post<{ entity: DotContentDriveSearchResponse }>('/api/v1/drive/search', request)
            .pipe(
                map((response: { entity: DotContentDriveSearchResponse }) =>
                    withFolderInodes(response.entity)
                )
            );
    }
}

/**
 * Gives folder rows the `inode` the drive-search view leaves out.
 *
 * The table keys rows on `inode` (`dataKey="inode"`, chosen because language variants of one
 * contentlet share an identifier), and the Action Center's contentlet-only actions read it as well.
 * A folder arriving without one collides with every other folder on `undefined`.
 *
 * Safe because dotCMS keeps the two equal for folders: `Folder.setIdentifier` backfills `inode` when
 * it is unset, and nothing since has separated them. Any folder that does carry one is left alone,
 * so legacy data keeps whatever it has rather than being rewritten from here.
 */
const withFolderInodes = (entity: DotContentDriveSearchResponse): DotContentDriveSearchResponse => {
    if (!entity?.list?.length) {
        return entity;
    }

    return {
        ...entity,
        list: entity.list.map((item) =>
            item?.type === 'folder' && !item.inode ? { ...item, inode: item.identifier } : item
        )
    };
};
