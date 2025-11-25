import { Injectable } from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';

/**
 *Store the current Content Type to Add or Edit the Relationships
 *
 * @export
 * @class DotEditContentTypeCacheService
 */
@Injectable()
export class DotEditContentTypeCacheService {
    private currentContentType: DotCMSContentType;

    /**
     *Strore the current {@see ContentTye} in cache
     *
     * @param {DotCMSContentType} contentType
     * @memberof DotEditContentTypeCacheService
     */
    set(contentType: DotCMSContentType): void {
        this.currentContentType = contentType;
    }

    /**
     *Return the current {@see ContentType} from cache
     *
     * @returns {DotCMSContentType}
     * @memberof DotEditContentTypeCacheService
     */
    get(): DotCMSContentType {
        return structuredClone(this.currentContentType);
    }
}
