import { Injectable } from '@angular/core';
import { ContentType } from '../shared/content-type.model';
import * as _ from 'lodash';
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
