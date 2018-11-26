import { Injectable } from '@angular/core';
import { ContentType } from '../../../../../shared/content-type.model';
import * as _ from 'lodash';

/**
 * Store the current Content Type to Add or Edit the Relationships
 */
@Injectable()
export class DotEditContentTypeCacheService {
    private currentContentType: ContentType;

    set(contentType: ContentType): void {
        this.currentContentType = contentType;
    }

    get(): ContentType {
        return _.cloneDeep(this.currentContentType);
    }
}
