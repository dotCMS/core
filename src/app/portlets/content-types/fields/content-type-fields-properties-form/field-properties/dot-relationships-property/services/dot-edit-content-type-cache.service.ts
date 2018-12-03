import { Injectable } from '@angular/core';
import { ContentType } from '../../../../../shared/content-type.model';
import * as _ from 'lodash';

/**
 *Store the current Content Type to Add or Edit the Relationships
 *
 * @export
 * @class DotEditContentTypeCacheService
 */
@Injectable()
export class DotEditContentTypeCacheService {
    private currentContentType: ContentType;


    /**
     *Strore the current {@see ContentTye} in cache
     *
     * @param {ContentType} contentType
     * @memberof DotEditContentTypeCacheService
     */
    set(contentType: ContentType): void {
        this.currentContentType = contentType;
    }

    /**
     *Return the current {@see ContentType} from cache
     *
     * @returns {ContentType}
     * @memberof DotEditContentTypeCacheService
     */
    get(): ContentType {
        return _.cloneDeep(this.currentContentType);
    }
}
