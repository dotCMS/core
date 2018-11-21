import { Injectable } from '@angular/core';
import { ContentType } from '../shared/content-type.model';

@Injectable()
export class DotEditContentTypeCacheService {
    private currentContentType: ContentType;

    setContentType(contentType: ContentType) {
        this.currentContentType = contentType;
    }

    getContentType(): ContentType {
        return this.currentContentType;
    }
}
