import { Injectable } from '@angular/core';
import { ContentType } from '../shared/content-type.model';

@Injectable()
export class EditContentTypeCacheService {
    private currentContentType: ContentType;

    set contentType(contentType: ContentType) {
        this.currentContentType = contentType;
    }

    get contentType(): ContentType {
        return this.currentContentType;
    }
}
